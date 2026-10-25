package tech.raaf.structure.graph;

import static io.vertx.core.json.Json.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.bus.GenericActor;
import tech.raaf.structure.bus.InstanceActor;
import tech.raaf.structure.graph.repositories.InstanceRepository;
import tech.raaf.structure.graph.repositories.TypeRepository;
import tech.raaf.structure.helper.InstrumentRunner;
import tech.raaf.structure.helper.JsonHelper;

/**
 * Give a description of Instance here.
 *
 * @author Rubin Simons <rubin@raaf.tech>
 * @since 2014-11-27
 */
public abstract class Instance extends Node {

  // The type of a new instance. A persisted instance reads it from the graph.
  protected Type instanceOf;

  protected static final InstanceRepository INSTANCE_REPOSITORY = Context.getInstanceRepository();

  protected static final TypeRepository TYPE_REPOSITORY = Context.getTypeRepository();

  JsonNode instanceData;

  /****************/
  /* Constructors */
  /****************/

  // TODO: Move to builder pattern, just too many constructors

  public Instance() {}

  public Instance(String name, String desc, Type instanceOf) {
    super(name, desc);
    this.instanceOf = instanceOf;
  }

  public Instance(ObjectNode json, Type isa) {
    this(json.get("name").asText(), json.get("desc").asText(), isa);
    instanceData = json.deepCopy();
    setProperties(json);
  }

  /******************/
  /* Public Methods */
  /******************/

  public Type getInstanceOf() {
    if (instanceOf == null && persisted) {
      instanceOf = relatedOne("-[:INSTANCE_OF]->(b:Type)");
    }
    return instanceOf;
  }

  /**
   * This method is transform JSON to the data map.
   * @param properties The JSON to use for the properties.
   * @return The Instance setProperties was called on.
   */
  protected Instance setProperties(ObjectNode properties) {
    boolean success = JsonHelper.validateToSchema(instanceOf.getSchema().getAsObjectNode(), properties);
    if (success) {
      // Name and desc are saved already, so we can remove those.
      properties.remove("name");
      properties.remove("desc");
      Iterator<String> iter = properties.fieldNames();
      while (iter.hasNext()) {
        String key = iter.next();
        if (properties.get(key).isObject()) {
          data.put(key, properties.get(key));
        } else {
          data.put(key, properties.get(key).asText());
        }
      }
    } else {
      throw new RuntimeException("Supplied JSON did not match Type schema.");
    }

    return this;
  }

  // Run a controller on this instance with default allocated time
  public Instance runController(Controller controller, ObjectNode arguments, String taskId) {
    new InstrumentRunner(this, controller, arguments, taskId).startInstrument();
    return this;
  }

  // Run a controller on this instance with overridden allocated time
  public Instance runController(Controller controller, ObjectNode arguments, String taskId, long timeAllocated) {
    new InstrumentRunner(this, controller, arguments, taskId, timeAllocated).startInstrument();
    return this;
  }

  /**
   * Return whether this instance is an instance of <code>type</code> or
   * a subtype of <code>type</code>.
   * @param type The name of the type we want to check.
   * @return Are we an instance of <code>type</code> or an instance of a subtype of <code>type</code>? Then true.
   */
  public boolean isOfType(String type) {
    return INSTANCE_REPOSITORY.isOfType(getUuid(), type) > 0;
  }

  /**
   * Return whether this instance is an instance of <code>type</code> or
   * a subtype of <code>type</code>.
   * @param type The type we want to check.
   * @return Are we an instance of <code>type</code> or an instance of a subtype of <code>type</code>? Then true.
   */
  public boolean isOfType(Type type) {
    return type != null && isOfType(type.getName());
  }

  // Get the newest result of this Instance
  public Result getStartOfResultChain() {
    return relatedOne("-[:RESULTS]->(b:Result)");
  }

  /**
   * Make a stored result the newest result of this instance. The
   * previous newest result gets a NEXT_RESULT to the new one.
   * @param result The result, which must be in the graph.
   * @return This instance.
   */
  public Instance addResult(Result result) {
    Graph.query(
      "MATCH (i:Node {uuid: $instance}), (new:Result {uuid: $result}) " +
        "OPTIONAL MATCH (i)-[old:RESULTS]->(head:Result) " +
        "DELETE old " +
        "MERGE (i)-[:RESULTS]->(new) " +
        "WITH head, new WHERE head IS NOT NULL " +
        "MERGE (head)-[:NEXT_RESULT]->(new)",
      ImmutableMap.of("instance", getUuid(), "result", result.getUuid())
    );
    return this;
  }

  // An entity with the properties and relations of this instance
  protected Entity toEntity() {
    final Entity entity = new Entity();
    entity.setUuid(getUuid());
    entity.setName(getName());
    entity.setDesc(getDesc());
    entity.setBundle(getBundle());
    entity.setPath(getPath());
    entity.setChecksum(getChecksum());
    entity.setData(data);
    entity.setFile(file);
    entity.addRelations("INSTANCE_OF", Direction.OUTGOING, ImmutableSet.of(UUID.fromString(getInstanceOf().getUuid())));
    return entity;
  }

  // The new instances that a save of this instance also saves
  protected Set<Instance> getNewPeers() {
    return Collections.emptySet();
  }

  // Write this instance, after its new peers, without validation
  protected void write() {
    for (Instance peer : getNewPeers()) {
      if (!peer.isPersisted()) {
        peer.save();
      }
    }
    toEntity().write(
      Entity.getClassLabels(getClass().getSimpleName()).orElseThrow(() ->
        new IllegalStateException("No labels for " + getClass().getSimpleName())
      )
    );
    persisted = true;
  }

  public Instance save() {
    final boolean newNode = !persisted;
    Graph.transaction(() -> {
      if (newNode) {
        if (!getInstanceOf().getSchema().validateJson(instanceData)) {
          throw new RuntimeException("Can't create instanceNode, missing properties.");
        }
        data.putAll(getInstanceOf().getKv());
      }
      write();
      if (newNode && Context.getVertx() != null) {
        Graph.afterCommit(() -> Context.getVertx().deployVerticle(new InstanceActor(this), GenericActor.worker()));
      }
    });

    return this;
  }

  public ObjectNode toJson() {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode result = mapper.createObjectNode();

    result.put("name", getName());
    result.put("desc", getDesc());
    result.put("uuid", getUuid());
    result.put("_hierarchyPath", getInstanceOf().getHierarchyPath());
    data.forEach((key, val) -> result.put(key, val.toString()));

    return result;
  }

  /**
   * JSON array values in entities are saved in the graph (in the data map) as string representations of those arrays.
   * The type of the values in that array depends on the type of entity. This method can be used to easily
   * coerce those string representations of those JSON arrays into a List of type {@link T}.
   * @param key The key of the value in the {@link Entity#data data} map we want to coerce.
   * @param map A function used to map every element of the array to some instance of {@link T}.
   * @param <T> The type to coerce the elements of the array to.
   * @return A list containing all elements of the JSON array coerced to type {@link T}.
   */
  protected <T> Optional<List<T>> dataArrayStringToList(String key, Function<JsonNode, T> map) {
    final Object value = data.get(key);
    final IllegalArgumentException exception = new IllegalArgumentException(
      "Value belonging to \"" + key + "\" is not a JSON array."
    );
    if (value == null) {
      return Optional.empty();
    } else if (!(value instanceof String)) {
      throw exception;
    }

    try {
      ArrayNode array = (ArrayNode) mapper.readTree((String) value);
      return Optional.of(StreamSupport.stream(array.spliterator(), false).map(map).collect(Collectors.toList()));
    } catch (IOException e) {
      throw exception;
    }
  }

  /**
   * JSON object values in entities are saved in the graph as string representations of those objects.
   * This method deserializes those strings to JsonNode objects.
   * @param key The key belonging to the JsonNode's value.
   * @return Optionally the deserialized JsonNode, or empty if no value belongs to the key.
   */
  protected Optional<JsonNode> dataObjectToJsonNode(String key) {
    final Object value = data.get(key);
    final IllegalArgumentException exception = new IllegalArgumentException(
      "Value belonging to \"" + key + "\" is not a JSON object."
    );
    if (value == null) {
      return Optional.empty();
    } else if (!(value instanceof String)) {
      throw exception;
    }

    try {
      return Optional.of(mapper.readTree((String) value));
    } catch (IOException e) {
      throw exception;
    }
  }
}
