package com.raaftech.structure.helper;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

public abstract class HandlerPlugin extends AbstractVerticle {

    // Metadata object containing the name and address defined
    // in the plugin.properties file.
    protected JsonObject metadata;
    // The arguments the plugin was called with
    protected JsonNode args;
    // The UUID this handler was called on (also in args, for convenience)
    protected String node;
    // The task ID of this action (also in args, for convenience)
    protected String task;
    // Result we get back from a implementation
    protected JsonObject pluginResult = new JsonObject();

    protected ObjectMapper mapper = new ObjectMapper();


    protected abstract void implementation();

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        metadata = context.config();
        JsonSchemaBuilder schema;

        try {
            ObjectNode pluginSchema = (ObjectNode)loadSchema(metadata.getString("path") + File.separator + "schema.json");
            schema = new JsonSchemaBuilder(pluginSchema);
            schema.addProperty("node", "string", true, "UUID of the node this was called on.")
                .setPattern("^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89aAbB][a-f0-9]{3}-[a-f0-9]{12}$");
            schema.addProperty("task", "string", true, "The ID of this particular task.");

            JsonSchemaBuilder bodySchema = new JsonSchemaBuilder()
                .setType("object")
                .setAdditionalProperties(false);
            bodySchema.addProperty("msgh", "string", true, "The action called (ping or execute).");
            bodySchema.addProperty("args", "object", true, "The arguments needed for the action.");

            // Listen on specified address
            vertx.eventBus().consumer("com.raaftech.structure.plugins." + metadata.getString("address"), message -> {
                if (message.body() instanceof JsonObject) {
                    JsonNode body = convertFromJsonObject((JsonObject)message.body());
                    // Validate body to JSON schema
                    if(bodySchema.validateJson(body)) {
                        String msgh = body.get("msgh").asText();
                        if (msgh.equals("execute")) {
                            args = body.get("args");
                            if(schema.validateJson(args)) {
                                node = args.get("node").asText();
                                task = args.get("task").asText();
                                implementation();
                            } else {
                                log("info", "Received message didn't validate against plugin's schema.");
                            }
                        } else if (msgh.equals("ping")) {
                            message.reply("pong");
                        }
                    } else {
                        log("info", "Received message didn't validate against PluginHandler schema.");
                        System.out.println(bodySchema.getAsObjectNode());
                        System.out.println(body);
                    }
                } else {
                    log("info", "Received non-JSON on eventbus.");
                }

                if (pluginResult.size() > 0) {
                    message.reply(pluginResult);
                }
            });
        } catch (IOException | RuntimeException e) {
            startFuture.fail(e);
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
                log("error", report.toString());
            }
            return report.isSuccess();
        } catch (ProcessingException x) {
            log("error", x.getMessage());
            return false;
        }
    }

    // Convert a Vert.x JsonObject to a Jackson JsonNode
    protected JsonNode convertFromJsonObject(JsonObject jsonObject) {
        JsonNode jsonNode = JsonNodeFactory.instance.objectNode();

        try {
            jsonNode = mapper.readTree(jsonObject.toString());
        } catch (IOException x) {
            log("error", x.getMessage());
        }

        return jsonNode;
    }

    // A logger for actors with nice defaults.
    protected void log(String level, String message) {
        System.out.println(level.toUpperCase() + ": " + message);
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
    protected void createCommandResult(String result) {
        JsonObject resultMessage = new JsonObject()
                .put("msgh", "createcommandresult")
                .put("args", new JsonObject()
                .put("node", node)
                .put("task", task)
                .put("commandresult", result));
        vertx.eventBus().send("com.raaftech.structure.actor.statemanager", resultMessage);
    }
    // Log a DerivedStatusResult to the database
    protected void createDerivedStatusResult(boolean result) {
        JsonObject resultMessage = new JsonObject()
                .put("msgh", "createderivedstatusresult")
                .put("args", new JsonObject()
                .put("node", node)
                .put("task", task)
                .put("status", result));
        vertx.eventBus().send("com.raaftech.structure.actor.statemanager", resultMessage);
    }
}
