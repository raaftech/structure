package tech.raaf.structure.bus.manager;

import io.vertx.core.json.JsonObject;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.graph.Entity;
import tech.raaf.structure.schema.TypeSchemaTest;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static io.vertx.core.json.Json.mapper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Queues tasks on a deployed TaskManager and checks the result chain
 * in the raw graph. The command does not exist, so no process runs
 * and each job ends with an error result. The writes commit.
 */
public class TaskManagerTest extends TypeSchemaTest {

    private static final String TASK_MANAGER = "tech.raaf.structure.bus.manager.taskmanager";
    private static final String OPERATING_SYSTEM = "f05e325e-1aee-4892-96e4-8bcd6e97c528";
    private static final String COMMAND = "structure-test-no-such-command";
    private static final RelationshipType RESULTS = DynamicRelationshipType.withName("RESULTS");
    private static final RelationshipType NEXT_RESULT = DynamicRelationshipType.withName("NEXT_RESULT");

    @Autowired
    GraphDatabaseService graphDatabaseService;

    @Test
    public void resultChain() throws Exception {
        CompletableFuture<String> deployed = new CompletableFuture<>();
        Context.getVertx().deployVerticle(new TaskManager(1), ar -> {
            if (ar.succeeded()) {
                deployed.complete(ar.result());
            } else {
                deployed.completeExceptionally(ar.cause());
            }
        });
        deployed.get();

        String origin = UUID.randomUUID().toString();
        try (Transaction tx = graphDatabaseService.beginTx()) {
            mapper.readValue(String.format("{\"name\": \"task-origin\", \"desc\": \"Origin.\", "
                    + "\"meta\": {\"uuid\": \"%s\", \"relations\": {\"INSTANCE_OF\": "
                    + "{\"direction\": \"outgoing\", \"to\": [\"%s\"]}}}, \"data\": {\"ver\": \"1\", "
                    + "\"shortName\": \"os\", \"longName\": \"Operating system\"}}", origin, OPERATING_SYSTEM),
                    Entity.class).toGraph();
            tx.success();
        }

        // Run the tasks one by one, so that the order is known
        List<String> tasks = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String task = UUID.randomUUID().toString();
            tasks.add(task);
            queue(task, origin);
            awaitHead(origin, task);
        }

        try (Transaction tx = graphDatabaseService.beginTx()) {
            Node node = find(origin);
            assertFalse(node.hasRelationship(Direction.INCOMING, RESULTS));
            Node head = single(node, Direction.OUTGOING, RESULTS).getEndNode();
            assertFalse(head.hasRelationship(Direction.OUTGOING, NEXT_RESULT));

            // Walk from the newest result to the oldest one
            LinkedList<String> chain = new LinkedList<>();
            for (Node result = head; result != null; result = previous(result)) {
                assertTrue(result.hasLabel(DynamicLabel.label("Result")));
                assertEquals(COMMAND, result.getProperty("data-process"));
                assertTrue(result.hasProperty("data-error"));
                assertEquals(result.equals(head), result.hasRelationship(RESULTS));
                chain.addFirst((String) result.getProperty("data-task"));
            }
            assertEquals(tasks, chain);
            tx.success();
        }
    }

    private static void queue(String task, String origin) throws Exception {
        JsonObject request = new JsonObject(String.format("{\"msgh\": \"queueTask\", \"args\": "
                + "{\"taskId\": \"%s\", \"sequential\": true, \"jobs\": [{\"command\": \"%s\", "
                + "\"metadata\": {\"originName\": \"task-origin\", \"originUuid\": \"%s\", "
                + "\"instrument\": \"DummyController\", \"capability\": \"DummyCapability\"}}]}}",
                task, COMMAND, origin));
        CompletableFuture<Object> reply = new CompletableFuture<>();
        Context.getVertx().eventBus().send(TASK_MANAGER, request, ar -> {
            if (ar.succeeded()) {
                reply.complete(ar.result().body());
            } else {
                reply.completeExceptionally(ar.cause());
            }
        });
        assertEquals(task, mapper.readTree(reply.get().toString()).path("result").path("data").asText());
    }

    // The TaskManager timer writes the result, so poll the graph
    private void awaitHead(String origin, String task) throws Exception {
        long deadline = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < deadline) {
            try (Transaction tx = graphDatabaseService.beginTx()) {
                for (Relationship r : find(origin).getRelationships(Direction.OUTGOING, RESULTS)) {
                    if (task.equals(r.getEndNode().getProperty("data-task", null))) {
                        return;
                    }
                }
                tx.success();
            }
            Thread.sleep(50);
        }
        fail("No result for task " + task);
    }

    private Node find(String uuid) {
        return graphDatabaseService.findNodesByLabelAndProperty(DynamicLabel.label("Node"), "uuid", uuid)
                .iterator().next();
    }

    private static Node previous(Node result) {
        if (!result.hasRelationship(Direction.INCOMING, NEXT_RESULT)) {
            return null;
        }
        return single(result, Direction.INCOMING, NEXT_RESULT).getStartNode();
    }

    private static Relationship single(Node node, Direction direction, RelationshipType type) {
        List<Relationship> found = new ArrayList<>();
        node.getRelationships(direction, type).forEach(found::add);
        assertEquals(node.getProperty("uuid") + " " + direction + " " + type.name(), 1, found.size());
        return found.get(0);
    }
}
