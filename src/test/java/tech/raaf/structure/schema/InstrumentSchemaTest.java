package tech.raaf.structure.schema;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import org.junit.jupiter.api.Test;
import tech.raaf.structure.helper.JsonHelper;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public class InstrumentSchemaTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void requiresInstrumentTypeAndLocality() {
    for (String schema : new String[] { "instrument", "controller", "marker", "parser" }) {
      ObjectNode instrument = instrument(schema.equals("instrument") ? "controller" : schema);
      assertTrue(valid(schema, instrument), schema);
      instrument.remove("instrumentType");
      assertFalse(valid(schema, instrument), schema);
      instrument.put("instrumentType", "controller");
      instrument.remove("subjectIsLocal");
      assertFalse(valid(schema, instrument), schema);
    }
  }

  @Test
  public void acceptsInstrumentTypes() {
    for (String type : new String[] { "controller", "marker", "parser" }) {
      assertTrue(valid("instrument", instrument(type)), type);
    }
  }

  @Test
  public void rejectsInvalidInstrumentTypes() {
    for (String type : new String[] { "controllerSuffix", "prefixmarker", "prefixparser", "other" }) {
      assertFalse(valid("instrument", instrument(type)), type);
    }
  }

  @Test
  public void rejectsOldFilePathProperty() {
    for (String schema : new String[] { "controller", "marker", "parser" }) {
      ObjectNode instrument = instrument(schema).put("filePath", "unused");
      assertFalse(valid(schema, instrument), schema);
    }
  }

  @Test
  public void acceptsResultParserReference() {
    ObjectNode controller = instrument("controller").put("resultParser", "bc9c7276-491a-419f-b01f-fd7373ba0338");
    assertTrue(valid("controller", controller));
  }

  private ObjectNode instrument(String type) {
    return mapper
      .createObjectNode()
      .put("name", "TestInstrument")
      .put("desc", "A test instrument.")
      .put("instrumentType", type)
      .put("subjectIsLocal", true);
  }

  private boolean valid(String schema, ObjectNode instrument) {
    return JsonHelper.validateToSchema(
      URI.create("resource:/bundles/core/schema/" + schema + ".schema.json"),
      instrument
    );
  }
}
