package tech.raaf.structure.bus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

public class GenericActorTest {

  public static class Probe extends GenericActor {

    // A handler without help
    public MessageHandler silent() {
      return probe(request -> response(request, JsonNodeFactory.instance.objectNode().put("data", true)));
    }

    // A result without data or error
    public MessageHandler empty() {
      return probe(request -> response(request, JsonNodeFactory.instance.objectNode()));
    }

    // A result with data and error
    public MessageHandler both() {
      return probe(request ->
        response(request, JsonNodeFactory.instance.objectNode().put("data", 1).put("error", "Failed."))
      );
    }

    public MessageHandler nothing() {
      return probe(request -> null);
    }

    private MessageHandler probe(java.util.function.UnaryOperator<JsonNode> exec) {
      return new MessageHandler() {
        public JsonNode exec(JsonNode request) {
          return exec.apply(request);
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
    JsonNode data = new Probe().help().exec(request).path("result").path("data");
    assertFalse(data.has("silent"));
    assertEquals("help", data.path("help").path("name").asString());
    assertEquals("version", data.path("version").path("name").asString());
  }

  // Each reply has data or an error, also when a handler breaks this
  @Test
  public void repliesWithAnErrorToAnInvalidResponse() {
    Vertx vertx = Vertx.vertx();
    try {
      Probe probe = new Probe();
      vertx.deployVerticle(probe).await();
      for (String msgh : List.of("empty", "both", "nothing")) {
        JsonObject result = result(vertx, probe, new JsonObject().put("msgh", msgh));
        assertEquals(1, result.size(), result.encode());
        assertTrue(result.getString("error").startsWith("Invalid response: "), result.encode());
      }
      JsonObject version = result(vertx, probe, new JsonObject().put("msgh", "version"));
      assertEquals("Structure", version.getJsonObject("data").getJsonObject("release").getString("name"));
      JsonObject text = result(vertx, probe, "version");
      assertTrue(text.getString("error").startsWith("Request didn't validate against schema: "), text.encode());
    } finally {
      vertx.close().await();
    }
  }

  private static JsonObject result(Vertx vertx, Probe probe, Object body) {
    JsonObject reply = (JsonObject) vertx.eventBus().request(probe.getActorName(), body).await().body();
    return reply.getJsonObject("result");
  }
}
