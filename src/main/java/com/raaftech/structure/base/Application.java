package com.raaftech.structure.base;

import ch.qos.logback.classic.Level;
import com.raaftech.structure.graph.InstanceNode;
import com.raaftech.structure.graph.TemplateNode;
import com.raaftech.structure.graph.TypeNode;
import com.raaftech.structure.graph.repositories.InstanceNodeRepository;
import com.raaftech.structure.graph.repositories.TemplateNodeRepository;
import com.raaftech.structure.graph.repositories.TypeNodeRepository;
import com.raaftech.structure.helper.loader.BundleLoader;
import com.raaftech.structure.manager.TaskManager;
import com.raaftech.structure.node.InstanceNodeActor;
import com.raaftech.structure.node.TemplateNodeActor;
import com.raaftech.structure.node.TypeNodeActor;
import com.raaftech.structure.repository.*;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;

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

            // TRACING mode
            if(line.hasOption("trace")) {
                System.setProperty("traceMode", "true");
                log.info("TRACE mode enabled");
            }

            // MODE
            if(line.hasOption("unsafe")){
                log.info("UNSAFE mode enabled");
                System.setProperty("unsafeMode", "true");
            }
        } catch (ParseException e) {
            log.error("Can't parse commandline options: " + e.getMessage());
        }

        Vertx vertx;
        HttpServer httpServer;
        Router router;
        SockJSHandler sockJsHandler;
        final int port = 8080;

        // Tell the user we're starting up.
        log.info("Structure is starting up..");

        // Load Spring
        log.debug("Setting up Spring's annotation aware application context..");
        new AnnotationConfigApplicationContext(SpringConfig.class, ConverterConfig.class);

        // Now that Spring's context is available, the repositories are autowired in Context
        final TypeNodeRepository typeNodeRepository = Context.getTypeNodeRepository();
        final InstanceNodeRepository instanceNodeRepository = Context.getInstanceNodeRepository();
        final TemplateNodeRepository templateNodeRepository = Context.getTemplateNodeRepository();

        // Setup vertx with a maxWorkerExecuteTime of 5 minutes.
        final String workerTimeoutConfiguration = getConfiguration("workertimeout");
        Long workerTimeout = workerTimeoutConfiguration == null ?
                5L*60*1000*1000000 :
                Long.parseLong(workerTimeoutConfiguration);
        vertx = Vertx.vertx(new VertxOptions().setMaxWorkerExecuteTime(workerTimeout));
        router = Router.router(vertx);
        sockJsHandler = SockJSHandler.create(vertx);
        httpServer = vertx.createHttpServer();

        Context.setVertx(vertx);

        // Enable bridging on /eventbus, and add some in- and outbound permissions.
        BridgeOptions bridgeOptions = new BridgeOptions()
                .addInboundPermitted(new PermittedOptions().setAddress("com.raaftech.structure.manager.schemamanager"))
                .addInboundPermitted(new PermittedOptions().setAddress("com.raaftech.structure.manager.taskmanager"))
                .addInboundPermitted(new PermittedOptions().setAddressRegex("com\\.raaftech\\.structure\\.repository\\..*"))
                .addInboundPermitted(new PermittedOptions().setAddressRegex("com\\.raaftech\\.structure\\.node\\..*"))
                .addOutboundPermitted(new PermittedOptions().setAddress("com.raaftech.structure.status"));

        // Enable sockJS Server on our HTTP server.
        sockJsHandler.bridge(bridgeOptions);
        router.route("/eventbus/*").handler(sockJsHandler);
        
        // If we're in unsafe mode allow direct access to everything
        // on the eventbus and serve the debug page.
        if (Boolean.parseBoolean(System.getProperty("unsafeMode"))) {
            log.debug("Setting InboundPermitted and OutboundPermitted to any address (\".*\")");
            bridgeOptions.addInboundPermitted(new PermittedOptions().setAddressRegex(".*"));
            bridgeOptions.addOutboundPermitted(new PermittedOptions().setAddressRegex(".*"));
        }

        // Setup default route.
        router.route("/*").handler(StaticHandler.create().setWebRoot("web/"));

        // Setup indexes and constraints
        log.info("Creating indices.");
        CypherQueryExecutor queryExecutor = Context.getNeo4jTemplate().getInfrastructure().getCypherQueryExecutor();
        queryExecutor.query("CREATE CONSTRAINT ON (n:TypeNode) ASSERT n.name IS UNIQUE", null, null);
        queryExecutor.query("CREATE CONSTRAINT ON (n:Node) ASSERT n.uuid IS UNIQUE", null, null);

        // Purge all instruments
        log.info("Purging instruments.");
        instanceNodeRepository.purgeHandlers();
        Context.setInstrumentsLoaded(false);

        // Prepend the default bundle to the configured bundles
        final String bundlesProperty = "bundles";
        final String raafBundle = new File("bundles/raaf/bundle.json").getAbsolutePath();
        if (System.getProperty(bundlesProperty) == null) {
            System.setProperty(bundlesProperty, raafBundle);
        } else {
            StringBuilder sb = new StringBuilder()
                    .append(raafBundle)
                    .append(",")
                    .append(System.getProperty(bundlesProperty));
            System.setProperty(bundlesProperty, sb.toString());
        }
        String bundles = System.getProperty(bundlesProperty);

        // Iterate over the bundles and load them
        Arrays.asList(bundles.split(",")).stream()
                .map(Paths::get)
                .distinct()
                .forEach(bundle -> {
                    try {
                        new BundleLoader(bundle).load();
                    } catch (IOException e) {
                        log.error(e.getMessage());

                        if (Boolean.parseBoolean(System.getProperty("traceMode"))) {
                            e.printStackTrace();
                        }
                    }
                });
        Context.setInstrumentsLoaded(true);

        // Deploy our main actors.
        vertx.deployVerticle("com.raaftech.structure.manager.BusManager", ar -> {
            if (ar.succeeded()) {
                log.info("Deployed BusManager with id: {}", ar.result());
            }
        });
        vertx.deployVerticle("com.raaftech.structure.manager.InstrumentManager", new DeploymentOptions().setWorker(false), ar -> {
            if (ar.succeeded()) {
                log.info("Deployed InstrumentManager with id: {}", ar.result());
            }
        });
        String jobpoolConfiguration = getConfiguration("jobpool");
        int jobpool = jobpoolConfiguration == null ?
                50 :
                Integer.parseInt(jobpoolConfiguration);
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
        vertx.deployVerticle(new InstanceNodes(Context.getInstanceNodeRepository()), ar -> {
            if (ar.succeeded()) {
                log.info("Deployed InstanceNodeRepository with id: {}", ar.result());
            }
        });
        vertx.deployVerticle(new SchemaNodes(Context.getSchemaNodeRepository()), ar -> {
            if (ar.succeeded()) {
                log.info("Deployed SchemaNodeRepository with id: {}", ar.result());
            }
        });
        vertx.deployVerticle(new TemplateNodes(Context.getTemplateNodeRepository()), ar -> {
            if (ar.succeeded()) {
                log.info("Deployed TemplateNodeRepository with id: {}", ar.result());
            }
        });
        vertx.deployVerticle(new TypeNodes(Context.getTypeNodeRepository()), ar -> {
            if (ar.succeeded()) {
                log.info("Deployed TypeNodeRepository with id: {}", ar.result());
            }
        });

        // Deploy TypeNodeActors.
        log.debug("Deploying TypeNodeActors..");
        Transaction tx = Context.getGraphDatabase().beginTx();
        for (TypeNode typeNode : typeNodeRepository.findAll()) {
            TypeNodeActor typeNodeActor = new TypeNodeActor(typeNode);
            vertx.deployVerticle(typeNodeActor);
        }
        tx.close();

        // Deploy InstanceNodeActors.
        log.debug("Deploying InstanceNodeActors..");
        tx = Context.getGraphDatabase().beginTx();
        for (InstanceNode instanceNode : instanceNodeRepository.findAll()) {
            InstanceNodeActor instanceNodeActor = new InstanceNodeActor(instanceNode);
            vertx.deployVerticle(instanceNodeActor, new DeploymentOptions().setWorker(true));
        }
        tx.close();

        // Deploy TemplateNodeActors.
        log.debug("Deploying TemplateNodeActors..");
        tx = Context.getGraphDatabase().beginTx();
        for (TemplateNode templateNode : templateNodeRepository.findAll()) {
            TemplateNodeActor templateNodeActor = new TemplateNodeActor(templateNode);
            vertx.deployVerticle(templateNodeActor);
        }
        tx.close();

        // Wait a second so debug messages are done
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.error(e.getMessage());
            throw e;
        }

        // A friendly message telling the user we're done.
        httpServer.requestHandler(router::accept).listen(port);
        log.info("Structure is running at http://localhost:" + port);
    }

    /**
     * Get a configuration property, giving priority to environment variables, then properties.
     * @param configurationKey The name of the configuration key
     * @return The value of the configuration key, or null if there was no configuration
     */
    private static String getConfiguration(String configurationKey) {
        if (System.getenv(configurationKey.toUpperCase()) != null) {
            return System.getenv(configurationKey.toUpperCase());
        } else if (System.getProperty(configurationKey) != null){
            return System.getProperty(configurationKey);
        } else {
            return null;
        }
    }
}
