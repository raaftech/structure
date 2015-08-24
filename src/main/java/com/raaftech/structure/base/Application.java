package com.raaftech.structure.base;

import java.util.Iterator;

import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raaftech.structure.actor.node.InstanceNodeActor;
import com.raaftech.structure.actor.node.TemplateNodeActor;
import com.raaftech.structure.actor.node.TypeNodeActor;
import com.raaftech.structure.actor.repository.InstanceNodes;
import com.raaftech.structure.actor.repository.Nodes;
import com.raaftech.structure.actor.repository.SchemaNodes;
import com.raaftech.structure.actor.repository.TemplateNodes;
import com.raaftech.structure.actor.repository.TypeNodes;
import com.raaftech.structure.graph.InstanceNode;
import com.raaftech.structure.graph.TemplateNode;
import com.raaftech.structure.graph.TypeNode;
import com.raaftech.structure.graph.repositories.InstanceNodeRepository;
import com.raaftech.structure.graph.repositories.TemplateNodeRepository;
import com.raaftech.structure.graph.repositories.TypeNodeRepository;

import ch.qos.logback.classic.Level;
import io.vertx.core.DeploymentOptions;
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

    final static Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        Vertx vertx;
        HttpServer httpServer;
        Router router;
        SockJSHandler sockJsHandler;
        final boolean serveDebug;
        final int port = 8080;

        if (args.length < 1) {
            serveDebug = false;
        } else {
            serveDebug = args[0].equals("--debug");
            //ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            ch.qos.logback.classic.Logger classic = (ch.qos.logback.classic.Logger) logger;
            classic.setLevel(Level.DEBUG);
            logger.debug("Enabling debug output..");
        }

        // Setup vertx with a maxWorkerExecuteTime of 5 minutes.
        // TODO: Make maxWorkerExecuteTime user configurable?
        vertx = Vertx.vertx(new VertxOptions().setMaxWorkerExecuteTime(5l * 60 * 1000 * 1000000));
        router = Router.router(vertx);
        sockJsHandler = SockJSHandler.create(vertx);
        httpServer = vertx.createHttpServer();

        Context.INSTANCE.setVertx(vertx);

        // Enable bridging on /eventbus, and add some in- and outbound permissions.
        BridgeOptions bridgeOptions = new BridgeOptions()
                .addInboundPermitted(new PermittedOptions().setAddress("com.raaftech.structure.actor.schemamanager"))
                .addInboundPermitted(new PermittedOptions().setAddress("com.raaftech.structure.actor.pluginmanager"))
                .addInboundPermitted(new PermittedOptions().setAddress("com.raaftech.structure.actor.statemanager"))
                .addInboundPermitted(new PermittedOptions().setAddress("com.raaftech.structure.actor.discoverymanager"));

        // If we're in debug mode allow direct access to everything
        // on the eventbus and serve the debug page.
        if (serveDebug) {
            router.route("/debug/*").handler(StaticHandler.create().setWebRoot("web/html/debug/"));
            bridgeOptions.addInboundPermitted(new PermittedOptions().setAddressRegex(".*"));
        }

        // Enable sockJS Server on our HTTP server.
        sockJsHandler.bridge(bridgeOptions);
        router.route("/eventbus/*").handler(sockJsHandler);
        httpServer.requestHandler(router::accept).listen(port);

        // Deploy our main actors.
        vertx.deployVerticle("com.raaftech.structure.actor.SchemaManager");
        vertx.deployVerticle("com.raaftech.structure.actor.PluginManager");
        vertx.deployVerticle("com.raaftech.structure.actor.StateManager",
                new DeploymentOptions().setWorker(true));
        vertx.deployVerticle("com.raaftech.structure.actor.DiscoveryManager");

        // A tryout state handler that can can check if an IP address is up.
        // Will be done differently, but kept as reference for a bit.
        // (hint: state handlers are scripts executed by a generic handler)
        vertx.deployVerticle("com.raaftech.structure.actor.worker.ScanWorker",
                new DeploymentOptions().setWorker(true).setInstances(256));

        // Deploy interfaces to the NodeRepositories.
        vertx.deployVerticle(new Nodes(Context.INSTANCE.getNodeRepository()));
        vertx.deployVerticle(new InstanceNodes(Context.INSTANCE.getInstanceNodeRepository()));
        vertx.deployVerticle(new SchemaNodes(Context.INSTANCE.getSchemaNodeRepository()));
        vertx.deployVerticle(new TemplateNodes(Context.INSTANCE.getTemplateNodeRepository()));
        vertx.deployVerticle(new TypeNodes(Context.INSTANCE.getTypeNodeRepository()));

        // Load system plugins (not sure if this will be superseded by new state scripts idea).
        //PluginLoader pluginLoader = new PluginLoader(vertx);
        //pluginLoader.loadSystemPlugins();

        // Wait a second?
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            logger.error(e.getMessage());;
        }

        // Deploy TypeNodeActors.
        logger.info("Deploying TypeNodeActors..");
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
        logger.info("Deploying InstanceNodeActors..");
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
        logger.info("Deploying TemplateNodeActors..");
        TemplateNodeRepository templateNodeRepository = Context.INSTANCE.getTemplateNodeRepository();
        tx = Context.INSTANCE.getGraphDatabase().beginTx();
        Iterator<TemplateNode> templateNodeIterator = templateNodeRepository.findAll().iterator();
        while (templateNodeIterator.hasNext()) {
            TemplateNode templateNode = templateNodeIterator.next();
            TemplateNodeActor templateNodeActor = new TemplateNodeActor(templateNode);
            vertx.deployVerticle(templateNodeActor);
        }
        tx.close();

        // A friendly message telling the user we're done.
        logger.info("Structure is running at http://localhost:" + port + "/html/");
        if (serveDebug) {
            logger.debug("Debug interface is running at http://localhost:" + port + "/debug/");
        }
    }
}
