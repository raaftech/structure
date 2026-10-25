package tech.raaf.structure.base;

import ch.qos.logback.classic.Level;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import tech.raaf.structure.base.Configuration.Setting;
import tech.raaf.structure.bus.GenericActor;
import tech.raaf.structure.bus.InstanceActor;
import tech.raaf.structure.bus.TypeActor;
import tech.raaf.structure.bus.manager.EntityManager;
import tech.raaf.structure.bus.manager.InstrumentManager;
import tech.raaf.structure.bus.manager.SchemaManager;
import tech.raaf.structure.bus.manager.StructureManager;
import tech.raaf.structure.bus.manager.TaskManager;
import tech.raaf.structure.bus.repository.Instances;
import tech.raaf.structure.bus.repository.Nodes;
import tech.raaf.structure.bus.repository.Types;
import tech.raaf.structure.exceptions.GraphException;
import tech.raaf.structure.graph.Component;
import tech.raaf.structure.graph.Graph;
import tech.raaf.structure.graph.Group;
import tech.raaf.structure.graph.repositories.InstanceRepository;
import tech.raaf.structure.graph.repositories.TypeRepository;
import tech.raaf.structure.helper.ProcessRunner;
import tech.raaf.structure.helper.loader.BundleLoader;

/**
 * The main application entry point.
 *
 * @author Boy van Duuren <boy@raaf.tech>
 * @since 2014-11-17
 */
public class Application {

  private static final Logger log = LoggerFactory.getLogger(Application.class);

  // Make Application uninstantiable by hiding the public constructor
  private Application() {}

  /**
   * This is Structure's start of execution point.
   */
  public static void main(String[] args) throws InterruptedException {
    // Black magic.
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();

    try {
      Configuration.init(args);
      if (Configuration.get(Setting.HELP)) {
        Configuration.printUsage();
        return;
      }
      Configuration.check();
    } catch (ParseException | IllegalArgumentException e) {
      log.error(e.getMessage());
      Configuration.printUsage();
      System.exit(1);
    }

    ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
      Logger.ROOT_LOGGER_NAME
    );
    root.setLevel(Level.toLevel(Configuration.get(Setting.LOG_LEVEL), Level.INFO));
    log.info("Log level is {}", root.getLevel());

    if (Configuration.get(Setting.UNSAFE)) {
      log.info("UNSAFE mode enabled");
    }

    Vertx vertx;
    HttpServer httpServer;
    Router router;
    SockJSHandler sockJsHandler;

    // Tell the user we're starting up.
    log.info("Structure is starting up..");

    // Connect to the Neo4j server
    final String neo4jUri = Configuration.get(Setting.NEO4J_URI);
    log.info("Connecting to Neo4j at {}", neo4jUri);
    try {
      Graph.connect(neo4jUri, Configuration.get(Setting.NEO4J_USER), Configuration.get(Setting.NEO4J_PASS));
    } catch (GraphException e) {
      handleUnrecoverableLoadingError(e, 1, "Can't connect to Neo4j at " + neo4jUri + ": " + e.getMessage());
    }

    // Close the connection to Neo4j when Structure stops
    Runtime.getRuntime().addShutdownHook(
      new Thread(() -> {
        Graph.close();
        log.info("Closed the connection to Neo4j");
      })
    );

    // Setup vertx with the maximum worker run time, in ns.
    vertx = Vertx.vertx(new VertxOptions().setMaxWorkerExecuteTime(Configuration.get(Setting.WORKER_TIMEOUT)));

    Context.setVertx(vertx);

    // Enable bridging on /eventbus, and add some in- and outbound permissions.
    BridgeOptions bridgeOptions = new BridgeOptions()
      .addInboundPermitted(new PermittedOptions().setAddressRegex("tech\\.raaf\\.structure\\..*"))
      .addOutboundPermitted(new PermittedOptions().setAddressRegex("tech\\.raaf\\.structure\\..*"));

    // Setup httpServer, router and sockJs.
    httpServer = vertx.createHttpServer();
    sockJsHandler = SockJSHandler.create(vertx);
    sockJsHandler.bridge(bridgeOptions);
    router = Router.router(vertx);
    // One of the endpoints will be routed to SchemaManager, so instance it
    SchemaManager schemaManager = new SchemaManager();
    // Setup routes
    router.route("/eventbus/*").handler(sockJsHandler);
    router.route("/bundles/*").handler(StaticHandler.create().setWebRoot("src/main/resources/bundles/"));
    // The schema handler queries the graph, so it must not run on the event loop
    router.route("/schemas/:schema").blockingHandler(schemaManager::getSchema, false);
    router.route("/*").handler(StaticHandler.create().setWebRoot("src/main/resources/webroot/"));

    // If we're in unsafe mode allow direct access to everything
    // on the eventbus and serve the debug page.
    if (Configuration.get(Setting.UNSAFE)) {
      log.debug("Setting InboundPermitted and OutboundPermitted to any address (\".*\")");
      bridgeOptions.addInboundPermitted(new PermittedOptions().setAddressRegex(".*"));
      bridgeOptions.addOutboundPermitted(new PermittedOptions().setAddressRegex(".*"));
    }

    // Setup indexes and constraints
    log.info("Creating indices.");
    createSchema();

    // Deploy our main actors. Structure stops if one of them fails.
    // The actors that use the graph run on worker threads.
    deploy(vertx, new StructureManager(), GenericActor.worker());
    deploy(vertx, new InstrumentManager(), new DeploymentOptions().setWorker(false));
    deploy(vertx, new EntityManager(), GenericActor.worker());

    deploy(vertx, new TaskManager(Configuration.get(Setting.JOB_POOL)), GenericActor.worker());

    // Deploy interfaces to the NodeRepositories.
    deploy(vertx, new Nodes(Context.getNodeRepository()), GenericActor.worker());
    deploy(vertx, new Instances(Context.getInstanceRepository()), GenericActor.worker());
    deploy(vertx, new Types(Context.getTypeRepository()), GenericActor.worker());

    // Open up the http server and send a friendly message telling the user we're done.
    final String host = Configuration.get(Setting.HTTP_HOST);
    final int port = Configuration.get(Setting.HTTP_PORT);
    try {
      Context.setSchemaNamespace(new URI(String.format("http://%s:%s/", host, port)));
    } catch (URISyntaxException e) {
      handleUnrecoverableLoadingError(e, 1, "Unrecoverable error while setting schema namespace");
    }
    final HttpServer server = httpServer.requestHandler(router::accept);
    try {
      Application.<HttpServer>await(handler -> server.listen(port, host, handler));
    } catch (InterruptedException | ExecutionException e) {
      handleUnrecoverableLoadingError(e, 1, "Failed to start the web server: " + e.getMessage());
    }

    init(vertx);
    log.info("Structure is running at {}", Context.getSchemaNamespace());
  }

  // The constraints and indexes, each in its own transaction
  public static void createSchema() {
    Graph.query("CREATE CONSTRAINT type_name IF NOT EXISTS FOR (n:Type) REQUIRE n.name IS UNIQUE");
    Graph.query("CREATE CONSTRAINT node_uuid IF NOT EXISTS FOR (n:Node) REQUIRE n.uuid IS UNIQUE");
    Graph.query("CREATE INDEX node_name IF NOT EXISTS FOR (n:Node) ON (n.name)");
  }

  public static void init(Vertx vertx) {
    final InstanceRepository instanceRepository = Context.getInstanceRepository();
    final TypeRepository typeRepository = Context.getTypeRepository();

    try {
      new BundleLoader().load();
    } catch (Exception e) {
      handleUnrecoverableLoadingError(e, 1, "Unrecoverable error while loading bundles. Shutting down...");
    }

    // Deploy TypeActors.
    log.debug("Deploying TypeActors..");
    try {
      final List<CompletableFuture<String>> deployments = Graph.transaction(() -> {
        return typeRepository.findAll().stream().map(TypeActor::deploy).collect(Collectors.toList());
      });
      CompletableFuture.allOf(deployments.toArray(new CompletableFuture<?>[0])).get();
    } catch (Exception e) {
      handleUnrecoverableLoadingError(e, 1, "Unrecoverable error while deploying TypeActors. Shutting down...");
    }

    // Deploy InstanceActors.
    log.debug("Deploying InstanceActors..");
    try {
      final List<CompletableFuture<String>> deployments = Graph.transaction(() -> {
        return instanceRepository.findAll().stream().map(InstanceActor::deploy).collect(Collectors.toList());
      });
      CompletableFuture.allOf(deployments.toArray(new CompletableFuture<?>[0])).get();
    } catch (Exception e) {
      handleUnrecoverableLoadingError(e, 1, "Unrecoverable error while deploying InstanceActors. Shutting down...");
    }

    final Optional<String> maybeHostname = getHostname();
    if (maybeHostname.isPresent()) {
      Context.setHostname(maybeHostname.get());

      Component host = Context.getInstanceRepository().findDnsName(Context.getHostname());
      if (host != null) {
        Group structureInstance = Context.getInstanceRepository().getStartOfComponentGroup(host.getUuid());
        if (structureInstance != null) {
          Context.setSelfGroup(structureInstance);
          Context.setSelfIdentifier(structureInstance.getUuid());
        }
      }
    } else {
      final String error = "Couldn't find own hostname";
      handleUnrecoverableLoadingError(new IllegalStateException(error), 1, error);
    }
  }

  // Deploy a verticle and wait for it. Stop Structure if it fails.
  private static void deploy(Vertx vertx, Verticle verticle, DeploymentOptions options) {
    final String name = verticle.getClass().getSimpleName();
    try {
      final String id = await(handler -> vertx.deployVerticle(verticle, options, handler));
      log.info("Deployed {} with id: {}", name, id);
    } catch (InterruptedException | ExecutionException e) {
      handleUnrecoverableLoadingError(e, 1, "Failed to deploy " + name + ": " + e.getMessage());
    }
  }

  // Start an asynchronous Vert.x call and wait for its result.
  private static <T> T await(Consumer<Handler<AsyncResult<T>>> call) throws InterruptedException, ExecutionException {
    final CompletableFuture<T> result = new CompletableFuture<>();
    call.accept(ar -> {
      if (ar.succeeded()) {
        result.complete(ar.result());
      } else {
        result.completeExceptionally(ar.cause());
      }
    });
    return result.get();
  }

  private static Optional<String> getHostname() {
    ProcessRunner hostnameProcess = new ProcessRunner("hostname");
    Optional<String> stdout = Optional.empty();
    try {
      hostnameProcess.run();
      int count = 0;
      while (hostnameProcess.isRunning() && count < 10) {
        Thread.sleep(1000);
        count++;
      }
      ProcessRunner.ProcessResult result = hostnameProcess.finish();
      stdout = result.stdout.isPresent() ? Optional.of(result.stdout.get().trim()) : result.stdout;
    } catch (IOException | InterruptedException e) {
      if (Configuration.isTraceModeOn()) {
        e.printStackTrace();
      }
      log.error("Failed to execute getHostName: " + e.getMessage());
    }
    return stdout;
  }

  /**
   *
   * @param e The exception object that was thrown
   * @param exitCode The System.exit code to terminate the Application with
   * @param message Extra message to print before shutting down
   */
  private static void handleUnrecoverableLoadingError(Exception e, int exitCode, String message) {
    if (Configuration.isTraceModeOn()) e.printStackTrace();
    log.error(message);
    System.exit(exitCode);
  }
}
