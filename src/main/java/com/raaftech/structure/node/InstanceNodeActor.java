package com.raaftech.structure.node;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.annotations.SchemaPath;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.base.NodeActor;
import com.raaftech.structure.graph.InstanceNode;
import com.raaftech.structure.graph.repositories.InstanceNodeRepository;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class InstanceNodeActor extends NodeActor<InstanceNode> {

    private InstanceNodeRepository instanceNodeRepository = Context.INSTANCE.getInstanceNodeRepository();

    private Logger log = LoggerFactory.getLogger(InstanceNodeActor.class);

    public InstanceNodeActor(InstanceNode node) {
        super(node);
    }

    @Override
    protected String getSecondaryAddress() {
        return null;
    }

    /**
     * Query the instancenode for results. Field "task" may
     * be given as an argument with a specific task ID to
     * filter on a particular task.
     * @return An array containing the queried results.
     */
    @SchemaPath("schema/node/instancenode/getresults.schema.json")
    public MessageHandler getResults() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();
                JsonNode args = request.get("args");
                InstanceNode instanceNode = getNode();

                ArrayNode allResults = result.putArray("data");
                if (instanceNode != null) {
                    if (args.get("task") == null) {
                        instanceNode.getResults().stream()
                        .forEachOrdered(r -> allResults.add(r.toJson()));
                    } else {
                        instanceNode.getResults(args.get("task").asText()).stream()
                        .forEachOrdered(r -> allResults.add(r.toJson()));
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
     * This method takes an InstanceNode as argument and finds
     * child components of that InstanceNode of a particular type.
     * @return An array containing the found components.
     */
    public MessageHandler getComponentsByType() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();
                JsonNode args = request.get("args");
                String type = args.get("type").asText();

                Set<InstanceNode> found = instanceNodeRepository.findComponentsByType(uuid, type);
                if (found != null && found.size() > 0) {
                    ArrayNode foundComponents = result.putArray("data");
                    found.stream()
                    .map(n -> n.toJson())
                    .forEach(json -> foundComponents.add(json));
                } else {
                    result.put("error", "Couldn't find components of type \"" + type + "\".");
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
     * Get all the handlers of the InstanceNode this is called on and its components.
     * @return An array containing the UUID's of components that have handlers, with the
     * handlers categorized by capability type.
     */
    public MessageHandler getHandlers() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();
                InstanceNode structure = instanceNodeRepository.findOneByName(Context.selfIdentifier);

                if (structure == null) {
                    result.put("error", "Couldn't find structure node.");
                    return response(request, result);
                }

                // handlerMap will contain the results and will eventually be marshalled to a JSON object.
                Map<String, Map<String, ArrayList<String>>> handlerMap = new LinkedHashMap<>();
                // Query all the applicable handlers in the subtree.
                List<InstanceNodeRepository.HandlerQueryResult> handlerList = instanceNodeRepository.getHandlers(uuid, structure.getUuid());

                // Construct our handlerMap with the results
                handlerList.forEach(hqr -> {
                    Map<String, ArrayList<String>> capabilityHandlerMap = handlerMap.get(hqr.limb.getUuid());

                    if (capabilityHandlerMap == null) {
                        capabilityHandlerMap = new LinkedHashMap<String, ArrayList<String>>();
                        handlerMap.put(hqr.limb.getUuid(), capabilityHandlerMap);
                    }

                    ArrayList<String> handlers = capabilityHandlerMap.get(hqr.capability.getName());
                    if (handlers == null) {
                        handlers = new ArrayList<>();
                        capabilityHandlerMap.put(hqr.capability.getName(), handlers);
                    }

                    handlers.add(hqr.handler.getName());

                });
                result.set("data", mapper.valueToTree(handlerMap));

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

    @SchemaPath("schema/node/instancenode/runHandler.schema.json")
    public MessageHandler runHandler() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();
                JsonNode args = request.get("args");
                JsonNode interactive = null;
                InstanceNode handler;

                String handlerId = args.get("handler").asText();
                if (isUuid(handlerId)) {
                    handler = instanceNodeRepository.findByUuid(handlerId);
                } else {
                    handler = instanceNodeRepository.findOneByName(handlerId);
                }
                if (handler != null) {

                    // Sanitize our input
                    ObjectNode sanitized = null;
                    if (args.has("interactive")) {
                        interactive = args.get("interactive");
                        sanitized = mapper.createObjectNode();
                        Iterator<Map.Entry<String, JsonNode>> iter = interactive.fields();
                        while (iter.hasNext()) {
                            Map.Entry<String, JsonNode> entry = iter.next();
                            String value = entry.getValue().asText();
                            value = value.replaceAll(" ", "\\\\ ");
                            value = value.replaceAll("-", "\\\\-");
                            sanitized.put(entry.getKey(), value);
                        }
                    }

                    try {
                        String resultUuid = getNode().runHandler(handler, sanitized);
                        result.put("data", resultUuid);
                    } catch (RuntimeException e) {
                        result.put("error", e.getMessage());
                    }
                } else {
                    result.put("error", "Couldn't find handler.");
                }

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }
}
