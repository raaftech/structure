package tech.raaf.structure.bus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static tech.raaf.structure.helper.JsonHelper.mapper;

import com.google.common.collect.ImmutableMap;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.bus.manager.TaskManager;
import tech.raaf.structure.graph.Component;
import tech.raaf.structure.graph.Graph;
import tech.raaf.structure.graph.Group;
import tech.raaf.structure.graph.repositories.TypeRepository;
import tech.raaf.structure.schema.TypeSchemaTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Runs a controller on an instance through its actor, as the frontend
 * does, and checks the result in the graph. The ping controller is not
 * local: Structure runs the command itself, so no transport is needed.
 */
public class ControllerRunTest extends TypeSchemaTest {

  private static final String PING = "be5d22bd-7588-4d76-bb78-08d7767ae240";
  private static final String NODE = "tech.raaf.structure.bus.node.";

  private final TypeRepository typeRepository = Context.getTypeRepository();

  // Each TaskManager starts a timer on the shared queue, so deploy one
  private static boolean deployed = false;

  @BeforeEach
  public void deploy() throws Exception {
    if (deployed) {
      return;
    }
    Context.getVertx().deployVerticle(new TaskManager(1)).await();
    deployed = true;
  }

  @AfterEach
  public void reset() {
    Context.setSelfGroup(null);
    Context.setSelfIdentifier(null);
  }

  @Test
  public void pingAddsResult() throws Exception {
    // Structure runs on a host that has iputils and the address
    Group structure = new Group(
      json("{\"name\": \"Computer\", \"desc\": \"The Structure host.\"}"),
      typeRepository.findOneByName("Group")
    );
    Component host = new Component(
      json(
        "{\"name\": \"Generic Linux\", \"desc\": \"A generic Linux distro\", " +
          "\"longName\": \"Generic Linux\", \"shortName\": \"genericlinux\", \"ver\": \"unknown\"}"
      ),
      typeRepository.findOneByName("Linux")
    );
    Component iputils = new Component(
      json(
        "{\"name\": \"Iputils\", \"desc\": \"Iputils.\", " +
          "\"longName\": \"Iputils\", \"shortName\": \"iputils\", \"ver\": \"unknown\"}"
      ),
      typeRepository.findOneByName("Iputils")
    );
    Component address = new Component(
      json(
        "{\"name\": \"localhost\", \"desc\": \"The loopback address.\", " +
          "\"addr\": \"127.0.0.1\", \"reachable\": true}"
      ),
      typeRepository.findOneByName("Ipv4Address")
    );
    // The IP stack of the host answers the ping
    Component icmp = new Component(
      json(
        "{\"name\": \"IcmpServer\", \"desc\": \"The IP stack.\", " +
          "\"longName\": \"IcmpServer\", \"shortName\": \"icmp\", \"ver\": \"unknown\"}"
      ),
      typeRepository.findOneByName("IcmpServer")
    );
    host.addComponent(iputils).addComponent(address).addComponent(icmp);
    structure.addMember(host);
    structure.save();
    Context.setSelfGroup(structure);
    Context.setSelfIdentifier(structure.getUuid());

    NodeActor.deploy(address).get();
    ObjectNode args = mapper.createObjectNode().put("instrument", PING);
    args.putObject("arguments").put("count", "1");
    JsonNode reply = send(NODE + address.getUuid(), "runInstrument", args);
    assertFalse(reply.path("result").has("error"), reply.toString());
    String task = reply.path("result").path("data").asString();

    Map<String, Object> result = awaitResult(address.getUuid(), task);
    assertEquals(PING, result.get("controller"));
    assertTrue(((String) result.get("data-process")).startsWith("ping "), result.toString());
    assertTrue(((String) result.get("data-process")).endsWith(" '127.0.0.1'"), result.toString());
    assertEquals(task, result.get("data-task"));
    // The ping can fail on a machine without ping, so check only that it ran to an end
    assertTrue(result.containsKey("data-stateCode") || result.containsKey("data-error"), result.toString());
  }

  private static ObjectNode json(String json) throws Exception {
    return (ObjectNode) mapper.readTree(json);
  }

  private static JsonNode send(String address, String msgh, ObjectNode args) throws Exception {
    ObjectNode message = mapper.createObjectNode().put("msgh", msgh);
    message.set("args", args);
    final Object reply = Context.getVertx()
      .eventBus()
      .request(address, new JsonObject(message.toString()))
      .await()
      .body();
    return mapper.readTree(reply.toString());
  }

  // The TaskManager timer writes the result, so poll the graph
  @SuppressWarnings("unchecked")
  private static Map<String, Object> awaitResult(String origin, String task) throws Exception {
    long deadline = System.currentTimeMillis() + 30000;
    while (System.currentTimeMillis() < deadline) {
      List<Map<String, Object>> rows = Graph.query(
        "MATCH (:Node {uuid: $uuid})-[:HAS_RESULT]->(r:Result {`data-task`: $task}) " +
          "OPTIONAL MATCH (c:Controller)-[:PRODUCED]->(r) " +
          "RETURN r {.*, controller: c.uuid} AS result",
        ImmutableMap.of("uuid", origin, "task", task)
      );
      if (!rows.isEmpty()) {
        return (Map<String, Object>) rows.get(0).get("result");
      }
      Thread.sleep(100);
    }
    fail("No result for task " + task);
    return null;
  }
}
