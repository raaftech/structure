package com.raaftech.structure.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.base.GenericActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BusManager extends GenericActor {

    private Logger log = LoggerFactory.getLogger(BusManager.class);

    public MessageHandler list() {
        return new MessageHandler() {
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                ArrayNode data = result.putArray("data");

                vertx.deploymentIDs().stream()
                        .forEach(id -> data.add(id));

                return response(request, result);
            }

            public JsonNode help(JsonNode request) {
                return jsonFromResource("help/busmanager.list.json");
            }
        };
    }
}
