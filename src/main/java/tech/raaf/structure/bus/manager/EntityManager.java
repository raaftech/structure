package tech.raaf.structure.bus.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.neo4j.graphdb.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import tech.raaf.structure.annotations.SchemaPath;
import tech.raaf.structure.base.Configuration;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.graph.Graph;
import tech.raaf.structure.bus.GenericActor;
import tech.raaf.structure.bus.NodeActor;
import tech.raaf.structure.exceptions.EntityDeserializationException;
import tech.raaf.structure.exceptions.EntitySerializationException;
import tech.raaf.structure.graph.*;
import tech.raaf.structure.graph.repositories.InstanceRepository;
import tech.raaf.structure.graph.repositories.NodeRepository;
import tech.raaf.structure.graph.repositories.TypeRepository;
import tech.raaf.structure.helper.JsonHelper;
import tech.raaf.structure.helper.LambdaHelper;
import tech.raaf.structure.helper.Triple;
import tech.raaf.structure.helper.loader.NaiveSorter;

import java.net.URI;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class EntityManager extends GenericActor {

    private static final Logger log = LoggerFactory.getLogger(EntityManager.class);
    private static final Neo4jTemplate neo4jTemplate = Context.getNeo4jTemplate();
    private static final NodeRepository nodeRepository = Context.getNodeRepository();
    private static final InstanceRepository instanceRepository = Context.getInstanceRepository();
    private static final TypeRepository typeRepository = Context.getTypeRepository();

    private static final String META = "meta";
    private static final String UUlD = "uuid";
    private static final String CHECKSUM = "checksum";
    private static final String ERROR = "error";
    private static final String START = "start";
    private static final String INCLUDESTART = "includeStart";
    private static final String LABELS = "labels";

    // An entity is new when it has no UUlD, or the UUlD is unknown in the database
    private Predicate<ObjectNode> isNewEntity = json -> {
        if (json.has(META) && !json.get(META).has(UUlD)) {
            return true;
        }
        String uuid = json.get(META).get(UUlD).asText();

        return nodeRepository.findByUuid(uuid) == null;
    };

    // An entity was updated if the checksum changed
    private Predicate<Entity> wasUpdated = entity ->
            !Objects.equals(entity.getChecksum(),
                    nodeRepository.findByUuid(entity.getUuid()).getChecksum());

    /**
     * Create takes an array containing entities as input. It'll check which of them
     * are new, and insert the new ones in the database in the proper order.
     * An entity is new when it has no UUlD defined, or its UUlD doesn't exist in the database.
     *
     * @return A list containing the UUIDs of the created entities, in the order they were persisted.
     */
    @SchemaPath("resource:/schema/manager/entitymanager/create.schema.json")
    public MessageHandler create() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                final JsonNode args = request.get("args");
                final ObjectNode result = mapper.createObjectNode();
                final ArrayNode data = (ArrayNode) args.get("entities");

                try {
                    List<String> newUuids = Graph.transaction(() -> {
                        // Transform the received array to a stream
                        List<Entity> entities = StreamSupport.stream(data.spliterator(), false)
                                // If an object doesn't adhere to entity.schema.json, throw an exception
                                .peek(json -> {
                                    if (!JsonHelper.validateToSchema(
                                            URI.create("resource:/schema/entity.schema.json"), json)) {
                                        throw new EntityDeserializationException("Received object does not validate against entity.schema.json");
                                    }
                                })
                                // Validation successful, cast to ObjectNode
                                .map(json -> (ObjectNode) json)
                                // Remove all the objects whose UUlD already exist in the database
                                .filter(isNewEntity)
                                // Map those objects to an Entity instance
                                .map(json -> LambdaHelper.wrapWithExceptionPropagation(
                                        () -> mapper.treeToValue(json, Entity.class)).get())
                                .collect(Collectors.toList());

                        return sortForCreation(entities).stream()
                                // Persist them
                                .peek(Entity::toGraph)
                                // Deploy them as actors after the commit
                                .peek(entity -> Graph.afterCommit(() -> NodeActor.deploy(entity)))
                                // Map to the new UUlD's
                                .map(Entity::getUuid)
                                // Collect
                                .collect(Collectors.toList());
                    });

                    result.set("data", mapper.valueToTree(newUuids));
                } catch (Exception e) {
                    result.put(ERROR, e.getMessage());
                }

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

    /**
     * Read uses a start node and a collection of labels to create a traversal, see
     * {@link #labelBasedTraversal(Set, Node, boolean) labelBasedTraversal} for more information.
     * Read takes an array of labels, a UUlD for the start node and a boolean whether the start node should
     * be included in the result as arguments.
     *
     * @return An array containing the vertices that could be reached with the traversal.
     */
    @SchemaPath("resource:/schema/manager/entitymanager/read.schema.json")
    public MessageHandler read() {

        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode args = (ObjectNode) request.get("args");
                ObjectNode result = mapper.createObjectNode();

                final String startUuid = args.get(START).asText();
                final boolean includeStart = args.has(INCLUDESTART) ? args.get(INCLUDESTART).asBoolean() : true;
                final Set<String> labels = ImmutableSet.copyOf(
                        StreamSupport.stream(args.get(LABELS).spliterator(), false)
                                .map(JsonNode::asText)
                                .collect(Collectors.toList())
                );

                Graph.transaction(() -> {
                    Node start = nodeRepository.findByUuid(startUuid);

                    if (start != null) {
                        try {
                            ArrayNode data = result.putArray("data");
                            labelBasedTraversal(labels, start, includeStart).stream()
                                    .map(mapper::valueToTree)
                                    .forEach(t -> data.add((JsonNode) t));
                        } catch (EntitySerializationException e) {
                            result.put(ERROR, e.getMessage());
                        }
                    } else {
                        result.put(ERROR, "Couldn't find starting node.");
                    }
                });

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

    /**
     * Update takes an array containing entities as input. It'll check which of them
     * are updated, and persist those updates to the graph.
     * An entity is updated when it has a UUlD that is known in the graph, and its checksum is changed.
     * Optionally, {@code update} can be provided with an {@code oldEntities} array that contains
     * the entities in their original state, used to see if entities were updated while we were editing them.
     *
     * @return A list containing the UUIDs of the updated entities, in the order they were persisted.
     */
    @SchemaPath("resource:/schema/manager/entitymanager/update.schema.json")
    public MessageHandler update() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                final JsonNode args = request.get("args");
                final ObjectNode result = mapper.createObjectNode();
                final Stream<JsonNode> entities = StreamSupport.stream(args.get("entities").spliterator(), false);
                final Map<String, String> oldEntitiesUuidToChecksum;

                if (args.has("oldEntities")) {
                    oldEntitiesUuidToChecksum = StreamSupport.stream(args.get("oldEntities").spliterator(), false)
                            .collect(Collectors.toMap(e -> e.get(META).get(UUlD).asText(),
                                    e -> e.get(META).get(CHECKSUM).asText()));
                } else {
                    oldEntitiesUuidToChecksum = ImmutableMap.of();
                }

                final Consumer<Throwable> exceptionHandler = e -> {
                    if (!result.has(ERROR)) {
                        result.putArray(ERROR);
                    }
                    ((ArrayNode) result.get(ERROR)).add(e.getMessage());
                };

                /*
                 Checks whether an entity was updated between the read operation and the update operation,
                 simply by checking if an entity's checksum before editing it is still the same as its checksum
                 in the database.
                  */
                final BiPredicate<Map<String, String>, String> entityAlreadyChanged = (map, uuid) -> {
                    final String persistedChecksum = nodeRepository.findByUuid(uuid).getChecksum();
                    return !Objects.equals(oldEntitiesUuidToChecksum.get(uuid), persistedChecksum);
                };

                final List<Entity> updatedEntities = entities
                        .map(e -> (ObjectNode) e)
                        // only keep existing entities
                        .filter(isNewEntity.negate())
                        // calculate and inject checksum
                        .peek(entity -> ((ObjectNode) entity.get("meta"))
                                .put("checksum", NaiveSorter.calculateChecksum(entity, NaiveSorter
                                        .ChecksumAlgorithm.SHA1)))
                        // deserialize to Entity
                        .map(e -> LambdaHelper.wrapWithExceptionHandler(
                                () -> mapper.treeToValue(e, Entity.class), exceptionHandler))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        // do the checksums differ?
                        .filter(wasUpdated)
                        // then collect
                        .collect(Collectors.toList());

                if (!updatedEntities.isEmpty()) {
                    if (oldEntitiesUuidToChecksum.size() > 0) {
                        if (updatedEntities.stream()
                                .map(e -> entityAlreadyChanged.test(oldEntitiesUuidToChecksum, e.getUuid()))
                                .reduce((a, b) -> a || b).get()) {
                            result.put(ERROR, "Entity work set was updated in the mean while.");
                            return response(request, result);
                        }
                    }
                    try {
                        final ArrayNode data = result.putArray("data");
                        Graph.transaction(() -> updatedEntities.forEach(Entity::toGraph));
                        updatedEntities.stream()
                                .map(Entity::getUuid)
                                .forEach(data::add);
                    } catch (Exception exception) {
                        if (Configuration.isTraceModeOn()) {
                            exception.printStackTrace();
                        }
                        log.error(exception.getMessage());
                        // This overwrites any existing error property, which is ok.
                        result.put(ERROR, "Error while persisting updated entities to database.");
                    }
                }

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

    /**
     * Given a UUlD of an entity and an array of labels, check if we can safely delete it, and act accordingly.
     * For a description of what "safe deletion" means, see {@link #safeDeleteTraversal(List, Node)}.
     *
     * @return Returns the UUlD of the entity in case of deletion, an empty result otherwise.
     */
    @SchemaPath("resource:/schema/manager/entitymanager/delete.schema.json")
    public MessageHandler delete() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode args = (ObjectNode) request.get("args");
                ObjectNode result = mapper.createObjectNode();

                List<String> labels = StreamSupport.stream(args.get("labels").spliterator(), false)
                        .map(JsonNode::asText)
                        .collect(Collectors.toList());

                final String uuid = args.get("uuid").asText();
                Node toDelete = nodeRepository.findByUuid(args.get("uuid").asText());
                if (toDelete == null) {
                    result.put(ERROR, "Couldn't find node with UUID \"" + uuid + "\".");
                    return response(request, result);
                }
                try {
                    final boolean deleted = Graph.transaction(() -> {
                        if (!safeDeleteTraversal(labels, toDelete)) {
                            return false;
                        }
                        nodeRepository.delete(toDelete);
                        return true;
                    });
                    if (deleted) {
                        result.put("data", toDelete.getUuid());
                    } else {
                        result.put("error", "Couldn't delete entity.");
                    }
                } catch (Exception e) {
                    result.put(ERROR, "Error while deleting node \"" + toDelete.getUuid() + "\", exception: "
                            + e.getMessage());
                }

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

    @SchemaPath("resource:/schema/manager/entitymanager/specialize.schema.json")
    public MessageHandler specialize() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode args = (ObjectNode) request.get("args");
                ObjectNode result = mapper.createObjectNode();
                final String instanceUuid = args.get("instance").asText();
                final String isaUuid = args.get("isa").asText();

                Graph.transaction(() -> {
                    Instance instance = instanceRepository.findByUuid(instanceUuid);
                    if (instance == null) {
                        result.put(ERROR, "Could not find an instance with uuid " + instanceUuid);
                        return;
                    }
                    Type instanceType = instance.getInstanceOf();
                    Type isa = typeRepository.findByUuid(isaUuid);

                    if (isa == null) {
                        result.put(ERROR, "Could not find a type with uuid " + isaUuid);
                    } else if (!instanceType.getChildIsaSet().contains(isa)) {
                        result.put(ERROR, "Invalid specialization requested. Type [" + isa.getName() + "] is not in the specialization set of: [" + instanceType.getName() + "]");
                    } else {
                        instance.setInstanceOf(isa);
                        instance.save();
                        result.put("data", "Instance [" + instanceType.getName() + "] specialized to [" + isa.getName() + "]");
                    }
                });

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

    @SchemaPath("resource:/schema/manager/entitymanager/group.schema.json")
    public MessageHandler group() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                final JsonNode args = request.get("args");
                final ObjectNode result = mapper.createObjectNode();

                final String groupingUuid = args.get("grouping").asText();
                final String groupeeUuid = args.get("groupee").asText();

                final Group group = Context.getGroupingRepository().findByUuid(groupingUuid);
                final Node groupee = nodeRepository.findByUuid(groupeeUuid);

                if (group == null) {
                    result.put("error", "Group with UUlD \"" + groupingUuid + "\" not found.");
                    return response(request, result);
                }
                if (groupee == null) {
                    result.put("error", "Node with UUlD \"" + groupeeUuid + "\" not found.");
                    return response(request, result);
                }

                final Entity groupingEntity;
                try {
                    groupingEntity = mapper.treeToValue(mapper.valueToTree(group), Entity.class);
                } catch (JsonProcessingException e) {
                    if (Configuration.isTraceModeOn()) {
                        e.printStackTrace();
                    }
                    log.error(e.getMessage());
                    result.put("error", e.getMessage());
                    return response(request, result);
                }
                final String groupRelName = "MEMBER_OF";
                // The entity has only outgoing relations, so ask the group
                final boolean changed = !group.getGroupees().contains(groupee);
                groupingEntity.addRelations(groupRelName,
                        Direction.INCOMING, ImmutableSet.of(UUID.fromString(groupeeUuid)));

                Graph.transaction(groupingEntity::toGraph);

                result.set("data", mapper.createObjectNode().put("changed", changed));

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

    @SchemaPath("resource:/schema/manager/entitymanager/group.schema.json")
    public MessageHandler ungroup() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                final JsonNode args = request.get("args");
                final ObjectNode result = mapper.createObjectNode();

                final String groupingUuid = args.get("grouping").asText();
                final String groupeeUuid = args.get("groupee").asText();

                final Group group = Context.getGroupingRepository().findByUuid(groupingUuid);
                final Node groupee = nodeRepository.findByUuid(groupeeUuid);

                if (group == null) {
                    result.put("error", "Group with UUlD \"" + groupingUuid + "\" not found.");
                    return response(request, result);
                }
                if (groupee == null) {
                    result.put("error", "Node with UUlD \"" + groupeeUuid + "\" not found.");
                    return response(request, result);
                }

                final boolean changed = Graph.transaction(() -> {
                    final boolean removed = group.getGroupees().remove(groupee);
                    Context.getGroupingRepository().save(group);
                    return removed;
                });

                result.set("data", mapper.createObjectNode().put("changed", changed));

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

    /**

    /**
     * Given a starting point v_s, find collect every vertex v_e that where the path from v_s to v_e contains
     * only vertices that have at least one of the labels in {@code labels}.
     *
     * @param labels       If a traversal encounters a vertex that does not have at least one label included in this parameter
     *                     the traversal will be pruned.
     * @param start        The start node v_s.
     * @param includeStart Should we include v_s in the resulting list?
     * @return A list containing every vertex that can be found by the traversal and, optionally, v_s.
     */
    List<Node> labelBasedTraversal(Set<String> labels, Node start, boolean includeStart) {
        final String nextLevel = "MATCH (a:Node)-->(n) " +
                "WHERE a.uuid IN {frontier} AND any(l IN labels(n) WHERE l IN {labels}) " +
                "RETURN DISTINCT n";
        final List<Node> found = new ArrayList<>();
        if (includeStart) {
            found.add(start);
        }
        // Go breadth-first, one query for each level of the graph
        final Set<String> visited = new HashSet<>(Collections.singleton(start.getUuid()));
        List<String> frontier = Collections.singletonList(start.getUuid());
        while (!frontier.isEmpty()) {
            final Map<String, Object> params = ImmutableMap.of(
                    "frontier", frontier, "labels", new ArrayList<>(labels));
            final List<String> next = new ArrayList<>();
            for (Map<String, Object> row : neo4jTemplate.query(nextLevel, params)) {
                final org.neo4j.graphdb.Node n = (org.neo4j.graphdb.Node) row.get("n");
                final String uuid = (String) n.getProperty("uuid");
                if (visited.add(uuid)) {
                    next.add(uuid);
                    found.add(neo4jTemplate.convert(n, Node.class));
                }
            }
            frontier = next;
        }
        return found;
    }

    /**
     * Given a starting point v_s, and a list of labels, check if we can safely delete v_s. We do this by checking
     * every neighbor v_n of v_s. For the following: if v_s is a
     * <a href="https://en.wikipedia.org/wiki/Glossary_of_graph_theory_terms#predecessor">predecessor</a> of v_n,
     * and v_n contains a label that is also in {@code labels}, deletion is unsafe.
     * If v_s is a <a href="https://en.wikipedia.org/wiki/Glossary_of_graph_theory_terms#successor">successor</a> of v_n
     * , and v_n does not contains a label that is also in {@code labels}, deletion is unsafe.
     *
     * @param labels The list of labels that dictate whether deletion is safe or not.
     * @param start  The start node v_s.
     * @return True if we can safely delete v_s, false otherwise.
     */
    private boolean safeDeleteTraversal(List<String> labels, Node start) {
        final String safe = "MATCH (s:Node {uuid: {uuid}}) " +
                "OPTIONAL MATCH (s)-->(o) WHERE any(l IN labels(o) WHERE l IN {labels}) " +
                "WITH s, count(o) AS badOut " +
                "OPTIONAL MATCH (s)<--(i) WHERE none(l IN labels(i) WHERE l IN {labels}) " +
                "RETURN badOut + count(i) = 0 AS safe";
        final Map<String, Object> params = ImmutableMap.of("uuid", start.getUuid(), "labels", labels);
        return (Boolean) neo4jTemplate.query(safe, params).single().get("safe");
    }

    /**
     * This method takes a list of entities, and sorts them. Entities that are leafs in the
     * resulting graph are at the start of the list, their parents after that, etc.
     *
     * @param entities A list of entities.
     * @return A list of entities that can persisted to the graph in that order.
     */
    private List<Entity> sortForCreation(List<Entity> entities) {
        // Create a map from the entity UUIDs to the entities, generating UUIDs if we need to.
        Map<String, Entity> uuidToEntity = entities.stream()
                .peek(e -> {
                    if (e.getUuid() == null) {
                        e.setUuid(java.util.UUID.randomUUID().toString());
                    }
                })
                .collect(Collectors.toMap(Entity::getUuid, e -> e));

        // Used to collect our sorted entities.
        List<Entity> sorted = new ArrayList<>();

        // Takes all the UUIDs an entity is related to, checks which ones are in our working set,
        // then checks if those are sorted already. If so, return true.
        Predicate<Entity> peersAreSorted = entity -> {
            Set<Triple<String, String, Set<String>>> relations = entity.getRelations();
            return relations.stream()
                    .flatMap(triple -> triple.getRight().stream())
                    .filter(uuidToEntity::containsKey)
                    .map(uuidToEntity::get)
                    .allMatch(sorted::contains);
        };

        // Sort!
        while (sorted.size() < entities.size()) {
            entities.stream()
                    .filter(e -> !sorted.contains(e))
                    .filter(peersAreSorted)
                    .forEach(sorted::add);
        }

        return sorted;
    }
}
