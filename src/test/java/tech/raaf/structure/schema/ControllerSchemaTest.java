package tech.raaf.structure.schema;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableSet;
import java.net.URI;
import java.util.Set;
import org.junit.Test;
import tech.raaf.structure.helper.JsonHelper;
import tools.jackson.databind.node.ObjectNode;

public class ControllerSchemaTest extends SchemaTest {

  @Test
  public void acceptsTypePathsAndStrings() {
    for (String type : new String[] { "string", "type.component", "type.component.software.service" }) {
      ObjectNode controller = controller();
      value(controller).put("type", type);
      assertTrue(type, valid(controller));
    }
  }

  @Test
  public void rejectsInvalidTypePaths() {
    for (String type : new String[] { "type.component!", "prefixstring", "stringSuffix" }) {
      ObjectNode controller = controller();
      value(controller).put("type", type);
      assertFalse(type, valid(controller));
    }
  }

  @Test
  public void acceptsErrorHandlers() {
    for (String onError : new String[] { "fail", "askUser", "ignore" }) {
      ObjectNode controller = controller();
      value(controller).put("onError", onError);
      assertTrue(onError, valid(controller));
    }
  }

  @Test
  public void rejectsInvalidErrorHandlers() {
    for (String onError : new String[] { "failSuffix", "prefixignore", "other" }) {
      ObjectNode controller = controller();
      value(controller).put("onError", onError);
      assertFalse(onError, valid(controller));
    }
  }

  @Test
  public void requiresNonNegativeIntegerArgumentIndexes() {
    ObjectNode controller = controller();
    ObjectNode pass = ((ObjectNode) controller.path("arguments").path("origin")).putObject("pass");
    pass.put("index", 0);
    assertTrue(valid(controller));
    pass.put("index", 2);
    assertTrue(valid(controller));
    pass.put("index", -1);
    assertFalse(valid(controller));
    pass.put("index", 0.5);
    assertFalse(valid(controller));
  }

  private ObjectNode controller() {
    return (ObjectNode) JsonHelper.jsonFromResource("schema/controllerGood01.json");
  }

  private ObjectNode value(ObjectNode controller) {
    return (ObjectNode) controller.path("arguments").path("origin").path("value");
  }

  private boolean valid(ObjectNode controller) {
    return JsonHelper.validateToSchema(URI.create(schema()), controller);
  }

  @Override
  protected Set<String> good() {
    return ImmutableSet.of("schema/controllerGood01.json");
  }

  @Override
  protected Set<String> bad() {
    return ImmutableSet.of(
      "schema/controllerBad01.json", // Invalid instrument type.
      "schema/controllerBad02.json", // Invalid argument source.
      "schema/controllerBad03.json", // Invalid selection strategy.
      "schema/controllerBad04.json" // Argument source with a suffix.
    );
  }

  @Override
  protected String schema() {
    return "resource:/bundles/core/schema/controller.schema.json";
  }
}
