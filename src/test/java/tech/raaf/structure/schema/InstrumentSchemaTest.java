package tech.raaf.structure.schema;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import org.junit.Test;
import tech.raaf.structure.helper.JsonHelper;

public class InstrumentSchemaTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void requiresInstrumentTypeAndLocality() {
    for (String schema : new String[] { "instrument", "controller", "marker", "parser" }) {
      ObjectNode instrument = instrument(schema.equals("instrument") ? "controller" : schema);
      assertTrue(schema, valid(schema, instrument));
      instrument.remove("instrumentType");
      assertFalse(schema, valid(schema, instrument));
      instrument.put("instrumentType", "controller");
      instrument.remove("subjectIsLocal");
      assertFalse(schema, valid(schema, instrument));
    }
  }

  @Test
  public void acceptsInstrumentTypes() {
    for (String type : new String[] { "controller", "marker", "parser" }) {
      assertTrue(type, valid("instrument", instrument(type)));
    }
  }

  @Test
  public void rejectsInvalidInstrumentTypes() {
    for (String type : new String[] { "controllerSuffix", "prefixmarker", "prefixparser", "other" }) {
      assertFalse(type, valid("instrument", instrument(type)));
    }
  }

  @Test
  public void rejectsOldFilePathProperty() {
    for (String schema : new String[] { "controller", "marker", "parser" }) {
      ObjectNode instrument = instrument(schema).put("filePath", "unused");
      assertFalse(schema, valid(schema, instrument));
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
    return JsonHelper.validateToSchema(URI.create("resource:/bundles/core/schema/" + schema + ".schema.json"), instrument);
  }
}
