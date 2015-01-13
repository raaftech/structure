package org.openpcf.universal.base;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.routematcher.RouteMatcher;
import io.vertx.ext.sockjs.BridgeOptions;
import io.vertx.ext.sockjs.SockJSServer;
import io.vertx.ext.sockjs.SockJSServerOptions;

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
        SockJSServer sockJSServer;

        // Setup vertx with a maxWorkerExecuteTime of 5 minutes
        // TODO: Make maxWorkerExecuteTime user configurable?
        vertx = Vertx.vertx(new VertxOptions().setMaxWorkerExecuteTime(5l * 60 * 1000 * 1000000));
        SpringContext.INSTANCE.setVertx(vertx);
        // Setup HTTP server on port 8080
        httpServer = vertx.createHttpServer(new HttpServerOptions().setPort(8080));

        // Setup Routematcher to serve our web-interface
        // This needs to be done before setting up SockJS
        RouteMatcher routeMatcher = RouteMatcher.routeMatcher();
        // Handle our requests
        routeMatcher.matchMethodWithRegEx(HttpMethod.GET, "/.*", httpServerRequest -> {
            if (httpServerRequest.path().contains("..")) {
                // Disallow urls crafted to see resources outside of the webroot
                // TODO: more sophisticated checks
                httpServerRequest.response().setStatusCode(403).end();
            } else if (httpServerRequest.path().equals("/html/")) {
                httpServerRequest.response().sendFile("web/html/index.html");
            } else {
                httpServerRequest.response().sendFile("web/" + httpServerRequest.path());
            }
        });
        httpServer.requestHandler(routeMatcher::accept);

        // Enable sockJS Server on our HTTP server
        sockJSServer = SockJSServer.sockJSServer(vertx, httpServer);
        // Enable bridging on /eventbus, and add some in- and outbound permissions
        sockJSServer.bridge(
                new SockJSServerOptions().setPrefix("/eventbus"),
                new BridgeOptions()
                        .addInboundPermitted(new JsonObject().put(
                                "address", "org.openpcf.universal.actor.framehelper"))
                        .addInboundPermitted(new JsonObject().put(
                                "address", "org.openpcf.universal.actor.schemamanager"))
                        .addInboundPermitted(new JsonObject().put(
                                "address", "org.openpcf.universal.actor.statemanager"))
                        .addOutboundPermitted(new JsonObject().put(
                                "address_re", "org.openpcf.universal.worker" +
                                        ".status_[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
        );

        httpServer.listen();

        vertx.deployVerticle("org.openpcf.universal.actor.FrameHelper");
        vertx.deployVerticle("org.openpcf.universal.actor.StateManager");
        // SchemaManager is blocking, so we're deploying it as a worker.
        vertx.deployVerticle("org.openpcf.universal.actor.SchemaManager", new DeploymentOptions().setWorker(true));
    }
}
