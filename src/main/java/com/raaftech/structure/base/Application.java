package com.raaftech.structure.base;

import ch.qos.logback.classic.Level;
import com.raaftech.structure.graph.Template;
import com.raaftech.structure.graph.Type;
import com.raaftech.structure.graph.repositories.*;
import com.raaftech.structure.helper.loader.BundleLoader;
import com.raaftech.structure.manager.TaskManager;
import com.raaftech.structure.node.TemplateActor;
import com.raaftech.structure.node.TypeActor;
import com.raaftech.structure.repository.Instances;
import com.raaftech.structure.repository.Nodes;
import com.raaftech.structure.repository.Templates;
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
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

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

        // Now that Spring's context is available, the repositories are autowired in Context
        final TypeRepository typeRepository = Context.getTypeRepository();
        final InstanceRepository instanceRepository = Context.getInstanceRepository();
        final InstrumentRepository instrumentRepository = Context.getInstrumentRepository();
        final TemplateRepository templateRepository = Context.getTemplateRepository();
        final ProfileRepository profileRepository = Context.getProfileRepository();

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
        if (Configuration.isUnsafeModeOn()) {
            log.debug("Setting InboundPermitted and OutboundPermitted to any address (\".*\")");
            bridgeOptions.addInboundPermitted(new PermittedOptions().setAddressRegex(".*"));
            bridgeOptions.addOutboundPermitted(new PermittedOptions().setAddressRegex(".*"));
        }

        // Setup default route.
        router.route("/*").handler(StaticHandler.create().setWebRoot("web/"));

        // Setup indexes and constraints
        log.info("Creating indices.");
        CypherQueryExecutor queryExecutor = Context.getNeo4jTemplate().getInfrastructure().getCypherQueryExecutor();
        queryExecutor.query("CREATE CONSTRAINT ON (n:Type) ASSERT n.name IS UNIQUE", null, null);
        queryExecutor.query("CREATE CONSTRAINT ON (n:Node) ASSERT n.uuid IS UNIQUE", null, null);

        // Purge all instruments. When new checksum based create/read/update/delete loader is finished, this will be unneccessary. 
        log.info("Purging instruments.");
        instrumentRepository.purgeInstruments();
        Context.setInstrumentsLoaded(false);

        // Purge all profiles. When new checksum based create/read/update/delete loader is finished, this will be unneccessary.
        log.info("Purging profiles.");
        profileRepository.purgeProfiles();

        final String propertyFile = "config.properties";
        final String bundlePathProperty = "bundlePath";

        // TODO: implement environment configuration
        // Open propertyFile from resources and read bundlePathProperty or default it to 'bundles'
        try {
            Properties p = new Properties();
            Thread currentThread = Thread.currentThread();
            ClassLoader classLoader = currentThread.getContextClassLoader();
            InputStream inputStream = classLoader.getResourceAsStream(propertyFile);

            if(inputStream.available() > 0) {
                p.load(inputStream);

                if(p.containsKey(bundlePathProperty)){
                    log.info("Setting {} from {}", bundlePathProperty, propertyFile);
                    System.setProperty(bundlePathProperty, p.getProperty(bundlePathProperty));
                }
            }
            else {
                log.info("inputStream not available..");
            }
        } catch (Exception e){
            log.error(e.getMessage());

            if(Configuration.isTraceModeOn()){
                e.printStackTrace();
            }
        } finally {
            log.error("bundlePathProperty is empty, defaulting to 'bundles'");
            System.setProperty(bundlePathProperty, "bundles");
        }

        // loop over directories in bundlePath and search for *.json
        final List<Path> matchingFiles = new ArrayList<>();
        for (String location : Arrays.asList(System.getProperty(bundlePathProperty).split(File.pathSeparator))) {

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

        // TODO: add sorting (see issue/70)
        matchingFiles.stream().forEach(bundle -> {
            try {
                new BundleLoader(bundle).load();
            } catch (IOException e) {
                log.error(e.getMessage());

                if(Configuration.isTraceModeOn()){
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
        vertx.deployVerticle(new Instances(Context.getInstanceRepository()), ar -> {
            if (ar.succeeded()) {
                log.info("Deployed InstanceRepository with id: {}", ar.result());
            }
        });
        vertx.deployVerticle(new Templates(Context.getTemplateRepository()), ar -> {
            if (ar.succeeded()) {
                log.info("Deployed TemplateRepository with id: {}", ar.result());
            }
        });
        vertx.deployVerticle(new Types(Context.getTypeRepository()), ar -> {
            if (ar.succeeded()) {
                log.info("Deployed TypeRepository with id: {}", ar.result());
            }
        });

        // Deploy TypeActors.
        log.debug("Deploying TypeActors..");
        Transaction tx = Context.getGraphDatabase().beginTx();
        for (Type type : typeRepository.findAll()) {
            TypeActor typeActor = new TypeActor(type);
            vertx.deployVerticle(typeActor);
        }
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

        // Deploy TemplateActors.
        log.debug("Deploying TemplateActors..");
        tx = Context.getGraphDatabase().beginTx();
        for (Template template : templateRepository.findAll()) {
            TemplateActor templateActor = new TemplateActor(template);
            vertx.deployVerticle(templateActor);
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
