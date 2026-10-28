package tech.raaf.structure.helper;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.raaf.structure.base.Context;
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
    return JsonHelper.validateToSchema(schema, JsonHelper.mapper.createObjectNode());
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
}
