package com.raaftech.structure.helper.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.raaftech.structure.base.Configuration;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.exceptions.EntityDeserializationException;
import com.raaftech.structure.graph.Constraint;
import com.raaftech.structure.graph.Type;
import com.raaftech.structure.graph.repositories.TypeRepository;
import com.raaftech.structure.helper.LambdaHelper;
import com.raaftech.structure.helper.Triple;
import com.raaftech.structure.next.Entity;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.query.CypherQueryExecutor;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * BundleLoader sorts all entities found in the bundle paths and loads them.
 */
public class BundleLoader {

    final private List<JsonNode> sortedNodes;
    final private List<Entity> typeEntities;
    final private List<Entity> nonTypeEntities;

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final TypeRepository typeRepository = Context.getTypeRepository();
    private static final Neo4jTemplate neo4jTemplate = Context.getNeo4jTemplate();

    private static final Logger log = LoggerFactory.getLogger(BundleLoader.class);

    /**
     * Create a new BundleLoader using the configured bundle path.
     * @throws Exception Exceptions may occur when the list of entities is unsortable
     * because of cyclic dependencies.
     */
    public BundleLoader() throws Exception {
        this(Configuration.getBundlePath());
    }

    /**
     * Create a new BundleLoader.
     * @throws Exception Exceptions may occur when the list of entities is unsortable
     * because of cyclic dependencies.
     */
    public BundleLoader(String bundlePath) throws Exception {
        NaiveSorter naiveSorter = new NaiveSorter(bundlePath);
        sortedNodes = naiveSorter.getUpdateList(naiveSorter.sortIt(naiveSorter.readAllJson()));

        // Find all the type entities
        typeEntities = getSortedTypes().stream()
                .map(json -> LambdaHelper.wrapWithExceptionPropagation(() -> mapper.treeToValue(json, Entity.class)).get())
                .collect(Collectors.toList());
        // Find all the instance entities
        nonTypeEntities = getSortedInstances().stream()
                .map(json -> LambdaHelper.wrapWithExceptionPropagation(() -> mapper.treeToValue(json, Entity.class)).get())
                .collect(Collectors.toList());
    }

    /**
     * Iterate over a list of entities, retrieving the type entities.
     * @return A list of type entities.
     */
    private List<JsonNode> getSortedTypes() {
        return sortedNodes.stream()
                .filter(json -> Objects.equals("Type", json.get("type").asText()))
                .collect(Collectors.toList());
    }

    /**
     * Iterate over a list of entities, retrieving all non-type entities.
     * @return A list of non-type entities.
     */
    private List<JsonNode> getSortedInstances() {
        return sortedNodes.stream()
                .filter(json -> !Objects.equals("Type", json.get("type").asText()))
                .collect(Collectors.toList());
    }

    /**
     * THIS IS A WORKAROUND.
     * SDN has a bug that doesn't allow a type to have a relationship to itself. However,
     * when we manually create those in the graph they are mapped properly onto the resulting objects.
     * So we use a hack for reflexive constraints: manually creating constraints where the
     * beginning and end is the same.
     */
    private void createReflexiveConstraint(Type type, Constraint.Cardinality cardinality, String relName) {
        final String createConstraintQuery = "MATCH (n {uuid:{uuid}}) " +
                "CREATE (n)-[:CONSTRAINTS {__type__:\"Constraint\", forRelationship: {relName}, cardinality: {cardinality}}]->(n)";

        Map<String, Object> params = ImmutableMap.of(
                "uuid", type.getUuid(), "cardinality", cardinality.toString(), "relName", relName);

        CypherQueryExecutor queryExecutor = neo4jTemplate.getInfrastructure().getCypherQueryExecutor();
        queryExecutor.query(createConstraintQuery, null, params);
    }

    private void persistTypeConstraints(Entity typeEntity) {
        final Type type = typeRepository.findByUuid(typeEntity.getUuid());
        Validate.notNull(type, "Can't find type with UUID " + typeEntity.getUuid());

        // We might not have constraints for this type
        if (!typeEntity.getConstraints().isPresent()) {
            return;
        }

        Iterator<Map.Entry<String, List<Pair<Constraint.Cardinality, UUID>>>> constraintIter =
                typeEntity.getConstraints().get().entrySet().iterator();

        /*
        Because SDN doesn't allow us to persist reflexive constraints we need to do two passes.
        The first one is for the non-reflexive constraints, but we'll collect the reflexive constraints.
         */
        final Set<Triple<Type, Constraint.Cardinality, String>> reflexiveConstraints = new HashSet<>();

        while(constraintIter.hasNext()) {
            Map.Entry<String, List<Pair<Constraint.Cardinality, UUID>>> entry =  constraintIter.next();
            final String relName = entry.getKey();

            entry.getValue().stream()
                    .filter(constraintData -> Objects.equals(type.getUuid(), constraintData.getRight().toString()))
                    .forEach(constraintData -> reflexiveConstraints.add(Triple.of(type, constraintData.getLeft(), relName)));

            entry.getValue().stream()
                    .filter(constraintData -> !Objects.equals(type.getUuid(), constraintData.getRight().toString()))
                    .forEach(constraintData -> type.addConstraint(typeRepository.findByUuid(constraintData.getRight().toString()), constraintData.getLeft(), relName));
        }
        /*
        Persist the type. If we do this after we added the reflexive relationships, only
        the non-reflexive will be persisted.
         */
        type.save();

        // Iterate over the reflexive constraints and create them
        reflexiveConstraints.forEach(constraint ->
                createReflexiveConstraint(constraint.getLeft(), constraint.getMiddle(), constraint.getRight()));
    }

    /**
     * Create the identity indexes for a type entity. Identity field will only ever be set on entities
     * of type "Type", so no check is needed.
     * @param entity A type entity.
     */
    private void createEntityIndex(Entity entity) {
        final String indexFormat = ":`type:" + entity.getName() + "`(`data-%s`)";
        final String createIndexFormat = "CREATE INDEX ON %s";

        CypherQueryExecutor queryExecutor = Context.getNeo4jTemplate().getInfrastructure().getCypherQueryExecutor();

        final Set<String> formatedIndices = Arrays.asList(entity.getIdentity()).stream()
                .map(prop -> String.format(indexFormat, prop))
                .collect(Collectors.toSet());

        if (Arrays.asList(entity.getIdentity()).stream().anyMatch(index -> index.contains("`"))) {
            throw new EntityDeserializationException("One of the identity properties " +
                    "contain `, which is not allowed. Identity: " + String.join(",", entity.getIdentity()));
        }

        formatedIndices.stream()
                .peek(index -> log.debug("Creating index {} ", index))
                .map(index -> String.format(createIndexFormat, index))
                .forEach(query -> queryExecutor.query(query, null, null));
    }

    /**
     * Load all the bundles and persist them to the graph.
     */
    public void load() {
        // Persist the types first
        typeEntities.forEach(Entity::toGraph);

        /*
        We'll now do a second pass on the types, but we'll get them from the graph
        so they're actual instances of Type on which we can create constraints.
         */
        typeEntities.forEach(this::persistTypeConstraints);

        // And we can save the rest of the entities
        nonTypeEntities.forEach(Entity::toGraph);
    }

    /**
     * Because you cannot do schema changes and updates in one transaction.
     */
    public void createIndices() {
        // Create all the identity indices
        typeEntities.stream()
                .filter(entity -> entity.getIdentity() != null)
                .forEach(this::createEntityIndex);
    }
}
