package tech.raaf.structure.bus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import tech.raaf.structure.annotations.SchemaPath;
import tech.raaf.structure.bus.GenericActor.MessageHandler;
import tech.raaf.structure.exceptions.JsonValidationException;
import tech.raaf.structure.helper.JsonHelper;
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

  public static class OutputProbe extends Probe {

    @SchemaPath(output = "resource:/schema/output/string.schema.json")
    public MessageHandler text() {
      return echo();
    }

    @SchemaPath(output = "resource:/schema/nullable.schema.json")
    public MessageHandler nullable() {
      return echo();
    }

    @SchemaPath(output = "resource:/schema/output/nodeList.schema.json")
    public MessageHandler nodes() {
      return echo();
    }

    @SchemaPath(output = OUTPUT)
    public MessageHandler any() {
      return echo();
    }

    private MessageHandler echo() {
      return probe(request -> response(request, request.get("args")));
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
    assertThrows(JsonValidationException.class, NoInputDocument::new);
    assertThrows(JsonValidationException.class, NoOutputDocument::new);
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
    List<String> errors = JsonHelper.validationErrors(
      URI.create("resource:/schema/base/response.schema.json"),
      JsonHelper.mapper.readTree(reply.encode())
    );
    assertTrue(errors.isEmpty(), errors.toString());
    return reply.getJsonObject("result");
  }

  @Test
  public void acceptsDataThatMatchesTheOutputSchema() {
    Vertx vertx = Vertx.vertx();
    try {
      OutputProbe probe = new OutputProbe();
      vertx.deployVerticle(probe).await();
      Map<String, List<String>> outputs = Map.of(
        "text", List.of("\"hello\""),
        "nullable", List.of("null", "\"hello\""),
        "nodes", List.of("[]", "[{\"name\":\"node\",\"uuid\":\"12345678-1234-4234-8234-123456789abc\"}]"),
        "any", List.of("null", "true", "42", "\"hello\"", "[]", "{}")
      );
      outputs.forEach((handler, values) -> values.forEach(value -> {
        JsonObject expected = new JsonObject("{\"data\":" + value + "}");
        assertEquals(expected, output(vertx, probe, handler, expected));
      }));
    } finally {
      vertx.close().await();
    }
  }

  @Test
  public void replacesDataThatDoesNotMatchTheOutputSchema() {
    Vertx vertx = Vertx.vertx();
    try {
      OutputProbe probe = new OutputProbe();
      vertx.deployVerticle(probe).await();
      Map<String, List<String>> outputs = Map.of(
        "text", List.of("null", "42", "{}", "[]"),
        "nullable", List.of("false"),
        "nodes", List.of("{}", "[{}]", "[{\"name\":\"node\",\"uuid\":\"invalid\"}]")
      );
      outputs.forEach((handler, values) -> values.forEach(value -> {
        JsonObject result = output(vertx, probe, handler, new JsonObject("{\"data\":" + value + "}"));
        assertEquals(1, result.size());
        assertTrue(result.getString("error").startsWith("Invalid response: "), result.encode());
      }));
    } finally {
      vertx.close().await();
    }
  }

  @Test
  public void checksErrorResponsesOnlyAgainstTheEnvelope() {
    Vertx vertx = Vertx.vertx();
    try {
      OutputProbe probe = new OutputProbe();
      vertx.deployVerticle(probe).await();
      JsonObject error = new JsonObject().put("error", "Failed.");
      assertEquals(error, output(vertx, probe, "text", error));
      JsonObject invalid = output(vertx, probe, "text", new JsonObject().put("error", 1));
      assertTrue(invalid.getString("error").startsWith("Invalid response: "));
      JsonObject unknown = output(vertx, probe, "unknown", new JsonObject());
      assertEquals("No handler for called method.", unknown.getString("error"));
    } finally {
      vertx.close().await();
    }
  }

  private static JsonObject output(Vertx vertx, OutputProbe probe, String handler, JsonObject output) {
    return result(vertx, probe, new JsonObject().put("msgh", handler).put("args", output));
  }
}
