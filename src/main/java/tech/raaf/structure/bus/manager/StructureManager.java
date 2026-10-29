package tech.raaf.structure.bus.manager;

import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.raaf.structure.annotations.SchemaPath;
import tech.raaf.structure.base.Application;
import tech.raaf.structure.base.Configuration;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.bus.GenericActor;
import tech.raaf.structure.bus.NodeActor;
import tech.raaf.structure.graph.Component;
import tech.raaf.structure.graph.Controller;
import tech.raaf.structure.graph.Direction;
import tech.raaf.structure.graph.Entity;
import tech.raaf.structure.graph.Graph;
import tech.raaf.structure.graph.Group;
import tech.raaf.structure.graph.repositories.*;
import tech.raaf.structure.helper.InstrumentRunner;
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
    final String hostname = Context.getHostname();
    Component host = Context.getInstanceRepository().findDnsName(hostname);
    if (host == null) {
      log.info("Registering the Structure host");
      final Path bootstrap = BundleFiles.resolve("03a764f2-a41f-47bb-bbe6-f28df84af398", "payload/main.js");
      if (!Files.isRegularFile(bootstrap)) {
        throw new IllegalStateException("Bootstrap program not found: " + bootstrap);
      }
      runDiscovery("node " + Quote.SH.quote(bootstrap.toString()) + " -h " + Quote.SH.quote(hostname), 60000);
      host = Context.getInstanceRepository().findDnsName(hostname);
    }
    if (host == null) {
      throw new IllegalStateException("Bootstrap did not register the Structure host");
    }
    final Group group = Context.getInstanceRepository().getStartOfComponentGroup(host.getUuid());
    if (group == null) {
      throw new IllegalStateException("The Structure host has no component group");
    }
    Context.setSelfGroup(group);
    Context.setSelfIdentifier(group.getUuid());

    final Component os = Context.getInstanceRepository().findOperatingSystem(host.getUuid());
    if (os == null) {
      throw new IllegalStateException("The Structure host has no operating system");
    }
    if (!ansibleAvailable()) {
      log.info("Ansible is unavailable; keeping the minimal host registration");
      return;
    }
    if (
      os
        .getComponents()
        .stream()
        .noneMatch(component -> component.isOfType("Ansible"))
    ) {
      registerComponent(
        os,
        new Component(
          mapper
            .createObjectNode()
            .put("name", "Ansible")
            .put("desc", "The local Ansible installation")
            .put("ver", "unknown")
            .put("shortName", "ansible")
            .put("longName", "Ansible"),
          typeRepository.findOneByName("Ansible")
        )
      );
    }
    final Component address = os
      .getComponents()
      .stream()
      .filter(component -> component.isOfType("Ipv4Address"))
      .findFirst()
      .orElseGet(() -> {
        final Component local = new Component(
          mapper
            .createObjectNode()
            .put("name", "127.0.0.1")
            .put("desc", "The local IPv4 address")
            .put("addr", "127.0.0.1")
            .put("reachable", true),
          typeRepository.findOneByName("Ipv4Address")
        );
        registerComponent(os, local);
        return local;
      });
    final Controller controller = (Controller) instrumentRepository.findOneByName("OsDiscover");
    final InstrumentRunner runner = new InstrumentRunner(address, controller, null, UUID.randomUUID().toString());
    if (runner.getJobs().isEmpty()) {
      throw new IllegalStateException("No Ansible OS discovery instrument is available");
    }
    for (var job : runner.getJobs()) {
      runDiscovery(job.getLeft(), job.getRight().orElse(60000L));
    }
  }

  private boolean ansibleAvailable() {
    final ProcessRunner runner = new ProcessRunner("ansible --version");
    try {
      runner.run();
      if (!runner.getProcess().waitFor(5, TimeUnit.SECONDS)) {
        runner.cancel();
        throw new IllegalStateException("Ansible availability check timed out");
      }
      if (runner.finish().returnCode.orElse(1) != 0) {
        throw new IllegalStateException("Ansible availability check failed: " + runner.finish().stderr.orElse(""));
      }
      return true;
    } catch (IOException e) {
      return false;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Ansible availability check interrupted", e);
    }
  }

  private void registerComponent(Component os, Component component) {
    Graph.transaction(() -> {
      component.save();
      final Entity entity = mapper.treeToValue(mapper.valueToTree(os), Entity.class);
      entity.addRelations("HAS", Direction.OUTGOING, Set.of(UUID.fromString(component.getUuid())));
      entity.toGraph();
    });
  }

  private void runDiscovery(String command, long timeout) {
    final ProcessRunner runner = new ProcessRunner(command);
    try {
      runner.run();
      if (!runner.getProcess().waitFor(timeout, TimeUnit.MILLISECONDS)) {
        runner.cancel();
        throw new IllegalStateException("Discovery timed out");
      }
      final ProcessRunner.ProcessResult result = runner.finish();
      result.stdout.ifPresent(log::info);
      if (result.returnCode.orElse(1) != 0) {
        throw new IllegalStateException("Discovery failed: " + result.stderr.orElse("No error output"));
      }
    } catch (IOException e) {
      throw new IllegalStateException("Could not start discovery", e);
    } catch (InterruptedException e) {
      runner.cancel();
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Discovery interrupted", e);
    }
  }
}
