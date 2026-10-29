package tech.raaf.structure.bus.manager;

import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.raaf.structure.annotations.SchemaPath;
import tech.raaf.structure.base.Application;
import tech.raaf.structure.base.Configuration;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.bus.GenericActor;
import tech.raaf.structure.bus.NodeActor;
import tech.raaf.structure.graph.Component;
import tech.raaf.structure.graph.Group;
import tech.raaf.structure.graph.repositories.*;
import tech.raaf.structure.helper.ProcessRunner;
import tech.raaf.structure.helper.handlebars.Quote;
import tech.raaf.structure.helper.loader.BundleFiles;
import tech.raaf.structure.helper.loader.BundleLoader;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public class StructureManager extends GenericActor {

  private Logger log = LoggerFactory.getLogger(StructureManager.class);

  NodeRepository nodeRepository = Context.getNodeRepository();
  TypeRepository typeRepository = Context.getTypeRepository();
  GroupingRepository groupingRepository = Context.getGroupingRepository();
  ResultRepository resultRepository = Context.getResultRepository();
  ComponentRepository componentRepository = Context.getComponentRepository();
  InstrumentRepository instrumentRepository = Context.getInstrumentRepository();

  public MessageHandler discover() {
    return new MessageHandler() {
      @Override
      public JsonNode exec(JsonNode request) {
        return response(request, mapper.createObjectNode().set("data", actorMetadata()));
      }

      @Override
      public JsonNode help(JsonNode request) {
        return jsonFromResource("help/structuremanager.discover.json");
      }
    };
  }

  public MessageHandler list() {
    return new MessageHandler() {
      public JsonNode exec(JsonNode request) {
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        ArrayNode data = result.putArray("data");

        vertx.deploymentIDs().stream().forEach(data::add);

        return response(request, result);
      }

      public JsonNode help(JsonNode request) {
        return jsonFromResource("help/structuremanager.list.json");
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
        // Unordered, so that the reinit does not stop the message handlers
        vertx
          .executeBlocking(() -> {
            Application.init(vertx);
            return null;
          }, false)
          .onComplete(res -> {
            publishStatus("structureReinitialization", mapper.createObjectNode().put("data", "finished"));
            log.info("Successfully reinitialized the database");
          });
        log.info("Started database reinitialization");

        return response(request, result);
      }

      @Override
      public JsonNode help(JsonNode request) {
        return jsonFromResource("help/structuremanager.reinitialize.json");
      }
    };
  }

  @SuppressWarnings("unused")
  @SchemaPath("resource:/schema/manager/structuremanager/selfdiscovery.schema.json")
  public MessageHandler selfDiscovery() {
    return new MessageHandler() {
      @Override
      public JsonNode exec(JsonNode request) {
        final ObjectNode result = mapper.createObjectNode();

        // Run discovery jobs in order while message handlers continue.
        vertx
          .executeBlocking(() -> {
            discoverHost();
            return null;
          }, true)
          .onComplete(resultHandler -> {
            if (resultHandler.failed()) {
              if (Configuration.isTraceModeOn()) {
                resultHandler.cause().printStackTrace();
              }
              log.error(resultHandler.cause().getMessage());
              publishStatus(
                "selfDiscovery",
                mapper.createObjectNode().put("error", resultHandler.cause().getMessage())
              );
            } else {
              publishStatus("selfDiscovery", mapper.createObjectNode().put("status", "done"));
            }
          });

        return response(request, result.put("data", "started"));
      }

      @Override
      public JsonNode help(JsonNode request) {
        return jsonFromResource("help/structuremanager.selfdiscovery.json");
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
        return jsonFromResource("help/structuremanager.reload.json");
      }
    };
  }

  private void discoverHost() {
    log.debug("Hostname is \"{}\"", Context.getHostname());
    Component host = Context.getInstanceRepository().findDnsName(Context.getHostname());
    if (host == null) {
      log.info("Structure is not registered in the database, now discovering itself");
      // Bootstrap runs directly until the host has a graph identity.
      final Path bootstrapLocation = BundleFiles.resolve("03a764f2-a41f-47bb-bbe6-f28df84af398", "payload/main.js");
      if (!Files.exists(bootstrapLocation)) {
        final String error = "Bootstrap program not found at location \"" + bootstrapLocation + "\"";
        log.error(error);
        throw new RuntimeException(error);
      }
      ProcessRunner runner = new ProcessRunner(
        "node " + Quote.SH.quote(bootstrapLocation.toString()) + " -c -h " + Quote.SH.quote(Context.getHostname())
      );
      try {
        runner.run();
        int scriptFinishedWaitCycles = 0;
        while (runner.isRunning() && scriptFinishedWaitCycles < 10) {
          Thread.sleep(1000);
          scriptFinishedWaitCycles++;
        }
        ProcessRunner.ProcessResult result = runner.finish();
        result.stdout.ifPresent(System.out::println);
        if (result.returnCode.orElse(1) != 0) {
          throw new IllegalStateException("Bootstrap failed: " + result.stderr.orElse("No error output"));
        }
      } catch (IOException | InterruptedException e) {
        if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        throw new IllegalStateException("Bootstrap failed", e);
      }
      // Wait a second so debug messages are done
      int newNodesWaitCycles = 0;
      Group structureInstance = null;
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
        structureInstance =
          structureInstance == null && host != null
            ? Context.getInstanceRepository().getStartOfComponentGroup(host.getUuid())
            : structureInstance;
      }
      if (host != null && structureInstance != null) {
        Context.setSelfGroup(structureInstance);
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
        return jsonFromResource("help/structuremanager.status.json");
      }
    };
  }
}
