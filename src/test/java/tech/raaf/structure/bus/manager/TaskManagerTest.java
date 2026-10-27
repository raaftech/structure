package tech.raaf.structure.bus.manager;

import static io.vertx.core.json.Json.mapper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.bus.InstanceActor;
import tech.raaf.structure.graph.Controller;
import tech.raaf.structure.graph.Entity;
import tech.raaf.structure.graph.Graph;
import tech.raaf.structure.schema.TypeSchemaTest;

/**
 * Queues tasks on a deployed TaskManager and checks the results in
 * the raw graph. The writes commit.
 */
public class TaskManagerTest extends TypeSchemaTest {

  private static final String TASK_MANAGER = "tech.raaf.structure.bus.manager.taskmanager";
  private static final String OPERATING_SYSTEM = "f05e325e-1aee-4892-96e4-8bcd6e97c528";
  private static final String NO_SUCH_COMMAND = "structure-test-no-such-command";

  // Each TaskManager starts a timer on the shared queue, so deploy one
  private static boolean deployed = false;

  @Before
  public void deploy() throws Exception {
    if (deployed) {
      return;
    }
    CompletableFuture<String> id = new CompletableFuture<>();
    Context.getVertx().deployVerticle(new TaskManager(1), ar -> {
      if (ar.succeeded()) {
        id.complete(ar.result());
      } else {
        id.completeExceptionally(ar.cause());
      }
    });
    id.get();
    deployed = true;
  }

  // The command does not exist, so no process runs and each job errs
  @Test
  public void resultChain() throws Exception {
    String origin = origin();

    // Run the tasks one by one, so that the order is known
    List<String> tasks = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      String task = UUID.randomUUID().toString();
      tasks.add(task);
      queue(task, origin, NO_SUCH_COMMAND, mapper.createObjectNode());
      awaitResult(origin, task);
    }

    assertEquals(0, count("MATCH (n:Node {uuid: $uuid})<-[r:RESULTS]-() RETURN count(r) AS n", origin));
    String head = single("MATCH (n:Node {uuid: $uuid})-[:RESULTS]->(r) RETURN r.uuid AS n", origin);
    assertEquals(0, count("MATCH (n:Node {uuid: $uuid})-[r:NEXT_RESULT]->() RETURN count(r) AS n", head));

    // Walk from the newest result to the oldest one
    LinkedList<String> chain = new LinkedList<>();
    for (String result = head; result != null; result = previous(result)) {
      Map<String, Object> r = Graph.query(
        "MATCH (r:Node {uuid: $uuid}) RETURN 'Result' IN labels(r) AS result, " +
          "properties(r) AS properties, exists { (r)<-[:RESULTS]-() } AS head",
        ImmutableMap.of("uuid", result)
      ).get(0);
      @SuppressWarnings("unchecked")
      Map<String, Object> properties = (Map<String, Object>) r.get("properties");
      assertTrue((Boolean) r.get("result"));
      assertEquals(NO_SUCH_COMMAND, properties.get("data-process"));
      assertTrue(properties.containsKey("data-error"));
      assertEquals(result.equals(head), r.get("head"));
      chain.addFirst((String) properties.get("data-task"));
    }
    assertEquals(tasks, chain);
  }

  // The JVM of the test runs, it exists on each machine that runs tests
  @Test
  public void finishedJob() throws Exception {
    String origin = origin();
    String task = UUID.randomUUID().toString();
    String command = System.getProperty("java.home") + "/bin/java -version";
    queue(
      task,
      origin,
      command,
      (ObjectNode) mapper.readTree(
        "{\"timeAllocated\": 30000, " +
          "\"stateMap\": {\"0\": {\"code\": 0, \"interpretation\": \"OK\", " +
          "\"description\": \"Java runs.\"}}}"
      )
    );
    awaitResult(origin, task);

    String uuid = single("MATCH (n:Node {uuid: $uuid})-[:RESULTS]->(r) RETURN r.uuid AS n", origin);
    assertEquals(0, count("MATCH (n:Node {uuid: $uuid})-[r:NEXT_RESULT]-() RETURN count(r) AS n", uuid));
    @SuppressWarnings("unchecked")
    Map<String, Object> result = (Map<String, Object>) Graph.query(
      "MATCH (r:Node {uuid: $uuid}) RETURN properties(r) AS properties",
      ImmutableMap.of("uuid", uuid)
    )
      .get(0)
      .get("properties");
    assertEquals(command, result.get("data-process"));
    assertEquals(0L, result.get("data-returnCode"));
    assertEquals("", result.get("data-stdout"));
    assertTrue(((String) result.get("data-stderr")).contains("version"));
    assertEquals(0L, result.get("data-stateCode"));
    assertEquals("OK", result.get("data-stateInterpretation"));
    assertEquals("Java runs.", result.get("data-stateDescription"));
    assertFalse(result.containsKey("data-error"));
  }

  // The pool has one process, so the second task finishes after the first
  @Test
  public void unknownOrigin() throws Exception {
    String lost = UUID.randomUUID().toString();
    queue(lost, UUID.randomUUID().toString(), NO_SUCH_COMMAND, mapper.createObjectNode());
    String origin = origin();
    String task = UUID.randomUUID().toString();
    queue(task, origin, NO_SUCH_COMMAND, mapper.createObjectNode());
    awaitResult(origin, task);

    assertEquals(0, count("MATCH (r:Result {`data-task`: $uuid}) RETURN count(r) AS n", lost));
  }

  @Test
  public void getResultsReturnsBothResultsNewestFirst() throws Exception {
    String origin = origin();
    List<String> expected = new ArrayList<>(queueResults(origin).keySet());
    Collections.reverse(expected);

    JsonNode results = getResults(origin, mapper.createObjectNode());
    List<String> actual = new ArrayList<>();
    results.forEach(result -> actual.add(result.path("task").asText()));
    assertEquals(expected, actual);
  }

  @Test
  public void getResultsFiltersByTask() throws Exception {
    String origin = origin();
    for (String task : queueResults(origin).keySet()) {
      JsonNode results = getResults(origin, mapper.createObjectNode().put("task", task));
      assertEquals("Results for task " + task, 1, results.size());
      assertEquals(task, results.get(0).path("task").asText());
    }
  }

  @Test
  public void resultsLinkToTheirControllerAndOrigin() throws Exception {
    String origin = origin();
    for (Map.Entry<String, String> task : queueResults(origin).entrySet()) {
      List<Map<String, Object>> rows = Graph.query(
        "MATCH (r:Result {`data-task`: $task}) " +
          "OPTIONAL MATCH (controller:Controller)-[:PRODUCED]->(r) " +
          "OPTIONAL MATCH (origin:Component)-[:HAS_RESULT]->(r) " +
          "RETURN controller.uuid AS controller, origin.uuid AS origin",
        ImmutableMap.of("task", task.getKey())
      );
      assertEquals("Stored results for task " + task.getKey(), 1, rows.size());
      assertEquals(
        "Result links for task " + task.getKey(),
        ImmutableMap.of("controller", task.getValue(), "origin", origin),
        rows.get(0)
      );
    }
  }

  private static Map<String, String> queueResults(String origin) throws Exception {
    Map<String, String> tasks = new LinkedHashMap<>();
    for (String name : Arrays.asList("BashInvoke", "IpNetworkDiscover")) {
      Controller controller = (Controller) Context.getInstrumentRepository().findOneByName(name);
      assertNotNull("Missing controller " + name, controller);
      String task = UUID.randomUUID().toString();
      tasks.put(task, controller.getUuid());
      ObjectNode metadata = mapper
        .createObjectNode()
        .put("instrument", controller.getUuid())
        .put("capability", controller.getCapability().getName());
      queue(task, origin, NO_SUCH_COMMAND, metadata);
      awaitResult(origin, task);
    }
    return tasks;
  }

  private static JsonNode getResults(String origin, ObjectNode arguments) {
    ObjectNode request = mapper.createObjectNode().put("msgh", "getResults");
    request.set("args", arguments);
    JsonNode reply = new InstanceActor(Context.getInstanceRepository().findByUuid(origin)).getResults().exec(request);
    assertFalse(reply.toString(), reply.path("result").has("error"));
    JsonNode results = reply.path("result").path("data");
    assertTrue(reply.toString(), results.isArray());
    return results;
  }

  private String origin() throws Exception {
    String uuid = UUID.randomUUID().toString();
    mapper
      .readValue(
        String.format(
          "{\"name\": \"task-origin\", \"desc\": \"Origin.\", " +
            "\"meta\": {\"uuid\": \"%s\", \"relations\": {\"INSTANCE_OF\": " +
            "{\"direction\": \"outgoing\", \"to\": [\"%s\"]}}}, \"data\": {\"ver\": \"1\", " +
            "\"shortName\": \"os\", \"longName\": \"Operating system\"}}",
          uuid,
          OPERATING_SYSTEM
        ),
        Entity.class
      )
      .toGraph();
    return uuid;
  }

  private static void queue(String task, String origin, String command, ObjectNode metadata) throws Exception {
    ObjectNode args = mapper.createObjectNode().put("taskId", task).put("sequential", true);
    args
      .putArray("jobs")
      .addObject()
      .put("command", command)
      .putObject("metadata")
      .put("originName", "task-origin")
      .put("originUuid", origin)
      .put("instrument", "DummyController")
      .put("capability", "DummyCapability")
      .setAll(metadata);
    ObjectNode message = mapper.createObjectNode().put("msgh", "queueTask");
    message.set("args", args);
    JsonObject request = new JsonObject(message.toString());
    CompletableFuture<Object> reply = new CompletableFuture<>();
    Context.getVertx()
      .eventBus()
      .send(TASK_MANAGER, request, ar -> {
        if (ar.succeeded()) {
          reply.complete(ar.result().body());
        } else {
          reply.completeExceptionally(ar.cause());
        }
      });
    assertEquals(task, mapper.readTree(reply.get().toString()).path("result").path("data").asText());
  }

  // The TaskManager timer writes the result, so poll the graph
  private static void awaitResult(String origin, String task) throws Exception {
    long deadline = System.currentTimeMillis() + 10000;
    while (System.currentTimeMillis() < deadline) {
      if (
        count("MATCH (n:Node {uuid: $uuid})-->(r:Result {`data-task`: $task}) RETURN count(r) AS n", origin, task) > 0
      ) {
        return;
      }
      Thread.sleep(50);
    }
    fail("No result for task " + task);
  }

  private static long count(String cypher, String uuid) {
    return count(cypher, uuid, null);
  }

  private static long count(String cypher, String uuid, String task) {
    Map<String, Object> params =
      task == null ? ImmutableMap.of("uuid", uuid) : ImmutableMap.of("uuid", uuid, "task", task);
    return ((Number) Graph.query(cypher, params).get(0).get("n")).longValue();
  }

  // The value of the one row that the query returns
  private static String single(String cypher, String uuid) {
    List<Map<String, Object>> rows = Graph.query(cypher, ImmutableMap.of("uuid", uuid));
    assertEquals(cypher + " for " + uuid, 1, rows.size());
    return (String) rows.get(0).get("n");
  }

  // The result before this one in the chain, or null
  private static String previous(String result) {
    List<Map<String, Object>> rows = Graph.query(
      "MATCH (r:Node {uuid: $uuid})<-[:NEXT_RESULT]-(p) " + "RETURN p.uuid AS n",
      ImmutableMap.of("uuid", result)
    );
    assertTrue(rows.size() <= 1);
    return rows.isEmpty() ? null : (String) rows.get(0).get("n");
  }
}
