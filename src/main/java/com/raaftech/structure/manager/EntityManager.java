package com.raaftech.structure.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.raaftech.structure.annotations.SchemaPath;
import com.raaftech.structure.base.Configuration;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.base.GenericActor;
import com.raaftech.structure.exceptions.EntityDeserializationException;
import com.raaftech.structure.exceptions.EntitySerializationException;
import com.raaftech.structure.graph.Node;
import com.raaftech.structure.graph.repositories.NodeRepository;
import com.raaftech.structure.helper.JsonHelper;
import com.raaftech.structure.helper.LambdaHelper;
import com.raaftech.structure.helper.Triple;
import com.raaftech.structure.helper.loader.EvenNaiverLoader;
import com.raaftech.structure.next.Entity;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.support.Neo4jTemplate;

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

    private static final String META = "meta";
    private static final String UUID = "uuid";
    private static final String CHECKSUM = "checksum";
    private static final String ERROR = "error";
    private static final String START = "start";
    private static final String INCLUDESTART = "includeStart";
    private static final String LABELS = "labels";

    // An entity is new when it has no UUID, or the UUID is unknown in the database
    private Predicate<ObjectNode> isNewEntity = json -> {
        if (json.has(META) && !json.get(META).has(UUID)) {
            return true;
        }
        String uuid = json.get(META).get(UUID).asText();

        return nodeRepository.findByUuid(uuid) == null;
    };

    // An entity was updated if the checksum changed
    private Predicate<Entity> wasUpdated = entity ->
            !Objects.equals(entity.getChecksum(),
                    nodeRepository.findByUuid(entity.getUuid()).getChecksum());

    /**
     * Create takes an array containing entities as input. It'll check which of them
     * are new, and insert the new ones in the database in the proper order.
     * An entity is new when it has no UUID defined, or its UUID doesn't exist in the database.
     * @return A list containing the UUIDs of the created entities, in the order they were persisted.
     */
    @SchemaPath("schema/manager/entitymanager/create.schema.json")
    public MessageHandler create() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                final JsonNode args = request.get("args");
                final ObjectNode result = mapper.createObjectNode();
                final ArrayNode data = (ArrayNode) args.get("entities");

                // Transform the received array to a stream
                Transaction tx = Context.getGraphDatabase().beginTx();
                try {
                     List<Entity> entities = StreamSupport.stream(data.spliterator(), true)
                            // If an object doesn't adhere to entity.schema.json, throw an exception
                            .peek(json -> {
                                if (!JsonHelper.validateToSchema(
                                        JsonHelper.jsonFromResource("schema/next/entity.schema.json"), json)) {
                                    throw new EntityDeserializationException("Received object does not validate against entity.schema.json");
                                }
                            })
                            // Validation successful, cast to ObjectNode
                            .map(json -> (ObjectNode) json)
                            // Remove all the objects whose UUID already exist in the database
                            .filter(isNewEntity)
                            // Map those objects to an Entity instance
                            .map(json -> {
                                try {
                                    return mapper.treeToValue(json, Entity.class);
                                } catch (JsonProcessingException e) {
                                    if (Configuration.isTraceModeOn()) {
                                        e.printStackTrace();
                                        log.error(e.getMessage());
                                    }
                                    throw new EntityDeserializationException("Exception thrown while deserializing " +
                                            "an entity, cannot continue.");
                                }
                            })
                            .collect(Collectors.toList());

                    List<String> newUuids = sortForCreation(entities).stream()
                            // Persist them
                            .peek(Entity::toGraph)
                            // Map to the new UUID's
                            .map(Entity::getUuid)
                            // Collect
                            .collect(Collectors.toList());

                    result.set("data", mapper.valueToTree(newUuids));
                    tx.success();
                } catch (Exception e) {
                    result.put(ERROR, e.getMessage());
                    tx.failure();
                } finally {
                    tx.close();
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
     * Read takes an array of labels, a UUID for the start node and a boolean whether the start node should
     * be included in the result as arguments.
     * @return An array containing the vertices that could be reached with the traversal.
     */
    @SchemaPath("schema/manager/entitymanager/read.schema.json")
    public MessageHandler read() {

        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode args = (ObjectNode) request.get("args");
                ObjectNode result = mapper.createObjectNode();

                final String startUuid = args.get(START).asText();
                final boolean includeStart = args.has(INCLUDESTART) ? args.get(INCLUDESTART).asBoolean() : true;
                final Set<String> labels = ImmutableSet.copyOf(
                        StreamSupport.stream(args.get(LABELS).spliterator(), true)
                        .map(JsonNode::asText)
                        .collect(Collectors.toList())
                );

                Transaction tx = Context.getGraphDatabase().beginTx();

                Node start = nodeRepository.findByUuid(startUuid);

                if (start != null) {
                    try {
                        ArrayNode data = result.putArray("data");
                        labelBasedTraversal(labels, start, includeStart).stream()
                                .map(mapper::valueToTree)
                                .forEach(t -> data.add((JsonNode)t));
                    } catch (EntitySerializationException e) {
                        result.put(ERROR, e.getMessage());
                    }
                } else {
                    result.put(ERROR, "Couldn't find starting node.");
                }

                tx.close();

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
     * An entity is updated when it has a UUID that is known in the graph, and its checksum is changed.
     * Optionally, {@code update} can be provided with an {@code oldEntities} array that contains
     * the entities in their original state, used to see if entities were updated while we were editing them.
     * @return A list containing the UUIDs of the updated entities, in the order they were persisted.
     */
    @SchemaPath("schema/manager/entitymanager/update.schema.json")
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
                            .collect(Collectors.toMap(e -> e.get(META).get(UUID).asText(),
                                    e -> e.get(META).get(CHECKSUM).asText()));
                } else {
                    oldEntitiesUuidToChecksum = ImmutableMap.of();
                }

                final Consumer<Throwable> exceptionHandler = e -> {
                    if (!result.has(ERROR)) {
                        result.putArray(ERROR);
                    }
                    ((ArrayNode)result.get(ERROR)).add(e.getMessage());
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
                                .put("checksum", EvenNaiverLoader.calculateChecksum(entity, EvenNaiverLoader
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

                if (updatedEntities.isEmpty()) {
                    if (oldEntitiesUuidToChecksum.size() > 0) {
                        if (updatedEntities.stream()
                                .map(e -> entityAlreadyChanged.test(oldEntitiesUuidToChecksum, e.getUuid()))
                                .reduce((a, b) -> a || b).get()) {
                            result.put(ERROR, "Entity work set was updated in the mean while.");
                            return response(request, result);
                        }
                    }
                    Transaction tx = Context.getGraphDatabase().beginTx();
                    try {
                        final ArrayNode data = result.putArray("data");
                        updatedEntities.stream()
                                .peek(Entity::toGraph)
                                .map(Entity::getUuid)
                                .collect(Collectors.toList())
                                .forEach(data::add);
                        tx.success();
                    } catch (Exception exception) {
                        if (Configuration.isTraceModeOn()) {
                            exception.printStackTrace();
                        }
                        log.error(exception.getMessage());
                        // This overwrites any existing error property, which is ok.
                        result.put(ERROR, "Error while persisting updated entities to database.");
                        tx.failure();
                    } finally {
                        tx.close();
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
     * Given a UUID of an entity and an array of labels, check if we can safely delete it, and act accordingly.
     * For a description of what "safe deletion" means, see {@link #safeDeleteTraversal(List, Node)}.
     * @return Returns the UUID of the entity in case of deletion, an empty result otherwise.
     */
    public MessageHandler delete() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode args = (ObjectNode) request.get("args");
                ObjectNode result = mapper.createObjectNode();

                List<String> labels = StreamSupport.stream(args.get("labels").spliterator(), true)
                        .map(JsonNode::asText)
                        .collect(Collectors.toList());

                Node toDelete = nodeRepository.findByUuid(args.get("uuid").asText());
                Transaction tx = Context.getGraphDatabase().beginTx();
                try {
                    if (safeDeleteTraversal(labels, toDelete)) {
                        nodeRepository.delete(toDelete);
                        result.put("data", toDelete.getUuid());
                        tx.success();
                    }
                } catch (Exception e) {
                    result.put(ERROR, "Error while deleting node \"" + toDelete.getUuid() + "\", exception: "
                            + e.getMessage());
                    tx.failure();
                } finally {
                    tx.close();
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
     * Given a starting point v_s, find collect every vertex v_e that where the path from v_s to v_e contains
     * only vertices that have at least one of the labels in {@code labels}.
     * @param labels If a traversal encounters a vertex that does not have at least one label included in this parameter
     *               the traversal will be pruned.
     * @param start The start node v_s.
     * @param includeStart Should we include v_s in the resulting list?
     * @return A list containing every vertex that can be found by the traversal and, optionally, v_s.
     */
    List<Node> labelBasedTraversal(Set<String> labels, Node start, boolean includeStart) {
        /*
        This traversal description simply follows all outgoing edges of the start vertex, and tests
        whether the end node's labels contains at least one label that is provided as an argument to this method.
         */
        TraversalDescription traversalDescription = Context.getGraphDatabase().traversalDescription()
                .depthFirst()
                .expand(PathExpanders.forDirection(Direction.OUTGOING))
                .evaluator(path -> {
                    if (path.length() == 0) {
                        return includeStart ? Evaluation.INCLUDE_AND_CONTINUE : Evaluation.EXCLUDE_AND_CONTINUE;
                    }
                    final boolean include = StreamSupport.stream(path.endNode().getLabels().spliterator(), false)
                            .map(Label::name)
                            .anyMatch(labels::contains);
                    return Evaluation.of(include, include);
                });

        // We'll need the org.neo4j.graphdb.Node counterpart of our "start" argument
        org.neo4j.graphdb.Node startNode = neo4jTemplate.getPersistentState(start);
        // Every vertex found by this traversal will be mapped to Node, collected, and returned
        return StreamSupport.stream(traversalDescription.traverse(startNode).spliterator(), true)
                .map(n -> neo4jTemplate.convert(n, Node.class))
                .collect(Collectors.toList());
    }

    /**
     * Given a starting point v_s, and a list of labels, check if we can safely delete v_s. We do this by checking
     * every neighbor v_n of v_s. For the following: if v_s is a
     * <a href="https://en.wikipedia.org/wiki/Glossary_of_graph_theory_terms#predecessor">predecessor</a> of v_n,
     * and v_n contains a label that is also in {@code labels}, deletion is unsafe.
     * If v_s is a <a href="https://en.wikipedia.org/wiki/Glossary_of_graph_theory_terms#successor">successor</a> of v_n
     * , and v_n does not contains a label that is also in {@code labels}, deletion is unsafe.
     * @param labels The list of labels that dictate whether deletion is safe or not.
     * @param start The start node v_s.
     * @return True if we can safely delete v_s, false otherwise.
     */
    private boolean safeDeleteTraversal(List<String> labels, Node start) {
        // We'll need the org.neo4j.graphdb.Node counterpart of our "start" argument
        org.neo4j.graphdb.Node startNode = neo4jTemplate.getPersistentState(start);

        TraversalDescription traversalDescription = Context.getGraphDatabase().traversalDescription()
                .depthFirst()
                .expand(PathExpanders.allTypesAndDirections())
                .evaluator(path -> {
                    if (path.length() == 0) {
                        return Evaluation.EXCLUDE_AND_CONTINUE;
                    }
                    if (path.length() > 1) {
                        return Evaluation.EXCLUDE_AND_PRUNE;
                    }
                    /*
                     Check if we are at the start of an outgoing edge, or the end of one. In the first case
                     we need to make sure we don't point to vertices of the same type, in the latter case we need to
                     make sure there aren't any vertices pointing to us that aren't of the same type.
                      */
                    final boolean include;
                    if (path.lastRelationship().getStartNode().getId() == startNode.getId()) {
                        include = StreamSupport.stream(path.endNode().getLabels().spliterator(), false)
                                .map(Label::name)
                                .anyMatch(labels::contains);
                    } else {
                        include = !StreamSupport.stream(path.endNode().getLabels().spliterator(), false)
                                .map(Label::name)
                                .anyMatch(labels::contains);
                    }
                    return Evaluation.of(include, include);
                });

        return StreamSupport.stream(traversalDescription.traverse(startNode).spliterator(), false).count() == 0;
    }

    /**
     * This method takes a list of entities, and sorts them. Entities that are leafs in the
     * resulting graph are at the start of the list, their parents after that, etc.
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
