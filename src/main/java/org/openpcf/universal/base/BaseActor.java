// Copyright © 2008-2014 RAAF Technology bv

package org.openpcf.universal.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Give a description of BaseActor here.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-09-15
 */
public abstract class BaseActor extends AbstractVerticle {

    JsonNode settings;

    JsonNode schemaForRequest;
    JsonNode schemaForResponse;
    JsonNode schemaForManual;

    Map<String, MessageHandler> handlers;
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

        // Read settings and initialize schema documents.
        settings = jsonFromResource("settings.json");
        schemaForRequest = jsonFromResource("schema/request.schema.json");
        schemaForResponse = jsonFromResource("schema/response.schema.json");
        schemaForManual = jsonFromResource("schema/manual.schema.json");
    }

    // Get the name of the actor (used with registerHandler).
    private String getActorName() {
        return this.getClass().getName().toLowerCase();
    }

    // A logger for actors with nice defaults.
    public void log(String level, String message) {
        System.out.println(level.toUpperCase() + ": " + message);
    }

    // A json file loader (looks in your classpath/resources.)
    public JsonNode jsonFromResource(String resource) {
        JsonNode result = JsonNodeFactory.instance.objectNode();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resource);

        try {
            result = mapper.readTree(inputStream);
        } catch (IOException x) {
            log("error", x.getMessage());
        }

        return result;
    }

    // The Neo4j cypher query helper (accepts a string query parameter).
    public JsonNode cypherQuery(String query) {
        return cypherQuery(query, null);
    }

    // The Neo4j cypher query helper (accepts a string query and params).
    public JsonNode cypherQuery(String query, String params) {
        ObjectNode request = JsonNodeFactory.instance.objectNode();
        if (params != null) { request.put("params", params); }
        request.put("query", query);
        return cypherQuery(request);
    }

    // The Neo4j cypher query helper (accepts a JsonNode parameter).
    public JsonNode cypherQuery(JsonNode request) {

        final String SERVER_ROOT_URI = settings.get("neo4j.rest.uri").asText();
        JsonNode result = JsonNodeFactory.instance.objectNode();

        Client client = Client.create();
        WebResource cypher = client.resource(SERVER_ROOT_URI + "cypher");

        ClientResponse cypherResponse = cypher
                .accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON )
                .entity( request.toString() )
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
    public JsonNode response(JsonNode request, JsonNode result) {
        ObjectNode response = JsonNodeFactory.instance.objectNode();

        response.set("request", request);
        response.set("result", result);
        return response;
    }

    // Get a string value from the argument object.
    public String getArgs(JsonNode args, String key, String fallback) {
        if (args.has(key)) {
            return args.get(key).asText();
        } else {
            return fallback;
        }
    }

    // Get an integer value from the argument object.
    public int getArgs(JsonNode args, String key, int fallback) {
        if (args.has(key)) {
            return args.get(key).asInt();
        } else {
            return fallback;
        }
    }

    // Get a JSON object from the argument object.
    public JsonNode getArgs(JsonNode args, String key, JsonNode fallback) {
        if (args.has(key)) {
            return args.get(key);
        } else {
            return fallback;
        }
    }

    // Check if some string is valid JSON.
    public boolean isValidJson(String body) {
        try {
            mapper.readTree(body);
        } catch (Exception x) {
            return false;
        }
        return true;
    }

    // Check if a string is alpha-numeric.
    public boolean isAlphaNumeric(String str) {
        return str.matches("[0-9a-zA-Z]+");
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

    // Filter a given JsonNode body and enforce basic validity.
    public JsonNode requestFilter(JsonNode body) {

        // Bodies; original and sanitized.
        JsonNode bodyRetrieved;
        ObjectNode bodyFiltered = JsonNodeFactory.instance.objectNode();

        // Defaults for msgh, args and error.
        String msghDefault = "help";
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
                    bodyFiltered.set("args", argsRetrieved);
                } else {
                    // Invalid args, replace args.
                    log("error", "Field \"args\" not of type JSON object: " + argsRetrieved);
                    valid = false;

                    log("warn", "Setting \"args\" to empty JSON object: " + argsDefault);
                    bodyFiltered.set("args", argsDefault);
                }
            } else {
                // Non-existing args, which is ok, adding default args.
                bodyFiltered.set("args", argsDefault);
            }

            // Set validity.
            bodyFiltered.put("valid", valid);

            // If an error occurred, copy the original body to error.
            if (!valid) {
                log("warn", "Copying original request body to error object");
                errorDefault.set("original-request", bodyRetrieved);
                bodyFiltered.set("error", errorDefault);
            }

        } catch (Exception x) {
            log("error", x.getMessage());

        }

        return bodyFiltered;
    }

    // The Vert.x Verticle start method.
    @Override
    public void start(Future<Void> startFuture) {
        // Tell the console we're up and running.
        log("info", getActorName() + " ready to serve");
        vertx.eventBus().consumer(getActorName(), new Handler<Message<JsonNode>>() {
        @Override
        public void handle(Message<JsonNode> message) {

            // Request!
            JsonNode request = requestFilter(message.body());
            String msgh = request.get("msgh").asText();

            // Response!
            JsonNode response = validate(schemaForResponse, handlers.get(msgh).exec(request));
            message.reply(response);
        }
    });
    }

    // A public interface for MessageHandler objects.
    public interface MessageHandler {
        public JsonNode exec(JsonNode request);
        public JsonNode help(JsonNode request);
    }

    // The default usage handler.
    public MessageHandler help() {
        return new MessageHandler() {
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();

                    for (String msgh : handlers.keySet()) {
                        try {
                            result.set(msgh, handlers.get(msgh).help(request));
                        } catch (NullPointerException x) {
                            log("error", x.getMessage());
                        }
                    }

                return response(request, result);
            }

            public JsonNode help(JsonNode request) {
                return jsonFromResource("help/baseactor.help.json");
            }
        };
    }

    // The default version handler.
    public MessageHandler version() {
        return new MessageHandler() {
            public JsonNode exec(JsonNode request) {
                JsonNode result = jsonFromResource("release.json");
                return response(request, result);
            }

            public JsonNode help(JsonNode request) {
                return jsonFromResource("help/baseactor.version.json");
            }
        };
    }
}
