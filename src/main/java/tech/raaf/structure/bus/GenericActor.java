package tech.raaf.structure.bus;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.ThreadingModel;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.raaf.structure.annotations.SchemaPath;
import tech.raaf.structure.base.Configuration;
import tech.raaf.structure.helper.JsonHelper;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * GenericActor is our default Abstract class for actors on the bus..
 *
 * @author Rubin Simons <rubin@raaf.tech>
 * @since 2014-09-15
 */
public abstract class GenericActor extends AbstractVerticle {

  private Logger log = LoggerFactory.getLogger(GenericActor.class);

  final URI schemaForRequest = URI.create("resource:/schema/base/request.schema.json");
  final URI schemaForResponse = URI.create("resource:/schema/base/response.schema.json");
  // used as a default schema in case we need to fallback on something
  final URI fallbackSchema = URI.create("resource:/schema/base/args.schema.json");

  Map<String, HandlerSchema> handlers = new HashMap<>();
  protected ObjectMapper mapper = new ObjectMapper();

  // statusAddress is used to push notifications of events within Structure
  // that the GUI may be interested about.
  private static final String statusAddress = "tech.raaf.structure.bus.status";
  // The worker pool of the actors that use the graph.
  private static final String POOL = "structure-actors";
  private static final String ACTORS = "structure-command-discovery";

  // Options to deploy an actor that uses the graph.
  public static DeploymentOptions worker() {
    return new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER).setWorkerPoolName(POOL);
  }

  class HandlerSchema {

    URI schemaPath;
    MessageHandler handler;

    public HandlerSchema(URI schemaPath, MessageHandler handler) {
      this.schemaPath = schemaPath;
      this.handler = handler;
    }
  }

  // Constructor. It gets the handlers of the subclass by reflection.
  @SuppressWarnings("this-escape")
  protected GenericActor() {
    // Initialize arrays.
    Method methods[] = this.getClass().getMethods();
    Map<Method, URI> messageHandlersAndSchemas = new HashMap<>();

    // Fill messageHandlers.
    for (Method method : methods) {
      if (method.getReturnType().equals(MessageHandler.class)) {
        if (method.isAnnotationPresent(SchemaPath.class)) {
          SchemaPath schemaPath = method.getAnnotation(SchemaPath.class);
          try {
            messageHandlersAndSchemas.put(method, new URI(schemaPath.value()));
          } catch (URISyntaxException e) {
            if (Configuration.isTraceModeOn()) {
              e.printStackTrace();
            }
            log.error("Error while parsing @SchemaPath value as URI");
            messageHandlersAndSchemas.put(method, fallbackSchema);
          }
        } else {
          messageHandlersAndSchemas.put(method, fallbackSchema);
        }
      }
    }

    messageHandlersAndSchemas
      .entrySet()
      .stream()
      .forEach(entry -> {
        Method mh = entry.getKey();
        URI schema = entry.getValue();
        String s = entry.getKey().getName();

        try {
          HandlerSchema handlerAndSchema = new HandlerSchema(schema, (MessageHandler) mh.invoke(this));
          handlers.put(s, handlerAndSchema);
        } catch (IllegalAccessException | InvocationTargetException e) {
          log.error("Failed to access or invoke method " + s + ": " + e.getMessage());

          if (Configuration.isTraceModeOn()) {
            e.printStackTrace();
          }
        }
      });
  }

  // A json file loader (looks in your classpath/resources.)
  public JsonNode jsonFromResource(String resource) {
    return JsonHelper.jsonFromResource(resource);
  }

  // Convert a Vert.x JsonObject to a Jackson JsonNode
  public JsonNode convertFromJsonObject(JsonObject jsonObject) {
    JsonNode jsonNode = JsonNodeFactory.instance.objectNode();

    try {
      jsonNode = mapper.readTree(jsonObject.toString());
    } catch (JacksonException e) {
      log.error(e.getMessage());

      if (Configuration.isTraceModeOn()) {
        e.printStackTrace();
      }
    }

    return jsonNode;
  }

  // Get the name of the bus (used with registerHandler).
  public String getActorName() {
    return this.getClass().getName().toLowerCase();
  }

  protected List<String> addresses() {
    return List.of(getActorName());
  }

  // Read metadata from each active deployment in this Vert.x instance.
  protected JsonNode actorMetadata() {
    ObjectNode actors = mapper.createObjectNode();
    vertx
      .sharedData()
      .<String, JsonObject>getLocalMap(ACTORS)
      .entrySet()
      .stream()
      .sorted(Map.Entry.comparingByKey())
      .forEach(entry -> actors.set(entry.getKey(), convertFromJsonObject(entry.getValue())));
    return actors;
  }

  private JsonObject metadata() {
    ObjectNode actor = mapper.createObjectNode();
    actor.set("addresses", mapper.valueToTree(addresses()));
    ObjectNode commands = actor.putObject("handlers");
    JsonNode request = mapper.createObjectNode().put("msgh", "help");
    handlers
      .entrySet()
      .stream()
      .sorted(Map.Entry.comparingByKey())
      .forEach(entry -> {
        HandlerSchema handler = entry.getValue();
        ObjectNode command = commands.putObject(entry.getKey());
        command.put("schemaUri", handler.schemaPath.toString());
        command.set("schema", jsonFromResource(handler.schemaPath.getPath().substring(1)));
        command.set("help", handler.handler.help(request));
      });
    return new JsonObject(actor.toString());
  }

  // The response helper.
  public JsonNode response(JsonNode request, JsonNode result) {
    ObjectNode response = JsonNodeFactory.instance.objectNode();

    response.set("request", request);
    response.set("result", result);
    return response;
  }

  // Check if a string is a valid UUID.
  @SuppressWarnings("unused")
  public static boolean isUuid(String str) {
    try {
      UUID uuid = UUID.fromString(str);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  // Log why a response is not valid; that is a fault of Structure
  public JsonNode validate(URI schemaDoc, JsonNode jsonDoc) {
    JsonHelper.validationErrors(schemaDoc, jsonDoc).forEach(error -> log.error("Invalid response: {}", error));
    return jsonDoc;
  }

  // Publish a status on the status channel
  protected void publishStatus(String event, JsonNode data) {
    EventBus eventBus = getVertx().eventBus();
    eventBus.publish(
      statusAddress,
      new JsonObject(
        "{\"source\":\"" +
          this.getClass().getSimpleName() +
          "\", " +
          "\"event\": \"" +
          event +
          "\", \"data\": " +
          new JsonObject(data.toString()) +
          "}"
      )
    );
  }

  // A public interface for MessageHandler objects.
  public interface MessageHandler {
    JsonNode exec(JsonNode request);
    JsonNode help(JsonNode request);
  }

  // The default usage handler.
  public MessageHandler help() {
    return new MessageHandler() {
      @Override
      public JsonNode exec(JsonNode request) {
        ObjectNode result = JsonNodeFactory.instance.objectNode();

        for (String msgh : handlers.keySet()) {
          try {
            JsonNode help = handlers.get(msgh).handler.help(request);
            if (help != null) {
              result.set(msgh, help);
            }
          } catch (NullPointerException e) {
            log.error(e.getMessage());

            if (Configuration.isTraceModeOn()) {
              e.printStackTrace();
            }
          }
        }

        return response(request, result);
      }

      @Override
      public JsonNode help(JsonNode request) {
        return jsonFromResource("help/genericactor.help.json");
      }
    };
  }

  // The default version handler.
  public MessageHandler version() {
    return new MessageHandler() {
      @Override
      public JsonNode exec(JsonNode request) {
        JsonNode result = jsonFromResource("release.json");
        return response(request, result);
      }

      @Override
      public JsonNode help(JsonNode request) {
        return jsonFromResource("help/genericactor.version.json");
      }
    };
  }

  // Logs and registers the actors on the bus.
  @Override
  public void start() {
    JsonObject metadata = metadata();
    for (String address : addresses()) {
      log.debug("Listen for {} on addr: {}", this.getClass().getSimpleName(), address);
      vertx.eventBus().consumer(address, this::handleMessage);
    }
    vertx.sharedData().<String, JsonObject>getLocalMap(ACTORS).put(deploymentID(), metadata);
  }

  @Override
  public void stop() {
    vertx.sharedData().getLocalMap(ACTORS).remove(deploymentID());
  }

  // Our message handler.
  public void handleMessage(Message<Object> message) {
    // Request!
    JsonObject body = (JsonObject) message.body();
    JsonNode request = convertFromJsonObject(body);
    JsonObject response;

    // Without a valid msgh and args we can't dispatch, reply with an error
    final List<String> requestErrors = JsonHelper.validationErrors(schemaForRequest, request);
    if (!requestErrors.isEmpty()) {
      JsonNode error = JsonNodeFactory.instance
        .objectNode()
        .put("error", "Request didn't validate against schema: " + requestErrors);
      message.reply(new JsonObject(validate(schemaForResponse, response(request, error)).toString()));
      return;
    }

    String msgh = request.get("msgh").asString();
    JsonNode args = request.get("args");

    if (handlers.containsKey(msgh)) {
      // If we didn't get any args create an empty JSON object, so we don't get
      // a NPE when we validate args
      if (args == null) {
        args = mapper.createObjectNode();
      }
      HandlerSchema handlerAndSchema = handlers.get(msgh);
      URI schema = handlerAndSchema.schemaPath;
      if (schema == null) {
        log.warn(
          "Couldn't load " +
            handlerAndSchema.schemaPath +
            " using empty schema as default. Validation will always pass."
        );
        schema = fallbackSchema;
      }
      final List<String> argsErrors = JsonHelper.validationErrors(schema, args);
      if (argsErrors.isEmpty()) {
        JsonNode result;
        try {
          result = handlerAndSchema.handler.exec(request);
        } catch (Exception e) {
          // Reply with the error, else the client waits until its timeout
          log.error("Handler {} failed", msgh, e);
          result = response(request, JsonNodeFactory.instance.objectNode().put("error", e.toString()));
        }
        // Response! Convert "Jackson JsonNode" to "Vert.x JsonObject".
        response = new JsonObject(validate(schemaForResponse, result).toString());
      } else {
        JsonNode error = JsonNodeFactory.instance
          .objectNode()
          .put("error", "Arguments didn't validate against schema: " + argsErrors);
        response = new JsonObject(validate(schemaForResponse, response(request, error)).toString());
      }
    } else {
      JsonNode error = JsonNodeFactory.instance.objectNode().put("error", "No handler for called method.");
      response = new JsonObject(validate(schemaForResponse, response(request, error)).toString());
    }
    message.reply(response);
  }
}
