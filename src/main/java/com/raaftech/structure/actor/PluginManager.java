package com.raaftech.structure.actor;

import io.vertx.core.json.JsonObject;

import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.base.BaseActor;
import com.raaftech.structure.base.Context;

public class PluginManager extends BaseActor {

    private Set<String> registeredPlugins = Context.INSTANCE.getRegisteredPlugins();

    public MessageHandler isregistered() {
        return new MessageHandler() {

            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                JsonNode args = request.get("args");
                JsonNode schema = jsonFromResource("schema/pluginmanager/isregistered.schema.json");
                if (isValidJson(schema, args)) {
                    String address = args.get("address").asText();
                    result.put("registered", registeredPlugins.contains(address));
                } else {
                    result.put("error", "JSON didn't pass validation.");
                }
                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                // TODO Auto-generated method stub
                return null;
            }

        };
    }

    public MessageHandler callplugin() {
        return new MessageHandler() {

            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                JsonNode args = request.get("args");
                JsonNode schema = jsonFromResource("schema/pluginmanager/callplugin.schema.json");
                if (isValidJson(schema, args)) {
                    String address = args.get("address").asText();
                    String node = args.get("node").asText();

                    if (registeredPlugins.contains(address)) {
                        String taskId = address + "_" + System.currentTimeMillis();
                        // send message
                        JsonObject message = new JsonObject()
                            .put("msgh", "execute")
                            .put("args", new JsonObject()
                                .put("node", node)
                                .put("task", taskId));
                        vertx.eventBus().send("com.raaftech.structure.plugins." + address, message);

                        result.put("task", taskId);
                    } else {
                        result.put("error", "Called plugin isn't registered.");
                    }
                }

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                // TODO Auto-generated method stub
                return null;
            }

        };
    }
}
