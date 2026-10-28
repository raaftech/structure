package tech.raaf.structure.bus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

public class GenericActorTest {

  public static class Probe extends GenericActor {

    public MessageHandler silent() {
      return new MessageHandler() {
        public JsonNode exec(JsonNode request) {
          return response(request, JsonNodeFactory.instance.objectNode());
        }

        public JsonNode help(JsonNode request) {
          return null;
        }
      };
    }
  }

  @Test
  public void helpSkipsHandlersWithoutHelp() {
    JsonNode request = JsonNodeFactory.instance.objectNode().put("msgh", "help");
    JsonNode result = new Probe().help().exec(request).path("result");
    assertFalse(result.has("silent"));
    assertEquals("help", result.path("help").path("name").asText());
    assertEquals("version", result.path("version").path("name").asText());
  }
}
