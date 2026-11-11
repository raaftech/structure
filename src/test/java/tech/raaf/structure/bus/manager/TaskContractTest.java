package tech.raaf.structure.bus.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.net.URI;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tech.raaf.structure.helper.JsonHelper;

public class TaskContractTest {

  @Test
  public void taskHandlersReturnTheirDeclaredOutputs() {
    Vertx vertx = Vertx.vertx();
    try {
      // A pool of zero keeps the jobs queued throughout the test.
      TaskManager manager = new TaskManager(0);
      vertx.deployVerticle(manager).await();
      String task = UUID.randomUUID().toString();
      JsonObject job = new JsonObject()
        .put("command", "unused")
        .put(
          "metadata",
          new JsonObject()
            .put("originName", "origin")
            .put("originUuid", UUID.randomUUID().toString())
            .put("instrumentName", "instrument")
            .put("instrumentUuid", UUID.randomUUID().toString())
            .put("capability", "test")
        );
      JsonObject args = new JsonObject().put("taskId", task);
      assertEquals(
        task,
        request(
          vertx,
          manager,
          "queueTask",
          args.copy().put("jobs", new JsonArray().add(job).add(job.copy()))
        ).getString("data")
      );
      assertEquals("SCHEDULED", request(vertx, manager, "taskStatus", args).getJsonObject("data").getString("status"));
      assertEquals(
        2,
        request(vertx, manager, "queueStatus", new JsonObject())
          .getJsonObject("data")
          .getJsonObject(task)
          .getJsonArray("jobs")
          .size()
      );
      assertEquals("PAUSED", request(vertx, manager, "toggleTaskPause", args).getString("data"));
      assertEquals("SCHEDULED", request(vertx, manager, "toggleTaskPause", args).getString("data"));
      assertEquals("CANCELLED", request(vertx, manager, "cancelJob", args.copy().put("jobId", 0)).getString("data"));
      assertEquals(new JsonArray().add(task + ".1"), request(vertx, manager, "cancelTask", args).getJsonArray("data"));
    } finally {
      vertx.close().await();
    }
  }

  private static JsonObject request(Vertx vertx, TaskManager manager, String handler, JsonObject args) {
    JsonObject reply = (JsonObject) vertx
      .eventBus()
      .request(manager.getActorName(), new JsonObject().put("msgh", handler).put("args", args))
      .await()
      .body();
    var errors = JsonHelper.validationErrors(
      URI.create("resource:/schema/base/response.schema.json"),
      JsonHelper.mapper.readTree(reply.encode())
    );
    assertTrue(errors.isEmpty(), errors.toString());
    assertFalse(reply.getJsonObject("result").containsKey("error"), reply.encode());
    return reply.getJsonObject("result");
  }
}
