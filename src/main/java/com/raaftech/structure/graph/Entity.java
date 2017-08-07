package com.raaftech.structure.graph;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.raaftech.structure.base.Configuration;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.bus.GenericActor;
import com.raaftech.structure.exceptions.IllegalEntityType;
import com.raaftech.structure.exceptions.TypeUnknownException;
import com.raaftech.structure.graph.converters.JsonNodeWriter;
import com.raaftech.structure.graph.repositories.InstanceRepository;
import com.raaftech.structure.graph.repositories.NodeRepository;
import com.raaftech.structure.graph.repositories.TypeRepository;
import com.raaftech.structure.helper.JsonHelper;
import com.raaftech.structure.helper.Triple;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.fieldaccess.DynamicProperties;
import org.springframework.data.neo4j.fieldaccess.DynamicPropertiesContainer;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.index.IndexType;
import org.springframework.data.neo4j.support.query.CypherQueryExecutor;

import java.net.URI;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static io.vertx.core.json.Json.mapper;

/**
 * A new root entity node with heavily simplified and structured construction.
 *
 * @author Rubin Simons <rubin@raaftech.com>
 * @since 2016-10-19
 */
@JsonDeserialize(using = EntityDeserializer.class)
public class Entity {

    /* Logging object. */
    @Transient
    private final static Logger log = LoggerFactory.getLogger(Entity.class);

    /* Entity repository. Used for querying the graph. */
    private NodeRepository nodeRepository = Context.getNodeRepository();
    private InstanceRepository instanceRepository = Context.getInstanceRepository();
    private TypeRepository typeRepository = Context.getTypeRepository();
    private Neo4jTemplate neo4jTemplate;

    public void setNeo4jTemplate(Neo4jTemplate neo4jTemplate) {
        this.neo4jTemplate = neo4jTemplate;
    }

    private Node persistedState;

    /* Internal graph database node id. */
    @GraphId
    private Long id;

    /* Main key value/pairs: bundle, type, name, desc, uuid. */
    @Indexed(indexType = IndexType.FULLTEXT, indexName = "bundle")
    private String bundle;

    @Indexed(indexType = IndexType.FULLTEXT, indexName = "name")
    private String name;

    @Indexed(indexType = IndexType.FULLTEXT, indexName = "desc")
    private String desc;

    @Indexed(indexType = IndexType.FULLTEXT, indexName = "uuid")
    private String uuid;

    /**
     * "Type" entities can be made abstract. This means no instance of them can be created.
     */
    private boolean isAbstract;

    /**
     * See {@link Type#identity}
     */
    private String[] identity;

    /* Generated key/value pairs: checksum, path. */
    private String checksum;
    private String path;

    /* Dynamic property containers for inline data and files. */
    private DynamicProperties data = new DynamicPropertiesContainer();
    private DynamicProperties file = new DynamicPropertiesContainer();

    /* A holder for relations to be written by persistRelations().
     * From left to right, the triple should contain the relationship name,
     * the direction (incoming, outgoing, undirected) and the set of UUIDs
     * of the nodes that should be related by this relationship. */
    @Transient
    private Set<Triple<String, String, Set<String>>> relations;

    /* Constraints are temporarily stored in this field. The field is a map from a string
     * to a list. The string of that map is the relation name that is constrainted. The list
     * contains tuples of Cardinality + UUID.
     */
    @Transient
    private Map<String, List<Pair<Constraint.Cardinality, UUID>>> constraints;

    /*
    The next two sets are used to see if a vertice incident to an edge can be
    queryable by name. This can only be guaranteed for types, so we constraint it
    to just types.
     */
    // Incoming relations that origin from a type
    private static final Set<String> incomingFromType = ImmutableSet.of(
            "ISA", "CONSTRAINTS", "IMPLEMENTS"
    );
    // Outgoing relations that point to a typ
    private static final Set<String> outgoingToType = ImmutableSet.of(
            "ISA", "CONSTRAINTS", "IMPLEMENTS", "PROVIDED_BY",
            "INSTANCE_OF", "APPLIES_TO", "GIVES_CAPABILITY"
    );

    /* Empty constructor for Spring Data Neo4j's purposes. */
    public Entity() {
        this.neo4jTemplate = Context.getNeo4jTemplate();
    }

    // Allow construction of Entity with a specific neo4j template, for testing purposes
    public Entity(Neo4jTemplate neo4jTemplate) {
        this.neo4jTemplate = neo4jTemplate;
    }


    /* Getters. */
    public String getBundle() {
        return bundle;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public String getUuid() {
        return uuid;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getPath() {
        return path;
    }

    public Map<String, Object> getData() {
        return data.asMap();
    }

    public Map<String, Object> getFiles() {
        return file.asMap();
    }

    public Map<String, Object> getFile() {
        return file.asMap();
    }

    public String[] getIdentity() {
        return identity;
    }

    /**
     * Get the triples describing this entity's relations.
     *
     * @return A triple containing left to right: relation name, direction, related UUIDs.
     */
    public Set<Triple<String, String, Set<String>>> getRelations() {
        return relations;
    }

    public Optional<Map<String, List<Pair<Constraint.Cardinality, UUID>>>> getConstraints() {
        return constraints == null ?
                Optional.empty() : Optional.of(constraints);
    }

    /**
     * Returns whether this entity is abstract. This is only used on "Type" entities.
     *
     * @return
     */
    public boolean isAbstract() {
        return isAbstract;
    }


    /* Setters. */
    public void setBundle(String bundle) {
        this.bundle = bundle;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setData(Map<String, Object> map) {
        this.data.setPropertiesFrom(map);
    }

    public void setFile(Map<String, Object> map) {
        this.file.setPropertiesFrom(map);
    }

    public void setRelations(Set<Triple<String, String, Set<String>>> relations) {
        this.relations = relations;
    }

    /**
     * Add a relation to or from a set of UUIDs to our relationships object. If the relationship
     * already exists, it checks if the direction is the same and adds the UUIDs to the set. If the direction
     * doesn't match, it throws an exception. If the relationship didn't exist yet, it'll just add it to the relations
     * object.
     *
     * @param relationName The relationship name.
     * @param direction    The direction of the relationship.
     * @param uuids        The UUIDs to add to the relationship.
     * @return Did the relations object change?
     */
    public boolean addRelations(String relationName, Direction direction, Set<UUID> uuids) {
        if (relations == null) {
            relations = new HashSet<>();
        }
        final Set<Triple<String, String, Set<String>>> relevantTriples = relations.stream()
                .filter(t -> t.getLeft().equals(relationName))
                .collect(Collectors.toSet());

        if (relevantTriples.size() > 1) {
            throw new IllegalStateException("Multiple triples found for relationship \"" + relationName
                    + "\" on node \"" + uuid + "\".");
        }
        Set<String> uuidsAsStrings = uuids.stream().map(UUID::toString).collect(Collectors.toSet());
        final boolean changed;
        if (relevantTriples.isEmpty()) {
            relations.add(Triple.of(relationName, direction.toString().toLowerCase(), uuidsAsStrings));
            changed = true;
        } else {
            // relevantTriples contains just a single triple
            final Triple<String, String, Set<String>> relevantTriple = relevantTriples.stream().findFirst().get();
            changed = relevantTriple.getRight().addAll(uuidsAsStrings);
        }
        return changed;
    }

    public void setIdentity(String[] identity) {
        this.identity = identity;
    }

    public void addConstraint(String relName, Constraint.Cardinality cardinality, UUID to) {
        if (constraints == null) {
            constraints = new HashMap<>();
        }

        final List<Pair<Constraint.Cardinality, UUID>> constraintList;
        if (constraints.containsKey(relName)) {
            constraintList = constraints.get(relName);
        } else {
            constraintList = new ArrayList<>();
            constraints.put(relName, constraintList);
        }

        constraintList.add(Pair.of(cardinality, to));
    }

    /**
     * An entity can either be of the "type" or "instance" variety.
     *
     * @return True if this entity is of the "instance" variety.
     */
    public boolean isInstance() {
        if (getRelations() == null) {
            throw new RuntimeException("Cannot determine type of entity " +
                    "when entity (\"" + getUuid() + "\" has no relations");
        }
        return getRelations().stream()
                .anyMatch(triple -> "INSTANCE_OF".equals(triple.getLeft()));
    }

    /**
     * Sets whether an entity is abstract. Can only be used on "Type" entities.
     *
     * @param isAbstract True if this is an abstract entity.
     * @throws IllegalArgumentException Thrown when called on a non "Type" entity.
     */
    public void setAbstract(boolean isAbstract) {
        if (isInstance()) {
            throw new IllegalArgumentException(createErrorMessage(400, "", "Only a \"Type\" entity can be made abstract."));
        } else {
            this.isAbstract = isAbstract;
        }
    }


    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;

        if (id == null)
            return false;

        if (!(other instanceof Entity))
            return false;

        return id.equals(((Entity) other).id);
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : id.hashCode();
    }


    public JsonNode toJson() {
        JsonNode entityJson = null;
        return entityJson;
    }

    private Pair<Boolean, Set<EntityValidationError>> validateAgainstConstraints() {
        Optional<Type> instanceOf = getInstanceOf();

        if (!instanceOf.isPresent()) {
            throw new TypeUnknownException(createErrorMessage(404, "", "Type for entity \"" + uuid + "\" doesn't exist"));
        }

        return instanceOf.get().validateEntityToConstraints(this);
    }

    /**
     * Get the the identity properties for the type of an instance, and the name of the
     * type these identity properties are defined on.
     *
     * @return Optionally the identity properties that must hold for an instance of a particular type,
     * and the type name those properties are defined on.
     */
    private Optional<Pair<String, Set<String>>> getIdentityProperties() {
        final Optional<Type> maybeInstanceOf = getInstanceOf();
        if (!maybeInstanceOf.isPresent()) {
            throw new TypeUnknownException(createErrorMessage(404, "", "Type for entity \"" + uuid + "\" doesn't exist"));
        }

        Type instanceOf = maybeInstanceOf.get();
        if (identity == null) {
            if (typeRepository.inheritsIdentity(instanceOf.getUuid())) {
                /*
                Retrieve first inherited identity. We already know the optional won't be empty
                because of the typeRepository#inheritsIdentity query.
                 */
                return instanceOf.getFirstInheritedIdentity();
            } else {
                // No identity to adhere to, return true
                return Optional.empty();
            }
        } else {
            // Use identity field
            return Optional.of(Pair.of(instanceOf.getName(), Arrays.stream(identity).collect(Collectors.toSet())));
        }
    }

    /**
     * Given a label and some identity properties, find duplicates.
     * We do this by getting all available values for the identity properties, and returning
     * node UUIDs of nodes with the given label and the given values for the properties in the
     * graph.
     *
     * @param label              The label to use in the query.
     * @param identityProperties The properties that need to be used for the query.
     * @return A set of UUIDs of nodes that have the given label and the same values
     * for the given properties as this entity.
     */
    private Set<String> findDuplicates(String label, Set<String> identityProperties) {
        final String propertyQueryPattern = "`data-%s`: %s";
        final String completeQueryPattern = "MATCH (n:`type:%s` {%s}) RETURN n.uuid";
        final String inQuotes = "\"%s\"";
        final String dataProperties = identityProperties.stream()
                .filter(id -> data.hasProperty(id))
                .map(id -> Pair.of(id, data.getProperty(id)))
                .map(pair -> pair.getRight() instanceof String ?
                        Pair.of(pair.getLeft(),
                                /*
                                We'll need to escape because we don't want to end up with
                                `data-foo`:"b"ar", but with `data-foo`:"b\"ar"
                                 */
                                String.format(inQuotes,
                                        StringEscapeUtils.escapeJava((String) pair.getRight()))) :
                        pair)
                // We don't have to check for backticks in the identity names, bundleloader does that for us
                .map(pair -> String.format(propertyQueryPattern, pair.getLeft(), pair.getRight()))
                .collect(Collectors.joining(", "));

        final String properties;
        // add name property if we identify on that, edge case because it's not part of the data map
        if (identityProperties.contains("name")) {
            final String nameProperty = String.format("`name`: \"%s\"", StringEscapeUtils.escapeJava(getName()));
            if (dataProperties.length() > 0) {
                properties = new StringBuilder(dataProperties).append(", ").append(nameProperty).toString();
            } else {
                properties = nameProperty;
            }
        } else {
            properties = dataProperties;
        }

        final String completeQuery = String.format(completeQueryPattern, label, properties);
        CypherQueryExecutor queryExecutor = Context.getNeo4jTemplate().getInfrastructure().getCypherQueryExecutor();
        Iterable<String> results = queryExecutor.query(completeQuery, String.class, null);

        return StreamSupport.stream(results.spliterator(), false).collect(Collectors.toSet());
    }

    private String createErrorMessage(int code, String conflictUuid, String message) {
        final JsonObject msg = new JsonObject()
                .put("code", code)
                .put("uuid", getUuid())
                .put("typeName", getName())
                .put("conflict", conflictUuid)
                .put("message", message);
        return msg.toString();
    }

    public void toGraph() {
        final Optional<List<String>> maybeClassLabels;
        if (isInstance()) {
            final Optional<Type> maybeInstanceOf = getInstanceOf();
            if (!maybeInstanceOf.isPresent()) {
                throw new IllegalEntityType(createErrorMessage(404, "", "instance-of type of this instance was not found"));
            }
            final Type instanceOf = maybeInstanceOf.get();
            final Optional<String> maybeDomainClass =
                    getDomainClassForPath(instanceOf.getHierarchyPath());

            if (!maybeDomainClass.isPresent()) {
                throw new IllegalEntityType(createErrorMessage(424, "", "No domain class found"));
            }
            maybeClassLabels = getClassLabels(maybeDomainClass.get());

            // See if the type we try to instantiate is abstract
            if (instanceOf.isAbstract()) {
                throw new IllegalEntityType(createErrorMessage(422, instanceOf.getUuid(), "Cannot instantiate based on an abstract type"));
            }

            // See if this instance validates against its type's constraints
            final Pair<Boolean, Set<EntityValidationError>> validation = validateAgainstConstraints();
            if (!validation.getLeft()) {
                throw new IllegalEntityType(validation.getRight().toString());
            }

            // Now check if this instance is unique per its type's identity array
            Optional<Pair<String, Set<String>>> maybeLabelAndIdentityProperties = getIdentityProperties();
            if (maybeLabelAndIdentityProperties.isPresent()) {
                final String label = maybeLabelAndIdentityProperties.get().getLeft();
                final Set<String> identityProperties = maybeLabelAndIdentityProperties.get().getRight();
                Set<String> duplicates = findDuplicates(label, identityProperties);
                if (duplicates.size() > 0) {
                    IllegalEntityType e = new IllegalEntityType(createErrorMessage(409, duplicates.toArray()[0].toString(), "Duplicate based on " + identityProperties));

                    // The "duplicate" might be the same as this (in case of an update)
                    if (duplicates.size() == 1) {
                        // Make sure it isn't before throwing an exception
                        if (!duplicates.toArray()[0].equals(getUuid())) {
                            throw e;
                        }
                        // Duplicates > 2, so at least one won't be the same as this
                    } else {
                        throw e;
                    }
                }
            }

            final String schemaUriPattern = "/schemas/%s.schema.json";
            final URI schemaUri = Configuration.getSchemaNamespace().resolve(String.format(schemaUriPattern, instanceOf.getName().toLowerCase()));
            final Map<String, Object> data = new HashMap(getData());
            data.put("name", getName());
            data.put("desc", getDesc());
            final boolean validated = JsonHelper.validateToSchema(schemaUri, mapper.valueToTree(data));
            if (!validated) {
                throw new IllegalEntityType(createErrorMessage(400, "", "Failed to validate against type schema"));
            }
        } else {
            maybeClassLabels = getClassLabels("Type");
            // This shouldn't happen
            if (!maybeClassLabels.isPresent()) {
                throw new IllegalEntityType(createErrorMessage(500, "", "Unable to retrieve labels for entity"));
            }
        }

        final List<String> classLabels = maybeClassLabels.get();

        persist(classLabels.toArray(new String[classLabels.size()]));
        Node node = getPersistentState();
        persistRelations(node);
        persistTypeLabels(node);
    }

    public Node getPersistentState() {
        return persistedState;
    }

    /**
     * Persist this entity to neo4j.
     *
     * @param labels The labels SDN needs to map this node onto an object.
     */
    private void persist(String... labels) {
        final List<String> labelsToPersist = ImmutableList.copyOf(Arrays.asList(labels));

        Map<String, Object> properties = new HashMap<>();
        properties.putAll(data.asMap().entrySet().stream()
                .map(entry -> new AbstractMap.SimpleEntry<>("data-".concat(entry.getKey()), entry.getValue()))
                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(),
                        entry.getValue() instanceof JsonNode ? new JsonNodeWriter().convert((JsonNode) entry.getValue()) : entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        properties.putAll(file.asMap().entrySet().stream()
                .map(entry -> new AbstractMap.SimpleEntry<>("file-".concat(entry.getKey()), entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        if (bundle != null) {
            properties.put("bundle", bundle);
        }
        if (name != null) {
            properties.put("name", name);
        }
        if (desc != null) {
            properties.put("desc", desc);
        }
        if (uuid != null) {
            properties.put("uuid", uuid);
        }
        if (checksum != null) {
            properties.put("checksum", checksum);
        }
        if (path != null) {
            properties.put("path", path);
        }
        if (identity != null) {
            properties.put("identity", identity);
        }
        if (!isInstance()) {
            properties.put("isAbstract", isAbstract);
        }

        /* we use neo4jTemplate.merge(...), which is basically getOrCreate(...). Now, if the entity
        already existed, but has changed properties, we need to update them using Node.setProperty(...).
        I did not find a way in the API to do this atomically, like "SET" would do in Cypher */
        persistedState = neo4jTemplate.merge("Node", "uuid", uuid, properties, labelsToPersist);
        properties.entrySet().stream().forEach(entry -> persistedState.setProperty(entry.getKey(), entry.getValue()));
        persistedState.getLabels().forEach(label -> persistedState.removeLabel(label));
        labelsToPersist.stream()
                .map(DynamicLabel::label)
                .forEach(persistedState::addLabel);
    }

    /**
     * Given a hierarchical path for a type from the type tree returns the domain class.
     *
     * @param path The hierarchical path for a type in the type tree.
     * @return Optionally the domain class name.
     */
    private static Optional<String> getDomainClassForPath(String path) {
        // A map containing the domain class belonging to a particular type hierarchical path
        Map<String, String> domainClassPerPath = ImmutableMap.<String, String>builder()
                .put("type.instrument.controller", "Controller")
                .put("type.instrument.marker", "Marker")
                .put("type.instrument.parser", "Parser")
                .put("type.component", "Component")
                .put("type.profile", "Profile")
                .put("type.result", "Result")
                .put("type.group", "Group")
                .build();

        return domainClassPerPath.entrySet().stream()
                .filter(entry -> path.startsWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    /**
     * Given a name of a domain class that can be mapped to the graph, return the labels
     * SDN needs to map onto objects of that class.
     *
     * @param nodeEntityName The name of the SDN mapped domain class.
     * @return Optionally a list of labels needed by SDN.
     */
    private static Optional<List<String>> getClassLabels(String nodeEntityName) {
        // A map containing the labels needed per nodeEntityName
        Map<String, List<String>> labelsPerType = ImmutableMap.<String, List<String>>builder()
                .put("Controller", ImmutableList.of("_Controller", "Controller", "Instrument", "Instance", "Node"))
                .put("Marker", ImmutableList.of("_Marker", "Marker", "Instrument", "Instance", "Node"))
                .put("Parser", ImmutableList.of("_Parser", "Parser", "Instrument", "Instance", "Node"))
                .put("Component", ImmutableList.of("_Component", "Component", "Instance", "Node"))
                .put("Profile", ImmutableList.of("_Profile", "Profile", "Instance", "Node"))
                .put("Result", ImmutableList.of("_Result", "Result", "Instance", "Node"))
                .put("Group", ImmutableList.of("_Group", "Group", "Instance", "Node"))
                .put("Type", ImmutableList.of("_Type", "Type", "Node"))
                .build();

        return labelsPerType.containsKey(nodeEntityName) ?
                Optional.of(labelsPerType.get(nodeEntityName)) : Optional.empty();
    }

    private void persistTypeLabels(Node node) {
        /* Add labels as Neo4j labels. Node will be null if entity is not persisted. */
        if (node != null && uuid != null) {
            final Set<String> labels;
            if (isInstance()) {
                Optional<Type> maybeType = getInstanceOf();
                if (maybeType.isPresent()) {
                    labels = nodeRepository.getTypeHierarchyForTypeUuid(maybeType.get().getUuid());
                } else {
                    throw new TypeUnknownException(createErrorMessage(404, "", "Type for entity \"" + uuid + "\" doesn't exist"));
                }
            } else {
                labels = nodeRepository.getTypeHierarchyForTypeUuid(uuid);
            }
            labels.stream()
                    .map(s -> "type:".concat(s))
                    .forEach(label -> node.addLabel(DynamicLabel.label(label)));
        } else {
            log.error("Failed to persist labels for entity \"{}\". Are you sure the entity is in a persisted state?", name);
        }
    }

    /**
     * Called on instance entities to get the Type object the entity is an instance of.
     *
     * @return The found type.
     */
    private Optional<Type> getInstanceOf() {
        if (!isInstance()) {
            throw new IllegalArgumentException("Cannot get the type of a type entity.");
        }
        Optional<String> instanceOfUuid = getRelations().stream()
                .filter(triple -> "INSTANCE_OF".equals(triple.getLeft()))
                .map(Triple::getRight)
                .flatMap(Set::stream)
                .findFirst();
        if (!instanceOfUuid.isPresent()) {
            throw new IllegalStateException("Instance " + getUuid() + " has no INSTANCE_OF relationship");
        }

        Type instanceOf = typeRepository.findByUuid(instanceOfUuid.get());
        if (instanceOf == null) {
            log.error("No type with UUID " + instanceOfUuid.get() + " exists in the database");
            return Optional.empty();
        } else {
            return Optional.of(instanceOf);
        }
    }

    private void persistRelations(Node node) {

        /* Add relations. Node will be null if entity is not persisted. */
        if (relations != null) {
            // Returns whether a relationship with a given direction implies the the entity can be queried by name
            BiPredicate<Direction, RelationshipType> isQueryableByName = (direction, relName) ->
                    (Direction.OUTGOING.equals(direction) ?
                            outgoingToType : incomingFromType).contains(relName.name());

			/*
             * Create a set of all peers of all relations for the purpose of
			 * existence checking.
			 */
            final Stream<Triple<String, String, String>> flattenedRelationsStream =
                    relations.stream().flatMap(triple -> triple.flatten());
            final boolean peersExist = flattenedRelationsStream
                    .map(triple -> Pair.of(
                            triple.getRight(),
                            isQueryableByName.test(Direction.valueOf(triple.getMiddle().toUpperCase()),
                                    DynamicRelationshipType.withName(triple.getLeft())))
                    )
                    .allMatch(pair -> doesPeerExist(pair.getLeft(), pair.getRight()));

            if (peersExist) {

                relations.forEach((relation) -> {
                    String rname = relation.getLeft();
                    String direction = relation.getMiddle();
                    Set<String> peers = relation.getRight();

                    /*
                    When a relation points towards a type it is possible to query that
                    type by name as well as by uuid, which is the default. This is because
                    types have a unique constraint on their names.
                     */
                    final boolean queryableByName = isQueryableByName.test(
                            Direction.valueOf(direction.toUpperCase()), DynamicRelationshipType.withName(rname));

                    peers.forEach((identifier) -> {
                        final Node peer;
                        if (queryableByName) {
                            peer = GenericActor.isUuid(identifier) ?
                                    nodeRepository.findGraphNodeByUuid(identifier) :
                                    neo4jTemplate.getPersistentState(typeRepository.findOneByName(identifier));
                        } else {
                            peer = nodeRepository.findGraphNodeByUuid(identifier);
                        }

                        // check if this relationship already exists by getting al relationships with the same name and direction
                        // and then checking if we have one to "peer".
                        boolean relExists = StreamSupport.stream(node.getRelationships(Direction.valueOf(direction.toUpperCase()), (RelationshipType) () -> rname).spliterator(), false)
                                .filter(r -> r.getEndNode().getId() == peer.getId())
                                .count() > 0;
                        if (!relExists) {
                            switch (direction) {
                                case "incoming":
                                    log.debug("Creating incoming \"{}\" relation from \"{}\" to \"{}\"", rname, (String) peer.getProperty("name"), name);
                                    peer.createRelationshipTo(node, () -> rname);
                                    break;
                                case "outgoing":
                                    log.debug("Creating outgoing \"{}\" relation from \"{}\" to \"{}\"", rname, name, (String) peer.getProperty("name"));
                                    node.createRelationshipTo(peer, () -> rname);
                                    break;
                                case "undirected":
                                    log.debug("Creating undirected \"{}\" relation between \"{}\" and \"{}\"", rname, name, (String) peer.getProperty("name"));
                                    peer.createRelationshipTo(node, () -> rname);
                                    node.createRelationshipTo(peer, () -> rname);
                            }
                        }
                    });

                });
            } else {
                log.error("One of the peers that entity \"{}\" would be related to, does not exist in the graph", name);
            }

        } else {
            log.error("Failed to persist relations for entity \"{}\". Are you sure the entity is in a persisted state?",
                    name);
        }
    }

    private boolean doesPeerExist(String peerIdentifier, boolean queryableByName) {
        if (GenericActor.isUuid(peerIdentifier)) {
            return nodeRepository.findByUuid(peerIdentifier) != null;
        } else if (queryableByName) {
            return typeRepository.findOneByName(peerIdentifier) != null;
        } else {
            return false;
        }
    }
}
