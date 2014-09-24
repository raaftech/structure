// Copyright © 2008-2014 RAAF Technology bv

package org.openpcf.universal.base;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import javax.ws.rs.core.MediaType;
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

    // The Neo4j cypher query helper.
    public JsonNode cypherQuery(JsonNode query) {
        final String SERVER_ROOT_URI = "http://localhost:7474/db/data/";
        JsonNode result = JsonNodeFactory.instance.objectNode();

        Client client = Client.create();
        WebResource cypher = client.resource(SERVER_ROOT_URI + "cypher");

        ClientResponse cypherResponse = cypher
                .accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON )
                .entity( query.toString() )
                .post( ClientResponse.class );

        try {
            result = mapper.readTree(cypherResponse.getEntity(String.class));
        } catch (IOException x) {
            log("error", x.getMessage());
        }

        cypherResponse.close();

        return result;
    }

    // The response helper.
    public JsonNode response(JsonNode request, JsonNode result, int exit) {
        ObjectNode response = JsonNodeFactory.instance.objectNode();

        response.put("request", request);
        response.put("result", result);
        response.put("exit", exit);

        return response;
    }

    // The response helper (with statistics).
    public JsonNode response(JsonNode request, JsonNode result, int exit, JsonNode stats) {
        ObjectNode response = JsonNodeFactory.instance.objectNode();

        response.put("request", request);
        response.put("result", result);
        response.put("exit", exit);
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
    public JsonNode requestFilter(JsonObject body) {

        // Bodies; original and sanitized.
        JsonNode bodyRetrieved;
        ObjectNode bodyFiltered = JsonNodeFactory.instance.objectNode();

        // Defaults for msgh, args and error.
        String msghDefault = "showUsage";
        String msghRetrieved;
        String msghAlphaNumeric;
        ObjectNode argsDefault = JsonNodeFactory.instance.objectNode();
        JsonNode argsRetrieved;
        ObjectNode errorDefault = JsonNodeFactory.instance.objectNode();

        // Assumed validity (we're optimists).
        Boolean valid = true;

        // Validate bodyRetrieved, construct sanitized bodyFiltered.
        try {
            bodyRetrieved = validate(schemaForRequest, mapper.readTree(body.toString()));

            // Verify if msgh field exists.
            if (bodyRetrieved.has("msgh")) {
                msghRetrieved = bodyRetrieved.get("msgh").asText();
                // Verify if msgh is a alpha-numeric string.
                if (isAlphaNumeric(msghRetrieved)) {
                    msghAlphaNumeric = msghRetrieved;
                    // Verify if msgh is an existing handler.
                    if (handlers.containsKey(msghAlphaNumeric)) {
                        bodyFiltered.put("msgh", msghRetrieved);
                    } else {
                        // Unknown message handler specified, replace msgh.
                        log("error", "Field \"msgh\" not a valid message handler: " + msghAlphaNumeric);
                        valid = false;
                        log("warn", "Valid \"msgh\" values are: " + handlers.keySet().toString());
                        log("warn", "Setting \"msgh\" to default value: " + msghDefault);
                        bodyFiltered.put("msgh", msghDefault);
                    }
                } else {
                    // Invalid characters in msgh, replace msgh.
                    log("error", "Field \"msgh\" contains invalid characters: " + msghRetrieved);
                    valid = false;

                    log("warn", "Setting \"msgh\" to default value: " + msghDefault);
                    bodyFiltered.put("msgh", msghDefault);
                }
            } else {
                // Non-existing msgh, adding default msgh.
                log("error", "Field \"msgh\" not in JSON body");
                valid = false;

                log("warn", "Setting \"msgh\" to default value: " + msghDefault);
                bodyFiltered.put("msgh", msghDefault);
            }

            // Verify if args field exists.
            if (bodyRetrieved.has("args")) {
                argsRetrieved = bodyRetrieved.get("args");
                // Verify if args is a json object.
                if (isValidJson(argsRetrieved.toString())) {
                    bodyFiltered.put("args", argsRetrieved);
                } else {
                    // Invalid args, replace args.
                    log("error", "Field \"args\" not of type JSON object: " + argsRetrieved);
                    valid = false;

                    log("warn", "Setting \"args\" to empty JSON object: " + argsDefault);
                    bodyFiltered.put("args", argsDefault);
                }
            } else {
                // Non-existing args, which is ok, adding default args.
                bodyFiltered.put("args", argsDefault);
            }

            // Set validity.
            bodyFiltered.put("valid", valid);

            // If an error occurred, copy the original body to error.
            if (!valid) {
                log("warn", "Copying original request body to error object");
                errorDefault.put("original-request", bodyRetrieved);
                bodyFiltered.put("error", errorDefault);
            }

        } catch (Exception x) {
            log("error", x.getMessage());

        }

        log("info", bodyFiltered.toString());
        return bodyFiltered;
    }

    // The Vert.x Verticle start method.
    public void start() {
        vertx.eventBus().registerHandler(getActorName(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                // Request! Convert "Vert.x JsonObject" to "Jackson JsonNode".
                JsonNode request = requestFilter(message.body());
                String msgh = request.get("msgh").asText();

                // Response! Convert "Jackson JsonNode" to "Vert.x JsonObject".
                JsonObject response = new JsonObject(validate(schemaForResponse, handlers.get(msgh).exec(request)).toString());
                message.reply(response);
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
                int exitCode = 0;

                    for (String msgh : handlers.keySet()) {
                        try {
                            result.put(msgh, handlers.get(msgh).help(request));
                        } catch (NullPointerException x) {
                            log("error", x.getMessage());
                            exitCode++;
                        }
                    }

                return response(request, result, exitCode);
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
                int exitCode = 0;

                try {
                    result = mapper.readTree(inputStream);
                } catch (IOException x) {
                    log("error", x.getMessage());
                    exitCode++;
                }

                return response(request, result, exitCode);
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
