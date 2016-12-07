package com.raaftech.structure.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.annotations.SchemaPath;
import com.raaftech.structure.base.Configuration;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.base.NodeActor;
import com.raaftech.structure.graph.Component;
import com.raaftech.structure.graph.Controller;
import com.raaftech.structure.graph.Instance;
import com.raaftech.structure.graph.Instrument;
import com.raaftech.structure.graph.repositories.ComponentRepository;
import com.raaftech.structure.graph.repositories.InstanceRepository;
import com.raaftech.structure.graph.repositories.InstrumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ComponentActor extends NodeActor<Component> {

    private static final InstanceRepository INSTANCE_REPOSITORY = Context.getInstanceRepository();
    private static final ComponentRepository componentRepository = Context.getComponentRepository();
    private static final InstrumentRepository instrumentRepository = Context.getInstrumentRepository();


    private Logger log = LoggerFactory.getLogger(ComponentActor.class);

    public ComponentActor(Component node) {
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
    @SchemaPath("schema/node/instance/getresults.schema.json")
    public MessageHandler getResults() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();
                JsonNode args = request.get("args");
                Component component = (Component)getNode();

                ArrayNode allResults = result.putArray("data");
                if (component != null) {
                    if (args.get("task") == null) {
                        component.getStartOfResultChain().getThisAndNewer().stream()
                        .forEachOrdered(r -> allResults.add(r.toJson()));
                    } else {
                        component.getResults(args.get("task").asText()).stream()
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
     * This method takes an Instance as argument and finds
     * child components of that Instance of a particular type.
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

                Set<Component> found = componentRepository.findChildrenByType(uuid, type);
                if (found != null && found.size() > 0) {
                    ArrayNode foundComponents = result.putArray("data");
                    found.stream()
                    .map(Instance::toJson)
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
     * Get all the instruments of the Instance this is called on and its components.
     * @return An array containing the UUID's of components that have instruments, with the
     * instruments categorized by capability type.
     */
    public MessageHandler getInstruments() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();

                Component structure = componentRepository.findByUuid(Context.getSelfIdentifier());
                String originUuid = getNode().getUuid();
                Component startOfComponentGroup = componentRepository.getStartOfComponentGroup(originUuid);

                if (structure == null) {
                    result.put("error", "Couldn't find structure node.");
                    return response(request, result);
                }

                Set<Controller> locallyRun = instrumentRepository.findLocalControllers(originUuid,
                        startOfComponentGroup.getUuid());
                Set<Controller> remotelyRun = instrumentRepository.findRemoteControllers(originUuid,
                        structure.getUuid());

                ObjectNode data = result.putObject("data");

//                locallyRun.stream()
//                        .forEach(instrument -> data.put(instrument.getName(), instrument.getUuid()));
//                remotelyRun.stream()
//                        .forEach(instrument -> data.put(instrument.getName(), instrument.getUuid()));

                for (Controller controller: locallyRun) {
                    data.put(controller.getName(), controller.getUuid());
                }
                for (Controller controller: remotelyRun) {
                    data.put(controller.getName(), controller.getUuid());
                }

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

    @SchemaPath("schema/node/instance/runInstrument.schema.json")
    @SuppressWarnings("unused")
    public MessageHandler runInstrument() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();
                JsonNode args = request.get("args");
                JsonNode instrumentArgs;
                Controller instrument = null;

                String instrumentId = args.get("instrument").asText();
                if (isUuid(instrumentId)) {
                    instrument = (Controller)instrumentRepository.findByUuid(instrumentId);
                } else {
                    // There is possibly more than one instrument that applies
                    // in that case we'll assume we're interested in the instrument
                    // that runs locally, since that'll be the most specific one.
                    Set<Instrument> foundInstruments = instrumentRepository.findByName(instrumentId);
                    if (foundInstruments.size() == 1) {
                        instrument = (Controller)foundInstruments.iterator().next();
                    } else {
                        // get the one that applies to this componental group
                        String originUuid = getNode().getUuid();
                        Instance startOfComponentGroup = INSTANCE_REPOSITORY
                                .getStartOfComponentGroup(originUuid);
                        if (startOfComponentGroup != null) {
                            instrument = instrumentRepository.findMatchingLocalController(originUuid,
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
                        if (args.has("timeAllocated")) {
                            int timeAllocated = args.get("timeAllocated").asInt();
                            ((Component) getNode()).runController(instrument, sanitized, taskId, timeAllocated);
                        } else {
                            ((Component) getNode()).runController(instrument, sanitized, taskId);
                        }
                        result.put("data", taskId);
                    } catch (RuntimeException e) {
                    	log.error(e.getMessage());

            			if (Configuration.isTraceModeOn()) {
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
