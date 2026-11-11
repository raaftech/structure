package tech.raaf.structure.helper;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.exceptions.JsonValidationException;
import tools.jackson.databind.JsonNode;

public class JsonHelperTest {

  // A server that counts its requests and serves an empty schema
  private final AtomicInteger requests = new AtomicInteger();
  private HttpServer server;
  private URI namespace;
  private String host;

  @BeforeEach
  public void setUp() throws Exception {
    server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    server.createContext("/", exchange -> {
      requests.incrementAndGet();
      final byte[] body = "{}".getBytes();
      exchange.sendResponseHeaders(200, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    server.start();
    host = "http://localhost:" + server.getAddress().getPort();
    namespace = Context.getSchemaNamespace();
    Context.setSchemaNamespace(URI.create(host + "/"));
  }

  @AfterEach
  public void tearDown() {
    server.stop(0);
    Context.setSchemaNamespace(namespace);
  }

  private boolean validates(String ref) {
    final JsonNode schema = JsonHelper.mapper.createObjectNode().put("$ref", ref);
    return JsonHelper.validationErrors(schema, JsonHelper.mapper.createObjectNode()).isEmpty();
  }

  @Test
  public void refInNamespaceLoads() {
    assertTrue(validates(host + "/schemas/other.schema.json"));
    assertEquals(1, requests.get());
  }

  @Test
  public void refToOtherHostFails() {
    assertFalse(validates("http://127.0.0.1:" + server.getAddress().getPort() + "/schemas/other.schema.json"));
    assertEquals(0, requests.get());
  }

  @Test
  public void refOutOfNamespaceFails() {
    assertFalse(validates(host + "/other.schema.json"));
    assertFalse(validates(host + "/schemas/../other.schema.json"));
    assertFalse(validates(host + "/schemas/%2E%2e/other.schema.json"));
    assertEquals(0, requests.get());
  }

  private static URI collectionUri(String name) {
    return URI.create("resource:/schema/collection/" + name + ".schema.json");
  }

  // Relative and local references, fragments, id scopes and a circle
  @Test
  public void collectsEachReferencedDocumentOnce() {
    final Map<URI, JsonNode> documents = JsonHelper.schemaCollection(List.of(collectionUri("first")));
    assertEquals(
      List.of(collectionUri("first"), collectionUri("second"), collectionUri("shared")),
      List.copyOf(documents.keySet())
    );
    // The documents keep their references and fragments
    final JsonNode properties = documents.get(collectionUri("first")).path("properties");
    assertEquals("shared.schema.json#/definitions/text", properties.path("shared").path("$ref").asString());
  }

  // A root with a fragment gives its document
  @Test
  public void collectsTheDocumentOfARootWithAFragment() {
    final URI root = URI.create(collectionUri("shared") + "#/definitions/text");
    assertEquals(Set.of(collectionUri("shared")), JsonHelper.schemaCollection(List.of(root)).keySet());
  }

  @Test
  public void reportsWhatIsWrongInTheCollection() {
    final Map<String, String> problems = Map.of(
      "missing",
      "missing document resource:/schema/collection/nothere.schema.json",
      "pointer",
      "invalid reference resource:/schema/collection/shared.schema.json#/definitions/nothere",
      "syntax",
      "invalid reference or id in resource:/schema/collection/syntax.schema.json",
      "conflict",
      "conflicting id resource:/schema/collection/same.schema.json",
      "taken",
      "conflicting id resource:/schema/collection/shared.schema.json"
    );
    problems.forEach((name, problem) -> {
      final JsonValidationException e = assertThrows(JsonValidationException.class, () ->
        JsonHelper.schemaCollection(List.of(collectionUri(name)))
      );
      assertTrue(e.getMessage().contains(problem), e.getMessage());
    });
  }

  // The validator follows a circular reference only as far as the data
  @Test
  public void validatesAgainstSchemasWithACircularReference() {
    final JsonNode valid = JsonHelper.mapper.readTree("{\"second\":{\"first\":{\"local\":1,\"shared\":\"x\"}}}");
    final JsonNode invalid = JsonHelper.mapper.readTree("{\"second\":{\"first\":{\"local\":2}}}");
    assertTrue(JsonHelper.validationErrors(collectionUri("first"), valid).isEmpty());
    assertFalse(JsonHelper.validationErrors(collectionUri("first"), invalid).isEmpty());
  }
}
