package tech.raaf.structure.helper.classification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tech.raaf.structure.helper.classification.ArgumentsToClassificationTreeParser.ClassificationTreeObjects;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public class ArgumentsToClassificationTreeParserTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void selectsAllArgumentsTogether() {
    ObjectNode arguments = mapper.createObjectNode();
    Set<JsonNode> input = new HashSet<>();
    for (int i = 0; i < 32; i++) {
      String key = "argument" + i;
      String from = i % 2 == 0 ? "static" : "origin";
      String type = i % 2 == 0 ? "string" : "type.component.test" + i;
      arguments.putObject(key).putObject("value").put("from", from).put("type", type).put("strategy", "mustBeOne");
      input.add(
        mapper.createObjectNode().put("name", key).put("data", key).put("_from", from).put("_hierarchyPath", type)
      );
    }
    for (int attempt = 0; attempt < 200; attempt++) {
      ClassificationTreeObjects tree = ArgumentsToClassificationTreeParser.asTree(arguments);
      Set<JsonNode> context = new HashSet<>();
      input.forEach(node -> context.add(node.deepCopy()));
      tree.root.consumeInput(context);
      assertTrue(tree.failed.isEmpty());
      assertEquals(32, tree.result.size());
      for (int i = 0; i < 32; i++) {
        String key = "argument" + i;
        assertEquals(key, (i % 2 == 0 ? tree.result.path(key) : tree.result.path(key).path("data")).asString());
      }
    }
  }

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

      assertTrue(tree.result.isEmpty(), onError);
      assertEquals(onError.isEmpty() || onError.equals("fail"), tree.failed.contains(origin), onError);
      assertEquals(onError.equals("askUser"), tree.askUser.contains(origin), onError);
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
