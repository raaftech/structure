package org.openpcf.universal.helper;

import java.util.Iterator;
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

    /**
     * Construct a JsonSchemaBuilder using a ObjectNode
     * @param objectNode An ObjectNode that is valid JSON Schema
     * @throws ProcessingException When the ObjectNode does not comply to the JSON Schema specification
     */
    public JsonSchemaBuilder(ObjectNode objectNode) throws ProcessingException {
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
        if (getType() == null || !getType().equals("object")) {
            throw new RuntimeException("Can't create properties for something that's not an object.");
        }

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
            throw new RuntimeException("Can't add properties to something that's not an object.");
        }

        ObjectNode properties = getOrCreateProperties();

        JsonSchemaBuilder property = null;
        try {
            property = new JsonSchemaBuilder(properties.putObject(name));
        } catch (ProcessingException e) {
            e.printStackTrace();
            return null;
        }
        try {
            property.setType(type);
        } catch (Exception e) {
            properties.remove(name);
            throw e;
        }
        if (req) {
            ArrayNode required = getOrCreateRequired();
            required.add(name);
        }

        return property;
    }

    /**
     * Remove a property from the JsonSchema.
     * @param name The name of the property.
     * @return The object the method was called on.
     * @throws RuntimeException Thrown when property doesn't exist.
     */
    public JsonSchemaBuilder removeProperty(String name) {
        ObjectNode properties = getProperties();

        if (properties != null) {
            properties.remove(name);
        } else {
            throw new RuntimeException("Can't remove property that doesn't exist.");
        }

        return this;
    }

    /**
     * Check if JsonSchemaBuilder contains a certain property.
     * @param name The name of the property to match.
     * @return True when it has the property, false if not.
     */
    public boolean containsProperty(String name) {
        ObjectNode properties = getProperties();
        if (properties != null) {
            return properties.has(name);
        } else {
            return false;
        }
    }

    public ObjectNode getItems() {
        JsonNode jsonNode = json.get("items");
        ObjectNode items = null;
        if (jsonNode != null && jsonNode.isObject()) {
            items = (ObjectNode)jsonNode;
        }

        return items;
    }

    public ObjectNode getOrCreateItems() {
        if (getType() == null | !getType().equals("array")) {
            throw new RuntimeException("Can't create an items object on something that's not of type array.");
        }

        ObjectNode items = getItems();
        if (items == null) {
            items = json.putObject("items");
        }

        return items;
    }

    public JsonSchemaBuilder addItem(String name, String type, boolean req) {
        if (getType() == null | !getType().equals("array")) {
            throw new RuntimeException("Can't add an item to something that's not of type array.");
        }

        ObjectNode items = getOrCreateItems();
        JsonSchemaBuilder item = null;
        ArrayNode required;

        try {
            item = new JsonSchemaBuilder(items.putObject(name));
        } catch (ProcessingException e) {
            e.printStackTrace();
            return null;
        }
        try {
            item.setType(type);
        } catch (Exception e) {
            items.remove(name);
            throw e;
        }
        if (req) {
            required = getOrCreateRequired();
            required.add(name);
        }

        return item;
    }

    public JsonSchemaBuilder setItemToSchema(ObjectNode objectNode) throws ProcessingException {
        if (getType() == null | !getType().equals("array")) {
            throw new RuntimeException("Can't add an item to something that's not of type array.");
        }

        final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        final SyntaxValidator syntaxValidator = factory.getSyntaxValidator();
        ProcessingReport report = syntaxValidator.validateSchema(objectNode);

        if (report.isSuccess()) {
            ObjectNode items = getOrCreateItems();
            items.putAll(objectNode);
        } else {
            throw new ProcessingException("Can't construct JsonSchemaBuilder with invalid JSON schema as argument.");
        }

        return this;
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
     * Check if a property is required.
     * @param property The name of the property
     * @return True when it's required, false if not.
     */
    public boolean isRequired(String property) {
        ArrayNode required = getRequired();
        if (required == null) {
            return false;
        } else {
            boolean ret = false;
            Iterator<JsonNode> fields = required.elements();
            while (fields.hasNext()) {
                if (property.equals(fields.next().asText())) {
                    ret = true;
                }
            }
            return ret;
        }
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