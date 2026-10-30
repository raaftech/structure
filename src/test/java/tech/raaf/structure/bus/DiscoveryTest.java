package tech.raaf.structure.bus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.raaf.structure.annotations.SchemaPath;
import tech.raaf.structure.base.Application;
import tech.raaf.structure.bus.manager.StructureManager;
import tech.raaf.structure.graph.Type;
import tech.raaf.structure.helper.JsonHelper;
import tools.jackson.databind.JsonNode;

public class DiscoveryTest {

  private Vertx vertx;
  private String managerAddress;

  public static class Probe extends GenericActorTest.Probe {

    @Override
    @SchemaPath("resource:/schema/repository/findOneByName.schema.json")
    public MessageHandler silent() {
      return super.silent();
    }
  }

  public static class NodeProbe extends NodeActor<Type> {

    public NodeProbe(Type node) {
      super(node);
    }

    @Override
    protected String getSecondaryAddress() {
      return "discovery-test";
    }
  }

  @BeforeEach
  public void start() {
    vertx = Application.createVertx();
    StructureManager manager = new StructureManager();
    managerAddress = manager.getActorName();
    vertx.deployVerticle(manager, GenericActor.worker()).await();
  }

  @AfterEach
  public void stop() {
    vertx.close().await();
  }

  @Test
  public void discoversHandlersWithoutHelpAndTheirArgumentSchemas() {
    Probe probe = new Probe();
    String id = vertx.deployVerticle(probe).await();
    JsonNode actor = discover().path(id);
    assertEquals(probe.getActorName(), actor.path("addresses").get(0).asString());
    JsonNode handlers = actor.path("handlers");
    assertTrue(handlers.path("silent").path("help").isNull());
    assertEquals("help", handlers.path("help").path("help").path("name").asString());
    assertEquals("object", handlers.path("help").path("schema").path("type").asString());
    JsonNode command = handlers.path("silent");
    String schemaUri = command.path("schemaUri").asString();
    assertEquals("resource:/schema/repository/findOneByName.schema.json", schemaUri);
    assertEquals(JsonHelper.jsonFromResource(URI.create(schemaUri).getPath().substring(1)), command.path("schema"));
    assertTrue(request(probe.getActorName(), "silent").path("result").has("error"));
    JsonNode reply = request(probe.getActorName(), "silent", new JsonObject().put("name", "test"));
    assertFalse(reply.path("result").has("error"));
  }

  @Test
  public void removesOnlyTheStoppedDeployment() {
    String first = vertx.deployVerticle(new Probe()).await();
    String second = vertx.deployVerticle(new Probe()).await();
    assertTrue(discover().has(first));
    assertTrue(discover().has(second));
    vertx.undeploy(first).await();
    assertFalse(discover().has(first));
    assertTrue(discover().has(second));
    JsonNode ids = request(managerAddress, "list").path("result").path("data");
    assertTrue(ids.isArray());
    assertTrue(ids.toString().contains(second));
  }

  @Test
  public void discoversNodeAddressesAndRemovesThemOnUndeploy() {
    Type node = new Type();
    String primary = "tech.raaf.structure.bus.node." + node.getUuid();
    String alias = "tech.raaf.structure.bus.node.discovery-test";
    String id = vertx.deployVerticle(new NodeProbe(node), GenericActor.worker()).await();
    JsonNode addresses = discover().path(id).path("addresses");
    assertEquals(JsonHelper.mapper.valueToTree(List.of(primary, alias)), addresses);
    for (String address : List.of(primary, alias)) {
      assertEquals(node.getUuid(), request(address, "uuid").path("result").path("data").asString());
    }
    vertx.undeploy(id).await();
    assertFalse(discover().has(id));
    assertFalse(vertx.sharedData().getLocalMap(NodeActor.SHAREDMAP_ID).containsKey(primary));
    String replacement = vertx.deployVerticle(new NodeProbe(node)).await();
    assertTrue(discover().has(replacement));
  }

  private JsonNode discover() {
    JsonNode reply = request(managerAddress, "discover");
    assertTrue(JsonHelper.validationErrors(URI.create("resource:/schema/base/response.schema.json"), reply).isEmpty());
    assertFalse(reply.path("result").has("error"), reply.toString());
    return reply.path("result").path("data");
  }

  private JsonNode request(String address, String handler) {
    return request(address, handler, new JsonObject());
  }

  private JsonNode request(String address, String handler, JsonObject args) {
    Object body = vertx
      .eventBus()
      .request(
        address,
        new JsonObject().put("msgh", handler).put("args", args),
        new DeliveryOptions().setSendTimeout(5000)
      )
      .await()
      .body();
    return JsonHelper.mapper.readTree(body.toString());
  }
}
