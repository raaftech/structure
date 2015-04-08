package org.openpcf.universal.helper;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.processors.syntax.SyntaxValidator;

/**
 * JsonSchemaBuilder wraps Jackson JsonNode while providing an easy way
 * of creating JsonSchema's.
 * We only implemented a subset of the
 * JsonSchema v4 specification and will be adding more functionality
 * when we need it.
 *
 * @author Boy van Duuren &lt;boy.van.duuren@raaftech.com&gt;
 * @since 2015-04-07
 *
 */
public class JsonSchemaBuilder {

    private ObjectMapper mapper;
    private ObjectNode json;

    /**
     * Parameterless constructor for JsonSchemaBuilder.
     * Creates a JsonSchemaBuilder with just the $schema
     * specified.
     */
    public JsonSchemaBuilder() {
        mapper = new ObjectMapper();
        json = mapper.createObjectNode();

        json.put("$schema", "http://json-schema.org/schema#");
    }

    /**
     * Constructor for JsonSchema.
     * Same as the parameterless constructor, but also sets values
     * for id, title and description.
     * @param id Used for schema addressing
     * @param title The title of the JsonSchema
     * @param description A description of the JsonSchema
     */
    public JsonSchemaBuilder(String id, String title, String description) {
        this();
        json.put("id", id);
        json.put("title", title);
        json.put("description", description);
    }

    private JsonSchemaBuilder(ObjectNode objectNode) throws ProcessingException {
        final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        final SyntaxValidator syntaxValidator = factory.getSyntaxValidator();
        ProcessingReport report = syntaxValidator.validateSchema(objectNode);

        if (report.isSuccess()) {
                json = objectNode;
        } else {
            throw new ProcessingException("Can't construct JsonSchemaBuilder with invalid JSON schema as argument.");
        }
    }

    /**
     * Get the JsonSchema's properties as ObjectNode.
     * @return An ObjectNode containing the schema's properties.
     */
    public ObjectNode getProperties() {
        JsonNode jsonNode = json.get("properties");
        ObjectNode properties = null;
        if (jsonNode != null && jsonNode.isObject()) {
            properties = (ObjectNode)jsonNode;
        }

        return properties;
    }

    /**
     * Get the JsonSchema's properties, creating them if needed.
     * @return An ObjectNode containing the schema's properties.
     */
    public ObjectNode getOrCreateProperties() {
        ObjectNode properties = getProperties();
        if (properties == null) {
            properties = json.putObject("properties");
        }

        return properties;
    }

    /**
     * Add a property to the JsonSchema.
     * @param name The name of the property
     * @param type The type of the property, has the same constraints as setType(String type)
     * @param req Sets the property to required, or not
     * @return A new JsonSchemaBuilder with the newly created property as root, or null when
     * we catch a ProcessingException.
     */
    public JsonSchemaBuilder addProperty(String name, String type, boolean req) {
        if (getType() == null || !getType().equals("object")) {
            throw new RuntimeException("Can't add properties to something that's not a object.");
        }

        ObjectNode properties = getOrCreateProperties();
        ArrayNode required = getOrCreateRequired();

        JsonSchemaBuilder property = null;
        try {
            property = new JsonSchemaBuilder(properties.putObject(name));
        } catch (ProcessingException e) {
            e.printStackTrace();
            return null;
        }
        if (req) {
            required.add(name);
        }

        try {
            property.setType(type);
        } catch (Exception e) {
            properties.remove(name);
            throw e;
        }

        return property;
    }

    /**
     * Set whether or not this JsonSchemaBuilder accepts JSON that isn't
     * defined in the schema.
     * @param bool Accept undefined JSON
     * @return The JsonSchemaBuilder this method was called on.
     */
    public JsonSchemaBuilder setAdditionalProperties(boolean bool) {
        json.put("additionalProperties", bool);

        return this;
    }

    /**
     * Get a list of the properties that are marked required.
     * @return An ArrayNode containing the names of the properties
     * that are marked as required.
     */
    public ArrayNode getRequired() {
        JsonNode jsonNode = json.get("required");
        ArrayNode required = null;
        if (jsonNode != null && jsonNode.isArray()) {
            required = (ArrayNode)jsonNode;
        }

        return required;
    }


    /**
     * Get the JsonSchema's required properties, creating the array if needed.
     * @return An ArrayNode containing the names of the properties
     * that are marked as required.
     */
    public ArrayNode getOrCreateRequired() {
        ArrayNode required = getRequired();
        if (required == null) {
            required = json.putArray("required");
        }

        return required;
    }

    /**
     * Get the type of the JsonSchema.
     * @return A String containing the type of the schema.
     */
    public String getType() {
        JsonNode type = json.get("type");

        if (type != null) {
            return type.asText();
        } else {
            return null;
        }
    }

    /**
     * Set the type of the JsonSchema.
     * @param type The type. Valid values are: string, number, object, array, boolean and null.
     * @return The JsonSchemaBuilder this method was called on.
     */
    public JsonSchemaBuilder setType(String type) {
        if (json.get("type") == null) {
            switch (type) {
            case "string":
            case "number":
            case "object":
            case "array":
            case "boolean":
            case "null":
                json.put("type", type);
                break;
            default:
                throw new IllegalArgumentException("Type unrecognized.");
            }
        } else {
            throw new RuntimeException("Can't change type after it has been set.");
        }

        return this;
    }

    /**
     * Set the pattern of a string property.
     * @param pattern A valid regular expression.
     * @throws RuntimeException Thrown when trying to add a pattern to a non-string property.
     * @throws PatternSyntaxException Thrown when trying to set an invalid pattern.
     * @return The JsonSchemaBuilder this method was called on.
     */
    public JsonSchemaBuilder setPattern(String pattern) {
        if (getType().equals("string")) {
            Pattern.compile(pattern);
            json.put("pattern", pattern);
        } else {
            throw new RuntimeException("Can't add a pattern to a non-string type.");
        }

        return this;
    }

    /**
     * Get the pattern set to this JsonSchema.
     * @return A String containing the set regular expression.
     */
    public String getPattern() {
        JsonNode pattern = json.get("pattern");
        if (pattern != null) {
            return pattern.asText();
        }

        return null;
    }

    /**
     * Get the actual JSON schema as ObjectNode.
     * @return The wrapped ObjectNode.
     */
    public ObjectNode getAsObjectNode() {
        return json;
    }
}