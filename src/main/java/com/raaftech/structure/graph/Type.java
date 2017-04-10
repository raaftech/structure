package com.raaftech.structure.graph;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.raaftech.structure.base.Configuration;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.repositories.TypeRepository;
import com.raaftech.structure.helper.JsonSchemaBuilder;
import com.raaftech.structure.helper.Triple;
import com.raaftech.structure.next.Entity;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.annotation.RelatedToVia;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.fieldaccess.DynamicProperties;
import org.springframework.data.neo4j.fieldaccess.DynamicPropertiesContainer;

import java.io.IOException;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.vertx.core.json.Json.mapper;

/**
 * Give a description of Type here.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-11-27
 *
 * @Deprecated We need to redesign the domain classes to use Entity.
 */
@Deprecated
@NodeEntity
public class Type extends Node {

    final static Logger log = LoggerFactory.getLogger(Type.class);

    @RelatedTo(type="ISA", direction = Direction.OUTGOING, elementClass = Type.class, enforceTargetType = true)
    private Type isa;

    @RelatedTo(type="ISA", direction = Direction.INCOMING, elementClass = Type.class, enforceTargetType = true)
    @SuppressWarnings("unused")
    private Set<Type> childIsaSet;

    private int glyphId;

    @RelatedTo(type="HAS_INSTRUMENT", direction = Direction.OUTGOING)
    @Fetch
    private Set<Instrument> instruments;

    @RelatedTo(type="IMPLEMENTS", direction = Direction.OUTGOING)
    @Fetch
    private Set<Type> implementsProtocols;

    @RelatedToVia(type="CONSTRAINTS", direction = Direction.OUTGOING)
    private Set<Constraint> outgoingConstraints;
    @RelatedToVia(type="CONSTRAINTS", direction = Direction.INCOMING)
    private Set<Constraint> incomingConstraints;

    public DynamicProperties kv = new DynamicPropertiesContainer();

    // isaUuid exists only to temporarily save a UUID of a Type's parent
    // in case the parent doesn't exist in the database yet.
    @Transient
    private String isaUuid;

    @Transient
    private TypeRepository typeRepository = Context.getTypeRepository();

    /****************/
    /* Constructors */
    /****************/

    public Type() {}

    public Type(String name, String desc, int glyphId) {
        super(name, desc);
        this.glyphId = glyphId;
        initSchema();
    }

    public Type(String name, String desc, int glyphId, String checksum) {
        super(name, desc);
        this.glyphId = glyphId;
        this.checksum = checksum;
        initSchema();
    }

    public Type(String name, String desc, String uuid) {
        super(name, desc, uuid);
        initSchema();
    }

    public Type(String name, String desc, String uuid, int glyphId) {
        super(name, desc, uuid);
        this.glyphId = glyphId;
        initSchema();
    }

    public Type(String name, String desc, String uuid, int glyphId, String checksum) {
        super(name, desc, uuid, checksum);
        this.glyphId = glyphId;
        this.checksum = checksum;
        initSchema();
    }


    /*******************/
    /* Private Methods */
    /*******************/

    private void initSchema() {
        // TODO: Entity, this can go, i think
    }

    /******************/
    /* Public Methods */
    /******************/

    @Override
    public int hashCode() {
        if (getId() != null) {
            return getId().hashCode();
        } else {
            return super.hashCode();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return hashCode() == o.hashCode();
    }


    public Set<Type> getChildIsaSet() {
        GraphDatabase graphDatabase = Context.getGraphDatabase();
        Transaction tx = graphDatabase.beginTx();
        Context.getNeo4jTemplate().fetch(childIsaSet);
        tx.success();
        tx.close();
        return childIsaSet;
    }

    /**
     * Add a constraint to this type. See {@link Constraint}.
     * @param to Add a constraint to the given type.
     * @param cardinality The cardinality of the constraint.
     * @param relationshipName What relationship between this and <code>to</code> should be constrained?
     * @return The type this method was called on.
     */
    public Type addConstraint(Type to, Constraint.Cardinality cardinality, String relationshipName) {
        Validate.notNull(to, "Cannot create constraint to null type.");
        Validate.notEmpty(relationshipName, "Cannot create constraint for empty relationship name.");

        if (outgoingConstraints == null) {
            outgoingConstraints = new LinkedHashSet<>();
        }

        outgoingConstraints.add(new Constraint(this, to, cardinality, relationshipName));

        return this;
    }

    /**
     * Return the constraints an instance of this type should validate against.
     * We might inherit some constraints which can be specialized deeper in the type tree
     * by some other constraint. We want to keep the most specific constraints only.
     * @param inheritedConstraints All the inherited constraints.
     * @return A set containing constraints of which no generalization exists in the set.
     */
    private Set<Constraint> getApplicableConstraints(Set<Constraint> inheritedConstraints) {
        // This predicate is used to remove constraints that are specialized by other constraints
        BiPredicate<Set<Constraint>, Constraint> isNotSpecialized = (setToFilter, inheritedConstraint) -> setToFilter.stream()
                .allMatch(ownConstraint -> !inheritedConstraint.isSpecialization(ownConstraint));

        // First filter the inherited constraints so we only keep "specialized" constraints
        final Set<Constraint> filteredInheritedConstraints = inheritedConstraints.stream()
                .filter(inheritedConstraint -> isNotSpecialized.test(inheritedConstraints, inheritedConstraint))
                .collect(Collectors.toSet());

        // Return the constraints we should actually validate to
        return Stream.concat(
                outgoingConstraints.stream(),
                filteredInheritedConstraints.stream()
                        .filter(inheritedConstraint -> isNotSpecialized.test(outgoingConstraints, inheritedConstraint)))
                .collect(Collectors.toSet());
    }

    /**
     * Given an entity, verify it satisfies every (outward) constraint applied to this
     * type.
     * @param entity The entity to verify.
     * @return A tuple containing whether verification succeeded, and a set of that may
     * contain errors encountered during validation.
     */
    public Pair<Boolean, Set<String>> validateEntityToConstraints(Entity entity) {
        if ("Type".equals(entity.getType())) {
            throw new IllegalArgumentException("Constraints can only be validated for non-type entities.");
        }

        Set<String> errors = new HashSet<>();
        boolean validated = true;

        /*
        We need to whitelist a few relationships. Constraints only work on relationships between instances or
        its subtypes. This whitelist contains all relationships that don't belong to this category and should
        be ignored during constraint checks.
         */
        final Set<String> whitelist = ImmutableSet.of(
                "INSTANCE_OF",
                "HAS_INSTRUMENT",
                "HANDLES",
                "GIVES_CAPABILITY"
        );

        /*
        The merged set of inherited constraints and outgoingConstraints might contain
        generalizations of other constraints in the set, we don't want those so we remove them.
         */
        Set<Constraint> constraintsToValidateTo = getApplicableConstraints(typeRepository.getInheritedConstraint(getUuid()));

        /*
        There are a few relationships that aren't inter-instance, but between instances and types.
        These don't fall under any constraint, so we need to filter them using the whitelist.
         */
        final Map<String, Set<String>> relationsToTest = entity.getRelations().stream()
                .filter(t -> !whitelist.contains(t.getLeft()))
                .filter(t -> t.getMiddle().toLowerCase().equals(Direction.OUTGOING.name().toLowerCase()))
                .collect(Collectors.toMap(Triple::getLeft, Triple::getRight));

        // Make sure there's a constraint for every relationship, fail if there isn't
        final Predicate<String> constraintExists = relName -> constraintsToValidateTo.stream()
                .map(Constraint::getForRelationship)
                .anyMatch(constraintRel -> Objects.equals(constraintRel, relName));

        Set<String> relationsNotUnderConstraint = relationsToTest.keySet().stream()
                .filter(relName -> !constraintExists.test(relName))
                .collect(Collectors.toSet());
        if (!relationsNotUnderConstraint.isEmpty()) {
            validated = false;
            errors.add("Found relationships that aren't under constraint while " +
                    "instantiating " + getName() + ": " + relationsNotUnderConstraint.toString());
        }

        // Now we can check every constraint
        final Set<Constraint.ValidationReport> validationOutput =
                constraintsToValidateTo.stream()
                .map(constraint -> constraint.validateConstraint(entity))
                .collect(Collectors.toSet());

        // Get all the constraints that were violated
        Set<Constraint.ValidationReport> violatedConstraints = validationOutput.stream()
                .filter(report -> !report.isSuccess())
                .collect(Collectors.toSet());

        // Get the errors from the violated constraints
        errors = Stream.concat(errors.stream(),
                violatedConstraints.stream()
                .map(Constraint.ValidationReport::getMessage)
                .filter(Optional::isPresent)
                .map(Optional::get)).collect(Collectors.toSet());


        /*
        Create a map of every relation name verified by the constraints to the UUIDs
        that were verified.
        Sometimes there may be multiple constraints for the same relation name so we
        need to take care not to overwrite the values belonging to such a relation name.
         */
        final Map<String, Set<String>> validatedRelationships = validationOutput.stream()
                .collect(Collectors.toMap(Constraint.ValidationReport::getForRelationship, report -> report.getValidatedUuids(),
                        (left, right) -> Stream.of(left, right).flatMap(Collection::stream).collect(Collectors.toSet())));

        // Get all the relations that weren't validated by any constraint
        Set<UUID> missedRelations = Maps.difference(relationsToTest, validatedRelationships).entriesDiffering().keySet().stream()
                .map(key -> Sets.difference(relationsToTest.get(key), validatedRelationships.get(key)))
                .map(Sets.SetView::immutableCopy)
                .flatMap(Set::stream)
                .map(UUID::fromString)
                .collect(Collectors.toSet());

        if (!missedRelations.isEmpty()) {
            validated = false;
            errors.add("Found relationships that weren't validated by any constraint: " + missedRelations.toString());
        }

        /*
        All constraints validate when all validations return true, and every relationship the
        given entity has been validated by at least one constraint.
         */
        return Pair.of(validated && violatedConstraints.isEmpty() && missedRelations.isEmpty(), ImmutableSet.copyOf(errors));
    }

    public Type setDescriptor(String key, String valueType, boolean required, String description, String defaultValue,
                              String pattern) {
        Type parentNode = getIsa();
        if (parentNode != null && parentNode.getSchema().containsProperty(key)) {
            throw new RuntimeException("Can't override parent schema.");
        } else {
            getSchema().addProperty(key, valueType, required, description)
                    .setDefault(defaultValue)
                    .setPattern(pattern);
        }

        return this;
    }

    public Set<Type> findCommonProtocols(Instance subject) {
        return typeRepository.getCommonProtocols(this.getUuid(), subject.getUuid());
    }

    public int getGlyphId() {
        return glyphId;
    }

    public String getHierarchyPath() {
        String path = typeRepository.getHierarchyPath(getUuid());
        if (path != null) {
            return path.toLowerCase();
        } else {
            return null;
        }
    }

    public Type getIsa() {
        // Sometimes the parent isn't properly loaded from the database
        // I don't know why exactly, and don't have time to find out. This is a workaround.
        // TODO: find out what's going on here
        if (isa == null || isa.getName() == null) {
            GraphDatabase graphDatabase = Context.getGraphDatabase();
            Transaction tx = graphDatabase.beginTx();
            Context.getNeo4jTemplate().fetch(isa);
            tx.success();
            tx.close();
        }
        return isa;
    }

    public Type setIsa(Type isa) {
        // TODO: Entity, This can go, i think
        return this;
    }

    public Type setIsa(String isa) {
        // We can't assume our parentNode has been persisted to the database yet at this point.
        // If the parentNode doesn't exist in the database yet, we just save it's UUID to a
        // transient field for later use (at createNode time).
        // If it does exist, we create the relations and inherit constraints in setIsa(Type isa).

        Type parentNode = typeRepository.findByUuid(isa);
        if (parentNode != null) {
            setIsa(parentNode);
        } else {
            // This means our parent node hasn't been persisted to DB yet. Save UUID for later.
            setIsaUuid(isa);
        }

        return this;
    }

    public String getIsaUuid() {
        return isaUuid;
    }

    public void setIsaUuid(String isaUuid) {
        this.isaUuid = isaUuid;
    }

    public DynamicProperties getKv() {
        return kv;
    }

    public JsonSchemaBuilder getSchema() {
        JsonSchemaBuilder schema = null;
        try {
            String dataSchemaContent = mapper.readTree((String)getFiles().get("dataSchema")).get("content").asText();
            schema = new JsonSchemaBuilder((ObjectNode)mapper.readTree(dataSchemaContent));
        } catch (ProcessingException | IOException e) {
            if (Configuration.isTraceModeOn()) {
                e.printStackTrace();
            }
        }
        return schema;
    }

    public Type addInstrument(Instrument instrument) {
        if (instruments == null) {
            instruments = new HashSet<>();
        }
        instruments.add(instrument);

        return this;
    }

    public Set<Instrument> getInstruments() {
        return instruments;
    }

    public Type addImplementedProtocol(Type protocol) {
        if (protocol == null) {
            log.error("Provided argument to addImplementedProtocol cannot be null.");
            return this;
        }
        if (!protocol.isOfType("Protocol")) {
            log.error("Can't add non-protocol type " + protocol.getName() + " as implemented protocol.");
            return this;
        }
        if (!isOfType("Software")) {
            log.error("Can't add an implemented protocol to a non-software type.");
            return this;
        }

        if (implementsProtocols == null) {
            implementsProtocols = new HashSet<>();
        }
        implementsProtocols.add(protocol);

        return this;
    }

    public boolean implementsProtocol(Type protocol) {
        return implementsProtocols == null ? false : implementsProtocols.contains(protocol);
    }

    public boolean isOfType(String type) {
        return typeRepository.isOfType(getUuid(), type) > 0;
    }

    public Type save() {
        typeRepository.save(this);
        return this;
    }

    public ObjectNode toJson() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = mapper.createObjectNode();

        List<String> childIsaUuids = null;
        if (getChildIsaSet() != null) {
            getChildIsaSet().stream()
                    .map(Node::getUuid)
                    .collect(Collectors.toList());
        }

        json.put("name", getName());
        json.put("uuid", getUuid());
        json.put("glyphId", Integer.toHexString(glyphId));
        if (getIsa() != null) {
            json.put("isa", getIsa().getUuid());
        }
        json.put("desc", getDesc());
        json.put("checksum", getChecksum());
        try {
            if (getSchema() != null) {
                json.set("schema", getSchema().getAsObjectNode());
            }
            if (childIsaUuids != null) {
                json.put("children", mapper.writeValueAsString(childIsaUuids));
            }
        } catch (JsonProcessingException e) {
        	log.error(e.getMessage());

			if (Configuration.isTraceModeOn()) {
				e.printStackTrace();
			}
		}

        json.put("_nodeType", getNodeType());

        return json;
    }
}
