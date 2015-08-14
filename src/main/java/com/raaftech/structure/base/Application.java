package com.raaftech.structure.base;

import com.raaftech.structure.actor.node.InstanceNodeActor;
import com.raaftech.structure.actor.node.TemplateNodeActor;
import com.raaftech.structure.actor.node.TypeNodeActor;
import com.raaftech.structure.actor.repository.*;
import com.raaftech.structure.graph.InstanceNode;
import com.raaftech.structure.graph.TemplateNode;
import com.raaftech.structure.graph.TypeNode;
import com.raaftech.structure.graph.repositories.InstanceNodeRepository;
import com.raaftech.structure.graph.repositories.TemplateNodeRepository;
import com.raaftech.structure.graph.repositories.TypeNodeRepository;
import com.raaftech.structure.helper.PluginLoader;
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

import java.util.Iterator;

/**
 * Give a description of Application here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-11-17
 */
public class Application {

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
        }

        // Setup vertx with a maxWorkerExecuteTime of 5 minutes
        // TODO: Make maxWorkerExecuteTime user configurable?
        vertx = Vertx.vertx(new VertxOptions().setMaxWorkerExecuteTime(5l * 60 * 1000 * 1000000));
        router = Router.router(vertx);
        sockJsHandler = SockJSHandler.create(vertx);
        httpServer = vertx.createHttpServer();

        Context.INSTANCE.setVertx(vertx);

        // Enable sockJS Server on our HTTP server
        // Enable bridging on /eventbus, and add some in- and outbound permissions
        BridgeOptions bridgeOptions  = new BridgeOptions()
                .addInboundPermitted(new PermittedOptions().setAddress(
                        "com.raaftech.structure.actor.framehelper"))
                .addInboundPermitted(new PermittedOptions().setAddress(
                        "com.raaftech.structure.actor.schemamanager"))
                .addInboundPermitted(new PermittedOptions().setAddress(
                        "com.raaftech.structure.actor.statemanager"))
                .addInboundPermitted(new PermittedOptions().setAddress(
                        "com.raaftech.structure.actor.pluginmanager"));
        // If we're in debug mode allow direct access to everything on the eventbus
        // and serve the debug page
        if (serveDebug) {
            router.route("/debug/*").handler(StaticHandler.create().setWebRoot("web/html/debug/"));
            bridgeOptions.addInboundPermitted(new PermittedOptions().setAddressRegex(".*"));
        }

        sockJsHandler.bridge(bridgeOptions);
        router.route("/eventbus/*").handler(sockJsHandler);
        httpServer.requestHandler(router::accept).listen(port);

        vertx.deployVerticle("com.raaftech.structure.actor.StateManager");
        vertx.deployVerticle("com.raaftech.structure.actor.PluginManager");
        vertx.deployVerticle("com.raaftech.structure.actor.SchemaManager",
                new DeploymentOptions().setWorker(true));
        vertx.deployVerticle("com.raaftech.structure.actor.DiscoveryManager");
        vertx.deployVerticle("com.raaftech.structure.actor.worker.ScanWorker",
                new DeploymentOptions().setWorker(true).setInstances(256));

        // Deploy interfaces to the NodeRepositories
        vertx.deployVerticle(new Nodes(Context.INSTANCE.getNodeRepository()));
        vertx.deployVerticle(new InstanceNodes(Context.INSTANCE.getInstanceNodeRepository()));
        vertx.deployVerticle(new SchemaNodes(Context.INSTANCE.getSchemaNodeRepository()));
        vertx.deployVerticle(new TemplateNodes(Context.INSTANCE.getTemplateNodeRepository()));
        vertx.deployVerticle(new TypeNodes(Context.INSTANCE.getTypeNodeRepository()));

        PluginLoader pluginLoader = new PluginLoader(vertx);
        pluginLoader.loadSystemPlugins();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Structure is running at http://localhost:" + port + "/html/");
        if (serveDebug) {
            System.out.println("Debug interface is running at http://localhost:" + port + "/debug/");
        }

        System.out.print("Deploying TypeNodeActors...");
        TypeNodeRepository typeNodeRepository = Context.INSTANCE.getTypeNodeRepository();
        Transaction tx = Context.INSTANCE.getGraphDatabase().beginTx();
        Iterator<TypeNode> typeNodeIterator = typeNodeRepository.findAll().iterator();
        while (typeNodeIterator.hasNext()) {
            TypeNode typeNode = typeNodeIterator.next();
            TypeNodeActor typeNodeActor = new TypeNodeActor(typeNode);
            vertx.deployVerticle(typeNodeActor);
        }
        tx.close();
        System.out.println("done.");

        System.out.print("Deploying InstanceNodeActors...");
        InstanceNodeRepository instanceNodeRepository = Context.INSTANCE.getInstanceNodeRepository();
        tx = Context.INSTANCE.getGraphDatabase().beginTx();
        Iterator<InstanceNode> instanceNodeIterator = instanceNodeRepository.findAll().iterator();
        while (instanceNodeIterator.hasNext()) {
            InstanceNode instanceNode = instanceNodeIterator.next();
            InstanceNodeActor instanceNodeActor = new InstanceNodeActor(instanceNode);
            vertx.deployVerticle(instanceNodeActor);
        }
        tx.close();
        System.out.println("done.");

        System.out.print("Deploying TemplateNodeActors...");
        TemplateNodeRepository templateNodeRepository = Context.INSTANCE.getTemplateNodeRepository();
        tx = Context.INSTANCE.getGraphDatabase().beginTx();
        Iterator<TemplateNode> templateNodeIterator = templateNodeRepository.findAll().iterator();
        while (templateNodeIterator.hasNext()) {
            TemplateNode templateNode = templateNodeIterator.next();
            TemplateNodeActor templateNodeActor = new TemplateNodeActor(templateNode);
            vertx.deployVerticle(templateNodeActor);
        }
        tx.close();
        System.out.println("done.");
    }
}
