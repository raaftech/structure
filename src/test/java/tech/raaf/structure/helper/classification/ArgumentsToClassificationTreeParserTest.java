package tech.raaf.structure.helper.classification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import org.junit.Test;
import tech.raaf.structure.helper.classification.ArgumentsToClassificationTreeParser.ClassificationTreeObjects;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public class ArgumentsToClassificationTreeParserTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void acceptsTheSpecifiedTypeAndItsSubtypes() {
    assertTrue(selects("type.component.software.service"));
    assertTrue(selects("type.component.software.service.sshserver"));
  }

  @Test
  public void rejectsTypesWithTheSameNamePrefix() {
    assertFalse(selects("type.component.software.servicemanager"));
    assertFalse(selects("type.component.software.serviceextra"));
  }

  @Test
  public void requiresLiteralPathSeparators() {
    assertFalse(selects("typeXcomponent.software.service"));
    assertFalse(selects("type.component.software.serviceXsshserver"));
    assertFalse(selects("type.component.software"));
  }

  @Test
  public void usesTheSelectedErrorHandler() {
    for (String onError : new String[] { "fail", "askUser", "ignore", "" }) {
      ObjectNode arguments = mapper.createObjectNode();
      ObjectNode value = arguments
        .putObject("origin")
        .putObject("value")
        .put("from", "origin")
        .put("type", "type.component")
        .put("strategy", "mustBeMany");
      if (!onError.isEmpty()) {
        value.put("onError", onError);
      }
      ClassificationTreeObjects tree = ArgumentsToClassificationTreeParser.asTree(arguments);
      ObjectNode origin = mapper
        .createObjectNode()
        .put("name", "origin")
        .put("_from", "origin")
        .put("_hierarchyPath", "type.component.software");
      tree.root.consumeInput(Collections.singleton(origin));

      assertTrue(onError, tree.result.isEmpty());
      assertEquals(onError, onError.isEmpty() || onError.equals("fail"), tree.failed.contains(origin));
      assertEquals(onError, onError.equals("askUser"), tree.askUser.contains(origin));
    }
  }

  private boolean selects(String hierarchyPath) {
    ObjectNode arguments = mapper.createObjectNode();
    arguments
      .putObject("origin")
      .putObject("value")
      .put("from", "origin")
      .put("type", "type.component.software.service");
    ClassificationTreeObjects tree = ArgumentsToClassificationTreeParser.asTree(arguments);
    ObjectNode origin = mapper
      .createObjectNode()
      .put("name", "origin")
      .put("_from", "origin")
      .put("_hierarchyPath", hierarchyPath);
    tree.root.consumeInput(Collections.singleton(origin));
    return tree.result.has("origin");
  }
}
