package com.raaftech.structure.bus.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.annotations.SchemaPath;
import com.raaftech.structure.base.Application;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.bus.GenericActor;
import com.raaftech.structure.bus.NodeActor;
import com.raaftech.structure.graph.Component;
import com.raaftech.structure.graph.Instance;
import com.raaftech.structure.graph.repositories.*;
import com.raaftech.structure.helper.ProcessRunner;
import com.raaftech.structure.helper.loader.BundleLoader;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class StructureManager extends GenericActor {

    private Logger log = LoggerFactory.getLogger(StructureManager.class);

    NodeRepository nodeRepository = Context.getNodeRepository();
    TypeRepository typeRepository = Context.getTypeRepository();
    GroupingRepository groupingRepository = Context.getGroupingRepository();
    ResultRepository resultRepository = Context.getResultRepository();
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

    /**
     * Reinitialize the complete database. This means undeploying all NodeActor verticles,
     * purging the database from all vertices and edges and then reloading the bundles.
     * @return All the NodeActor addresses that are being undeployed.
     */
    @SuppressWarnings("unused")
    public MessageHandler reinitialize() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();

                log.info("Starting reinitialization of Structure");
                publishStatus("structureReinitialization", mapper.createObjectNode().put("data", "started"));
                // First undeploy all the NodeActors
                final JsonObject message = new JsonObject().put("msgh", "undeploy");
                LocalMap<String, String> nodeActors = vertx.sharedData().getLocalMap(NodeActor.SHAREDMAP_ID);
                nodeActors.keySet().forEach(address -> vertx.eventBus().send(address, message));
                // Add the undeployed NodeActor addresses to the result
                result.set("data", mapper.valueToTree(nodeActors.keySet()));
                // Empty the map containing the loaded NodeActors
                nodeActors.clear();
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

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

    @SuppressWarnings("unused")
    @SchemaPath("resource:/schema/manager/structuremanager/selfdiscovery.schema.json")
    public MessageHandler selfDiscovery() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                JsonNode args = request.get("args");
                final ObjectNode result = mapper.createObjectNode();
                final String token = args.get("token").asText();

                vertx.executeBlocking(
                        future -> {
                            selfDiscovery(token);
                            future.complete();
                },
                        resultHandler -> {
                            if (resultHandler.failed()) {
                                resultHandler.cause().printStackTrace();
                                log.error(resultHandler.cause().getMessage());
                                publishStatus("selfDiscovery",
                                        mapper.createObjectNode().put("error", resultHandler.cause().getMessage()));
                            } else {
                                publishStatus("selfDiscovery", mapper.createObjectNode().put("status", "done"));
                            }
                        });

                return response(request, result.put("data", token));
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

    @SuppressWarnings("unused")
    public MessageHandler reload() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();
                log.info("Reloading Type entities of Structure");

                try {
                    BundleLoader loader = new BundleLoader();
                    loader.loadTypes();
                    loader.loadInstances();
                    result.put("data", "Structure reloaded");
                } catch (Exception e) {
                    e.printStackTrace();
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

    private void selfDiscovery(String token) {
        // TODO: refactor, ugly as hell, but i had it ready so i'll reuse it.
        log.debug("Hostname is \"{}\"", Context.getHostname());
        Component host = Context.getInstanceRepository().findDnsName(Context.getHostname());
        if (host == null) {
            log.info("Structure is not registered in the database, now discovering itself");
            ProcessRunner runner = new ProcessRunner("node tools/bootstrap/bootstrap.js -c -h "+ Context.getHostname() +" -t " + token);
            try {
                runner.run();
                int scriptFinishedWaitCycles = 0;
                while (runner.isRunning() && scriptFinishedWaitCycles < 10) {
                    Thread.sleep(1000);
                    scriptFinishedWaitCycles++;
                }
                ProcessRunner.ProcessResult result = runner.finish();
                System.out.println(result.stdout.get());
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            // Wait a second so debug messages are done
            int newNodesWaitCycles = 0;
            Instance structureInstance = null;
            while (host == null && structureInstance == null && newNodesWaitCycles < 5) {
                if (newNodesWaitCycles == 0) {
                    log.info("Waiting for registration...");
                } else {
                    log.info("...");
                }
                newNodesWaitCycles++;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                }
                host = host == null ? Context.getInstanceRepository().findDnsName(Context.getHostname()) : host;
                structureInstance = structureInstance == null && host != null ?
                        Context.getInstanceRepository().getStartOfComponentGroup(host.getUuid()) : structureInstance;
            }
            if (host != null && structureInstance != null) {
                Context.setSelfIdentifier(structureInstance.getUuid());
            } else {
                throw new RuntimeException("Self discovery script didn't create any new nodes.");
            }
        } else {
            log.info("Structure was already registered in the database");
        }
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

                ObjectNode structureInfo = result.putObject("structure");
                boolean bootstrapped = Context.getInstanceRepository().findDnsName(Context.getHostname()) != null;
                structureInfo.put("bootstrapped", bootstrapped);
                structureInfo.put("host", Context.getHostname());

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }
}
