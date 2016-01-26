// Copyright © 2008-2014 RAAF Technology bv

package com.raaftech.structure.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.raaftech.structure.annotations.SchemaPath;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Transient;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Give a description of BaseActor here.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-09-15
 */
public abstract class GenericActor extends AbstractVerticle {

    private Logger log = LoggerFactory.getLogger(GenericActor.class);

    @Transient
    JsonNode schemaForRequest;
    @Transient
    JsonNode schemaForResponse;
    @Transient
    JsonNode schemaForManual;

    @Transient
    Map<String, HandlerSchema> handlers = new HashMap<>();
    @Transient
    protected ObjectMapper mapper = new ObjectMapper();

    // statusAddress is used to push notifications of events within Structure
    // that the GUI may be interested about.
    private static final String statusAddress = "com.raaftech.structure.status";

    class HandlerSchema {
        String schemaPath;
        MessageHandler handler;

        public HandlerSchema(String schemaPath, MessageHandler handler) {
            this.schemaPath = schemaPath;
            this.handler = handler;
        }
    }

    // Constructor.
    protected GenericActor() {
        // Initialize arrays.
        Method methods[] = this.getClass().getMethods();
        Map<Method, String> messageHandlersAndSchemas = new HashMap<>();

        // Fill messageHandlers.
        for (Method method : methods) {
            if (method.getReturnType().equals(MessageHandler.class)) {
                if (method.isAnnotationPresent(SchemaPath.class)) {
                    SchemaPath schemaPath = method.getAnnotation(SchemaPath.class);
                    messageHandlersAndSchemas.put(method, schemaPath.value());
                } else {
                    messageHandlersAndSchemas.put(method, "schema/args.schema.json");
                }
            }
        }

        messageHandlersAndSchemas.entrySet().stream()
        .forEach((entry) -> {
            Method mh = entry.getKey();
            String schema = entry.getValue();
            String s = entry.getKey().getName();

            try {
                HandlerSchema handlerAndSchema = new HandlerSchema(schema, (MessageHandler) mh.invoke(this));
                handlers.put(s, handlerAndSchema);
            } catch (IllegalAccessException | InvocationTargetException x) {
                log.error("Failed to access or invoke method " + s + ": " + x.getMessage());
            }
        });

        // Read settings and initialize schema documents.
        schemaForRequest = jsonFromResource("schema/request.schema.json");
        schemaForResponse = jsonFromResource("schema/response.schema.json");
        schemaForManual = jsonFromResource("schema/manual.schema.json");
    }

    // A json file loader (looks in your classpath/resources.)
    public JsonNode jsonFromResource(String resource) {
        JsonNode result;
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resource);

        try {
            result = mapper.readTree(inputStream);
        } catch (IOException x) {
            log.error(x.getMessage());
            return null;
        }

        return result;
    }

    // Convert a Vert.x JsonObject to a Jackson JsonNode
    public JsonNode convertFromJsonObject(JsonObject jsonObject) {
        JsonNode jsonNode = JsonNodeFactory.instance.objectNode();

        try {
            jsonNode = mapper.readTree(jsonObject.toString());
        } catch (IOException x) {
            log.error(x.getMessage());
        }

        return jsonNode;
    }

    // Get the name of the actor (used with registerHandler).
    public String getActorName() {
        return this.getClass().getName().toLowerCase();
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

    // Check if a string is a valid UUID.
    @SuppressWarnings("unused")
    public boolean isUuid(String str) {
        try {
            UUID uuid = UUID.fromString(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // The validator.
    public JsonNode validate(JsonNode schemaDoc, JsonNode jsonDoc) {
        try {
            final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
            final JsonSchema schema = factory.getJsonSchema(schemaDoc);
            ProcessingReport report = schema.validate(jsonDoc);
            if (!report.isSuccess()) {
                log.error(report.toString());
            }
        } catch (ProcessingException x) {
            log.error(x.getMessage());
        }

        return jsonDoc;
    }

    // Return whether JSON is valid according to JSON Schema
    public boolean isValidJson(JsonNode schemaDoc, JsonNode jsonDoc) {
        try {
            final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
            final JsonSchema schema = factory.getJsonSchema(schemaDoc);
            ProcessingReport report = schema.validate(jsonDoc);

            if (!report.isSuccess()) {
                log.error(report.toString());
            }
            return report.isSuccess();
        } catch (ProcessingException x) {
            log.error(x.getMessage());
            return false;
        }
    }

    // Publish a status on the status channel
    protected void publishStatus(String event, JsonNode data) {
        EventBus eventBus = getVertx().eventBus();
        eventBus.publish(statusAddress,
                new JsonObject("{\"source\":\"" + this.getClass().getSimpleName() + "\", "
                        + "\"event\": \"" + event + "\", \"data\": " + new JsonObject(data.toString()) + "}"));
    }

    // Filter a given JsonNode body and enforce basic validity.
    @SuppressWarnings("unused")
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
            bodyRetrieved = validate(schemaForRequest, body);

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
                        log.error("Field \"msgh\" not a valid message handler: " + msghAlphaNumeric);
                        valid = false;
                        log.warn("Valid \"msgh\" values are: " + handlers.keySet().toString());
                        log.warn("warn", "Setting \"msgh\" to default value: " + msghDefault);
                        bodyFiltered.put("msgh", msghDefault);
                    }
                } else {
                    // Invalid characters in msgh, replace msgh.
                    log.warn("Field \"msgh\" contains invalid characters: " + msghRetrieved);
                    valid = false;

                    log.warn("Setting \"msgh\" to default value: " + msghDefault);
                    bodyFiltered.put("msgh", msghDefault);
                }
            } else {
                // Non-existing msgh, adding default msgh.
                log.error("Field \"msgh\" not in JSON body");
                valid = false;

                log.warn("Setting \"msgh\" to default value: " + msghDefault);
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
                    log.error("Field \"args\" not of type JSON object: " + argsRetrieved);
                    valid = false;

                    log.warn("Setting \"args\" to empty JSON object: " + argsDefault);
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
                log.warn("Copying original request body to error object");
                errorDefault.set("original-request", bodyRetrieved);
                bodyFiltered.set("error", errorDefault);
            }

        } catch (Exception x) {
            log.error(x.getMessage());

        }

        return bodyFiltered;
    }

    // A public interface for MessageHandler objects.
    public interface MessageHandler {
        JsonNode exec(JsonNode request);
        JsonNode help(JsonNode request);
    }

    // The default usage handler.
    public MessageHandler help() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();

                for (String msgh : handlers.keySet()) {
                    try {
                        HandlerSchema handlerAndSchema = handlers.get(msgh);
                        result.set(msgh, handlerAndSchema.handler.help(request));
                    } catch (NullPointerException x) {
                        log.error(x.getMessage());
                    }
                }

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return jsonFromResource("help/baseactor.help.json");
            }
        };
    }

    // The default version handler.
    public MessageHandler version() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                JsonNode result = jsonFromResource("release.json");
                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return jsonFromResource("help/baseactor.version.json");
            }
        };
    }

    // Logs and registers the actors on the bus.
    @Override
    public void start() {
        log.debug("Listen for {} on addr: {}", this.getClass().getSimpleName(), getActorName());
        vertx.eventBus().consumer(getActorName(), this::handleMessage);
    }

    // Our message handler.
    public void handleMessage(Message<Object> message) {
        // Request!
        JsonObject body = (JsonObject)message.body();
        JsonNode request = convertFromJsonObject(body);
        JsonObject response;
        String msgh = request.get("msgh").asText();
        JsonNode args = request.get("args");

        if (handlers.containsKey(msgh)) {
            // If we didn't get any args create an empty JSON object, so we don't get
            // a NPE when calling isValidJson(schema, args)
            if (args == null) {
                args = mapper.createObjectNode();
            }
            HandlerSchema handlerAndSchema = handlers.get(msgh);
            JsonNode schema = jsonFromResource(handlerAndSchema.schemaPath);
            if (schema == null) {
                log.warn("Couldn't load " + handlerAndSchema.schemaPath + " using empty schema as default. Validation will always pass.");
                schema = jsonFromResource("schema/args.schema.json");
            }
            if (isValidJson(schema, args)) {
                // Response! Convert "Jackson JsonNode" to "Vert.x JsonObject".
                response = new JsonObject(
                        validate(schemaForResponse, handlerAndSchema.handler.exec(request)).toString());
            } else {
                JsonNode error = JsonNodeFactory.instance.objectNode().put("error", "Arguments didn't validate against schema.");
                response = new JsonObject(validate(schemaForResponse, response(request, error)).toString());
            }
        } else {
            JsonNode error = JsonNodeFactory.instance.objectNode().put("error", "No handler for called method.");
            response = new JsonObject(validate(schemaForResponse, response(request, error)).toString());
        }
        message.reply(response);
    }
}
