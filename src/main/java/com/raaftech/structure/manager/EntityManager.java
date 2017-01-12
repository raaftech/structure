package com.raaftech.structure.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.base.GenericActor;
import com.raaftech.structure.helper.loader.EvenNaiverLoader;
import com.raaftech.structure.next.Entity;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class EntityManager extends GenericActor {

    private static final Logger log = LoggerFactory.getLogger(EntityManager.class);

    public MessageHandler insertEntities() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                final JsonNode args = request.get("args");
                final ArrayNode entities = (ArrayNode) args.get("data");
                try {
                    System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(entities));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                List<JsonNode> ent = StreamSupport.stream(entities.spliterator(), true)
                        .collect(Collectors.toList());
                persistEntities(ent);

                return response(request, mapper.createObjectNode());
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

    private void persistEntities(List<JsonNode> entities) {
        EvenNaiverLoader evenNaiverLoader = new EvenNaiverLoader();
        Transaction tx = Context.getGraphDatabase().beginTx();
        try {
//            evenNaiverLoader.getUpdateList(evenNaiverLoader.sortIt(entities));
            evenNaiverLoader.getUpdateList(entities).stream().map(e -> {
                try {
                    return mapper.treeToValue(e, Entity.class);
                } catch (JsonProcessingException e1) {
                    e1.printStackTrace();
                    return null;
                }
            })
            .forEach(Entity::toGraph);
            tx.success();
        } catch (Exception e) {
            e.printStackTrace();
            tx.failure();
        } finally {
            tx.close();
        }
    }
}
