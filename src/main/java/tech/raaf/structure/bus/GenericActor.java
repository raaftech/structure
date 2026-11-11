package tech.raaf.structure.bus;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.ThreadingModel;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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

  // The bus address where actors publish events, for clients to follow.
  private static final String EVENTS = "tech.raaf.structure.bus.events";
  // The worker pool of the actors that use the graph.
  private static final String POOL = "structure-actors";
  // Shared maps for introspection: handlers by class, and active actors.
  private static final String CLASSES = "structure-introspection-classes";
  private static final String ACTORS = "structure-introspection-actors";

  // Options to deploy an actor that uses the graph.
  public static DeploymentOptions worker() {
    return new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER).setWorkerPoolName(POOL);
  }

  // The actor classes with verified schemas and help.
  private static final Set<Class<?>> VERIFIED = ConcurrentHashMap.newKeySet();

  // A message handler with the schemas of its args and its result data.
  record HandlerSchema(URI input, URI output, MessageHandler handler) {}

  // Constructor. It gets the handlers of the subclass by reflection.
  // A handler declaration that is not complete stops the actor.
  @SuppressWarnings("this-escape")
  protected GenericActor() {
    // The schemas and help are the same for each actor of a class.
    final boolean verify = !VERIFIED.contains(getClass());
    final JsonNode helpRequest = mapper.createObjectNode().put("msgh", "help");
    for (Method method : getClass().getMethods()) {
      if (!method.getReturnType().equals(MessageHandler.class)) {
        continue;
      }
      final String name = getClass().getSimpleName() + "." + method.getName();
      final SchemaPath path = method.getAnnotation(SchemaPath.class);
      if (path == null || path.output().isEmpty()) {
        throw new IllegalStateException("Handler " + name + " declares no output schema");
      }
      final MessageHandler handler;
      try {
        handler = (MessageHandler) method.invoke(this);
      } catch (ReflectiveOperationException e) {
        throw new IllegalStateException("Cannot get handler " + name, e);
      }
      if (handler == null) {
        throw new IllegalStateException("Handler " + name + " is null");
      }
      // URI.create throws if the path is not a valid URI.
      final URI input = path.input().isEmpty() ? fallbackSchema : URI.create(path.input());
      final URI output = URI.create(path.output());
      if (verify && handler.help(helpRequest) == null) {
        throw new IllegalStateException("Handler " + name + " has no help");
      }
      handlers.put(method.getName(), new HandlerSchema(input, output, handler));
    }
    if (verify) {
      // This throws if a document is missing or a reference is not valid.
      JsonHelper.schemaCollection(schemaUris());
    }
    VERIFIED.add(getClass());
  }

  // The URIs of the schemas of the messages and of each handler.
  private Set<URI> schemaUris() {
    final Set<URI> uris = new TreeSet<>(Set.of(schemaForRequest, schemaForResponse));
    handlers.values().forEach(handler -> uris.addAll(List.of(handler.input(), handler.output())));
    return uris;
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

  // Read the active actors, and the handlers of their classes.
  protected JsonNode actorMetadata() {
    ObjectNode metadata = mapper.createObjectNode();
    ObjectNode classes = metadata.putObject("classes");
    ObjectNode actors = metadata.putObject("actors");
    ObjectNode schemas = metadata.putObject("schemas");
    Set<URI> uris = new TreeSet<>(Set.of(schemaForRequest, schemaForResponse));
    Map<String, JsonObject> handlers = vertx.sharedData().getLocalMap(CLASSES);
    Map<String, JsonObject> active = new TreeMap<>(vertx.sharedData().getLocalMap(ACTORS));
    Set<String> names = new TreeSet<>();
    active.forEach((id, actor) -> {
      actors.set(id, convertFromJsonObject(actor));
      names.add(actor.getString("class"));
    });
    names.forEach(name -> {
      JsonNode actorClass = convertFromJsonObject(handlers.get(name));
      classes.set(name, actorClass);
      actorClass.path("handlers").values().forEach(handler -> {
        uris.add(URI.create(handler.path("inputSchemaUri").asString()));
        uris.add(URI.create(handler.path("outputSchemaUri").asString()));
      });
    });
    JsonHelper.schemaCollection(uris).forEach((uri, schema) -> schemas.set(uri.toString(), schema));
    return metadata;
  }

  // The handlers of the class of this actor, with their schemas and help.
  private JsonObject handlerMetadata() {
    ObjectNode actor = mapper.createObjectNode();
    ObjectNode commands = actor.putObject("handlers");
    JsonNode request = mapper.createObjectNode().put("msgh", "help");
    handlers
      .entrySet()
      .stream()
      .sorted(Map.Entry.comparingByKey())
      .forEach(entry -> {
        HandlerSchema handler = entry.getValue();
        ObjectNode command = commands.putObject(entry.getKey());
        command.put("inputSchemaUri", handler.input().toString());
        command.put("outputSchemaUri", handler.output().toString());
        command.set("help", handler.handler().help(request));
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

  // Replace a response that is not valid with an error response
  private JsonNode checked(JsonNode request, JsonNode response) {
    List<String> errors =
      response == null
        ? List.of("the handler gave no response")
        : JsonHelper.validationErrors(schemaForResponse, response);
    if (errors.isEmpty() && response.path("result").has("data")) {
      HandlerSchema handler = handlers.get(request.path("msgh").asString());
      errors = JsonHelper.validationErrors(handler.output(), response.path("result").get("data"));
    }
    if (errors.isEmpty()) {
      return response;
    }
    log.error("Invalid response to {}: {}", request.path("msgh").asString(), errors);
    return response(request, JsonNodeFactory.instance.objectNode().put("error", "Invalid response: " + errors));
  }

  // Publish an event on the events address
  protected void publishEvent(String event, JsonNode data) {
    EventBus eventBus = getVertx().eventBus();
    eventBus.publish(
      EVENTS,
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

  // The default usage handler, for each handler or for the named one.
  @SchemaPath(
    input = "resource:/schema/base/help.schema.json",
    output = "resource:/schema/output/help.schema.json"
  )
  public MessageHandler help() {
    return new MessageHandler() {
      @Override
      public JsonNode exec(JsonNode request) {
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        JsonNode wanted = request.path("args").path("handler");
        if (!wanted.isMissingNode() && !handlers.containsKey(wanted.asString())) {
          return response(request, result.put("error", "No handler named " + wanted.asString() + "."));
        }
        ObjectNode data = result.putObject("data");

        handlers.forEach((msgh, handler) -> {
          if (wanted.isMissingNode() || msgh.equals(wanted.asString())) {
            data.set(msgh, handler.handler().help(request));
          }
        });

        return response(request, result);
      }

      @Override
      public JsonNode help(JsonNode request) {
        return jsonFromResource("help/genericactor.help.json");
      }
    };
  }

  // The default version handler.
  @SchemaPath(output = "resource:/schema/output/version.schema.json")
  public MessageHandler version() {
    return new MessageHandler() {
      @Override
      public JsonNode exec(JsonNode request) {
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.set("data", jsonFromResource("release.json"));
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
    for (String address : addresses()) {
      log.debug("Listen for {} on addr: {}", this.getClass().getSimpleName(), address);
      vertx.eventBus().consumer(address, this::handleMessage);
    }
    // The handlers are the same for each actor of a class, thus share them
    vertx
      .sharedData()
      .<String, JsonObject>getLocalMap(CLASSES)
      .computeIfAbsent(getActorName(), name -> handlerMetadata());
    JsonObject actor = new JsonObject().put("class", getActorName()).put("addresses", new JsonArray(addresses()));
    vertx.sharedData().<String, JsonObject>getLocalMap(ACTORS).put(deploymentID(), actor);
    publishEvent("actorStarted", actorEvent());
  }

  @Override
  public void stop() {
    vertx.sharedData().getLocalMap(ACTORS).remove(deploymentID());
    publishEvent("actorStopped", actorEvent());
  }

  // The data of an actorStarted or actorStopped event: the actor ID, the
  // actor class and the bus addresses. Clients use it to introspect again.
  private JsonNode actorEvent() {
    ObjectNode data = mapper.createObjectNode().put("id", deploymentID()).put("class", getActorName());
    addresses().forEach(data.putArray("addresses")::add);
    return data;
  }

  // Our message handler.
  public void handleMessage(Message<Object> message) {
    // A body that is not a JSON object gives a request without msgh
    JsonNode request =
      message.body() instanceof JsonObject body ? convertFromJsonObject(body) : mapper.createObjectNode();
    message.reply(new JsonObject(checked(request, dispatch(request)).toString()));
  }

  // Give the request to its message handler, and return the response
  private JsonNode dispatch(JsonNode request) {
    // Without a valid msgh and args we can't dispatch, reply with an error
    final List<String> requestErrors = JsonHelper.validationErrors(schemaForRequest, request);
    if (!requestErrors.isEmpty()) {
      JsonNode error = JsonNodeFactory.instance
        .objectNode()
        .put("error", "Request didn't validate against schema: " + requestErrors);
      return response(request, error);
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
      final List<String> argsErrors = JsonHelper.validationErrors(handlerAndSchema.input(), args);
      if (argsErrors.isEmpty()) {
        try {
          return handlerAndSchema.handler().exec(request);
        } catch (Exception e) {
          // Reply with the error, else the client waits until its timeout
          log.error("Handler {} failed", msgh, e);
          return response(request, JsonNodeFactory.instance.objectNode().put("error", e.toString()));
        }
      }
      JsonNode error = JsonNodeFactory.instance
        .objectNode()
        .put("error", "Arguments didn't validate against schema: " + argsErrors);
      return response(request, error);
    }
    JsonNode error = JsonNodeFactory.instance.objectNode().put("error", "No handler for called method.");
    return response(request, error);
  }
}
