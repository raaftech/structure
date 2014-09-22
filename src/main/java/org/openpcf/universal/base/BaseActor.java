// Copyright © 2008-2014 RAAF Technology bv

package org.openpcf.universal.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Give a description of BaseActor here.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-09-15
 */
public abstract class BaseActor extends Verticle {

    Map<String, MessageHandler> handlers;
    Map<String, Loggable> loggers;
    ObjectMapper mapper = new ObjectMapper();

    // Constructor.
    protected BaseActor() {
        // Initialize arrays.
        Method methods[] = this.getClass().getMethods();
        List<Method> messageHandlers = new ArrayList<>();

        // Fill messageHandlers.
        for (Method method : methods) {
            if (method.getReturnType().equals(MessageHandler.class)) {
                messageHandlers.add(method);
            }
        }

        // Create the handlers hash map.
        handlers = new HashMap<>();
        for (Method mh : messageHandlers) {
            String s = mh.getName();
            try {
                handlers.put(s, (MessageHandler) mh.invoke(this));
            } catch (IllegalAccessException | InvocationTargetException x) {
                log("error", "Failed to access or invoke method " + s + ": " + x.getMessage());
            }
        }

        // Create the loggers hash map.
        loggers = new HashMap<>();
        loggers.put("info", new Loggable() {
            public void log(String message) {
                container.logger().info(getActorName() + ": " + message);
            }
        });
        loggers.put("warn", new Loggable() {
            public void log(String message) {
                container.logger().warn(getActorName() + ": " + message);
            }
        });
        loggers.put("error", new Loggable() {
            public void log(String message) {
                container.logger().error(getActorName() + ": " + message);
            }
        });
        loggers.put("fatal", new Loggable() {
            public void log(String message) {
                container.logger().fatal(getActorName() + ": " + message);
            }
        });
        loggers.put("debug", new Loggable() {
            public void log(String message) {
                container.logger().debug(getActorName() + ": " + message);
            }
        });
    }

    // Get the name of the actor (used with registerHandler).
    private String getActorName() {
        return this.getClass().getName().toLowerCase();
    }

    // A private interface for the log helper method.
    private interface Loggable {
        public abstract void log(String message);
    }

    // A logger for actors with nice defaults.
    public void log(String level, String message) {
        if (!loggers.containsKey(level)) {
            container.logger().error(getActorName() + ": invalid level " + level + "specified");
            container.logger().warn(getActorName() + ": setting level to \"info\"");
            level = "info";
        }
        loggers.get(level).log(message);
    }

    // The tryout validator.
    public void testValidator(JsonNode body) {

        try {
            final JsonNode requestSchema = JsonLoader.fromResource("/request.schema.json");
            final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
            final JsonSchema schema = factory.getJsonSchema(requestSchema);
            ProcessingReport report;
            report = schema.validate(body);
            log("info", report.toString());
        } catch (IOException|ProcessingException x) {
            log("error", x.getMessage());
        }
    }

    // The Vert.x Verticle start method.
    public void start() {
        vertx.eventBus().registerHandler(getActorName(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message){

                try {
                    // Request! Convert "Vert.x JsonObject" to "Jackson JsonNode".
                    JsonNode request = mapper.readTree(message.body().toString());
                    String msgh = request.get("msgh").asText();
                    JsonNode args = request.get("args");

                    // Response! Convert "Jackson JsonNode" to "Vert.x JsonObject".
                    JsonObject response = new JsonObject(handlers.get(msgh).exec(args).toString());
                    message.reply(response);
                } catch (Exception x) {
                    log("error", x.getMessage());
                }
            }
        });

        // Tell the console we're up and running.
        log("info", "started");
    }

    // A public interface for MessageHandler objects.
    public interface MessageHandler {
        public JsonNode exec(JsonNode args);
        public JsonNode help(JsonNode args);
    }

    // The default usage  handler.
    public MessageHandler showUsage() {
        return new MessageHandler() {
            public JsonNode exec(JsonNode args) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                for (String msgh : handlers.keySet()) {
                    result.put(msgh, handlers.get(msgh).help(args));
                }
                return result;
            }

            public JsonNode help(JsonNode args) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                result.put("name", "showUsage");
                result.put("description", "showUsage shows a cumulative help message from all message handlers in this actor.");
                return result;
            }
        };
    }

    // The default version handler.
    public MessageHandler showVersion() {
        return new MessageHandler() {
            public JsonNode exec(JsonNode args) {
                JsonNode result = JsonNodeFactory.instance.objectNode();
                InputStream inputStream = getClass().getClassLoader().getResourceAsStream("release.json");

                try {
                    result = mapper.readTree(inputStream);
                } catch (IOException x) {
                    log("error", x.getMessage());
                }

                return result;
            }

            public JsonNode help(JsonNode args) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                result.put("name", "showVersion");
                result.put("description", "showVersion returns the version of the actor or framework.");
                return result;
            }
        };
    }
}
