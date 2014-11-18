package org.openpcf.universal.base;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.routematcher.RouteMatcher;
import io.vertx.ext.sockjs.BridgeOptions;
import io.vertx.ext.sockjs.SockJSServer;
import io.vertx.ext.sockjs.SockJSServerOptions;
import org.neo4j.graphdb.GraphDatabaseService;
import org.openpcf.universal.repositories.ComponentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.rest.SpringRestGraphDatabase;
/**
 * Give a description of Application here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-11-17
 */
@Configuration
@EnableNeo4jRepositories(basePackages="org.openpcf.universal.repositories")
public class Application extends Neo4jConfiguration implements CommandLineRunner {

    Vertx vertx;
    HttpServer httpServer;
    SockJSServer sockJSServer;
    EventBus eventBus;

    public Application() {
        setBasePackage("org.openpcf.universal.model");

        // Setup vertx
        vertx = Vertx.vertx();
        JsonObject config = vertx.context().config();
        // Setup HTTP server on port 8080
        httpServer = vertx.createHttpServer(new HttpServerOptions().setPort(8080));

        // Setup Routematcher to serve our web-interface
        // This needs to be done before setting up SockJS
        RouteMatcher routeMatcher = RouteMatcher.routeMatcher();
        // TODO: tighten up the pattern
        routeMatcher.matchMethodWithRegEx(HttpMethod.GET, "/.*", this::handleInterfaceRequest);
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
                                "address", "beacon"))
        );

        httpServer.listen();
        eventBus = vertx.eventBus();
    }

    public void handleInterfaceRequest(HttpServerRequest httpServerRequest) {
        if (httpServerRequest.path().contains("..")) {
            // Disallow urls crafted to see resources outside of the webroot
            // TODO: more sophisticated checks
            httpServerRequest.response().setStatusCode(403).end();
        } else if (httpServerRequest.path().equals("/html/")) {
            httpServerRequest.response().sendFile("web/html/index.html");
        } else {
            httpServerRequest.response().sendFile("web/" + httpServerRequest.path());
        }
    }

    @Bean
    GraphDatabaseService graphDatabaseService() {
        return new SpringRestGraphDatabase("http://localhost:7474/db/data");
    }

    @Autowired
    ComponentRepository componentRepository;

    public void run(String... args) throws Exception {
        vertx.deployVerticle("org.openpcf.universal.actor.FrameHelper");
        vertx.deployVerticle("org.openpcf.universal.actor.SchemaManager");
        vertx.deployVerticle("org.openpcf.universal.actor.StateManager");
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
