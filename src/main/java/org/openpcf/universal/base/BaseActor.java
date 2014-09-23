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

    JsonNode schemaForRequest;
    JsonNode schemaForResponse;
    JsonNode schemaForManual;

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

        // Initialize schema documents.
        try {
            schemaForRequest = JsonLoader.fromResource("/request.schema.json");
            schemaForResponse = JsonLoader.fromResource("/response.schema.json");
            schemaForManual = JsonLoader.fromResource("/manual.schema.json");
        } catch (IOException x) {
            log("error", x.getMessage());
        }
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

    // The validator.
    public JsonNode validate(JsonNode schemaDoc, JsonNode jsonDoc) {
        try {
            final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
            final JsonSchema schema = factory.getJsonSchema(schemaDoc);
            ProcessingReport report = schema.validate(jsonDoc);
            if (!report.isSuccess()) {
                log("error", report.toString());
            }
        } catch (ProcessingException x) {
            log("error", x.getMessage());
        }

        return jsonDoc;
    }

    // The response helper.
    public JsonNode response(JsonNode request, JsonNode result) {
        ObjectNode response = JsonNodeFactory.instance.objectNode();

        response.put("request", request);
        response.put("result", result);

        return response;
    }

    // The response helper (with statistics).
    public JsonNode response(JsonNode request, JsonNode result, JsonNode stats) {
        ObjectNode response = JsonNodeFactory.instance.objectNode();

        response.put("request", request);
        response.put("result", result);
        response.put("stats", stats);

        return response;
    }

    // Check if some string is valid JSON.
    public boolean isValidJson(String body) {
        try {
            new JsonObject(body);
        } catch (Exception x) {
            return false;
        }
        return true;
    }

    // Check if a string is alpha-numeric.
    public boolean isAlphaNumeric(String str) {
        return str.matches("[0-9a-zA-Z]+");
    }

    // Filter a given JsonObject body and enforce basic validity.
    public JsonObject bodyFilter(JsonObject body) {

        String msghDefault = "showUsage";
        String msghRetrieved;
        String msghAlphaNumeric;

        JsonObject argsDefault = new JsonObject("{}");
        String argsRetrieved;

        // Verify if msgh field exists.
        if (body.containsField("msgh")) {
            msghRetrieved = body.getString("msgh");
            // Verify if msgh is a alpha-numeric string.
            if (isAlphaNumeric(msghRetrieved)) {
                msghAlphaNumeric = msghRetrieved;
                // Verify if msgh is an existing handler.
                if (!handlers.containsKey(msghAlphaNumeric)) {
                    // Unknown message handler specified, replace msgh.
                    log("error", "Field \"msgh\" not a valid message handler: " + msghAlphaNumeric);
                    log("warn", "Valid \"msgh\" values are: " + handlers.keySet().toString());
                    log("warn",  "Setting \"msgh\" to default value: " + msghDefault);
                    body.removeField("msgh");
                    body.putString("msgh", msghDefault);
                }
            } else {
                // Invalid characters in msgh, replace msgh.
                log("error", "Field \"msgh\" contains invalid characters: " + msghRetrieved);
                log("warn",  "Setting \"msgh\" to default value: " + msghDefault);
                body.removeField("msgh");
                body.putString("msgh", msghDefault);
            }
        } else {
            // Non-existing msgh, adding default msgh.
            log("warn", "Field \"msgh\" not in JSON body");
            log("warn",  "Setting \"msgh\" to default value: " + msghDefault);
            body.putString("msgh", msghDefault);
        }

        // Verify if args field exists.
        if (body.containsField("args")) {
            argsRetrieved = body.getValue("args").toString();
            // Verify if args is a json object.
            if (!isValidJson(argsRetrieved)) {
                // Invalid args, replace args.
                log("error", "Field \"args\" not of type JSON object: " + argsRetrieved);
                log("warn",  "Setting \"args\" to empty JSON object: " + argsDefault);
                body.removeField("args");
                body.putObject("args", argsDefault);
            }
        } else {
            // Non-existing args, which is ok, adding default args.
            body.putObject("args", argsDefault);
        }

        return body;
    }

    // The Vert.x Verticle start method.
    public void start() {
        vertx.eventBus().registerHandler(getActorName(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message){
                try {
                    // Request! Convert "Vert.x JsonObject" to "Jackson JsonNode".
                    JsonObject body = bodyFilter(message.body());
                    JsonNode request = validate(schemaForRequest, mapper.readTree(body.toString()));
                    String msgh = request.get("msgh").asText();

                    // Response! Convert "Jackson JsonNode" to "Vert.x JsonObject".
                    JsonObject response = new JsonObject(validate(schemaForResponse, handlers.get(msgh).exec(request)).toString());
                    message.reply(response);
                } catch (Exception x) {
                    log("error", x.getMessage());
                }
            }
        });

        // Tell the console we're up and running.
        log("info", "ready to serve");
    }

    // A public interface for MessageHandler objects.
    public interface MessageHandler {
        public JsonNode exec(JsonNode request);
        public JsonNode help(JsonNode request);
    }

    // The default usage  handler.
    public MessageHandler showUsage() {
        return new MessageHandler() {
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                for (String msgh : handlers.keySet()) {
                    result.put(msgh, handlers.get(msgh).help(request));
                }

                return response(request, result);
            }

            public JsonNode help(JsonNode request) {
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
            public JsonNode exec(JsonNode request) {
                JsonNode result = JsonNodeFactory.instance.objectNode();
                InputStream inputStream = getClass().getClassLoader().getResourceAsStream("release.json");

                try {
                    result = mapper.readTree(inputStream);
                } catch (IOException x) {
                    log("error", x.getMessage());
                }

                return response(request, result);
            }

            public JsonNode help(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                result.put("name", "showVersion");
                result.put("description", "showVersion returns the version of the actor or framework.");

                return result;
            }
        };
    }
}
