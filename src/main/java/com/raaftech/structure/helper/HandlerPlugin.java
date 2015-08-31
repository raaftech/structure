package com.raaftech.structure.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

public abstract class HandlerPlugin extends AbstractVerticle {

    private Logger log = LoggerFactory.getLogger(HandlerPlugin.class);

    // Metadata object containing the name and address defined
    // in the plugin.properties file.
    protected JsonObject metadata;
    // The arguments the plugin was called with
    protected JsonNode args;
    // Result we get back from a implementation
    protected JsonObject pluginResult = new JsonObject();

    protected ObjectMapper mapper = new ObjectMapper();


    protected abstract void implementation();

    @Override
    public void start() throws Exception {
        metadata = context.config();
        JsonSchemaBuilder schema;

        try {
            ObjectNode pluginSchema = (ObjectNode)loadSchema(metadata.getString("path") + File.separator + "schema.json");
            schema = new JsonSchemaBuilder(pluginSchema);

            JsonSchemaBuilder bodySchema = new JsonSchemaBuilder()
                    .setType("object")
                    .setAdditionalProperties(false);
            bodySchema.addProperty("msgh", "string", true, "The action called (ping or execute).");
            bodySchema.addProperty("args", "object", true, "The arguments needed for the action.");

            // Listen on specified address
            String handlerPluginAddress = "com.raaftech.structure.plugins." + metadata.getString("address");
            log.debug("Listen for {} on addr: {}", this.getClass().getSimpleName(), handlerPluginAddress);
            vertx.eventBus().consumer(handlerPluginAddress, message -> {
                if (message.body() instanceof JsonObject) {
                    JsonNode body = convertFromJsonObject((JsonObject)message.body());
                    // Validate body to JSON schema
                    if(bodySchema.validateJson(body)) {
                        String msgh = body.get("msgh").asText();
                        if (msgh.equals("execute")) {
                            args = body.get("args");
                            if(schema.validateJson(args)) {
                                implementation();
                            } else {
                                log.warn("Received message didn't validate against plugin's schema.");
                            }
                        } else if (msgh.equals("ping")) {
                            message.reply("pong");
                        }
                    } else {
                        log.warn("Received message didn't validate against plugin's schema.");
                    }
                } else {
                    log.warn("Received non-JSON on eventbus.");
                }

                if (pluginResult.size() > 0) {
                    message.reply(pluginResult);
                }
            });
        } catch (IOException | RuntimeException e) {
            log.error(e.getMessage());
            this.stop();
        }
    }

    /*+***********************
     *    Helper methods     *
     *************************/

    // Return whether JSON is valid according to JSON Schema
    protected boolean isValidJson(JsonNode schemaDoc, JsonNode jsonDoc) {
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

    // Convert a Vert.x JsonObject to a Jackson JsonNode
    protected JsonNode convertFromJsonObject(JsonObject jsonObject) {
        JsonNode jsonNode = JsonNodeFactory.instance.objectNode();

        try {
            jsonNode = mapper.readTree(jsonObject.toString());
        } catch (IOException x) {
            log.error(x.getMessage());
        }

        return jsonNode;
    }

    // Convert a Jackson JsonNode to a Vert.x JsonObject
    protected JsonObject convertFromJsonNode(JsonNode jsonNode) {
        return new JsonObject(jsonNode.toString());
    }

    // Load a JSON file
    protected JsonNode loadSchema(String path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode result = JsonNodeFactory.instance.objectNode();
        InputStream schemaStream = new FileInputStream(new File(path));

        result = mapper.readTree(schemaStream);

        return result;
    }

    // Log a CommandResult to the database
    protected void createCommandResult(String node, String task, String result) {
        JsonObject resultMessage = new JsonObject()
                .put("msgh", "createcommandresult")
                .put("args", new JsonObject()
                        .put("node", node)
                        .put("task", task)
                        .put("commandresult", result));
        vertx.eventBus().send("com.raaftech.structure.statemanager", resultMessage);
    }
    // Log a DerivedStatusResult to the database
    protected void createDerivedStatusResult(String node, String task, boolean result) {
        JsonObject resultMessage = new JsonObject()
                .put("msgh", "createderivedstatusresult")
                .put("args", new JsonObject()
                        .put("node", node)
                        .put("task", task)
                        .put("status", result));
        vertx.eventBus().send("com.raaftech.structure.statemanager", resultMessage);
    }
}
