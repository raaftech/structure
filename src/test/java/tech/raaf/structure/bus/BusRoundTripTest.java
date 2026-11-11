package tech.raaf.structure.bus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.raaf.structure.helper.JsonHelper.mapper;

import com.networknt.schema.AbsoluteIri;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import com.networknt.schema.resource.InputStreamSource;
import com.networknt.schema.resource.SchemaLoader;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import mockit.Expectations;
import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.bus.manager.EntityManager;
import tech.raaf.structure.bus.manager.InstrumentManager;
import tech.raaf.structure.bus.manager.StructureManager;
import tech.raaf.structure.bus.repository.Instances;
import tech.raaf.structure.bus.repository.Types;
import tech.raaf.structure.graph.Entity;
import tech.raaf.structure.graph.Graph;
import tech.raaf.structure.graph.Result;
import tech.raaf.structure.helper.JsonHelper;
import tech.raaf.structure.helper.ProcessRunner;
import tech.raaf.structure.schema.TypeSchemaTest;
import tools.jackson.databind.JsonNode;

/**
 * Sends requests to managers, repositories and nodes. Checks each reply
 * against the response envelope and the declared handler output.
 * The actors run on the test Vert.x instance. The writes commit.
 */
@ExtendWith(JMockitExtension.class)
public class BusRoundTripTest extends TypeSchemaTest {

  private static final String BUS = "tech.raaf.structure.bus.";
  private static final String TYPES = BUS + "repository.types";
  private static final String INSTANCES = BUS + "repository.instances";
  private static final String ENTITIES = BUS + "manager.entitymanager";
  private static final String STRUCTURE = BUS + "manager.structuremanager";
  private static final String INSTRUMENTS = BUS + "manager.instrumentmanager";
  private static final String OPERATING_SYSTEM = "f05e325e-1aee-4892-96e4-8bcd6e97c528";
  private static final String UNIX_LIKE = "6a1eea8b-1462-4439-8c60-12edbee6c397";
  private static final String COMPONENT_GROUP = "ee92c308-80f4-40b2-af3a-77d26cd99517";
  private static final String DNS_NAME = "99c0da37-3599-418d-8ecc-de36d37ae9f4";
  private static final String EVENTS = BUS + "events";
  private static final URI RESPONSE = URI.create("resource:/schema/base/response.schema.json");

  private final String host = UUID.randomUUID().toString();
  private final String group = UUID.randomUUID().toString();
  private final String created = UUID.randomUUID().toString();

  @BeforeEach
  public void deploy() throws Exception {
    // Application sets these, info and reload need them
    Context.setHostname("bus-test-host");
    for (Verticle verticle : Arrays.asList(
      new EntityManager(),
      new StructureManager(),
      new InstrumentManager(),
      new Types(Context.getTypeRepository()),
      new Instances(Context.getInstanceRepository())
    )) {
      Context.getVertx().deployVerticle(verticle).await();
    }
    mapper.readValue(os(host, "bus-host", "A host for the bus test."), Entity.class).toGraph();
    mapper
      .readValue(entity(group, "bus-group", "A group for the bus test.", COMPONENT_GROUP, "{}"), Entity.class)
      .toGraph();
    for (String uuid : Arrays.asList(OPERATING_SYSTEM, host)) {
      NodeActor.deploy(Context.getNodeRepository().findByUuid(uuid)).get();
    }
    // The instrument handlers need a bootstrapped Structure
    Context.setSelfGroup(Context.getGroupingRepository().findByUuid(group));
    Context.setSelfIdentifier(group);
  }

  @AfterEach
  public void reset() {
    Context.setSelfGroup(null);
    Context.setSelfIdentifier(null);
  }

  @Test
  public void roundTrip() throws Exception {
    String hostAddress = BUS + "node." + host;
    String typeAddress = BUS + "node." + OPERATING_SYSTEM;
    // Address, msgh, args and the expected error, if there is one.
    // Reinitialize and selfDiscovery each have their own test.
    String[][] rows = {
      { TYPES, "getAll", "{}", null },
      { TYPES, "findOneByName", "{'name': 'ComponentGroup'}", null },
      { TYPES, "findOneByCaseInsensitiveName", "{'name': 'componentgroup'}", null },
      { TYPES, "findAllTypeGlyphs", "{}", null },
      { TYPES, "findOne", "{'node': '" + OPERATING_SYSTEM + "'}", null },
      { ENTITIES, "read", "{'labels': ['Type'], 'start': '" + OPERATING_SYSTEM + "', 'includeStart': true}", null },
      { ENTITIES, "create", "{'entities': [" + os(created, "bus-created", "Created.") + "]}", null },
      { ENTITIES, "update", "{'entities': [" + os(created, "bus-created", "Updated.") + "]}", null },
      { ENTITIES, "specialize", "{'instance': '" + created + "', 'isa': '" + UNIX_LIKE + "'}", null },
      { ENTITIES, "group", "{'group': '" + group + "', 'member': '" + host + "'}", null },
      { ENTITIES, "ungroup", "{'group': '" + group + "', 'member': '" + host + "'}", null },
      { ENTITIES, "delete", "{'labels': ['Instance'], 'uuid': '" + created + "', 'includeStart': true}", null },
      { INSTANCES, "nLastCreated", "{'amount': 5}", null },
      { INSTANCES, "findByName", "{'name': 'bus-host'}", null },
      { INSTANCES, "findOneByData", "{'property': 'shortName', 'value': 'os'}", null },
      { INSTANCES, "findOneByData", "{'property': 'shortName', 'value': 'missing'}", null },
      {
        INSTANCES,
        "putNode",
        "{'node': '" + DNS_NAME + "', 'name': 'dns', 'desc': '', 'dnsname': 'test.example'}",
        null,
      },
      { STRUCTURE, "reload", "{}", null },
      { STRUCTURE, "list", "{}", null },
      { STRUCTURE, "introspect", "{}", null },
      { STRUCTURE, "info", "{}", null },
      { STRUCTURE, "help", "{}", null },
      { STRUCTURE, "help", "{'name': 'info'}", null },
      { STRUCTURE, "help", "{'name': 'noSuchHandler'}", "No handler named noSuchHandler." },
      { STRUCTURE, "help", "{'name': 1}", "Arguments didn't validate against schema: [" },
      { STRUCTURE, "version", "{}", null },
      { typeAddress, "help", "{}", null },
      { hostAddress, "version", "{}", null },
      { typeAddress, "getSubtypes", "{}", null },
      { typeAddress, "getSchema", "{}", null },
      { typeAddress, "getPath", "{}", null },
      { typeAddress, "uuid", "{}", null },
      { typeAddress, "getConstraints", "{}", null },
      { hostAddress, "json", "{}", null },
      { hostAddress, "getInstruments", "{}", null },
      { hostAddress, "getCapabilities", "{}", null },
      { hostAddress, "getResults", "{}", null },
      // A match would run a script on this machine, so use no match
      { hostAddress, "runInstrument", "{'instrument': 'NoSuchInstrument'}", "Couldn't find instrument." },
      {
        hostAddress,
        "runCapability",
        "{'capability': 'NoSuchCapability', 'arguments': {'token': 'x'}}",
        "Couldn't find heaviest controller for capability",
      },
      // The error paths of the msgh dispatch, a null msgh is absent
      { ENTITIES, "noSuchHandler", "{}", "No handler for called method." },
      { TYPES, "findOneByName", "{}", "Arguments didn't validate against schema: [" },
      { INSTANCES, "nLastCreated", "{'amount': 5, 'labels': 'Group'}", "Arguments didn't validate against schema: [" },
      { INSTANCES, "nLastCreated", "{'amount': 5, 'labels': [1]}", "Arguments didn't validate against schema: [" },
      { INSTANCES, "nLastCreated", "{'amount': 5, 'labels': null}", "Arguments didn't validate against schema: [" },
      { ENTITIES, null, "{}", "Request didn't validate against schema: [" },
      { ENTITIES, "no.such.handler", "{}", "Request didn't validate against schema: [" },
      { ENTITIES, "read", "[]", "Request didn't validate against schema: [" },
      { INSTRUMENTS, "removeInstruments", "{}", null },
      { INSTRUMENTS, "reloadInstruments", "{}", null },
      { hostAddress, "undeploy", "{}", null },
    };
    List<String> failures = new ArrayList<>();
    for (String[] row : rows) {
      String name = row[0] + " " + row[1] + ": ";
      try {
        JsonNode response = send(row[0], row[1], row[2].replace('\'', '"'));
        JsonNode error = response.path("result").path("error");
        if (!JsonHelper.validationErrors(RESPONSE, response).isEmpty()) {
          failures.add(name + "invalid response " + response);
        } else if (row[3] == null ? !error.isMissingNode() : !error.asString().startsWith(row[3])) {
          failures.add(name + response.get("result"));
        }
      } catch (ExecutionException e) {
        failures.add(name + e.getCause().getMessage());
      }
    }
    assertTrue(failures.isEmpty(), String.join("\n", failures));
  }

  @Test
  public void relationResponsesMatchTheirContract() throws Exception {
    String dns = UUID.randomUUID().toString();
    mapper
      .readValue(entity(dns, "dns", "A DNS name.", DNS_NAME, "{\"dnsname\":\"test.example\"}"), Entity.class)
      .toGraph();
    String args = new JsonObject().put("from", host).put("relation", "HAS").put("to", dns).encode();
    for (String handler : List.of("relate", "unrelate")) {
      for (boolean changed : List.of(true, false)) {
        JsonNode reply = send(ENTITIES, handler, args);
        assertFalse(reply.path("result").has("error"), reply.toString());
        assertEquals(changed, reply.path("result").path("data").path("changed").asBoolean());
        if (handler.equals("relate") && changed) {
          JsonNode children = send(BUS + "node." + host, "getComponentsByType", "{\"type\":\"DnsName\"}");
          assertFalse(children.path("result").has("error"), children.toString());
          assertEquals(1, children.path("result").path("data").size());
        }
      }
    }
  }

  @Test
  public void reinitializeResponseMatchesItsContract() throws Exception {
    CompletableFuture<JsonObject> done = new CompletableFuture<>();
    MessageConsumer<JsonObject> events = Context.getVertx()
      .eventBus()
      .consumer(EVENTS, message -> {
        if (
          "structureReinitialization".equals(message.body().getString("event")) &&
          "finished".equals(message.body().getJsonObject("data").getString("data"))
        ) {
          done.complete(message.body());
        }
      });
    try {
      JsonNode reply = send(STRUCTURE, "reinitialize", "{}");
      assertFalse(reply.path("result").has("error"), reply.toString());
      assertTrue(reply.path("result").path("data").isArray());
      done.get(30, TimeUnit.SECONDS);
    } finally {
      events.unregister().await();
    }
  }

  @Test
  public void infoDescribesStructureTheGraphAndTheJvm() throws Exception {
    JsonNode reply = send(STRUCTURE, "info", "{}");
    assertFalse(reply.path("result").has("error"), reply.toString());
    JsonNode data = reply.path("result").path("data");
    assertEquals(Context.getSelfGroup() != null, data.path("structure").path("bootstrapped").asBoolean());
    assertEquals(Context.getHostname(), data.path("structure").path("host").asString());
    assertEquals(Context.getTypeRepository().count(), data.path("graph").path("types").asLong());
    assertEquals(Context.getInstrumentRepository().count(), data.path("graph").path("instruments").asLong());
    assertEquals(Runtime.version().feature(), data.path("jvm").path("javaVersion").asInt());
    assertTrue(data.path("jvm").path("processors").asInt() > 0);
  }

  @Test
  public void resolvesPublishedContractsOverWebSocket() throws Exception {
    Router router = Router.router(Context.getVertx());
    SockJSBridgeOptions options = new SockJSBridgeOptions().addInboundPermitted(
      new PermittedOptions().setAddressRegex("tech\\.raaf\\.structure\\..*")
    );
    router.route("/eventbus/*").subRouter(SockJSHandler.create(Context.getVertx()).bridge(options));
    HttpServer server = Context.getVertx().createHttpServer().requestHandler(router).listen(0, "localhost").await();
    WebSocketClient client = Context.getVertx().createWebSocketClient();
    try {
      WebSocket socket = client.connect(server.actualPort(), "localhost", "/eventbus/websocket").await();
      JsonNode discovery = send(socket, STRUCTURE, "introspect", "{}");
      JsonNode metadata = discovery.path("result").path("data");
      JsonNode documents = metadata.path("schemas");
      SchemaRegistry registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_4, builder ->
        builder.schemaLoader(
          new SchemaLoader(iri -> null) {
            @Override
            public InputStreamSource getSchemaResource(AbsoluteIri iri) {
              if (iri.toString().equals("http://json-schema.org/draft-04/schema")) {
                return super.getSchemaResource(iri);
              }
              JsonNode document = documents.get(iri.toString());
              assertNotNull(document, "Unpublished schema: " + iri);
              return () -> new ByteArrayInputStream(document.toString().getBytes(StandardCharsets.UTF_8));
            }
          }
        )
      );
      documents.properties().forEach(entry -> {
        assertValid(registry, "http://json-schema.org/draft-04/schema", entry.getValue());
        registry.getSchema(SchemaLocation.of(entry.getKey()));
      });
      String[][] rows = {
        { STRUCTURE, STRUCTURE, "introspect", "{}" },
        { STRUCTURE, STRUCTURE, "info", "{}" },
        { STRUCTURE, STRUCTURE, "help", "{}" },
        { STRUCTURE, STRUCTURE, "version", "{}" },
        { TYPES, TYPES, "findOneByName", "{\"name\":\"OperatingSystem\"}" },
        { TYPES, TYPES, "findByName", "{\"name\":\"OperatingSystem\"}" },
        {
          ENTITIES,
          ENTITIES,
          "read",
          "{\"labels\":[\"Type\"],\"start\":\"" + OPERATING_SYSTEM + "\",\"includeStart\":true}",
        },
        { BUS + "node." + host, BUS + "instanceactor", "json", "{}" },
        { BUS + "node." + OPERATING_SYSTEM, BUS + "typeactor", "getConstraints", "{}" },
      };
      for (String[] row : rows) {
        JsonNode handler = metadata.path("classes").path(row[1]).path("handlers").path(row[2]);
        JsonNode reply = send(socket, row[0], row[2], row[3]);
        assertFalse(reply.path("result").has("error"), reply.toString());
        assertValid(registry, "resource:/schema/base/request.schema.json", reply.path("request"));
        assertValid(registry, RESPONSE.toString(), reply);
        assertValid(registry, handler.path("inputSchemaUri").asString(), mapper.readTree(row[3]));
        assertValid(registry, handler.path("outputSchemaUri").asString(), reply.path("result").path("data"));
      }
      // A known invalid input and output must fail with this same registry.
      JsonNode find = metadata.path("classes").path(TYPES).path("handlers").path("findOneByName");
      assertFalse(
        registry
          .getSchema(SchemaLocation.of(find.path("inputSchemaUri").asString()))
          .validate(mapper.createObjectNode())
          .isEmpty()
      );
      assertFalse(
        registry
          .getSchema(SchemaLocation.of(find.path("outputSchemaUri").asString()))
          .validate(mapper.createObjectNode())
          .isEmpty()
      );
      socket.close().await();
    } finally {
      client.close().await();
      server.close().await();
    }
  }

  private static void assertValid(SchemaRegistry registry, String uri, JsonNode value) {
    var errors = registry.getSchema(SchemaLocation.of(uri)).validate(value);
    assertTrue(errors.isEmpty(), uri + ": " + errors);
  }

  private static JsonNode send(WebSocket socket, String address, String handler, String args) throws Exception {
    CompletableFuture<JsonObject> reply = new CompletableFuture<>();
    socket.binaryMessageHandler(buffer -> reply.complete(buffer.toJsonObject()));
    socket.exceptionHandler(reply::completeExceptionally);
    socket
      .writeTextMessage(
        new JsonObject()
          .put("type", "send")
          .put("address", address)
          .put("replyAddress", UUID.randomUUID().toString())
          .put("body", new JsonObject().put("msgh", handler).put("args", new JsonObject(args)))
          .encode()
      )
      .await();
    JsonObject message = reply.get(10, TimeUnit.SECONDS);
    assertEquals("rec", message.getString("type"), message.encode());
    return mapper.readTree(message.getJsonObject("body").encode());
  }

  // The reply has the task ID that each instance gets
  @Test
  public void runCapabilityGivesTaskId() throws Exception {
    JsonNode data = send(
      INSTRUMENTS,
      "runCapability",
      "{\"capability\": \"NoSuchCapability\", \"uuids\": [\"" + host + "\"]}"
    )
      .path("result")
      .path("data");
    assertEquals(data.asString(), UUID.fromString(data.asString()).toString());
  }

  // The graph has the name of the host, thus the job does not run the bootstrap
  @Test
  public void selfDiscoveryFindsTheHost() throws Exception {
    new MockUp<ProcessRunner>() {
      @Mock
      public void run() throws IOException {
        throw new IOException("Ansible is not installed");
      }
    };
    // Context keeps the first hostname that a test sets
    String data = "{\"dnsname\": \"" + Context.getHostname() + "\"}";
    String dnsUuid = UUID.randomUUID().toString();
    String dns = entity(dnsUuid, "bus-dns", "The test host.", DNS_NAME, data);
    mapper.readValue(dns, Entity.class).toGraph();
    Graph.query(
      "MATCH (os {uuid: $os}), (dns {uuid: $dns}), (g {uuid: $group}) " +
        "MERGE (os)-[:HAS]->(dns) MERGE (os)-[:MEMBER_OF]->(g)",
      Map.of("os", host, "dns", dnsUuid, "group", group)
    );
    CompletableFuture<JsonObject> done = new CompletableFuture<>();
    MessageConsumer<JsonObject> events = Context.getVertx()
      .eventBus()
      .consumer(EVENTS, message -> {
        if ("selfDiscovery".equals(message.body().getString("event"))) {
          done.complete(message.body().getJsonObject("data"));
        }
      });
    try {
      JsonNode reply = send(STRUCTURE, "selfDiscovery", "{}");
      assertTrue(reply.path("result").path("error").isMissingNode(), reply.toString());
      assertEquals("done", done.get(10, TimeUnit.SECONDS).getString("status"));
    } finally {
      events.unregister().await();
    }
  }

  @Test
  public void findsTheOperatingSystemOfAComponent() throws Exception {
    String dnsUuid = UUID.randomUUID().toString();
    mapper
      .readValue(entity(dnsUuid, "dns", "A host name.", DNS_NAME, "{\"dnsname\":\"host.example\"}"), Entity.class)
      .toGraph();
    assertTrue(
      send(INSTANCES, "findOperatingSystem", "{\"node\":\"" + dnsUuid + "\"}")
        .path("result")
        .path("data")
        .isEmpty()
    );
    Graph.query(
      "MATCH (os {uuid: $os}), (dns {uuid: $dns}) MERGE (os)-[:HAS]->(dns)",
      Map.of("os", host, "dns", dnsUuid)
    );
    assertEquals(host, Context.getInstanceRepository().findOperatingSystem(dnsUuid).getUuid());
    JsonNode reply = send(INSTANCES, "findOperatingSystem", "{\"node\":\"" + dnsUuid + "\"}");
    JsonNode found = reply.path("result").path("data");
    assertEquals(host, found.path("meta").path("uuid").asString(), reply.toString());
    assertEquals(
      host,
      send(INSTANCES, "findOperatingSystem", "{\"node\":\"" + host + "\"}")
        .path("result")
        .path("data")
        .path("meta")
        .path("uuid")
        .asString()
    );
    String other = UUID.randomUUID().toString();
    mapper.readValue(os(other, "other", "Another OS."), Entity.class).toGraph();
    Graph.query(
      "MATCH (os {uuid: $os}), (dns {uuid: $dns}) MERGE (os)-[:HAS]->(dns)",
      Map.of("os", other, "dns", dnsUuid)
    );
    assertTrue(
      send(INSTANCES, "findOperatingSystem", "{\"node\":\"" + dnsUuid + "\"}")
        .path("result")
        .has("error")
    );
  }

  @Test
  public void selfDiscoveryUsesTheAnsibleInstrument(@Mocked Process process) throws Exception {
    String dnsUuid = UUID.randomUUID().toString();
    mapper
      .readValue(
        entity(dnsUuid, "local", "The Structure host.", DNS_NAME, "{\"dnsname\":\"" + Context.getHostname() + "\"}"),
        Entity.class
      )
      .toGraph();
    Graph.query(
      "MATCH (os {uuid: $os}), (dns {uuid: $dns}), (g {uuid: $group}) " +
        "MERGE (os)-[:HAS]->(dns) MERGE (os)-[:MEMBER_OF]->(g)",
      Map.of("os", host, "dns", dnsUuid, "group", group)
    );
    List<String> commands = new ArrayList<>();
    new MockUp<ProcessRunner>() {
      @Mock
      public void run(Invocation invocation) {
        ProcessRunner runner = invocation.getInvokedInstance();
        commands.add(runner.getCommand());
      }

      @Mock
      public Process getProcess() {
        return process;
      }

      @Mock
      public ProcessRunner.ProcessResult finish() {
        ProcessRunner.ProcessResult result = new ProcessRunner.ProcessResult();
        result.returnCode = Optional.of(0);
        result.stdout = Optional.empty();
        result.stderr = Optional.empty();
        return result;
      }
    };
    new Expectations() {
      {
        process.waitFor(anyLong, (TimeUnit) any);
        result = true;
      }
    };
    for (int i = 0; i < 2; i++) {
      CompletableFuture<JsonObject> done = new CompletableFuture<>();
      MessageConsumer<JsonObject> events = Context.getVertx()
        .eventBus()
        .consumer(EVENTS, message -> {
          if ("selfDiscovery".equals(message.body().getString("event"))) {
            done.complete(message.body().getJsonObject("data"));
          }
        });
      try {
        send(STRUCTURE, "selfDiscovery", "{}");
        JsonObject response = done.get(10, TimeUnit.SECONDS);
        assertEquals("done", response.getString("status"), response.toString());
      } finally {
        events.unregister().await();
      }
      assertEquals(group, Context.getSelfIdentifier());
      assertEquals(host, Context.getInstanceRepository().findOperatingSystem(dnsUuid).getUuid());
    }
    assertEquals(4, commands.size());
    assertEquals("ansible --version", commands.get(0));
    assertTrue(commands.get(1).startsWith("python3 '"));
    assertTrue(commands.get(1).endsWith("/bundles/ansible/payload/main.py' -i '127.0.0.1' --self-id '" + group + "'"));
    assertEquals(commands.subList(0, 2), commands.subList(2, 4));
    assertEquals(1, Context.getGroupingRepository().getChildrenOfType(group, "Ansible").size());
    assertEquals(1, Context.getGroupingRepository().getChildrenOfType(group, "Ipv4Address").size());
  }

  @Test
  public void nLastCreatedReturnsComponentsAndGroups() throws Exception {
    JsonNode data = send(INSTANCES, "nLastCreated", "{\"amount\": 5, \"labels\": [\"Component\", \"Group\"]}")
      .path("result")
      .path("data");
    Set<String> uuids = new HashSet<>();
    data.forEach(instance -> uuids.add(instance.path("uuid").asString()));
    assertEquals(new HashSet<>(Arrays.asList(host, group)), uuids);
  }

  @Test
  public void nLastCreatedSupportsOptionalLabels() throws Exception {
    Result result = new Entity.ResultBuilder().process("test").taskId("latest-instances-test").buildAndStore();
    JsonNode all = send(INSTANCES, "nLastCreated", "{\"amount\": 1000}").path("result").path("data");
    assertEquals(Context.getInstanceRepository().count(), all.size());
    assertEquals(
      all,
      send(INSTANCES, "nLastCreated", "{\"amount\": 1000, \"labels\": []}").path("result").path("data")
    );
    JsonNode latest = send(INSTANCES, "nLastCreated", "{\"amount\": 1}").path("result").path("data");
    assertEquals(1, latest.size());
    assertEquals(result.getUuid(), latest.get(0).path("uuid").asString());
    JsonNode groups = send(INSTANCES, "nLastCreated", "{\"amount\": 1, \"labels\": [\"Group\"]}")
      .path("result")
      .path("data");
    assertEquals(1, groups.size());
    assertEquals(group, groups.get(0).path("uuid").asString());
    assertEquals(
      0,
      send(INSTANCES, "nLastCreated", "{\"amount\": 5, \"labels\": [\"NoSuchLabel\"]}")
        .path("result")
        .path("data")
        .size()
    );
  }

  private static JsonNode send(String address, String msgh, String args) throws Exception {
    JsonObject request = new JsonObject("{\"args\": " + args + "}");
    if (msgh != null) {
      request.put("msgh", msgh);
    }
    final Object reply = Context.getVertx()
      .eventBus()
      .request(address, request, new DeliveryOptions().setSendTimeout(10000))
      .await()
      .body();
    return mapper.readTree(reply.toString());
  }

  private static String os(String uuid, String name, String desc) {
    return entity(
      uuid,
      name,
      desc,
      OPERATING_SYSTEM,
      "{\"ver\": \"1\", \"shortName\": \"os\", \"longName\": \"Operating system\"}"
    );
  }

  private static String entity(String uuid, String name, String desc, String isa, String data) {
    return String.format(
      "{\"name\": \"%s\", \"desc\": \"%s\", \"meta\": {\"uuid\": \"%s\", " +
        "\"relations\": {\"INSTANCE_OF\": {\"direction\": \"outgoing\", \"to\": [\"%s\"]}}}, " +
        "\"data\": %s}",
      name,
      desc,
      uuid,
      isa,
      data
    );
  }
}
