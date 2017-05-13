package com.raaftech.structure.base;

import ch.qos.logback.classic.Level;
import com.raaftech.structure.graph.Instance;
import com.raaftech.structure.graph.repositories.InstanceRepository;
import com.raaftech.structure.graph.repositories.TypeRepository;
import com.raaftech.structure.helper.ProcessRunner;
import com.raaftech.structure.helper.loader.BundleLoader;
import com.raaftech.structure.manager.SchemaManager;
import com.raaftech.structure.manager.TaskManager;
import com.raaftech.structure.node.InstanceActor;
import com.raaftech.structure.repository.Instances;
import com.raaftech.structure.repository.Nodes;
import com.raaftech.structure.repository.Types;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import org.apache.commons.cli.*;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.neo4j.support.query.CypherQueryExecutor;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.stream.StreamSupport;

/**
 * The main application entry point.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-11-17
 */
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    // Make Application uninstantiable by hiding the public constructor
    private Application() {}

    /**
     * This is Structure's start of execution point.
     */
    @SuppressWarnings("resource")
	public static void main(String[] args) throws InterruptedException {

        final Options options;
        options = new Options()
                .addOption("b", "background", false, "Enable background mode (daemonized)")
                .addOption("f", "foreground", false, "Enable foreground mode (no-detach)")
                .addOption("h", "help", false, "Display help message")
                .addOption("l", "loglevel", true, "Set loglevel (INFO | DEBUG | TRACE)")
                .addOption("t", "trace", false, "Enable (back)traces")
                .addOption("u", "unsafe", false, "Enable unsafe mode");

        CommandLineParser parser = new DefaultParser();

        // Black magic.
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        ch.qos.logback.classic.Logger root =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

        try {
            CommandLine line = parser.parse(options, args);
            HelpFormatter formatter = new HelpFormatter();

            // HELP
            if(line.hasOption("help")){
                formatter.printHelp("Structure", options);
                return;
            }

            // DETACH || NODETACH
            if(line.hasOption("background")){
                log.info("Background mode not implemented");
            }

            // LOGGING level
            if(line.hasOption("loglevel")){
                String loglevel = line.getOptionValue("loglevel");
                switch(loglevel){
                    case "TRACE":
                        log.info("Setting loglevel to TRACE...");
                        root.setLevel(Level.TRACE);
                        break;
                    case "DEBUG":
                        log.info("Setting loglevel to DEBUG...");
                        root.setLevel(Level.DEBUG);
                        break;
                    case "INFO":
                        log.info("Setting loglevel to INFO...");
                        root.setLevel(Level.INFO);
                        break;
                    default:
                        log.info("Unknown loglevel {} defaulting to INFO...", loglevel);
                        root.setLevel(Level.INFO);
                        break;
                }
            }
            else{
                log.info("No loglevel specified defaulting to INFO...");
                root.setLevel(Level.INFO);
            }

            // TRACING mode
            if(line.hasOption("trace")) {
                log.info("TRACE mode enabled");
                Configuration.setTraceMode(true);
            }

            // MODE
            if(line.hasOption("unsafe")){
                log.info("UNSAFE mode enabled");
                Configuration.setUnsafeMode(true);
            }
        } catch (ParseException e) {
            log.error("Can't parse commandline options: {}", e.getMessage());
        }

        Vertx vertx;
        HttpServer httpServer;
        Router router;
        SockJSHandler sockJsHandler;

        // Tell the user we're starting up.
        log.info("Structure is starting up..");

        // Load Spring
        log.debug("Setting up Spring's annotation aware application context..");
        new AnnotationConfigApplicationContext(SpringConfig.class, ConverterConfig.class);

        // Add a shutdown hook so we always gracefully shutdown neo4j
        // This is done to work around an issue where on the first node creation after an ungraceful
        // shutdown constraints aren't enforced
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Context.getGraphDatabase().shutdown();
            log.info("Database has gracefully shut down");
        }));

        // Setup vertx with a maxWorkerExecuteTime of 5 minutes, in ns.
        Long workerTimeout = Long.parseLong(getConfiguration("workertimeout").orElse("300000000000"));
        vertx = Vertx.vertx(new VertxOptions().setMaxWorkerExecuteTime(workerTimeout));


        Context.setVertx(vertx);

        // Enable bridging on /eventbus, and add some in- and outbound permissions.
        BridgeOptions bridgeOptions = new BridgeOptions()
                .addInboundPermitted(new PermittedOptions().setAddress("com.raaftech.structure.manager.schemamanager"))
                .addInboundPermitted(new PermittedOptions().setAddress("com.raaftech.structure.manager.taskmanager"))
                .addInboundPermitted(new PermittedOptions().setAddressRegex("com\\.raaftech\\.structure\\.repository\\..*"))
                .addInboundPermitted(new PermittedOptions().setAddressRegex("com\\.raaftech\\.structure\\.node\\..*"))
                .addOutboundPermitted(new PermittedOptions().setAddress("com.raaftech.structure.status"));

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
        router.route("/schemas/:schema").handler(schemaManager::getSchema);
        router.route("/*").handler(StaticHandler.create().setWebRoot("src/main/resources/webroot/"));


        // If we're in unsafe mode allow direct access to everything
        // on the eventbus and serve the debug page.
        if (Configuration.isUnsafeModeOn()) {
            log.debug("Setting InboundPermitted and OutboundPermitted to any address (\".*\")");
            bridgeOptions.addInboundPermitted(new PermittedOptions().setAddressRegex(".*"));
            bridgeOptions.addOutboundPermitted(new PermittedOptions().setAddressRegex(".*"));
        }

        // Setup indexes and constraints
        log.info("Creating indices.");
        CypherQueryExecutor queryExecutor = Context.getNeo4jTemplate().getInfrastructure().getCypherQueryExecutor();
        queryExecutor.query("CREATE CONSTRAINT ON (n:Type) ASSERT n.name IS UNIQUE", null, null);
        queryExecutor.query("CREATE CONSTRAINT ON (n:Node) ASSERT n.uuid IS UNIQUE", null, null);

        // Initialize Configuration
        Configuration.init();

        // Deploy our main actors.
        vertx.deployVerticle("com.raaftech.structure.manager.StructureManager", ar -> {
            if (ar.succeeded()) {
                log.info("Deployed StructureManager with id: {}", ar.result());
            }
        });
        vertx.deployVerticle("com.raaftech.structure.manager.InstrumentManager", new DeploymentOptions().setWorker(false), ar -> {
            if (ar.succeeded()) {
                log.info("Deployed InstrumentManager with id: {}", ar.result());
            }
        });
        vertx.deployVerticle("com.raaftech.structure.manager.EntityManager", ar -> {
            if (ar.succeeded()) {
                log.info("Deployed EntityManager with id: {}", ar.result());
            }
        });

        final int jobpool = Integer.parseInt(getConfiguration("jobpool").orElse("50"));
        vertx.deployVerticle(new TaskManager(jobpool), ar -> {
            if (ar.succeeded()) {
                log.info("Deployed TaskManager with id: {}", ar.result());
            }
        });

        // Deploy interfaces to the NodeRepositories.
        vertx.deployVerticle(new Nodes(Context.getNodeRepository()), ar -> {
            if (ar.succeeded()) {
                log.info("Deployed NodeRepository with id: {}", ar.result());
            }
        });
        vertx.deployVerticle(new Instances(Context.getInstanceRepository()), ar -> {
            if (ar.succeeded()) {
                log.info("Deployed InstanceRepository with id: {}", ar.result());
            }
        });
        vertx.deployVerticle(new Types(Context.getTypeRepository()), ar -> {
            if (ar.succeeded()) {
                log.info("Deployed TypeRepository with id: {}", ar.result());
            }
        });

        init(vertx);

        // Wait a second so debug messages are done
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.error(e.getMessage());
            throw e;
        }

        // Open up the http server and send a friendly message telling the user we're done.
        final String webHost = getConfiguration("webHost").orElse("localhost");
        final int webPort = Integer.parseInt(getConfiguration("webPort").orElse("8080"));
        try {
            Configuration.setSchemaNamespace(new URI(String.format("http://%s:%s/", webHost, webPort)));
        } catch (URISyntaxException e) {
            handleUnrecoverableLoadingError(null, e, 1, "Unrecoverable error while setting schema namespace");
        }
        httpServer.requestHandler(router::accept).listen(webPort, webHost);
        log.info("Structure is running at {}", Configuration.getSchemaNamespace());
    }

    public static void init(Vertx vertx) {
        final InstanceRepository instanceRepository = Context.getInstanceRepository();
        final TypeRepository typeRepository = Context.getTypeRepository();

        Transaction tx = Context.getGraphDatabase().beginTx();
        try {
            BundleLoader bundleLoader = new BundleLoader();
            bundleLoader.load();
            tx.success();
        } catch (Exception e) {
            handleUnrecoverableLoadingError(tx, e, 1, "Unrecoverable error while loading types. Shutting down...");
        } finally {
            tx.close();
        }

        // Deploy TypeActors.
        log.debug("Deploying TypeActors..");
        tx = Context.getGraphDatabase().beginTx();

        try {
            StreamSupport.stream(typeRepository.findAll().spliterator(), false)
                    .forEach(InstanceActor::deploy);
        } catch (Exception e) {
            handleUnrecoverableLoadingError(tx, e, 1, "Unrecoverable error while deploying TypeActors. Shutting down...");
        } finally {
            tx.close();
        }

        // Deploy InstanceActors.
        log.debug("Deploying InstanceActors..");
        tx = Context.getGraphDatabase().beginTx();

        try {
            StreamSupport.stream(instanceRepository.findAll().spliterator(), false)
                    .forEach(InstanceActor::deploy);
        } catch (Exception e) {
            handleUnrecoverableLoadingError(tx, e, 1, "Unrecoverable error while deploying InstanceActors. Shutting down...");
        } finally {
            tx.close();
        }

        final Optional<String> maybeHostname = getHostname();
        if (maybeHostname.isPresent()) {
            Context.setHostname(maybeHostname.get());

            com.raaftech.structure.graph.Configuration host = Context.getInstanceRepository().findDnsName(Context.getHostname());
            if (host != null) {
                Instance structureInstance = Context.getInstanceRepository().getStartOfComponentGroup(host.getUuid());
                if (structureInstance != null) {
                    Context.setSelfIdentifier(structureInstance.getUuid());
                }
            }
        } else {
            final String error = "Couldn't find own hostname";
            handleUnrecoverableLoadingError(null, new IllegalStateException(error), 1, error);
        }
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
            log.error("Failed to execute getHostName: " +  e.getMessage());
        }
        return stdout;
    }

    /**
     * Get a configuration property, giving priority to environment variables, then properties.
     * @param configurationKey The name of the configuration key
     * @return The value of the configuration key, or null if there was no configuration
     */
    private static Optional<String> getConfiguration(String configurationKey) {
        if (System.getenv(configurationKey.toUpperCase()) != null) {
            return Optional.of(System.getenv(configurationKey.toUpperCase()));
        } else if (System.getProperty(configurationKey) != null){
            return Optional.of(System.getProperty(configurationKey));
        } else {
            return Optional.empty();
        }
    }

    /**
     *
     * @param tx The graphdb transaction context if available, otherwise set to null
     * @param e The exception object that was thrown
     * @param exitCode The System.exit code to terminate the Application with
     * @param message Extra message to print before shutting down
     */
    private static void handleUnrecoverableLoadingError(Transaction tx, Exception e, int exitCode, String message) {
        if(Configuration.isTraceModeOn()) e.printStackTrace();
        if(tx != null) tx.close();
        log.error(message);
        System.exit(exitCode);
    }
}
