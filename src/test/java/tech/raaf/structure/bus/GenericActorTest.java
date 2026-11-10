package tech.raaf.structure.bus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import tech.raaf.structure.annotations.SchemaPath;
import tech.raaf.structure.bus.GenericActor.MessageHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public class GenericActorTest {

  static final String OUTPUT = "resource:/schema/probe.schema.json";

  public static class Probe extends GenericActor {

    @SchemaPath(output = OUTPUT)
    public MessageHandler silent() {
      return probe(request -> response(request, JsonNodeFactory.instance.objectNode().put("data", true)));
    }

    // A result without data or error
    @SchemaPath(output = OUTPUT)
    public MessageHandler empty() {
      return probe(request -> response(request, JsonNodeFactory.instance.objectNode()));
    }

    // A result with data and error
    @SchemaPath(output = OUTPUT)
    public MessageHandler both() {
      return probe(request ->
        response(request, JsonNodeFactory.instance.objectNode().put("data", 1).put("error", "Failed."))
      );
    }

    @SchemaPath(output = OUTPUT)
    public MessageHandler nothing() {
      return probe(request -> null);
    }
  }

  // A handler with the given response and help
  static MessageHandler probe(UnaryOperator<JsonNode> exec, JsonNode help) {
    return new MessageHandler() {
      public JsonNode exec(JsonNode request) {
        return exec.apply(request);
      }

      public JsonNode help(JsonNode request) {
        return help;
      }
    };
  }

  static MessageHandler probe(UnaryOperator<JsonNode> exec) {
    ObjectNode help = JsonNodeFactory.instance.objectNode().put("name", "probe").put("description", "A probe.");
    help.putObject("synopsis");
    return probe(exec, help);
  }

  public static class NoOutput extends GenericActor {

    public MessageHandler probe() {
      return GenericActorTest.probe(request -> null);
    }
  }

  public static class NoInputDocument extends GenericActor {

    @SchemaPath(input = "resource:/schema/missing.schema.json", output = OUTPUT)
    public MessageHandler probe() {
      return GenericActorTest.probe(request -> null);
    }
  }

  public static class NoOutputDocument extends GenericActor {

    @SchemaPath(output = "resource:/schema/missing.schema.json")
    public MessageHandler probe() {
      return GenericActorTest.probe(request -> null);
    }
  }

  public static class InvalidPath extends GenericActor {

    @SchemaPath(output = "not a uri")
    public MessageHandler probe() {
      return GenericActorTest.probe(request -> null);
    }
  }

  public static class NullHandler extends GenericActor {

    @SchemaPath(output = OUTPUT)
    public MessageHandler probe() {
      return null;
    }
  }

  public static class NoHelp extends GenericActor {

    @SchemaPath(output = OUTPUT)
    public MessageHandler probe() {
      return GenericActorTest.probe(request -> null, null);
    }
  }

  // A handler declaration that is not complete stops the actor
  @Test
  public void failsAtStartupOnAnIncompleteHandler() {
    assertThrows(IllegalStateException.class, NoOutput::new);
    assertThrows(IllegalStateException.class, NoInputDocument::new);
    assertThrows(IllegalStateException.class, NoOutputDocument::new);
    assertThrows(IllegalArgumentException.class, InvalidPath::new);
    assertThrows(IllegalStateException.class, NullHandler::new);
    assertThrows(IllegalStateException.class, NoHelp::new);
    // The checks that run once for a class run again after a failure
    assertThrows(IllegalStateException.class, NoHelp::new);
  }

  // An omitted input gives the default argument schema
  @Test
  public void usesTheDefaultArgumentSchema() {
    GenericActor.HandlerSchema silent = new Probe().handlers.get("silent");
    assertEquals("resource:/schema/base/args.schema.json", silent.input().toString());
    assertEquals(OUTPUT, silent.output().toString());
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
