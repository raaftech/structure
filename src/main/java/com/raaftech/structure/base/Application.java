package com.raaftech.structure.base;

import ch.qos.logback.classic.Level;
import com.raaftech.structure.bus.InstanceActor;
import com.raaftech.structure.bus.TypeActor;
import com.raaftech.structure.bus.manager.SchemaManager;
import com.raaftech.structure.bus.manager.TaskManager;
import com.raaftech.structure.bus.repository.Instances;
import com.raaftech.structure.bus.repository.Nodes;
import com.raaftech.structure.bus.repository.Types;
import com.raaftech.structure.graph.Component;
import com.raaftech.structure.graph.Group;
import com.raaftech.structure.graph.repositories.InstanceRepository;
import com.raaftech.structure.graph.repositories.TypeRepository;
import com.raaftech.structure.helper.ConfigurationProxy;
import com.raaftech.structure.helper.ProcessRunner;
import com.raaftech.structure.helper.loader.BundleLoader;
import io.vertx.core.DeploymentOptions;
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
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.neo4j.support.query.CypherQueryExecutor;

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
        Long workerTimeout = Long.parseLong(configuration.getConfiguration("workertimeout").orElse("300000000000"));
        vertx = Vertx.vertx(new VertxOptions().setMaxWorkerExecuteTime(workerTimeout));


        Context.setVertx(vertx);

        // Enable bridging on /eventbus, and add some in- and outbound permissions.
        BridgeOptions bridgeOptions = new BridgeOptions()
                .addInboundPermitted(new PermittedOptions().setAddressRegex("com\\.raaftech\\.structure\\..*"))
                .addOutboundPermitted(new PermittedOptions().setAddressRegex("com\\.raaftech\\.structure\\..*"));

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
        Configuration.init(configuration);

        // Deploy our main actors.
        vertx.deployVerticle("com.raaftech.structure.bus.manager.StructureManager", ar -> {
            if (ar.succeeded()) {
                log.info("Deployed StructureManager with id: {}", ar.result());
            }
        });
        vertx.deployVerticle("com.raaftech.structure.bus.manager.InstrumentManager", new DeploymentOptions().setWorker(false), ar -> {
            if (ar.succeeded()) {
                log.info("Deployed InstrumentManager with id: {}", ar.result());
            }
        });
        vertx.deployVerticle("com.raaftech.structure.bus.manager.EntityManager", ar -> {
            if (ar.succeeded()) {
                log.info("Deployed EntityManager with id: {}", ar.result());
            }
        });

        final int jobpool = Integer.parseInt(configuration.getConfiguration("jobpool").orElse("50"));
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

        // Wait a second so debug messages are done
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.error(e.getMessage());
            throw e;
        }

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
            handleUnrecoverableLoadingError(null, e, 1, "Unrecoverable error while setting schema namespace");
        }
        httpServer.requestHandler(router::accept).listen(Configuration.getWebPort(), Configuration.getWebHost());

        init(vertx);
        log.info("Structure is running at {}", Configuration.getSchemaNamespace());
    }

    public static void init(Vertx vertx) {
        final InstanceRepository instanceRepository = Context.getInstanceRepository();
        final TypeRepository typeRepository = Context.getTypeRepository();

        Transaction tx = Context.getGraphDatabase().beginTx();
        try {
            BundleLoader bundleLoader = new BundleLoader();
            bundleLoader.loadTypes();
            tx.success();
            tx.close();

            tx = Context.getGraphDatabase().beginTx();
            bundleLoader.loadInstances();
            tx.success();
        } catch (Exception e) {
            handleUnrecoverableLoadingError(tx, e, 1, "Unrecoverable error while loading bundles. Shutting down...");
        } finally {
            tx.close();
        }

        // Deploy TypeActors.
        log.debug("Deploying TypeActors..");
        tx = Context.getGraphDatabase().beginTx();

        try {
            StreamSupport.stream(typeRepository.findAll().spliterator(), false)
                    .forEach(TypeActor::deploy);
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
