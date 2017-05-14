package com.raaftech.structure.actor.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.annotations.SchemaPath;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.actor.GenericActor;
import com.raaftech.structure.graph.Grouping;
import com.raaftech.structure.graph.repositories.InstanceRepository;
import com.raaftech.structure.graph.repositories.InstrumentRepository;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.*;

/**
 * Give a description of InstrumentManager here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2016-06-08
 */

public class InstrumentManager extends GenericActor {

    private final static Logger log = LoggerFactory.getLogger(InstrumentManager.class);
    private final InstrumentRepository instrumentRepository = Context.getInstrumentRepository();
    private final InstanceRepository instanceRepository = Context.getInstanceRepository();

    private void loadInstruments() {
        // TODO: entity, does throwing away + loading bundle recreate instruments?
//        // Load all instruments
//        if (System.getProperty("instruments") == null) {
//            System.setProperty("instruments", new File("ht").getAbsolutePath());
//        }
//        InstrumentLoader instrumentLoader = new InstrumentLoader();
//        String instruments = System.getProperty("instruments");
//        Arrays.asList(instruments.split(",")).stream()
//                .map(Paths::get)
//                .distinct()
//                .forEach(instrumentLoader::load);
//        Context.setInstrumentsLoaded(true);
    }

    private void purgeInstruments() {
        log.info("Purging instruments.");
        instrumentRepository.purgeInstruments();
        Context.setInstrumentsLoaded(false);
    }

    @SuppressWarnings("unused")
    public MessageHandler reloadInstruments() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();

                purgeInstruments();
                // TODO: this needs to be a vertx thread
                new Thread()
                {
                    public void run() {
                        loadInstruments();
                    }
                }.run();

                result.put("data", "Success");
                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

    @SuppressWarnings("unused")
    public MessageHandler removeInstruments() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                 ObjectNode result = mapper.createObjectNode();

                purgeInstruments();

                result.put("data", "Success");
                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

    /*
     * This MessageHandler is used to dispatch the invocation of a capability on multiple instances.
     * We'll simply get one of the UUIDs, determine the grouping it belongs to, assume all UUIDs that are passed
     * belong to that grouping, create a taskId and then send a message containing the capability, the arguments and
     * the taskId to those instances' "runCapability" handler.
     *
     * IMPORTANT: Take note that we assume all UUIDs belong to the same grouping.
     */
    @SchemaPath("resource:/schema/manager/instrumentmanager/runCapability.schema.json")
    @SuppressWarnings("unused")
    public MessageHandler runCapability() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                final String instanceActorFormat = "com.raaftech.structure.node.%s";
                final JsonNode args = request.get("args");
                final ObjectNode result = mapper.createObjectNode();
                final ArrayNode uuids = (ArrayNode) args.get("uuids");
                final Grouping grouping = instanceRepository.getStartOfComponentGroup(uuids.elements().next().asText());

                if (grouping == null) {
                    result.put("error", "Error while finding start of group. Couldn't call capability on instances.");
                    return response(request, result);
                }

                final String groupingUuid = grouping.getUuid();
                final UUID taskId = UUID.randomUUID();

                // Construct the message to pass to the instances
                final JsonObject message = new JsonObject()
                        .put("msgh", "runCapability")
                        .put("args", new JsonObject()
                            .put("capability", args.get("capability").asText())
                            .put("grouping", groupingUuid)
                            .put("taskId", taskId.toString()));
                if (args.has("arguments")) {
                    message.getJsonObject("args")
                            .put("arguments", new JsonObject(args.get("arguments").toString()));
                }
                if (args.has("timeAllocated")) {
                    message.getJsonObject("args")
                            .put("timeAllocated", args.get("timeAllocated").asInt());
                }

                // For every UUID send the message to its address on the bus
                for (final Iterator<JsonNode> iter = uuids.elements(); iter.hasNext();) {
                    final UUID uuid = UUID.fromString(iter.next().asText());
                    final String address = String.format(instanceActorFormat, uuid);
                    log.info("Sending message to " + address);
                    vertx.eventBus().send(address, message, handler -> {
                        if (handler.failed()) {
                            log.error("Error while sending message to "
                                    + address + ": " + handler.cause().getMessage());
                        }
                    });
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
