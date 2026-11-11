package tech.raaf.structure.bus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

public class IntrospectionTest {

  private Vertx vertx;
  private String managerAddress;

  public static class Probe extends GenericActorTest.Probe {

    @Override
    @SchemaPath(input = "resource:/schema/repository/findOneByName.schema.json", output = GenericActorTest.OUTPUT)
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
      return "introspection-test";
    }
  }

  public static class CollectionProbe extends GenericActorTest.Probe {

    @Override
    @SchemaPath(output = "resource:/schema/collection/first.schema.json")
    public MessageHandler silent() {
      return super.silent();
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
  public void listsHandlersWithTheirHelpAndSchemaUris() {
    Probe probe = new Probe();
    String id = vertx.deployVerticle(probe).await();
    JsonNode data = introspect();
    JsonNode actor = data.path("actors").path(id);
    assertEquals(probe.getActorName(), actor.path("class").asString());
    assertEquals(probe.getActorName(), actor.path("addresses").get(0).asString());
    JsonNode handlers = data.path("classes").path(probe.getActorName()).path("handlers");
    assertEquals("probe", handlers.path("silent").path("help").path("name").asString());
    assertEquals("help", handlers.path("help").path("help").path("name").asString());
    assertEquals(
      "resource:/schema/base/args.schema.json",
      handlers.path("help").path("inputSchemaUri").asString()
    );
    assertEquals(
      "resource:/schema/output/help.schema.json",
      handlers.path("help").path("outputSchemaUri").asString()
    );
    JsonNode command = handlers.path("silent");
    String schemaUri = command.path("inputSchemaUri").asString();
    assertEquals("resource:/schema/repository/findOneByName.schema.json", schemaUri);
    assertEquals(GenericActorTest.OUTPUT, command.path("outputSchemaUri").asString());
    assertEquals(
      JsonHelper.jsonFromResource(URI.create(schemaUri).getPath().substring(1)),
      data.path("schemas").path(schemaUri)
    );
    assertTrue(request(probe.getActorName(), "silent").path("result").has("error"));
    JsonNode reply = request(probe.getActorName(), "silent", new JsonObject().put("name", "test"));
    assertFalse(reply.path("result").has("error"));
  }

  @Test
  public void publishesReferencedDocumentsOnlyWhileTheirActorsAreActive() {
    JsonNode original = introspect().path("schemas");
    String first = vertx.deployVerticle(new CollectionProbe()).await();
    String second = vertx.deployVerticle(new CollectionProbe()).await();
    JsonNode schemas = introspect().path("schemas");
    // Three collection documents and the inherited probe output schema.
    assertEquals(original.size() + 4, schemas.size());
    for (String name : List.of("first", "second", "shared")) {
      String path = "schema/collection/" + name + ".schema.json";
      assertEquals(JsonHelper.jsonFromResource(path), schemas.path("resource:/" + path));
    }
    assertTrue(schemas.has("resource:/schema/base/request.schema.json"));
    assertTrue(schemas.has("resource:/schema/base/response.schema.json"));
    assertFalse(schemas.has("http://json-schema.org/draft-04/schema"));
    vertx.undeploy(first).await();
    assertEquals(schemas, introspect().path("schemas"));
    vertx.undeploy(second).await();
    assertEquals(original, introspect().path("schemas"));
  }

  @Test
  public void removesOnlyTheStoppedActor() {
    String name = new Probe().getActorName();
    String first = vertx.deployVerticle(new Probe()).await();
    String second = vertx.deployVerticle(new Probe()).await();
    assertTrue(actors().has(first));
    assertTrue(actors().has(second));
    vertx.undeploy(first).await();
    assertFalse(actors().has(first));
    assertTrue(actors().has(second));
    assertTrue(introspect().path("classes").has(name));
    JsonNode ids = request(managerAddress, "list").path("result").path("data");
    assertTrue(ids.isArray());
    assertTrue(ids.toString().contains(second));
    vertx.undeploy(second).await();
    assertFalse(introspect().path("classes").has(name));
  }

  @Test
  public void listsTheHandlersOnceForEachClass() {
    String first = vertx.deployVerticle(new NodeProbe(new Type())).await();
    String second = vertx.deployVerticle(new NodeProbe(new Type())).await();
    JsonNode data = introspect();
    String name = data.path("actors").path(first).path("class").asString();
    assertEquals(name, data.path("actors").path(second).path("class").asString());
    assertTrue(data.path("classes").path(name).path("handlers").has("uuid"));
    // The manager and the probe class
    assertEquals(2, data.path("classes").size());
  }

  @Test
  public void listsNodeAddressesAndRemovesThemOnUndeploy() {
    Type node = new Type();
    String primary = "tech.raaf.structure.bus.node." + node.getUuid();
    String alias = "tech.raaf.structure.bus.node.introspection-test";
    String id = vertx.deployVerticle(new NodeProbe(node), GenericActor.worker()).await();
    JsonNode addresses = actors().path(id).path("addresses");
    assertEquals(JsonHelper.mapper.valueToTree(List.of(primary, alias)), addresses);
    for (String address : List.of(primary, alias)) {
      assertEquals(node.getUuid(), request(address, "uuid").path("result").path("data").asString());
    }
    vertx.undeploy(id).await();
    assertFalse(actors().has(id));
    assertFalse(vertx.sharedData().getLocalMap(NodeActor.SHAREDMAP_ID).containsKey(primary));
    String replacement = vertx.deployVerticle(new NodeProbe(node)).await();
    assertTrue(actors().has(replacement));
  }

  private JsonNode introspect() {
    JsonNode reply = request(managerAddress, "introspect");
    assertTrue(JsonHelper.validationErrors(URI.create("resource:/schema/base/response.schema.json"), reply).isEmpty());
    assertFalse(reply.path("result").has("error"), reply.toString());
    JsonNode data = reply.path("result").path("data");
    List<String> errors = JsonHelper.validationErrors(
      URI.create("resource:/schema/output/introspection.schema.json"),
      data
    );
    assertTrue(errors.isEmpty(), errors.toString());
    JsonNode schemas = data.path("schemas");
    schemas.properties().forEach(entry -> {
      URI uri = URI.create(entry.getKey());
      assertTrue(uri.isAbsolute());
      assertNull(uri.getFragment());
    });
    data.path("classes").values().forEach(actorClass -> {
      actorClass.path("handlers").values().forEach(handler -> {
        assertFalse(handler.has("schemaUri"));
        assertFalse(handler.has("schema"));
        assertTrue(schemas.has(handler.path("inputSchemaUri").asString()));
        assertTrue(schemas.has(handler.path("outputSchemaUri").asString()));
      });
    });
    return data;
  }

  private JsonNode actors() {
    return introspect().path("actors");
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
