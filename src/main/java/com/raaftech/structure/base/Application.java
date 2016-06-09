package com.raaftech.structure.base;

import ch.qos.logback.classic.Level;
import com.raaftech.structure.graph.InstanceNode;
import com.raaftech.structure.graph.TemplateNode;
import com.raaftech.structure.graph.TypeNode;
import com.raaftech.structure.graph.repositories.InstanceNodeRepository;
import com.raaftech.structure.graph.repositories.TemplateNodeRepository;
import com.raaftech.structure.graph.repositories.TypeNodeRepository;
import com.raaftech.structure.helper.InstrumentLoader;
import com.raaftech.structure.helper.SchemaManagerUpdate;
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
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.File;
import java.util.Arrays;

/**
 * The main application entry point.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-11-17
 */
public class Application {

    final static Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        final boolean serveDebug;
        if (args.length > 0 && args[0].equals("--debug")) {
            serveDebug = true;
            System.setProperty("debugMode", "true");
        } else {
            serveDebug = false;
        }
        Vertx vertx;
        HttpServer httpServer;
        Router router;
        SockJSHandler sockJsHandler;
        final int port = 8080;

        final TypeNodeRepository typeNodeRepository = Context.INSTANCE.getTypeNodeRepository();
        final InstanceNodeRepository instanceNodeRepository = Context.INSTANCE.getInstanceNodeRepository();
        final TemplateNodeRepository templateNodeRepository = Context.INSTANCE.getTemplateNodeRepository();

        // Black magic.
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        if (serveDebug) {
            ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            log.debug("Setting log level to DEBUG..");
            root.setLevel(Level.DEBUG);
        } else {
            ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            log.info("Setting log level to INFO..");
            root.setLevel(Level.INFO);
        }

        // Tell the user we're starting up.
        log.info("Structure is starting up..");

        // Setup vertx with a maxWorkerExecuteTime of 5 minutes.
        // TODO: Make maxWorkerExecuteTime user configurable?
        vertx = Vertx.vertx(new VertxOptions().setMaxWorkerExecuteTime(5L * 60 * 1000 * 1000000));
        router = Router.router(vertx);
        sockJsHandler = SockJSHandler.create(vertx);
        httpServer = vertx.createHttpServer();

        Context.INSTANCE.setVertx(vertx);

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
        
        // If we're in debug mode allow direct access to everything
        // on the eventbus and serve the debug page.
        if (serveDebug) {
            log.debug("Setting InboundPermitted and OutboundPermitted to any address (\".*\")");
            bridgeOptions.addInboundPermitted(new PermittedOptions().setAddressRegex(".*"));
            bridgeOptions.addOutboundPermitted(new PermittedOptions().setAddressRegex(".*"));
        }
        
        // Setup default route.
        router.route("/*").handler(StaticHandler.create().setWebRoot("web/"));

        // Apply schema if it isn't applied yet.
        log.info("Checking for Structure's type hierarchy.");
        if (typeNodeRepository.findByUuid("b1bc963a-19cc-4518-aecb-a35d028c49e3") == null) {
            log.info("Didn't find the type hierarchy, creating it.");
            new SchemaManagerUpdate().applySchemaUpdates();
        } else {
            log.info("Type hierarchy found.");
        }

        // Deploy our main actors.
        vertx.deployVerticle("com.raaftech.structure.manager.BusManager", ar -> {
            if (ar.succeeded()) {
                log.info("Deployed BusManager with id: {}", ar.result());
            }
        });
        vertx.deployVerticle("com.raaftech.structure.manager.SchemaManager", new DeploymentOptions().setWorker(true), ar -> {
            if (ar.succeeded()) {
                log.info("Deployed SchemaManager with id: {}", ar.result());
            }
        });
        vertx.deployVerticle("com.raaftech.structure.manager.ServiceStatusManager", new DeploymentOptions().setWorker(true), ar -> {
            if (ar.succeeded()) {
                log.info("Deployed ServiceStatusManager with id: {}", ar.result());
            }
        });
        int jobpool = 50;
        if (System.getProperty("jobpool") != null) {
            jobpool = Integer.parseInt(System.getProperty("jobpool"));
        }
        vertx.deployVerticle(new TaskManager(jobpool), ar -> {
            if (ar.succeeded()) {
                log.info("Deployed SchemaManager with id: {}", ar.result());
            }
        });

        // Deploy interfaces to the NodeRepositories.
        vertx.deployVerticle(new Nodes(Context.INSTANCE.getNodeRepository()), ar -> {
            if (ar.succeeded()) {
                log.info("Deployed NodeRepository with id: {}", ar.result());
            }
        });
        vertx.deployVerticle(new InstanceNodes(Context.INSTANCE.getInstanceNodeRepository()), ar -> {
            if (ar.succeeded()) {
                log.info("Deployed InstanceNodeRepository with id: {}", ar.result());
            }
        });
        vertx.deployVerticle(new SchemaNodes(Context.INSTANCE.getSchemaNodeRepository()), ar -> {
            if (ar.succeeded()) {
                log.info("Deployed SchemaNodeRepository with id: {}", ar.result());
            }
        });
        vertx.deployVerticle(new TemplateNodes(Context.INSTANCE.getTemplateNodeRepository()), ar -> {
            if (ar.succeeded()) {
                log.info("Deployed TemplateNodeRepository with id: {}", ar.result());
            }
        });
        vertx.deployVerticle(new TypeNodes(Context.INSTANCE.getTypeNodeRepository()), ar -> {
            if (ar.succeeded()) {
                log.info("Deployed TypeNodeRepository with id: {}", ar.result());
            }
        });

        // Deploy TypeNodeActors.
        log.debug("Deploying TypeNodeActors..");
        Transaction tx = Context.INSTANCE.getGraphDatabase().beginTx();
        for (TypeNode typeNode : typeNodeRepository.findAll()) {
            TypeNodeActor typeNodeActor = new TypeNodeActor(typeNode);
            vertx.deployVerticle(typeNodeActor);
        }
        tx.close();

        // Deploy InstanceNodeActors.
        log.debug("Deploying InstanceNodeActors..");
        tx = Context.INSTANCE.getGraphDatabase().beginTx();
        for (InstanceNode instanceNode : instanceNodeRepository.findAll()) {
            InstanceNodeActor instanceNodeActor = new InstanceNodeActor(instanceNode);
            vertx.deployVerticle(instanceNodeActor, new DeploymentOptions().setWorker(true));
        }
        tx.close();

        // Deploy TemplateNodeActors.
        log.debug("Deploying TemplateNodeActors..");
        tx = Context.INSTANCE.getGraphDatabase().beginTx();
        for (TemplateNode templateNode : templateNodeRepository.findAll()) {
            TemplateNodeActor templateNodeActor = new TemplateNodeActor(templateNode);
            vertx.deployVerticle(templateNodeActor);
        }
        tx.close();

        // Load all instruments
        log.info("Purging instruments.");
        instanceNodeRepository.purgeHandlers();
        if (System.getProperty("instruments") == null) {
            System.setProperty("instruments", new File("ht").getAbsolutePath());
        }
        String instruments = System.getProperty("instruments");
        Arrays.asList(instruments.split(",")).stream()
                .distinct()
                .forEach(InstrumentLoader::loadHandlers);

        // Wait a second so debug messages are done
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }

        // A friendly message telling the user we're done.
        httpServer.requestHandler(router::accept).listen(port);
        log.info("Structure is running at http://localhost:" + port);
    }
}
