// Copyright © 2008-2014 RAAF Technology bv

package tech.raaf.structure.bus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Transient;
import tech.raaf.structure.annotations.SchemaPath;
import tech.raaf.structure.base.Configuration;
import tech.raaf.structure.helper.JsonHelper;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * GenericActor is our default Abstract class for actors on the bus..
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-09-15
 */
public abstract class GenericActor extends AbstractVerticle {

    private Logger log = LoggerFactory.getLogger(GenericActor.class);


    @Transient
    final URI schemaForRequest = URI.create("resource:/schema/base/request.schema.json");
    @Transient
    final URI schemaForResponse = URI.create("resource:/schema/base/response.schema.json");
    @Transient
    final URI schemaForManual = URI.create("resource:/schema/base/manual.schema.json");
    // used as a default schema in case we need to fallback on something
    @Transient
    final URI fallbackSchema = URI.create("resource:/schema/base/args.schema.json");

    @Transient
    Map<String, HandlerSchema> handlers = new HashMap<>();
    @Transient
    protected ObjectMapper mapper = new ObjectMapper();

    // statusAddress is used to push notifications of events within Structure
    // that the GUI may be interested about.
    private static final String statusAddress = "tech.raaf.structure.bus.status";

    class HandlerSchema {
        URI schemaPath;
        MessageHandler handler;

        public HandlerSchema(URI schemaPath, MessageHandler handler) {
            this.schemaPath = schemaPath;
            this.handler = handler;
        }
    }

    // Constructor.
    protected GenericActor() {
        // Initialize arrays.
        Method methods[] = this.getClass().getMethods();
        Map<Method, URI> messageHandlersAndSchemas = new HashMap<>();

        // Fill messageHandlers.
        for (Method method : methods) {
            if (method.getReturnType().equals(MessageHandler.class)) {
                if (method.isAnnotationPresent(SchemaPath.class)) {
                    SchemaPath schemaPath = method.getAnnotation(SchemaPath.class);
                    try {
                        messageHandlersAndSchemas.put(method, new URI(schemaPath.value()));
                    } catch (URISyntaxException e) {
                        if (Configuration.isTraceModeOn()) {
                            e.printStackTrace();
                        }
                        log.error("Error while parsing @SchemaPath value as URI");
                        messageHandlersAndSchemas.put(method, fallbackSchema);
                    }
                } else {
                    messageHandlersAndSchemas.put(method, fallbackSchema);
                }
            }
        }

        messageHandlersAndSchemas.entrySet().stream()
        .forEach((entry) -> {
            Method mh = entry.getKey();
            URI schema = entry.getValue();
            String s = entry.getKey().getName();

            try {
                HandlerSchema handlerAndSchema = new HandlerSchema(schema, (MessageHandler) mh.invoke(this));
                handlers.put(s, handlerAndSchema);
            } catch (IllegalAccessException | InvocationTargetException e) {
                log.error("Failed to access or invoke method " + s + ": " + e.getMessage());
                
                if (Configuration.isTraceModeOn()) {
                    e.printStackTrace();
                }
            }
        });
    }

    // A json file loader (looks in your classpath/resources.)
    public JsonNode jsonFromResource(String resource) {
        return JsonHelper.jsonFromResource(resource);
    }

    // Convert a Vert.x JsonObject to a Jackson JsonNode
    public JsonNode convertFromJsonObject(JsonObject jsonObject) {
        JsonNode jsonNode = JsonNodeFactory.instance.objectNode();

        try {
            jsonNode = mapper.readTree(jsonObject.toString());
        } catch (IOException e) {
            log.error(e.getMessage());
            
            if (Configuration.isTraceModeOn()) {
                e.printStackTrace();
            }
        }

        return jsonNode;
    }

    // Get the name of the bus (used with registerHandler).
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

    // Check if a string is a valid UUID.
    @SuppressWarnings("unused")
    public static boolean isUuid(String str) {
        try {
            UUID uuid = UUID.fromString(str);
            return true;
        } catch (IllegalArgumentException e) {
        	return false;
        }
    }

    // The validator.
    public JsonNode validate(URI schemaDoc, JsonNode jsonDoc) {
        try {
            final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
            final JsonSchema schema = factory.getJsonSchema(schemaDoc.toString());
            ProcessingReport report = schema.validate(jsonDoc);
            if (!report.isSuccess()) {
                log.error(report.toString());
            }
        } catch (ProcessingException e) {
            log.error(e.getMessage());
            
            if (Configuration.isTraceModeOn()) {
                e.printStackTrace();
            }
        }

        return jsonDoc;
    }

    // Return whether JSON is valid according to JSON Schema
    public boolean isValidJson(URI schemaDoc, JsonNode jsonDoc) {
        return JsonHelper.validateToSchema(schemaDoc, jsonDoc);
    }

    // Publish a status on the status channel
    protected void publishStatus(String event, JsonNode data) {
        EventBus eventBus = getVertx().eventBus();
        eventBus.publish(statusAddress,
                new JsonObject("{\"source\":\"" + this.getClass().getSimpleName() + "\", "
                        + "\"event\": \"" + event + "\", \"data\": " + new JsonObject(data.toString()) + "}"));
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
                    } catch (NullPointerException e) {
                        log.error(e.getMessage());
                        
                        if (Configuration.isTraceModeOn()) {
                            e.printStackTrace();
                        }
                    }
                }

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return jsonFromResource("help/genericactor.help.json");
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
                return jsonFromResource("help/genericactor.version.json");
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

        // Without a valid msgh and args we can't dispatch, reply with an error
        if (!isValidJson(schemaForRequest, request)) {
            JsonNode error = JsonNodeFactory.instance.objectNode().put("error", "Request didn't validate against schema.");
            message.reply(new JsonObject(validate(schemaForResponse, response(request, error)).toString()));
            return;
        }

        String msgh = request.get("msgh").asText();
        JsonNode args = request.get("args");

        if (handlers.containsKey(msgh)) {
            // If we didn't get any args create an empty JSON object, so we don't get
            // a NPE when calling isValidJson(schema, args)
            if (args == null) {
                args = mapper.createObjectNode();
            }
            HandlerSchema handlerAndSchema = handlers.get(msgh);
            URI schema = handlerAndSchema.schemaPath;
            if (schema == null) {
                log.warn("Couldn't load " + handlerAndSchema.schemaPath + " using empty schema as default. Validation will always pass.");
                schema = fallbackSchema;
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
