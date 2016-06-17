package com.raaftech.structure.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.base.GenericActor;
import com.raaftech.structure.graph.InstanceNode;
import com.raaftech.structure.graph.repositories.InstanceNodeRepository;
import com.raaftech.structure.helper.InstrumentRunner;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

import java.util.*;

/**
 * Give a description of ServiceStatusManager here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2016-06-08
 */

public class ServiceStatusManager extends GenericActor {

    // Used to keep track of services that are already being checked
    final static Map<String, Long> checkedServices = new HashMap<>();

    // statusAddress is used to push notifications of events within Structure
    // that the GUI may be interested about.
    private static final String statusAddress = "com.raaftech.structure.status";

    // The prefix used to send messages to instancenodes on the eventbus
    private static final String eventBusInstanceNodePrefix = "com.raaftech.structure.node.instance.";

    private static final InstanceNodeRepository instanceNodeRepository =
            Context.INSTANCE.getInstanceNodeRepository();

    // Publish a status on the status channel
    protected void publishStatus(String event, JsonNode data) {
        EventBus eventBus = getVertx().eventBus();
        eventBus.publish(statusAddress,
                new JsonObject("{\"source\":\"" + this.getClass().getSimpleName() + "\", "
                        + "\"event\": \"" + event + "\", \"data\": " + new JsonObject(data.toString()) + "}"));
    }


    @SuppressWarnings("unused")
    public MessageHandler startChecking() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                if (Context.INSTANCE.getInstrumentsLoaded()) {
                    Vertx vertx = getVertx();
                    EventBus eventBus = vertx.eventBus();
                    Set<InstanceNode> httpServers = instanceNodeRepository.getHttpServers();

                    httpServers.stream()
                            .forEach(httpd -> {
                                String uuid = httpd.getUuid();
                                if (checkedServices.get(uuid) == null) {
                                    InstanceNode origin = instanceNodeRepository.findByUuid(uuid);
                                    InstanceNode startOfGroup = instanceNodeRepository.getStartOfComponentGroup(uuid);
                                    InstanceNode instrument = instanceNodeRepository.findMatchingLocalInstrument(uuid, startOfGroup.getUuid(), "check");
                                    InstanceNode structure = instanceNodeRepository.findOneByName(Context.selfIdentifier);
                                    if (instrument != null && structure != null) {
                                        InstrumentRunner instrumentRunner = new InstrumentRunner(origin, instrument, mapper.createObjectNode(), UUID.randomUUID().toString());
                                        Long id = vertx.setPeriodic(5000, handler -> instrumentRunner.startInstrument());
                                        checkedServices.put(uuid, id);
                                    }
                                }
                            });
                }

                return response(request, mapper.createObjectNode());
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

    @SuppressWarnings("unused")
    public MessageHandler stopChecking() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                checkedServices.entrySet().stream()
                        .map(entry -> entry.getValue())
                        .forEach(id -> {
                            vertx.cancelTimer(id);

                        });

                checkedServices.clear();

                return response(request, mapper.createObjectNode());
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

}
