package com.raaftech.structure.base;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

import java.io.IOException;

import com.raaftech.structure.helper.PluginLoader;

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
        // If we're in debug mode allow direct access to handler plugins on the eventbus
        // and serve the debug page
        if (serveDebug) {
            router.route("/debug/*").handler(StaticHandler.create().setWebRoot("web/html/debug/"));
            bridgeOptions.addInboundPermitted(new PermittedOptions().setAddressRegex(
                        "com.raaftech.structure.plugins.*"));
        }

        sockJsHandler.bridge(bridgeOptions);
        router.route("/eventbus/*").handler(sockJsHandler);
        httpServer.requestHandler(router::accept).listen(port);

        vertx.deployVerticle("com.raaftech.structure.actor.FrameHelper");
        vertx.deployVerticle("com.raaftech.structure.actor.StateManager");
        vertx.deployVerticle("com.raaftech.structure.actor.PluginManager");
        vertx.deployVerticle("com.raaftech.structure.actor.SchemaManager",
                new DeploymentOptions().setWorker(true));

        PluginLoader pluginLoader = new PluginLoader(vertx);
        try {
            pluginLoader.loadPlugins();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Structure is running at http://localhost:" + port + "/html/");
        if (serveDebug) {
            System.out.println("Debug interface is running at http://localhost:" + port + "/debug/");
        }
    }
}
