package tech.raaf.structure.bus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static tech.raaf.structure.helper.JsonHelper.mapper;

import io.vertx.core.Verticle;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.bus.manager.EntityManager;
import tech.raaf.structure.bus.manager.InstrumentManager;
import tech.raaf.structure.bus.manager.StructureManager;
import tech.raaf.structure.bus.repository.Instances;
import tech.raaf.structure.bus.repository.Types;
import tech.raaf.structure.graph.Entity;
import tech.raaf.structure.graph.Result;
import tech.raaf.structure.helper.JsonHelper;
import tech.raaf.structure.schema.TypeSchemaTest;
import tools.jackson.databind.JsonNode;

/**
 * Sends each address and msgh pair that the REST models in
 * ext/rest/common/models send, and checks that the reply passes
 * response.schema.json and has no error. The managers, the
 * repositories and a few NodeActors run on the test Vert.x
 * instance. The writes commit.
 */
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
  private static final URI RESPONSE = URI.create("resource:/schema/base/response.schema.json");

  private final String host = UUID.randomUUID().toString();
  private final String group = UUID.randomUUID().toString();
  private final String created = UUID.randomUUID().toString();

  @Before
  public void deploy() throws Exception {
    // Application sets these, status and reload need them
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

  @After
  public void reset() {
    Context.setSelfGroup(null);
    Context.setSelfIdentifier(null);
  }

  @Test
  public void roundTrip() throws Exception {
    String hostAddress = BUS + "node." + host;
    String typeAddress = BUS + "node." + OPERATING_SYSTEM;
    // Address, msgh, args and the expected error, if there is one.
    // The table leaves out reinitialize, because it purges the graph.
    String[][] rows = {
      { TYPES, "getAll", "{}", null },
      { TYPES, "findOneByName", "{'name': 'ComponentGroup'}", null },
      { ENTITIES, "read", "{'labels': ['Type'], 'start': '" + OPERATING_SYSTEM + "', 'includeStart': true}", null },
      { ENTITIES, "create", "{'entities': [" + os(created, "bus-created", "Created.") + "]}", null },
      { ENTITIES, "update", "{'entities': [" + os(created, "bus-created", "Updated.") + "]}", null },
      { ENTITIES, "specialize", "{'instance': '" + created + "', 'isa': '" + UNIX_LIKE + "'}", null },
      { ENTITIES, "group", "{'grouping': '" + group + "', 'groupee': '" + host + "'}", null },
      { ENTITIES, "ungroup", "{'grouping': '" + group + "', 'groupee': '" + host + "'}", null },
      { ENTITIES, "delete", "{'labels': ['Instance'], 'uuid': '" + created + "', 'includeStart': true}", null },
      { INSTANCES, "nLastCreated", "{'amount': 5}", null },
      { INSTANCES, "findByName", "{'name': 'bus-host'}", null },
      { STRUCTURE, "selfDiscovery", "{'token': 'bus-token'}", null },
      { STRUCTURE, "reload", "{}", null },
      { STRUCTURE, "status", "{}", null },
      { typeAddress, "getSubtypes", "{}", null },
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
      { TYPES, "findOneByName", "{}", "Arguments didn't validate against schema." },
      { INSTANCES, "nLastCreated", "{'amount': 5, 'labels': 'Group'}", "Arguments didn't validate against schema." },
      { INSTANCES, "nLastCreated", "{'amount': 5, 'labels': [1]}", "Arguments didn't validate against schema." },
      { INSTANCES, "nLastCreated", "{'amount': 5, 'labels': null}", "Arguments didn't validate against schema." },
      { ENTITIES, null, "{}", "Request didn't validate against schema." },
      { ENTITIES, "no.such.handler", "{}", "Request didn't validate against schema." },
      { ENTITIES, "read", "[]", "Request didn't validate against schema." },
    };
    List<String> failures = new ArrayList<>();
    for (String[] row : rows) {
      String name = row[0] + " " + row[1] + ": ";
      try {
        JsonNode response = send(row[0], row[1], row[2].replace('\'', '"'));
        JsonNode error = response.path("result").path("error");
        if (!JsonHelper.validateToSchema(RESPONSE, response)) {
          failures.add(name + "invalid response " + response);
        } else if (row[3] == null ? !error.isMissingNode() : !row[3].equals(error.asText())) {
          failures.add(name + response.get("result"));
        }
      } catch (ExecutionException e) {
        failures.add(name + e.getCause().getMessage());
      }
    }
    assertTrue(String.join("\n", failures), failures.isEmpty());
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
    assertEquals(data.asText(), UUID.fromString(data.asText()).toString());
  }

  @Test
  public void nLastCreatedReturnsComponentsAndGroups() throws Exception {
    JsonNode data = send(INSTANCES, "nLastCreated", "{\"amount\": 5, \"labels\": [\"Component\", \"Group\"]}")
      .path("result")
      .path("data");
    Set<String> uuids = new HashSet<>();
    data.forEach(instance -> uuids.add(instance.path("uuid").asText()));
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
    assertEquals(result.getUuid(), latest.get(0).path("uuid").asText());
    JsonNode groups = send(INSTANCES, "nLastCreated", "{\"amount\": 1, \"labels\": [\"Group\"]}")
      .path("result")
      .path("data");
    assertEquals(1, groups.size());
    assertEquals(group, groups.get(0).path("uuid").asText());
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
