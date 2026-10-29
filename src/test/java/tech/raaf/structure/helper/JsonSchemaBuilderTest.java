package tech.raaf.structure.helper;

import static org.junit.jupiter.api.Assertions.*;

import java.util.regex.PatternSyntaxException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public class JsonSchemaBuilderTest {

  private JsonSchemaBuilder jsonSchema;

  @BeforeEach
  public void setUp() throws Exception {
    jsonSchema = new JsonSchemaBuilder();
  }

  @AfterEach
  public void tearDown() throws Exception {
    jsonSchema = null;
  }

  @Test
  public void testJsonSchema() {
    ObjectNode node = jsonSchema.getAsObjectNode();

    assertEquals("http://json-schema.org/draft-04/schema#", node.get("$schema").asString());
  }

  @Test
  public void testJsonSchemaStringString() {
    jsonSchema = new JsonSchemaBuilder("id", "foo", "bar");
    ObjectNode node = jsonSchema.getAsObjectNode();

    assertEquals("id", node.get("id").asString());
    assertEquals("foo", node.get("title").asString());
    assertEquals("bar", node.get("description").asString());
  }

  @Test
  public void testJsonSchemaSetType() {
    ObjectNode node = jsonSchema.getAsObjectNode();
    String type;

    jsonSchema.setType("string");
    type = node.get("type").asString();
    assertEquals("string", type);

    // Create new JsonSchema, because we can't change the type when it has been set
    jsonSchema = new JsonSchemaBuilder("id", "foo", "bar");
    node = jsonSchema.getAsObjectNode();

    jsonSchema.setType("null");
    type = node.get("type").asString();
    assertEquals("null", type);

    // Create new JsonSchema, because we can't change the type when it has been set
    jsonSchema = new JsonSchemaBuilder("id", "foo", "bar");

    assertThrows(IllegalArgumentException.class, () -> jsonSchema.setType("foo"));
  }

  @Test
  public void testJsonSchemaAddProperty() {
    ObjectNode node;
    JsonSchemaBuilder schema;
    jsonSchema.setType("object");

    node = jsonSchema.addProperty("prop1", "string", false, "description").getAsObjectNode();
    assertEquals("string", node.get("type").asString());

    schema = jsonSchema.addProperty("prop2", "object", true, "description");
    node = schema.getAsObjectNode();
    assertEquals("object", node.get("type").asString());

    schema.addProperty("subprop1", "boolean", true, "description");
    schema.addProperty("subprop2", "string", false, "description");

    node = jsonSchema.getProperties();
    assertNotEquals(null, node.get("prop1"));
    assertNotEquals(null, node.get("prop2"));

    node = schema.getProperties();
    assertNotEquals(null, node.get("subprop1"));
    assertNotEquals(null, node.get("subprop2"));
    assertEquals(null, node.get("arr3"));

    try {
      jsonSchema.addProperty("prop3", "foo", false, "description");
    } catch (IllegalArgumentException e) {
      // don't do anything, expected.
    }
    // Make sure prop3 has been removed from the schema
    assertEquals(null, node.get("prop3"));

    JsonSchemaBuilder json = jsonSchema.addProperty("prop4", "number", true, "description");
    assertThrows(RuntimeException.class, () -> json.addProperty("impossible", "boolean", false, "description"));
  }

  @Test
  public void testJsonSchemaGetRequired() {
    JsonSchemaBuilder schema;
    ArrayNode required;
    jsonSchema.setType("object");
    jsonSchema.addProperty("foo", "number", true, "description");
    jsonSchema.addProperty("bar", "string", false, "description");

    schema = jsonSchema.addProperty("baz", "object", true, "description");
    required = jsonSchema.getRequired();

    assertEquals("foo", required.get(0).asString());
    assertEquals("baz", required.get(1).asString());

    schema.addProperty("prop1", "array", true, "description");
    schema.addProperty("null", "null", false, "description");

    required = schema.getRequired();

    assertEquals("prop1", required.get(0).asString());
  }

  @Test
  public void testJsonSchemaSetPattern() {
    jsonSchema.setType("object");
    JsonSchemaBuilder schema = jsonSchema.addProperty("foo", "string", true, "description");
    schema.setPattern("^[a-z]+$");
    assertEquals("^[a-z]+$", schema.getPattern());

    assertThrows(PatternSyntaxException.class, () -> schema.setPattern("["));
  }

  @Test
  public void testJsonSchemaContainsProperty() {
    jsonSchema.setType("object");
    jsonSchema.addProperty("foo", "number", true, "description");
    assertTrue(jsonSchema.containsProperty("foo"));
  }

  @Test
  public void testJsonSchemaRemoveProperty() {
    jsonSchema.setType("object");
    jsonSchema.addProperty("foo", "number", true, "description");
    assertTrue(jsonSchema.containsProperty("foo"));
    assertEquals("foo", jsonSchema.getRequired().get(0).asString());
    jsonSchema.removeProperty("foo");
    assertFalse(jsonSchema.containsProperty("foo"));
    assertFalse(jsonSchema.getRequired().size() > 0);
  }

  @Test
  public void testJsonSchemaIsRequired() {
    jsonSchema.setType("object");
    jsonSchema.addProperty("foo", "number", true, "description");
    assertTrue(jsonSchema.isRequired("foo"));
  }

  @Test
  public void testJsonSchemaAddItem() {
    jsonSchema.setType("object");
    JsonSchemaBuilder foo = jsonSchema.addProperty("foo", "array", true, "description");
    foo.addItem("bar", "number", false);
    ObjectNode items = foo.getItems();

    assertEquals("number", items.get("bar").get("type").asString());
    assertEquals(false, foo.isRequired("bar"));

    foo.addItem("baz", "string", true);
    assertEquals("string", items.get("baz").get("type").asString());
    assertEquals(true, foo.isRequired("baz"));
  }

  @Test
  public void testJsonSchemaSetItemToSchema() {
    jsonSchema.setType("array");
    jsonSchema.addItem("a", "string", true);

    JsonSchemaBuilder schema = new JsonSchemaBuilder();
    schema.setType("object");
    schema.addProperty("foo", "boolean", true, "description");
    jsonSchema.setItemToSchema(schema.getAsObjectNode());

    assertEquals(null, jsonSchema.getItems().get("a"));
    assertEquals(null, jsonSchema.getRequired().get(0));
  }

  @Test
  public void testJsonSchemaSetDescription() {
    jsonSchema.setDescription("foobar");
    assertTrue(jsonSchema.getDescription().equals("foobar"));
  }

  @Test
  public void testJsonSchemaSetDefault() {
    jsonSchema.setType("string");
    jsonSchema.setDefault("foobar");
    assertEquals("foobar", jsonSchema.getDefault().asString());

    jsonSchema = new JsonSchemaBuilder();
    jsonSchema.setType("number");
    jsonSchema.setDefault("1");
    assertEquals(1, jsonSchema.getDefault().asInt());
    jsonSchema.setDefault("1.23");
    assertEquals(1.23, jsonSchema.getDefault().asDouble(), 1e-15);

    jsonSchema = new JsonSchemaBuilder();
    jsonSchema.setType("boolean");
    jsonSchema.setDefault("true");
    assertTrue(jsonSchema.getDefault().asBoolean());
  }

  @Test
  public void testJsonSchemaValidate() {
    final ObjectMapper mapper = new ObjectMapper();
    ObjectNode json = mapper.createObjectNode();

    jsonSchema.setType("object");
    jsonSchema
      .addProperty("uuid", "string", true, "description")
      .setPattern("^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89aAbB][a-f0-9]{3}-[a-f0-9]{12}$");
    jsonSchema.addProperty("name", "string", true, "description");
    jsonSchema.addProperty("desc", "string", true, "description");
    jsonSchema.setAdditionalProperties(false);

    // Valid according to our JsonSchema
    json.put("uuid", "adf58c7d-098f-4c58-a37b-8f1e07b917ba");
    json.put("name", "foo");
    json.put("desc", "description");

    assertTrue(jsonSchema.validationErrors(json).isEmpty());

    // Invalid, because we only allow known properties (additionalProperties set to false)
    json.put("invalid", "foo");
    assertFalse(jsonSchema.validationErrors(json).isEmpty());

    // Start again
    json = mapper.createObjectNode();
    json.put("uuid", "adf58c7d-098f-4c58-a37b-8f1e07b917ba");
    json.put("name", "foo");
    // desc is missing
    assertFalse(jsonSchema.validationErrors(json).isEmpty());

    // False again, because desc should be a string
    json.put("desc", 1);
    assertFalse(jsonSchema.validationErrors(json).isEmpty());

    // Valid again
    json.put("desc", "valid");
    assertTrue(jsonSchema.validationErrors(json).isEmpty());
  }
}
