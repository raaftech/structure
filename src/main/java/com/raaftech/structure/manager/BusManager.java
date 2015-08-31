package com.raaftech.structure.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.base.GenericActor;

public class BusManager extends GenericActor {

    private Logger log = LoggerFactory.getLogger(BusManager.class);

    public MessageHandler list() {
        return new MessageHandler() {
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();

                result.put("list", "yes, you reached me.");

                for (String id :vertx.deploymentIDs()) {

                    result.put("actor", id);

                }
                return response(request, result);
            }

            public JsonNode help(JsonNode request) {
                return jsonFromResource("help/busmanager.list.json");
            }
        };
    }
}
