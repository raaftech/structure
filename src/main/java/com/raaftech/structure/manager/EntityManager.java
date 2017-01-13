package com.raaftech.structure.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.annotations.SchemaPath;
import com.raaftech.structure.base.Configuration;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.base.GenericActor;
import com.raaftech.structure.exceptions.EntityDeserializationException;
import com.raaftech.structure.graph.repositories.NodeRepository;
import com.raaftech.structure.helper.JsonHelper;
import com.raaftech.structure.helper.Triple;
import com.raaftech.structure.helper.loader.EvenNaiverLoader;
import com.raaftech.structure.next.Entity;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class EntityManager extends GenericActor {

    private static final Logger log = LoggerFactory.getLogger(EntityManager.class);
    private static final NodeRepository nodeRepository = Context.getNodeRepository();

    private static final String META = "meta";
    private static final String UUID = "uuid";

    // An entity is new when it has no UUID, or the UUID is unknown in the database
    private Predicate<ObjectNode> isNewEntity = (json) -> {
        if (json.has(META) && !json.get(META).has(UUID)) {
            return true;
        }
        String uuid = json.get(META).get(UUID).asText();

        return nodeRepository.findByUuid(uuid) == null;
    };

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
                    result.put("error", e.getMessage());
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
                .collect(Collectors.toMap(e -> e.getUuid(), e -> e));

        // Used to collect our sorted entities.
        List<Entity> sorted = new ArrayList<>();

        // Takes all the UUIDs an entity is related to, checks which ones are in our working set,
        // then checks if those are sorted already. If so, return true.
        Predicate<Entity> peersAreSorted = (entity) -> {
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
