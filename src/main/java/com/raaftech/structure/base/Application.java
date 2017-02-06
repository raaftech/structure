package com.raaftech.structure.base;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.databind.JsonNode;
import com.raaftech.structure.graph.repositories.InstanceRepository;
import com.raaftech.structure.graph.repositories.InstrumentRepository;
import com.raaftech.structure.graph.repositories.ProfileRepository;
import com.raaftech.structure.graph.repositories.TypeRepository;
import com.raaftech.structure.helper.loader.EvenNaiverLoader;
import com.raaftech.structure.manager.TaskManager;
import com.raaftech.structure.next.Entity;
import com.raaftech.structure.node.TypeActor;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.StreamSupport;

import static io.vertx.core.json.Json.mapper;

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
        final int port = 8080;

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

        // Now that Spring's context is available, the repositories are autowired in Context
        final InstanceRepository instanceRepository = Context.getInstanceRepository();
        final TypeRepository typeRepository = Context.getTypeRepository();
        final InstrumentRepository instrumentRepository = Context.getInstrumentRepository();
        final ProfileRepository profileRepository = Context.getProfileRepository();

        // Setup vertx with a maxWorkerExecuteTime of 5 minutes.
        final String workerTimeoutConfiguration = getConfiguration("workertimeout");
        Long workerTimeout = workerTimeoutConfiguration == null ?
                5L*60*1000*1000000 :
                Long.parseLong(workerTimeoutConfiguration);
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
        router.route("/eventbus/*").handler(sockJsHandler);
        router.route("/bundles/*").handler(StaticHandler.create().setWebRoot("src/main/resources/bundles/"));
        router.route("/schema/*").handler(StaticHandler.create().setWebRoot("src/main/resources/schema/"));
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

        // loop over directories in bundlePath and search for *.json
        final List<Path> matchingFiles = new ArrayList<>();
        for (String location : Arrays.asList(System.getProperty("bundlePath").split(":"))) {

            Path path = Paths.get(new File(location).getAbsolutePath());
            String workingDirectory = path.getParent().toString().replace('\\', '/');
            String syntaxAndPattern = "glob:" + workingDirectory + "/" + "**/bundle.json".replace('\\', '/');
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher(syntaxAndPattern);

            try {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

	                @Override
	                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
	                    if (matcher.matches(file)) {
                            log.debug("visitFile: matched file: {}", file);
                            matchingFiles.add(file);
	                    }
	                    return FileVisitResult.CONTINUE;
	                }
	
	                @Override
	                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
	                    log.debug("visitFileFailed: failed to visit file: (file) {}", file);
	                    return FileVisitResult.CONTINUE;
	                }

                });
            } catch (IOException e){
                log.error(e.getMessage());

                if(Configuration.isTraceModeOn()){
                    e.printStackTrace();
                }
            }
        }

        /*
        * Instrument, Controller, Marker, Parser, Profile
         */
        EvenNaiverLoader evenNaiverLoader = new EvenNaiverLoader();

        Transaction tx = Context.getGraphDatabase().beginTx();
        try {
            evenNaiverLoader.getUpdateList(evenNaiverLoader.sortIt(evenNaiverLoader.readAllJson())).stream()
                    .map(node -> {
                        try {
                            return mapper.readValue(node.toString(), Entity.class);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    })
                    .filter(node -> node != null)
                    .forEach(Entity::toGraph);
            tx.success();
        } catch (Exception e) {
            e.printStackTrace();
            tx.failure();
        } finally {
            tx.close();
        }

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
        vertx.deployVerticle("com.raaftech.structure.manager.EntityManager", ar -> {
            if (ar.succeeded()) {
                log.info("Deployed EntityManager with id: {}", ar.result());
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

        // Deploy TypeActors.
        log.debug("Deploying TypeActors..");
        tx = Context.getGraphDatabase().beginTx();
        StreamSupport.stream(typeRepository.findAll().spliterator(), false)
                .map(TypeActor::new)
                .forEach(vertx::deployVerticle);
        tx.close();

        // TODO: adapt to new split instancenodes
        /*
        // Deploy InstanceActors.
        log.debug("Deploying InstanceActors..");
        tx = Context.getGraphDatabase().beginTx();
        for (Instance instanceNode : instanceNodeRepository.findAll()) {
            InstanceActor instanceNodeActor = new InstanceActor(instanceNode);
            vertx.deployVerticle(instanceNodeActor, new DeploymentOptions().setWorker(true));
        }
        tx.close();
        */

        // Wait a second so debug messages are done
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.error(e.getMessage());
            throw e;
        }

        // Open up the http server and send a friendly message telling the user we're done.
        httpServer.requestHandler(router::accept).listen(port);
        log.info("Structure is running at http://localhost:{}", port);
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
