package tech.raaf.structure.base;

import ch.qos.logback.classic.Level;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.SelfSignedCertificate;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
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
import tech.raaf.structure.helper.ConfigurationProxy;
import tech.raaf.structure.helper.ProcessRunner;
import tech.raaf.structure.helper.loader.BundleLoader;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * The main application entry point.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-11-17
 */
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);
    private static ConfigurationProxy configuration;

    // Make Application uninstantiable by hiding the public constructor
    private Application() {}

    /**
     * This is Structure's start of execution point.
     */
    @SuppressWarnings("resource")
    public static void main(String[] args) throws InterruptedException {

        // The self signed certificate for the vertx webserver
        SelfSignedCertificate selfSignedCertificate = null;
        // Option names for keystore
        final String KEYSTORE_LOCATION = "keystore-file";
        final String KEYSTORE_PASS = "keystore-password";
        // Variables to hold keystore data
        String keyStoreLocation = null;
        String keyStorePassword = null;

        // Black magic.
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        ch.qos.logback.classic.Logger root =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

        try {
            Options options = new Options()
                    .addOption("h", "help", false, "Display help message")
                    .addOption("l", "loglevel", true, "Set loglevel (INFO | DEBUG | TRACE)")
                    .addOption("t", "trace", false, "Enable (back)traces")
                    .addOption("u", "unsafe", false, "Enable unsafe mode")
                    .addOption("b", "bundles", true, "Bundle locations to load")
                    .addOption("s", "ssl", false, "Enable SSL")
                    .addOption("k", KEYSTORE_LOCATION, true, "Location of the keystore (used with -s)")
                    .addOption("p", KEYSTORE_PASS, true, "Password for the keystore");

            configuration = ConfigurationProxy.builder()
                    .options(options)
                    .args(args)
                    // make sure env keys are prepended with "STRUCTURE_" and replace - with _.
                    .transformer(s -> "STRUCTURE_".concat(s.toUpperCase().replace("-", "_")))
                    .build();

            HelpFormatter formatter = new HelpFormatter();

            // HELP
            final Optional<String> maybeHelp = configuration.getConfiguration("help");
            if (maybeHelp.isPresent()) {
                formatter.printHelp("Structure", options);
                return;
            }

            // LOGGING level
            final Optional<String> maybeLogLevel = configuration.getConfiguration("loglevel");
            if (maybeLogLevel.isPresent()) {
                String loglevel = maybeLogLevel.get();
                switch (loglevel) {
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
            } else {
                log.info("No loglevel specified defaulting to INFO...");
                root.setLevel(Level.INFO);
            }

            // TRACING mode
            final Optional<String> maybeTrace = configuration.getConfiguration("trace");
            if (maybeTrace.isPresent()) {
                log.info("TRACE mode enabled");
                Configuration.setTraceMode(true);
            }

            // MODE
            final Optional<String> maybeUnsafe = configuration.getConfiguration("unsafe");
            if (maybeUnsafe.isPresent()) {
                log.info("UNSAFE mode enabled");
                Configuration.setUnsafeMode(true);
            }

            if (configuration.getConfiguration("ssl").isPresent()) {
                // Check if a keystore was passed; if not, we generate a certificate
                Optional<String> keyStoreLocationDefault = configuration.getConfiguration(KEYSTORE_LOCATION);
                if (keyStoreLocationDefault.isPresent()) {
                    keyStoreLocation = keyStoreLocationDefault.get();
                } else {
                    log.info("Generating a self-signed certificate for HTTPS");
                    selfSignedCertificate = generateAndInjectCertificate();
                }

                if (keyStoreLocation != null) {
                    log.info("Using keystore " + keyStoreLocation + " for HTTPS");
                    System.setProperty("javax.net.ssl.trustStore", keyStoreLocation);
                }

                // Check for keystore password if a keystore password was given
                // option for this as passwords are better passed via environment.
                Optional<String> maybeKeyStorePassword = configuration.getConfiguration(KEYSTORE_PASS);
                if (maybeKeyStorePassword.isPresent()) {
                    keyStorePassword = maybeKeyStorePassword.get();
                }
            }
        } catch (ParseException e) {
            log.error("Can't parse commandline options: {}", e.getMessage());
        }

        Vertx vertx;
        HttpServer httpServer = null;
        Router router;
        SockJSHandler sockJsHandler;

        // Tell the user we're starting up.
        log.info("Structure is starting up..");

        // Connect to the Neo4j server
        final String neo4jUri = configuration.getConfiguration("neo4j-uri").orElse("bolt://localhost:7687");
        final String neo4jUsername = configuration.getConfiguration("neo4j-username").orElse("neo4j");
        // Only from the environment: other users can see command lines with ps
        final String neo4jPassword = System.getenv("STRUCTURE_NEO4J_PASSWORD");
        if (neo4jPassword == null) {
            final String error = "Set the Neo4j password in STRUCTURE_NEO4J_PASSWORD";
            handleUnrecoverableLoadingError(new IllegalStateException(error), 1, error);
        }
        log.info("Connecting to Neo4j at {}", neo4jUri);
        try {
            Graph.connect(neo4jUri, neo4jUsername, neo4jPassword);
        } catch (GraphException e) {
            handleUnrecoverableLoadingError(e, 1, "Can't connect to Neo4j at " + neo4jUri + ": " + e.getMessage());
        }

        // Close the connection to Neo4j when Structure stops
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Graph.close();
            log.info("Closed the connection to Neo4j");
        }));

        // Setup vertx with a maxWorkerExecuteTime of 5 minutes, in ns.
        Long workerTimeout = Long.parseLong(configuration.getConfiguration("workertimeout").orElse("300000000000"));
        vertx = Vertx.vertx(new VertxOptions().setMaxWorkerExecuteTime(workerTimeout));


        Context.setVertx(vertx);

        // Enable bridging on /eventbus, and add some in- and outbound permissions.
        BridgeOptions bridgeOptions = new BridgeOptions()
                .addInboundPermitted(new PermittedOptions().setAddressRegex("tech\\.raaf\\.structure\\..*"))
                .addOutboundPermitted(new PermittedOptions().setAddressRegex("tech\\.raaf\\.structure\\..*"));

        // Setup httpServer, router and sockJs.
        // If we don't have a self signed certificate we were passed a keystore
        if (configuration.getConfiguration("ssl").isPresent()) {
            if (selfSignedCertificate == null) {
                // If we don't have a keystore password, prompt for one
                if (keyStorePassword == null) {
                    System.out.print(String.format("Password for keystore %s: ",
                            Paths.get(keyStoreLocation).getFileName().toString()));
                    keyStorePassword = new String(System.console().readPassword());
                }
                httpServer = vertx.createHttpServer(new HttpServerOptions()
                        .setSsl(true)
                        .setKeyStoreOptions(new JksOptions()
                                .setPassword(keyStorePassword)
                                .setPath(keyStoreLocation)));
            } else {
                httpServer = vertx.createHttpServer(new HttpServerOptions()
                        .setSsl(true)
                        .setKeyCertOptions(selfSignedCertificate.keyCertOptions())
                        .setTrustOptions(selfSignedCertificate.trustOptions()));
            }
        } else {
            httpServer = vertx.createHttpServer();
        }
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
        if (Configuration.isUnsafeModeOn()) {
            log.debug("Setting InboundPermitted and OutboundPermitted to any address (\".*\")");
            bridgeOptions.addInboundPermitted(new PermittedOptions().setAddressRegex(".*"));
            bridgeOptions.addOutboundPermitted(new PermittedOptions().setAddressRegex(".*"));
        }

        // Setup indexes and constraints
        log.info("Creating indices.");
        createSchema();

        // Initialize Configuration
        Configuration.init(configuration);

        // Deploy our main actors. Structure stops if one of them fails.
        // The actors that use the graph run on worker threads.
        deploy(vertx, new StructureManager(), new DeploymentOptions().setWorker(true).setWorkerPoolName(Configuration.ACTOR_POOL));
        deploy(vertx, new InstrumentManager(), new DeploymentOptions().setWorker(false));
        deploy(vertx, new EntityManager(), new DeploymentOptions().setWorker(true).setWorkerPoolName(Configuration.ACTOR_POOL));

        final int jobpool = Integer.parseInt(configuration.getConfiguration("jobpool").orElse("50"));
        deploy(vertx, new TaskManager(jobpool), new DeploymentOptions().setWorker(true).setWorkerPoolName(Configuration.ACTOR_POOL));

        // Deploy interfaces to the NodeRepositories.
        deploy(vertx, new Nodes(Context.getNodeRepository()), new DeploymentOptions().setWorker(true).setWorkerPoolName(Configuration.ACTOR_POOL));
        deploy(vertx, new Instances(Context.getInstanceRepository()), new DeploymentOptions().setWorker(true).setWorkerPoolName(Configuration.ACTOR_POOL));
        deploy(vertx, new Types(Context.getTypeRepository()), new DeploymentOptions().setWorker(true).setWorkerPoolName(Configuration.ACTOR_POOL));

        // Open up the http server and send a friendly message telling the user we're done.
        Configuration.setWebHost(configuration.getConfiguration("webHost").orElse("localhost"));
        Configuration.setWebPort(Integer.parseInt(configuration.getConfiguration("webPort").orElse(
                configuration.getConfiguration("ssl").isPresent() ? "8443" : "8080"
        )));
        try {
            final String scheme;
            if (configuration.getConfiguration("ssl").isPresent()) {
                scheme = "https";
            } else {
                scheme = "http";
            }
            Configuration.setSchemaNamespace(new URI(String.format("%s://%s:%s/",
                    scheme, Configuration.getWebHost(), Configuration.getWebPort())));
        } catch (URISyntaxException e) {
            handleUnrecoverableLoadingError(e, 1, "Unrecoverable error while setting schema namespace");
        }
        final HttpServer server = httpServer.requestHandler(router::accept);
        try {
            Application.<HttpServer>await(handler ->
                    server.listen(Configuration.getWebPort(), Configuration.getWebHost(), handler));
        } catch (InterruptedException | ExecutionException e) {
            handleUnrecoverableLoadingError(e, 1, "Failed to start the web server: " + e.getMessage());
        }

        init(vertx);
        log.info("Structure is running at {}", Configuration.getSchemaNamespace());
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
                return typeRepository.findAll().stream()
                        .map(TypeActor::deploy)
                        .collect(Collectors.toList());
            });
            CompletableFuture.allOf(deployments.toArray(new CompletableFuture<?>[0])).get();
        } catch (Exception e) {
            handleUnrecoverableLoadingError(e, 1, "Unrecoverable error while deploying TypeActors. Shutting down...");
        }

        // Deploy InstanceActors.
        log.debug("Deploying InstanceActors..");
        try {
            final List<CompletableFuture<String>> deployments = Graph.transaction(() -> {
                return instanceRepository.findAll().stream()
                        .map(InstanceActor::deploy)
                        .collect(Collectors.toList());
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
    private static <T> T await(Consumer<Handler<AsyncResult<T>>> call)
            throws InterruptedException, ExecutionException {
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
            log.error("Failed to execute getHostName: " +  e.getMessage());
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
        if(Configuration.isTraceModeOn()) e.printStackTrace();
        log.error(message);
        System.exit(exitCode);
    }

    /**
     * Generate a self-signed certificate and inject that in a new {@link javax.net.ssl.TrustManager}
     * which in turn will be used by {@link SSLContext}. This ensures our JSON schema library will
     * be able to follow references using the exposed https server which uses the self-signed cert.
     * @return The self-signed certificate.
     */
    private static SelfSignedCertificate generateAndInjectCertificate() {
        final SelfSignedCertificate cert = SelfSignedCertificate.create();

        try {
            // Create an InputStream from our generated certificate.
            BufferedInputStream is = new BufferedInputStream(new FileInputStream(cert.certificatePath()));
            // Turn it into a X509Certificate object
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate caCert = (X509Certificate) cf.generateCertificate(is);
            // Get an instance of a new TrustManagerFactory
            TrustManagerFactory trustManagerFactory = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());
            // Get an instance of a new KeyStore
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            // We don't need to load a KeyStore from file, as ours will just contain `cert`
            ks.load(null);
            // Add an entry for `cert`
            ks.setCertificateEntry("localhost", caCert);
            // Init the factory with our new KeyStore
            trustManagerFactory.init(ks);

            // Create a new SSL Context using a TrustManager created by our factory
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
            // Set the default SSLSocketFactory to our a new SSLSocketFactory created from
            // our SSLContext
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = (hostname, session) -> Configuration.getWebHost().equals(hostname);
            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (Exception e) {
            log.error("Error while generating self-signed certificate: \n"+ e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        return cert;
    }
}
