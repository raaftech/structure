package tech.raaf.structure.helper.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.raaf.structure.base.Configuration;
import tech.raaf.structure.exceptions.EntityDeserializationException;
import tech.raaf.structure.graph.Entity;
import tech.raaf.structure.graph.Graph;
import tech.raaf.structure.helper.LambdaHelper;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * BundleLoader sorts all entities found in the bundle paths and loads them.
 */
public class BundleLoader {

    final private List<JsonNode> sortedNodes;
    final private List<Entity> typeEntities;
    final private List<Entity> nonTypeEntities;

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final Logger log = LoggerFactory.getLogger(BundleLoader.class);

    /**
     * Create a new BundleLoader using the configured bundle path.
     * @throws Exception Exceptions may occur when the list of entities is unsortable
     * because of cyclic dependencies.
     */
    public BundleLoader() throws Exception {
        this(Configuration.getBundlePaths());
    }

    /**
     * Create a new BundleLoader.
     * @throws Exception Exceptions may occur when the list of entities is unsortable
     * because of cyclic dependencies.
     */
    public BundleLoader(Set<Path> bundlePaths) throws Exception {
        NaiveSorter naiveSorter = new NaiveSorter(bundlePaths);
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
                .filter(json -> !isInstance(json))
                .collect(Collectors.toList());
    }

    /**
     * Iterate over a list of entities, retrieving all non-type entities.
     * @return A list of non-type entities.
     */
    private List<JsonNode> getSortedInstances() {
        return sortedNodes.stream()
                .filter(BundleLoader::isInstance)
                .collect(Collectors.toList());
    }

    /**
     * Given an entity JsonNode returns whether it's an instance or not.
     * @param entityJson The entity JSON.
     * @return True in case of an instance, false otherwise.
     */
    public static boolean isInstance(JsonNode entityJson) {
        return entityJson.path("meta").path("relations").has("INSTANCE_OF");
    }

    private void persistTypeConstraints(Entity typeEntity) {
        // We might not have constraints for this type
        if (!typeEntity.getConstraints().isPresent()) {
            return;
        }

        // The statement also works when the constraint points to the type itself
        final String constraint = "MATCH (a:Type {uuid: $from}), (b:Type {uuid: $to}) " +
                "MERGE (a)-[c:CONSTRAINTS {forRelationship: $rel}]->(b) " +
                "SET c.cardinality = $card " +
                "RETURN count(c) AS count";
        typeEntity.getConstraints().get().forEach((relName, constraints) -> constraints.forEach(pair -> {
            final String target = pair.getRight().toString();
            log.debug("Adding constraint for " + typeEntity.getUuid() + " to " + target + ": " + relName + ", " + pair.getLeft());
            final List<Map<String, Object>> rows = Graph.query(constraint, ImmutableMap.of(
                    "from", typeEntity.getUuid(), "to", target, "rel", relName, "card", pair.getLeft().toString()));
            Validate.isTrue(((Number) rows.get(0).get("count")).longValue() > 0,
                    "Can't find type " + typeEntity.getUuid() + " or " + target + " for constraint " + relName);
        }));
    }

    /**
     * Create the identity indexes for a type entity. Identity field will only ever be set on entities
     * of type "Type", so no check is needed.
     * @param entity A type entity.
     */
    private void createEntityIndex(Entity entity) {
        final String createIndexFormat = "CREATE INDEX IF NOT EXISTS FOR (n:%s) ON (n.%s)";

        final Set<String> formatedIndices = Arrays.asList(entity.getIdentity()).stream()
                .map(prop -> String.format(createIndexFormat,
                        Graph.escape("type:" + entity.getName()), Graph.escape("data-" + prop)))
                .collect(Collectors.toSet());

        if (Arrays.asList(entity.getIdentity()).stream().anyMatch(index -> index.contains("`"))) {
            throw new EntityDeserializationException("One of the identity properties " +
                    "contain `, which is not allowed. Identity: " + String.join(",", entity.getIdentity()));
        }

        // Each statement runs in its own transaction
        formatedIndices.stream()
                .peek(index -> log.debug("Creating index {} ", index))
                .forEach(Graph::query);
    }

    /**
     * Load all types and persist them to the graph.
     */
    public void loadTypes() {
        // Persist the types first
        typeEntities.forEach(Entity::toGraph);

        /*
        We'll now do a second pass on the types, but we'll get them from the graph
        so they're actual instances of Type on which we can create constraints.
         */
        typeEntities.forEach(this::persistTypeConstraints);
    }

    /**
     * Load all the instance entities and persist them.
     * You'll want to call this after the types have been persisted to the graph,
     * because schema references are resolved via the schemas web endpoint, which
     * queries the types from the graph.
     */
    public void loadInstances() {
        nonTypeEntities.forEach(Entity::toGraph);
    }

    // Schema changes and data writes need separate transactions
    public void load() {
        Graph.transaction(this::loadTypes);
        createIndices();
        Graph.transaction(this::loadInstances);
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
