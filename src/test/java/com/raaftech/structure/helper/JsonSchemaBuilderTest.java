package com.raaftech.structure.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.regex.PatternSyntaxException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;

public class JsonSchemaBuilderTest {

    private JsonSchemaBuilder jsonSchema;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        jsonSchema = new JsonSchemaBuilder();
    }

    @After
    public void tearDown() throws Exception {
        jsonSchema = null;
    }

    @Test
    public void testJsonSchema() {
        ObjectNode node = jsonSchema.getAsObjectNode();

        assertEquals("http://json-schema.org/schema#", node.get("$schema").asText());
    }

    @Test
    public void testJsonSchemaStringString() {
        jsonSchema = new JsonSchemaBuilder("id", "foo", "bar");
        ObjectNode node = jsonSchema.getAsObjectNode();

        assertEquals("id", node.get("id").asText());
        assertEquals("foo", node.get("title").asText());
        assertEquals("bar", node.get("description").asText());
    }

    @Test
    public void testJsonSchemaSetType() {
        ObjectNode node = jsonSchema.getAsObjectNode();
        String type;

        jsonSchema.setType("string");
        type = node.get("type").asText();
        assertEquals("string", type);

        // Create new JsonSchema, because we can't change the type when it has been set
        jsonSchema = new JsonSchemaBuilder("id", "foo", "bar");
        node = jsonSchema.getAsObjectNode();

        jsonSchema.setType("null");
        type = node.get("type").asText();
        assertEquals("null", type);

        // Create new JsonSchema, because we can't change the type when it has been set
        jsonSchema = new JsonSchemaBuilder("id", "foo", "bar");

        exception.expect(IllegalArgumentException.class);
        jsonSchema.setType("foo");
    }

    @Test
    public void testJsonSchemaAddProperty() {
        ObjectNode node;
        JsonSchemaBuilder schema;
        jsonSchema.setType("object");

        node = jsonSchema.addProperty("prop1", "string", false).getAsObjectNode();
        assertEquals("string", node.get("type").asText());

        schema = jsonSchema.addProperty("prop2", "object", true);
        node = schema.getAsObjectNode();
        assertEquals("object", node.get("type").asText());

        schema.addProperty("subprop1", "boolean", true);
        schema.addProperty("subprop2", "string", false);

        node = jsonSchema.getProperties();
        assertNotEquals(null, node.get("prop1"));
        assertNotEquals(null, node.get("prop2"));

        node = schema.getProperties();
        assertNotEquals(null, node.get("subprop1"));
        assertNotEquals(null, node.get("subprop2"));
        assertEquals(null, node.get("arr3"));

        try {
            jsonSchema.addProperty("prop3", "foo", false);
        } catch (IllegalArgumentException e) {
            // don't do anything, expected.
        }
        // Make sure prop3 has been removed from the schema
        assertEquals(null, node.get("prop3"));

        exception.expect(RuntimeException.class);
        JsonSchemaBuilder json = jsonSchema.addProperty("prop4", "number", true);
        json.addProperty("impossible", "boolean", false);
    }

    @Test
    public void testJsonSchemaGetRequired() {
        JsonSchemaBuilder schema;
        ArrayNode required;
        jsonSchema.setType("object");
        jsonSchema.addProperty("foo", "number", true);
        jsonSchema.addProperty("bar", "string", false);

        schema = jsonSchema.addProperty("baz", "object", true);
        required = jsonSchema.getRequired();

        assertEquals("foo", required.get(0).asText());
        assertEquals("baz", required.get(1).asText());

        schema.addProperty("prop1", "array", true);
        schema.addProperty("null", "null", false);

        required = schema.getRequired();

        assertEquals("prop1", required.get(0).asText());
    }

    @Test
    public void testJsonSchemaSetPattern() {
        jsonSchema.setType("object");
        JsonSchemaBuilder schema = jsonSchema.addProperty("foo", "string", true);
        schema.setPattern("^[a-z]+$");
        assertEquals("^[a-z]+$", schema.getPattern());

        exception.expect(PatternSyntaxException.class);
        schema.setPattern("[");
    }

    @Test
    public void testJsonSchemaContainsProperty() {
        jsonSchema.setType("object");
        jsonSchema.addProperty("foo", "number", true);
        assertTrue(jsonSchema.containsProperty("foo"));
    }

    @Test
    public void testJsonSchemaRemoveProperty() {
        jsonSchema.setType("object");
        jsonSchema.addProperty("foo", "number", true);
        assertTrue(jsonSchema.containsProperty("foo"));
        jsonSchema.removeProperty("foo");
        assertFalse(jsonSchema.containsProperty("foo"));
    }

    @Test
    public void testJsonSchemaIsRequired() {
        jsonSchema.setType("object");
        jsonSchema.addProperty("foo", "number", true);
        assertTrue(jsonSchema.isRequired("foo"));
    }

    @Test
    public void testJsonSchemaAddItem() {
        jsonSchema.setType("object");
        JsonSchemaBuilder foo = jsonSchema.addProperty("foo", "array", true);
        foo.addItem("bar", "number", false);
        ObjectNode items = foo.getItems();

        assertEquals("number", items.get("bar").get("type").asText());
        assertEquals(false, foo.isRequired("bar"));

        foo.addItem("baz", "string", true);
        assertEquals("string", items.get("baz").get("type").asText());
        assertEquals(true, foo.isRequired("baz"));
    }

    @Test
    public void testJsonSchemaSetItemToSchema() throws ProcessingException {
        jsonSchema.setType("array");
        jsonSchema.addItem("a", "string", true);

        JsonSchemaBuilder schema = new JsonSchemaBuilder();
        schema.setType("object");
        schema.addProperty("foo", "boolean", true);
        jsonSchema.setItemToSchema(schema.getAsObjectNode());

        assertEquals(null, jsonSchema.getItems().get("a"));
        assertEquals(null, jsonSchema.getRequired().get(0));
    }

    @Test
    public void testJsonSchemaValidate() throws ProcessingException {
        final ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = mapper.createObjectNode();

        jsonSchema.setType("object");
        jsonSchema.addProperty("uuid", "string", true)
            .setPattern("^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89aAbB][a-f0-9]{3}-[a-f0-9]{12}$");
        jsonSchema.addProperty("name", "string", true);
        jsonSchema.addProperty("desc", "string", true);
        jsonSchema.setAdditionalProperties(false);

        // Valid according to our JsonSchema
        json.put("uuid", "adf58c7d-098f-4c58-a37b-8f1e07b917ba");
        json.put("name", "foo");
        json.put("desc", "description");

        assertTrue(jsonSchema.validateJson(json));

        // Invalid, because we only allow known properties (additionalProperties set to false)
        json.put("invalid", "foo");
        assertFalse(jsonSchema.validateJson(json));

        // Start again
        json = mapper.createObjectNode();
        json.put("uuid", "adf58c7d-098f-4c58-a37b-8f1e07b917ba");
        json.put("name", "foo");
        // desc is missing
        assertFalse(jsonSchema.validateJson(json));

        // False again, because desc should be a string
        json.put("desc", 1);
        assertFalse(jsonSchema.validateJson(json));

        // Valid again
        json.put("desc", "valid");
        assertTrue(jsonSchema.validateJson(json));
    }
}