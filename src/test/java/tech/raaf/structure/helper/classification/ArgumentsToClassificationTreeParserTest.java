package tech.raaf.structure.helper.classification;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collections;
import org.junit.Test;
import tech.raaf.structure.helper.classification.ArgumentsToClassificationTreeParser.ClassificationTreeObjects;

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
