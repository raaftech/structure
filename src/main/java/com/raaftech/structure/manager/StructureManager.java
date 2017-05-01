package com.raaftech.structure.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.raaftech.structure.base.Application;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.base.GenericActor;
import com.raaftech.structure.base.NodeActor;
import com.raaftech.structure.graph.repositories.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StructureManager extends GenericActor {

    private Logger log = LoggerFactory.getLogger(StructureManager.class);

    NodeRepository nodeRepository = Context.getNodeRepository();
    TypeRepository typeRepository = Context.getTypeRepository();
    GroupingRepository groupingRepository = Context.getGroupingRepository();
    ResultRepository resultRepository = Context.getResultRepository();
    ConfigurationRepository configurationRepository = Context.getConfigurationRepository();
    ComponentRepository componentRepository = Context.getComponentRepository();
    InstrumentRepository instrumentRepository = Context.getInstrumentRepository();

    public MessageHandler list() {
        return new MessageHandler() {
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                ArrayNode data = result.putArray("data");

                vertx.deploymentIDs().stream()
                        .forEach(data::add);

                return response(request, result);
            }

            public JsonNode help(JsonNode request) {
                return jsonFromResource("help/busmanager.list.json");
            }
        };
    }

    public MessageHandler reinitialize() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                log.info("Starting reinitialization of Structure");
                publishStatus("structureReinitialization", mapper.createObjectNode().put("data", "started"));
                // First undeploy all the NodeActors
                final JsonObject message = new JsonObject().put("msgh", "undeploy");
                LocalMap<String, String> nodeActors = vertx.sharedData().getLocalMap(NodeActor.SHAREDMAP_ID);
                nodeActors.keySet().stream()
                        .forEach(address -> vertx.eventBus().send(address, message));
                log.info("Undeployed all NodeActors");

                // Then purge the database
                nodeRepository.purge();
                log.info("Purged database");

                // Then reinit
                vertx.executeBlocking(future -> {
                    Application.init(vertx);
                    future.complete();
                }, res -> {
                    publishStatus("structureReinitialization", mapper.createObjectNode().put("data", "finished"));
                    log.info("Successfully reinitialized the database");
                });
                log.info("Started database reinitialization");

                return response(request, mapper.createObjectNode().set("data", mapper.valueToTree(nodeActors.keySet())));
            };

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

    public MessageHandler status() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();
                ObjectNode graph = result.putObject("graph");
                graph.put("types", typeRepository.count());
                graph.put("groupings", groupingRepository.count());
                graph.put("results", resultRepository.count());
                graph.put("configurations", configurationRepository.count());
                graph.put("components", componentRepository.count());
                graph.put("instruments", instrumentRepository.count());

                ObjectNode jvm = result.putObject("jvm");
                Runtime runtime = Runtime.getRuntime();
                jvm.put("java_version", System.getProperty("java.specification.version"));
                jvm.put("version", System.getProperty("java.vm.version"));
                jvm.put("available_processors", runtime.availableProcessors());
                ObjectNode memory = jvm.putObject("memory");
                memory.put("max", runtime.maxMemory());
                memory.put("total", runtime.totalMemory());
                memory.put("free", runtime.freeMemory());
                memory.put("used", runtime.totalMemory() - runtime.freeMemory());

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }
}
