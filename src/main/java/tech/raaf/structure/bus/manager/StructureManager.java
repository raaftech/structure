package tech.raaf.structure.bus.manager;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
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

  // The bootstrap instrument, the time that it can take, and the bus
  // address of the TaskManager that runs it.
  private static final String BOOTSTRAP = "03a764f2-a41f-47bb-bbe6-f28df84af398";
  private static final long BOOTSTRAP_TIME = 60000L;
  private static final String TASKS = TaskManager.class.getName().toLowerCase();

  @SchemaPath(output = "resource:/schema/output/introspection.schema.json")
  public MessageHandler introspect() {
    return new MessageHandler() {
      @Override
      public JsonNode exec(JsonNode request) {
        return response(request, mapper.createObjectNode().set("data", actorMetadata()));
      }

      @Override
      public JsonNode help(JsonNode request) {
        return jsonFromResource("help/structuremanager.introspect.json");
      }
    };
  }

  @SchemaPath(output = "resource:/schema/output/stringList.schema.json")
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
  @SchemaPath(output = "resource:/schema/output/stringList.schema.json")
  public MessageHandler reinitialize() {
    return new MessageHandler() {
      @Override
      public JsonNode exec(JsonNode request) {
        ObjectNode result = mapper.createObjectNode();

        log.info("Starting reinitialization of Structure");
        publishEvent("structureReinitialization", mapper.createObjectNode().put("data", "started"));
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
            publishEvent("structureReinitialization", mapper.createObjectNode().put("data", "finished"));
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

  @SchemaPath(
    input = "resource:/schema/manager/structuremanager/selfDiscovery.schema.json",
    output = "resource:/schema/output/string.schema.json"
  )
  public MessageHandler selfDiscovery() {
    return new MessageHandler() {
      @Override
      public JsonNode exec(JsonNode request) {
        final ObjectNode result = mapper.createObjectNode();

        // The discovery runs while message handlers continue.
        discoverHost()
          .onComplete(resultHandler -> {
            if (resultHandler.failed()) {
              if (Configuration.isTraceModeOn()) {
                resultHandler.cause().printStackTrace();
              }
              log.error(resultHandler.cause().getMessage());
              publishEvent(
                "selfDiscovery",
                mapper.createObjectNode().put("error", resultHandler.cause().getMessage())
              );
            } else {
              publishEvent("selfDiscovery", mapper.createObjectNode().put("status", "done"));
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

  @SchemaPath(output = "resource:/schema/output/string.schema.json")
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

  @SchemaPath(output = "resource:/schema/output/info.schema.json")
  public MessageHandler info() {
    return new MessageHandler() {
      @Override
      public JsonNode exec(JsonNode request) {
        ObjectNode data = mapper.createObjectNode();
        ObjectNode structure = data.putObject("structure");
        structure.put("bootstrapped", Context.getSelfGroup() != null);
        structure.put("host", Context.getHostname());

        ObjectNode graph = data.putObject("graph");
        graph.put("types", typeRepository.count());
        graph.put("groups", groupingRepository.count());
        graph.put("results", resultRepository.count());
        graph.put("components", componentRepository.count());
        graph.put("instruments", instrumentRepository.count());

        ObjectNode jvm = data.putObject("jvm");
        Runtime runtime = Runtime.getRuntime();
        jvm.put("javaVersion", Runtime.version().feature());
        jvm.put("vmVersion", Runtime.version().toString());
        jvm.put("processors", runtime.availableProcessors());
        ObjectNode memory = jvm.putObject("memory");
        memory.put("max", runtime.maxMemory());
        memory.put("total", runtime.totalMemory());
        memory.put("free", runtime.freeMemory());
        memory.put("used", runtime.totalMemory() - runtime.freeMemory());

        return response(request, mapper.createObjectNode().set("data", data));
      }

      @Override
      public JsonNode help(JsonNode request) {
        return jsonFromResource("help/structuremanager.info.json");
      }
    };
  }

  // A task to start, and the time that its jobs can take.
  private record Run(String taskId, long timeout, Supplier<Future<?>> start) {}

  // Bootstrap Structure if the graph has no host with its name, and then
  // discover the operating system of the host. Each runs as a task of the
  // TaskManager, thus each gives a result.
  private Future<Void> discoverHost() {
    final String hostname = Context.getHostname();
    return vertx
      .executeBlocking(() -> Context.getInstanceRepository().findDnsName(hostname) == null ? bootstrap(hostname) : null)
      .compose(this::run)
      .compose(bootstrapped -> vertx.executeBlocking(() -> osDiscovery(hostname)))
      .compose(this::run);
  }

  // The bootstrap registers the host. The origin of its run is the
  // Structure component that it makes, thus Structure gives its UUID.
  private Run bootstrap(String hostname) {
    log.info("Registering the Structure host");
    final Path payload = BundleFiles.resolve(BOOTSTRAP, "payload/main.py");
    if (!Files.isRegularFile(payload)) {
      throw new IllegalStateException("Bootstrap program not found: " + payload);
    }
    final String origin = UUID.randomUUID().toString();
    final String taskId = UUID.randomUUID().toString();
    final ObjectNode message = mapper.createObjectNode().put("msgh", "queueTask");
    final ObjectNode job = message
      .putObject("args")
      .put("taskId", taskId)
      .put("sequential", true)
      .putArray("jobs")
      .addObject()
      .put(
        "command",
        "python3 " + Quote.SH.quote(payload.toString()) + " -h " + Quote.SH.quote(hostname) + " -u " + Quote.SH.quote(origin)
      );
    job
      .putObject("metadata")
      .put("originName", "Structure")
      .put("originUuid", origin)
      .put("instrumentName", "StructureBootstrap")
      .put("instrumentUuid", BOOTSTRAP)
      .put("capability", "Detect")
      .put("timeAllocated", BOOTSTRAP_TIME);
    return new Run(taskId, BOOTSTRAP_TIME, () ->
      vertx
        .eventBus()
        .<JsonObject>request(TASKS, new JsonObject(message.toString()))
        .compose(reply -> {
          final JsonObject result = reply.body().getJsonObject("result", new JsonObject());
          return result.containsKey("error")
            ? Future.failedFuture(result.getString("error"))
            : Future.succeededFuture();
        })
    );
  }

  // Set the self group, and give the OS discovery of the host. Without
  // Ansible, there is no OS discovery.
  private Run osDiscovery(String hostname) {
    final Component host = Context.getInstanceRepository().findDnsName(hostname);
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
    // The bootstrap adds Ansible if it finds it, thus the graph tells
    if (
      os
        .getComponents()
        .stream()
        .noneMatch(component -> component.isOfType("Ansible"))
    ) {
      log.info("The Structure host has no Ansible; keeping the minimal host registration");
      return null;
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
    final String taskId = UUID.randomUUID().toString();
    final InstrumentRunner runner = new InstrumentRunner(address, controller, null, taskId);
    if (runner.getJobs().isEmpty()) {
      throw new IllegalStateException("No Ansible OS discovery instrument is available");
    }
    final long timeout = runner
      .getJobs()
      .stream()
      .mapToLong(job -> job.getRight().orElse(BOOTSTRAP_TIME))
      .sum();
    return new Run(taskId, timeout, () -> {
      runner.startInstrument();
      return Future.succeededFuture();
    });
  }

  // Start a task, and wait until the TaskManager finishes it. Fails if a
  // job did not end with exit code 0.
  private Future<Void> run(Run run) {
    if (run == null) {
      return Future.succeededFuture();
    }
    final Promise<Void> finished = Promise.promise();
    final MessageConsumer<JsonObject> events = vertx
      .eventBus()
      .consumer(EVENTS, message -> {
        if (
          "taskFinished".equals(message.body().getString("event")) &&
          message.body().getValue("data") instanceof JsonObject data &&
          run.taskId().equals(data.getString("taskId"))
        ) {
          final String error = failure(data.getJsonObject("taskObj"));
          if (error == null) {
            finished.tryComplete();
          } else {
            finished.tryFail(error);
          }
        }
      });
    final long timer = vertx.setTimer(run.timeout() + 30000L, id ->
      finished.tryFail("Task " + run.taskId() + " did not finish")
    );
    return events
      .completion()
      .compose(registered -> run.start().get())
      .compose(started -> finished.future())
      .onComplete(done -> {
        vertx.cancelTimer(timer);
        events.unregister();
      });
  }

  // The error of the first job that did not end with exit code 0, or null.
  private static String failure(JsonObject task) {
    for (Object item : task.getJsonArray("jobs", new JsonArray())) {
      final JsonObject job = (JsonObject) item;
      final JsonObject result = job.getJsonObject("result", new JsonObject());
      if (!"FINISHED".equals(job.getString("status")) || result.getInteger("returnCode", 1) != 0) {
        return "Discovery failed: " + result.getString("stderr", job.getString("error", "No error output")).strip();
      }
    }
    return null;
  }

  private void registerComponent(Component os, Component component) {
    Graph.transaction(() -> {
      component.save();
      final Entity entity = mapper.treeToValue(mapper.valueToTree(os), Entity.class);
      entity.addRelations("HAS", Direction.OUTGOING, Set.of(UUID.fromString(component.getUuid())));
      entity.toGraph();
    });
  }
}
