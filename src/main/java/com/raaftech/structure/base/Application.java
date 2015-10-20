package com.raaftech.structure.base;

import java.util.Iterator;

import io.vertx.core.DeploymentOptions;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.raaftech.structure.graph.InstanceNode;
import com.raaftech.structure.graph.TemplateNode;
import com.raaftech.structure.graph.TypeNode;
import com.raaftech.structure.graph.repositories.InstanceNodeRepository;
import com.raaftech.structure.graph.repositories.TemplateNodeRepository;
import com.raaftech.structure.graph.repositories.TypeNodeRepository;
import com.raaftech.structure.node.InstanceNodeActor;
import com.raaftech.structure.node.TemplateNodeActor;
import com.raaftech.structure.node.TypeNodeActor;
import com.raaftech.structure.repository.InstanceNodes;
import com.raaftech.structure.repository.Nodes;
import com.raaftech.structure.repository.SchemaNodes;
import com.raaftech.structure.repository.TemplateNodes;
import com.raaftech.structure.repository.TypeNodes;

import ch.qos.logback.classic.Level;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

/**
 * The main application entry point.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-11-17
 */
public class Application {

    final static Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        Vertx vertx;
        HttpServer httpServer;
        Router router;
        SockJSHandler sockJsHandler;
        final boolean serveDebug;
        final int port = 8080;

        // Black magic.
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        if (args.length < 1) {
            serveDebug = false;
            ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            log.info("Setting log level to INFO..");
            root.setLevel(Level.INFO);
        } else {
            serveDebug = args[0].equals("--debug");
            ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            log.debug("Setting log level to DEBUG..");
            root.setLevel(Level.DEBUG);
        }

        // Tell the user we're starting up.
        log.info("Structure is starting up..");

        // Setup vertx with a maxWorkerExecuteTime of 5 minutes.
        // TODO: Make maxWorkerExecuteTime user configurable?
        vertx = Vertx.vertx(new VertxOptions().setMaxWorkerExecuteTime(5l * 60 * 1000 * 1000000));
        router = Router.router(vertx);
        sockJsHandler = SockJSHandler.create(vertx);
        httpServer = vertx.createHttpServer();

        Context.INSTANCE.setVertx(vertx);

        // Enable bridging on /eventbus, and add some in- and outbound permissions.
        BridgeOptions bridgeOptions = new BridgeOptions()
                .addInboundPermitted(new PermittedOptions().setAddress("com.raaftech.structure.manager.schemamanager"))
                .addInboundPermitted(new PermittedOptions().setAddress("com.raaftech.structure.manager.pluginmanager"))
                .addInboundPermitted(new PermittedOptions().setAddress("com.raaftech.structure.manager.statemanager"))
                .addInboundPermitted(new PermittedOptions().setAddress("com.raaftech.structure.manager.discoverymanager"));

        // Enable sockJS Server on our HTTP server.
        sockJsHandler.bridge(bridgeOptions);
        router.route("/eventbus/*").handler(sockJsHandler);
        
        // If we're in debug mode allow direct access to everything
        // on the eventbus and serve the debug page.
        if (serveDebug) {
            log.debug("Setting InboundPermitted to any address (\".*\")");
            bridgeOptions.addInboundPermitted(new PermittedOptions().setAddressRegex(".*"));
        }
        
        // Setup default route.
        router.route("/*").handler(StaticHandler.create().setWebRoot("web/"));

        httpServer.requestHandler(router::accept).listen(port);

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
        TypeNodeRepository typeNodeRepository = Context.INSTANCE.getTypeNodeRepository();
        Transaction tx = Context.INSTANCE.getGraphDatabase().beginTx();
        Iterator<TypeNode> typeNodeIterator = typeNodeRepository.findAll().iterator();
        while (typeNodeIterator.hasNext()) {
            TypeNode typeNode = typeNodeIterator.next();
            TypeNodeActor typeNodeActor = new TypeNodeActor(typeNode);
            vertx.deployVerticle(typeNodeActor);
        }
        tx.close();

        // Deploy InstanceNodeActors.
        log.debug("Deploying InstanceNodeActors..");
        InstanceNodeRepository instanceNodeRepository = Context.INSTANCE.getInstanceNodeRepository();
        tx = Context.INSTANCE.getGraphDatabase().beginTx();
        Iterator<InstanceNode> instanceNodeIterator = instanceNodeRepository.findAll().iterator();
        while (instanceNodeIterator.hasNext()) {
            InstanceNode instanceNode = instanceNodeIterator.next();
            InstanceNodeActor instanceNodeActor = new InstanceNodeActor(instanceNode);
            vertx.deployVerticle(instanceNodeActor);
        }
        tx.close();

        // Deploy TemplateNodeActors.
        log.debug("Deploying TemplateNodeActors..");
        TemplateNodeRepository templateNodeRepository = Context.INSTANCE.getTemplateNodeRepository();
        tx = Context.INSTANCE.getGraphDatabase().beginTx();
        Iterator<TemplateNode> templateNodeIterator = templateNodeRepository.findAll().iterator();
        while (templateNodeIterator.hasNext()) {
            TemplateNode templateNode = templateNodeIterator.next();
            TemplateNodeActor templateNodeActor = new TemplateNodeActor(templateNode);
            vertx.deployVerticle(templateNodeActor);
        }
        tx.close();

        // Wait a second so debug messages are done
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.error(e.getMessage());;
        }

        // A friendly message telling the user we're done.
        log.info("Structure is running at http://localhost:" + port);
        if (serveDebug) {
            log.debug("Debug interface is running at http://localhost:" + port + "/debug/");
        }
    }
}
