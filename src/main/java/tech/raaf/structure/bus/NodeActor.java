package tech.raaf.structure.bus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.MessageConsumer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.raaf.structure.base.Configuration;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.graph.Entity;
import tech.raaf.structure.graph.Graph;
import tech.raaf.structure.graph.Instance;
import tech.raaf.structure.graph.Node;
import tech.raaf.structure.graph.Type;
import tech.raaf.structure.graph.repositories.NodeRepository;

public abstract class NodeActor<T extends Node> extends GenericActor {

  protected static final String startOfAddress = "tech.raaf.structure.bus.node.";
  public static final String SHAREDMAP_ID = "nodeActors";

  // We keep a reference to the consumers in case we
  // need to reconfigure or close them later
  @SuppressWarnings("unused")
  private MessageConsumer<Object> primaryConsumer;

  @SuppressWarnings("unused")
  private MessageConsumer<Object> secondaryConsumer;

  private NodeRepository nodeRepository = Context.getNodeRepository();

  protected String uuid;
  protected String primaryConsumerAddress;

  public NodeActor(T node) {
    this.uuid = node.getUuid();
    primaryConsumerAddress = startOfAddress + uuid;
  }

  private static final Logger log = LoggerFactory.getLogger(NodeActor.class);

  @Override
  public void start() {
    primaryConsumer = vertx.eventBus().consumer(primaryConsumerAddress, this::handleMessage);
    vertx.sharedData().getLocalMap(SHAREDMAP_ID).put(primaryConsumerAddress, deploymentID());
    log.debug("Listen for {} on addr: {}", this.getClass().getSimpleName(), primaryConsumerAddress);
    if (getSecondaryAddress() != null) {
      String secondaryConsumerAddress = startOfAddress + getSecondaryAddress();
      secondaryConsumer = vertx.eventBus().consumer(secondaryConsumerAddress, this::handleMessage);
      log.debug("Listen for {} on addr: {}", this.getClass().getSimpleName(), secondaryConsumerAddress);
    }
  }

  @SuppressWarnings("unchecked")
  protected T getNode() {
    return (T) nodeRepository.findByUuid(uuid);
  }

  // Used to register an optional second handler address
  protected abstract String getSecondaryAddress();

  // Default methods every NodeActor exposes on the eventbus
  public MessageHandler uuid() {
    return new MessageHandler() {
      @Override
      public JsonNode exec(JsonNode request) {
        ObjectNode result = mapper.createObjectNode();
        result.put("data", uuid);
        return response(request, result);
      }

      @Override
      public JsonNode help(JsonNode request) {
        // TODO Auto-generated method stub
        return null;
      }
    };
  }

  /**
   * Serialize node to entity JSON and return it to the caller.
   * @return Serialized entity in the data property of the response.
   */
  public MessageHandler json() {
    return new MessageHandler() {
      @Override
      public JsonNode exec(JsonNode request) {
        return Graph.transaction(() -> {
          return response(request, mapper.createObjectNode().set("data", mapper.valueToTree(getNode())));
        });
      }

      @Override
      public JsonNode help(JsonNode request) {
        return null;
      }
    };
  }

  public MessageHandler undeploy() {
    return new MessageHandler() {
      @Override
      public JsonNode exec(JsonNode request) {
        vertx.undeploy(deploymentID(), result -> {
          if (result.succeeded()) {
            log.info("Undeployed NodeActor \"" + primaryConsumerAddress + "\"");
          } else {
            log.error("Error while undeploying \"" + primaryConsumerAddress + "\": " + result.cause().getMessage());
          }
        });
        return response(request, mapper.createObjectNode().put("data", deploymentID()));
      }

      @Override
      public JsonNode help(JsonNode request) {
        return null;
      }
    };
  }

  /**
   * A helper method to deploy NodeActors. Given a type entity it will deploy
   * a TypeActor, otherwise an InstanceActor.
   * @param entity The entity to deploy an bus for.
   */
  public static void deploy(Entity entity) {
    deploy(UUID.fromString(entity.getUuid()));
  }

  /**
   * A helper method to deploy NodeActors. Given the UUID of a type it will deploy
   * a TypeActor, otherwise an InstanceActor.
   * @param uuid The UUID of the node to deploy an bus for.
   */
  public static void deploy(UUID uuid) {
    final Node node = Context.getNodeRepository().findByUuid(uuid.toString());
    Validate.notNull(node, "Couldn't retrieve node \"" + uuid + "\" from database.");
    deploy(node);
  }

  /**
   * A helper method to deploy NodeActors. Given a type it will deploy
   * a TypeActor, otherwise an InstanceActor.
   * @param node The node to deploy an bus for.
   * @return The deployment ID, available when the deployment is done.
   */
  public static CompletableFuture<String> deploy(Node node) {
    final CompletableFuture<String> deploymentId = new CompletableFuture<>();
    final String address = startOfAddress + node.getUuid();
    // Make sure we deploy only once
    final Object deployed = Context.getVertx().sharedData().getLocalMap(SHAREDMAP_ID).get(address);
    if (deployed != null) {
      deploymentId.complete((String) deployed);
      return deploymentId;
    }
    final Verticle verticle = node instanceof Type ? new TypeActor((Type) node) : new InstanceActor((Instance) node);
    final DeploymentOptions options = new DeploymentOptions()
      .setWorker(true)
      .setWorkerPoolName(Configuration.ACTOR_POOL);
    Context.getVertx().deployVerticle(verticle, options, ar -> {
      if (ar.succeeded()) {
        deploymentId.complete(ar.result());
      } else {
        log.error("Failed to deploy actor for \"{}\"", node.getUuid(), ar.cause());
        deploymentId.completeExceptionally(ar.cause());
      }
    });
    return deploymentId;
  }
}
