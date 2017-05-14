package com.raaftech.structure.bus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.annotations.SchemaPath;
import com.raaftech.structure.base.Configuration;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.exceptions.InstrumentExecutionException;
import com.raaftech.structure.graph.*;
import com.raaftech.structure.graph.repositories.ComponentRepository;
import com.raaftech.structure.graph.repositories.InstanceRepository;
import com.raaftech.structure.graph.repositories.InstrumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class InstanceActor extends NodeActor<Instance> {

    private static final InstanceRepository INSTANCE_REPOSITORY = Context.getInstanceRepository();
    private static final ComponentRepository componentRepository = Context.getComponentRepository();
    private static final InstrumentRepository instrumentRepository = Context.getInstrumentRepository();
    private static final InstanceRepository instanceRepository = Context.getInstanceRepository();


    private Logger log = LoggerFactory.getLogger(InstanceActor.class);

    public InstanceActor(Instance node) {
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
    @SchemaPath("resource:/schema/node/instance/getresults.schema.json")
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
    @SchemaPath("resource:/schema/node/instance/getcomponentsbytype.schema.json")
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

                Instance structure = INSTANCE_REPOSITORY.findByUuid(Context.getSelfIdentifier());
                String originUuid = getNode().getUuid();
                Instance startOfComponentGroup = INSTANCE_REPOSITORY.getStartOfComponentGroup(originUuid);

                if (structure == null) {
                    result.put("error", "Couldn't find structure node.");
                    return response(request, result);
                }

                Set<Controller> locallyRun = instrumentRepository.findLocalControllers(originUuid);
                Set<Controller> remotelyRun = instrumentRepository.findRemoteControllers(originUuid,
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

    @SchemaPath("resource:/schema/node/instance/runInstrument.schema.json")
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
                        // get the one that applies to this component group
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
                    try {
                        UUID taskId = UUID.randomUUID();
                        runController(instrument, taskId, args);
                        result.put("data", taskId.toString());
                    } catch (InstrumentExecutionException e) {
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

    /*
     * Given a capability try to run an implementation (controller) of that capability on this node.
     * We can be passed a "grouping" as argument, which is needed to run a controller. If this is not provided we'll
     * try to retrieve it when we need it.
     * A "taskId" (UUID string) might be provided as well, in case it was pre-generated.
     * Other arguments are the same as for "runInstrument", and they'll simply be passed along to the instrument runner.
     */

    public MessageHandler getCapabilities() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();

                Instance structure = INSTANCE_REPOSITORY.findByUuid(Context.getSelfIdentifier());
                String originUuid = getNode().getUuid();

                if (structure == null) {
                    result.put("error", "Couldn't find structure node.");
                    return response(request, result);
                }

                Set<Type> capabilities = instrumentRepository.findCapabilities(originUuid);

                ObjectNode data = result.putObject("data");

                capabilities.stream()
                        .forEach(capability -> data.put(capability.getName(), capability.getUuid()));

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }


    @SchemaPath("resource:/schema/node/instance/runCapability.schema.json")
    @SuppressWarnings("unused")
    public MessageHandler runCapability() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                final ObjectNode result = mapper.createObjectNode();
                final JsonNode args = request.get("args");
                final String capability = args.get("capability").asText();
                final Optional<String> maybeGrouping = args.has("groupingUuid") ?
                        Optional.of(args.get("groupingUuid").asText()) :
                        Optional.empty();
                final UUID taskId = args.has("taskId") ? UUID.fromString(args.get("taskId").asText()) : UUID.randomUUID();

                final Set<Controller> controllersForCapability;
                final String groupingUuid;
                // If we have a groupingUuid already; use it. Else determine the groupingUuid
                if (maybeGrouping.isPresent()) {
                    groupingUuid = maybeGrouping.get();
                } else {
                    final Grouping grouping = instanceRepository.getStartOfComponentGroup(getNode().getUuid());
                    if (grouping == null) {
                        // We cannot find the grouping this node belongs to so we can't run an instrument on it
                        result.put("error", "We couldn't find the grouping which node "
                                + getNode().getUuid() + " belongs to.");
                        return response(request, result);
                    }
                    groupingUuid = grouping.getUuid();
                }

                controllersForCapability = instrumentRepository
                        .findControllersPerCapability(getNode().getUuid(), capability, groupingUuid);

                // We might have multiple controllers for a given Capability, in that case use the heaviest
                Optional<Controller> maybeHeaviestController = controllersForCapability.stream()
                        .max((controller1, controller2) -> Double.compare(controller1.getWeight(), controller2.getWeight()));

                if (!maybeHeaviestController.isPresent()) {
                    result.put("error", "Couldn't find heaviest controller for capability");
                    return response(request, result);
                }

                try {
                    result.put("data", runController(maybeHeaviestController.get(), taskId, args).toString());
                } catch (InstrumentExecutionException e) {
                    log.error(e.getMessage());
                    if (Configuration.isTraceModeOn()) {
                        e.printStackTrace();
                    }
                    result.put("error", e.getMessage());
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
     * Run a controller on this node.
     * @param controller The controller to run.
     * @param taskId The task ID for the generated task.
     * @param args The arguments passed to either "runController" or "runInstrument".
     * @return The given taskId.
     * @throws InstrumentExecutionException
     */
    private UUID runController(Controller controller, UUID taskId, JsonNode args) throws InstrumentExecutionException {
        final ObjectNode arguments = (ObjectNode) args.get("arguments");
        // Check if there is a timeAllocation field specified. If so, get the value there. Else the default value is used.
        vertx.executeBlocking(future -> {
            if (args.has("timeAllocated")) {
                getNode().runController(controller, arguments, taskId.toString(), args.get("timeAllocated").asLong());
            } else {
                getNode().runController(controller, arguments, taskId.toString());
            }
            future.complete();
        }, res -> {
            if (res.failed()) {
                if (Configuration.isTraceModeOn()) {
                    res.cause().printStackTrace();
                }
                log.error(res.cause().getMessage());
            } else {
                log.debug("RunController for " + controller.getName() + " and task ID " + taskId.toString() + " finished");
            }
        });

        return taskId;
    }
}
