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

    private InstanceNodeRepository instanceNodeRepository = Context.getInstanceNodeRepository();

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
                String originUuid = getNode().getUuid();
                InstanceNode startOfComponentGroup = instanceNodeRepository.getStartOfComponentGroup(originUuid);

                if (structure == null) {
                    result.put("error", "Couldn't find structure node.");
                    return response(request, result);
                }

                Set<InstanceNode> locallyRun = instanceNodeRepository.findLocalInstruments(originUuid,
                        startOfComponentGroup.getUuid());
                Set<InstanceNode> remotelyRun = instanceNodeRepository.findRemoteInstruments(originUuid,
                        structure.getUuid());

                ObjectNode data = result.putObject("data");

                locallyRun.stream()
                        .forEach(instrument -> data.put(instrument.getName(), instrument.getUuid()));
                remotelyRun.stream()
                        .forEach(instrument -> data.put(instrument.getName(), instrument.getUuid()));

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
                InstanceNode instrument = null;

                String instrumentId = args.get("instrument").asText();
                if (isUuid(instrumentId)) {
                    instrument = instanceNodeRepository.findByUuid(instrumentId);
                } else {
                    // There is possibly more than one instrument that applies
                    // in that case we'll assume we're interested in the instrument
                    // that runs locally, since that'll be the most specific one.
                    Set<InstanceNode> foundInstruments = instanceNodeRepository.findByName(instrumentId);
                    if (foundInstruments.size() == 1) {
                        instrument = foundInstruments.iterator().next();
                    } else {
                        // get the one that applies to this componental group
                        String originUuid = getNode().getUuid();
                        InstanceNode startOfComponentGroup = instanceNodeRepository
                                .getStartOfComponentGroup(originUuid);
                        if (startOfComponentGroup != null) {
                            instrument = instanceNodeRepository.findMatchingLocalInstrument(originUuid,
                                    startOfComponentGroup.getUuid(), instrumentId);
                        }
                    }
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

            			if (Boolean.parseBoolean(System.getProperty("traceMode"))) {
            				e.printStackTrace();
            			}
            			
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
