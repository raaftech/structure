package tech.raaf.structure.schema;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;
import tech.raaf.structure.helper.JsonHelper;
import tools.jackson.databind.JsonNode;

/**
 * Tests to check whether JSON schemas in Structure are correct.
 */
public abstract class SchemaTest {

  @Test
  public void testGoodJson() {
    List<String> rejected = good()
      .stream()
      .filter(file -> !valid(file))
      .collect(Collectors.toList());
    assertTrue("Valid fixtures rejected: " + rejected, rejected.isEmpty());
  }

  @Test
  public void testBadJson() {
    List<String> accepted = bad().stream().filter(this::valid).collect(Collectors.toList());
    assertTrue("Invalid fixtures accepted: " + accepted, accepted.isEmpty());
  }

  private boolean valid(String file) {
    JsonNode json = JsonHelper.jsonFromResourceOrFile(file);
    assertNotNull("Missing fixture " + file, json);
    return JsonHelper.validateToSchema(URI.create(schema()), json);
  }

  protected abstract Set<String> good();

  protected abstract Set<String> bad();

  protected abstract String schema();
}
