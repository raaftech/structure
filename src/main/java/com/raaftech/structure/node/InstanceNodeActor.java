package com.raaftech.structure.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.annotations.SchemaPath;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.base.NodeActor;
import com.raaftech.structure.graph.InstanceNode;
import com.raaftech.structure.graph.repositories.InstanceNodeRepository;
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
    @SuppressWarnings("unused")
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
                    .map(InstanceNode::toJson)
                    .forEach(foundComponents::add);
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
     * Get all the instruments of the InstanceNode this is called on and its components.
     * @return An array containing the UUID's of components that have instruments, with the
     * instruments categorized by capability type.
     */
    public MessageHandler getInstruments() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();
                InstanceNode structure = instanceNodeRepository.findOneByName(Context.selfIdentifier);

                if (structure == null) {
                    result.put("error", "Couldn't find structure node.");
                    return response(request, result);
                }

                // instrumentMap will contain the results and will eventually be marshalled to a JSON object.
                Map<String, Map<String, ArrayList<String>>> instrumentMap = new LinkedHashMap<>();
                // Query all the applicable instruments in the subtree.
                List<InstanceNodeRepository.InstrumentQueryResult> instrumentList = instanceNodeRepository.getInstruments(uuid, structure.getUuid());

                // Construct our instrumentMap with the results
                instrumentList.forEach(hqr -> {
                    Map<String, ArrayList<String>> capabilityInstrumentMap = instrumentMap.get(hqr.target.getUuid());

                    if (capabilityInstrumentMap == null) {
                        capabilityInstrumentMap = new LinkedHashMap<>();
                        instrumentMap.put(hqr.target.getUuid(), capabilityInstrumentMap);
                    }

                    ArrayList<String> instruments = capabilityInstrumentMap.get(hqr.capability.getName());
                    if (instruments == null) {
                        instruments = new ArrayList<>();
                        capabilityInstrumentMap.put(hqr.capability.getName(), instruments);
                    }

                    instruments.add(hqr.instrument.getName());

                });
                result.set("data", mapper.valueToTree(instrumentMap));

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

    @SchemaPath("schema/node/instancenode/runInstrument.schema.json")
    @SuppressWarnings("unused")
    public MessageHandler runInstrument() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();
                JsonNode args = request.get("args");
                JsonNode instrumentArgs;
                InstanceNode instrument;

                String instrumentId = args.get("instrument").asText();
                if (isUuid(instrumentId)) {
                    instrument = instanceNodeRepository.findByUuid(instrumentId);
                } else {
                    instrument = instanceNodeRepository.findOneByName(instrumentId);
                }
                if (instrument != null) {

                    // Sanitize our input
                    ObjectNode sanitized = null;
                    if (args.has("arguments")) {
                        instrumentArgs = args.get("arguments");
                        sanitized = mapper.createObjectNode();
                        Iterator<Map.Entry<String, JsonNode>> iter = instrumentArgs.fields();
                        while (iter.hasNext()) {
                            Map.Entry<String, JsonNode> entry = iter.next();
                            String value = entry.getValue().asText();
                            value = value.replaceAll(" ", "\\\\ ");
                            value = value.replaceAll("-", "\\\\-");
                            sanitized.put(entry.getKey(), value);
                        }
                    }

                    try {
                        String taskId = UUID.randomUUID().toString();
                        getNode().runInstrument(instrument, sanitized, taskId);
                        result.put("data", taskId);
                    } catch (RuntimeException e) {
                        log.error(e.getMessage());
                        result.put("error", e.getMessage());
                    }
                } else {
                    result.put("error", "Couldn't find instrument.");
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
