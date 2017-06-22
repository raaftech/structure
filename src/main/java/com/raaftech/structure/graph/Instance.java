package com.raaftech.structure.graph;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.bus.InstanceActor;
import com.raaftech.structure.graph.repositories.InstanceRepository;
import com.raaftech.structure.graph.repositories.ResultRepository;
import com.raaftech.structure.graph.repositories.TypeRepository;
import com.raaftech.structure.helper.InstrumentRunner;
import com.raaftech.structure.helper.JsonHelper;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.fieldaccess.DynamicPropertiesContainer;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static io.vertx.core.json.Json.mapper;

/**
 * Give a description of Instance here.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-11-27
 */
@NodeEntity
public abstract class Instance extends Node {

    private Logger log = LoggerFactory.getLogger(Instance.class);

    @RelatedTo(type="INSTANCE_OF", direction=Direction.OUTGOING)
    @Fetch
    protected Type instanceOf;

    @Transient
    protected static final InstanceRepository INSTANCE_REPOSITORY = Context.getInstanceRepository();

    @Transient
    protected static final TypeRepository TYPE_REPOSITORY = Context.getTypeRepository();

    @Transient
    private static final ResultRepository resultRepository = Context.getResultRepository();

    @RelatedTo(type="RESULTS", direction = Direction.OUTGOING)
    @Fetch
    private Result startOfResultChain;


    @Transient
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

    public Instance(String name, String desc, Type instanceOf, String uuid) {
        super(name, desc, uuid);
        this.instanceOf = instanceOf;
    }

    public Instance(String name, String desc, String instanceOfUuid) {
        super(name, desc);

        Type instanceOf = TYPE_REPOSITORY.findByUuid(instanceOfUuid);
        if (instanceOf != null) {
            this.instanceOf = TYPE_REPOSITORY.findByUuid(instanceOfUuid);
        } else {
            throw new RuntimeException("ERROR: can't create Instance, UUID " + instanceOfUuid + " unknown.");
        }
    }

    public Instance(String name, String desc, String instanceOfUuid, String uuid) {
        super(name, desc, uuid);

        Type instanceOf = TYPE_REPOSITORY.findByUuid(instanceOfUuid);
        if (instanceOf != null) {
            this.instanceOf = TYPE_REPOSITORY.findByUuid(instanceOfUuid);
        } else {
            throw new RuntimeException("ERROR: can't create Instance, UUID " + instanceOfUuid + " unknown.");
        }
    }

    public Instance(ObjectNode json, Type isa) {
        this(json.get("name").asText(), json.get("desc").asText(), isa);
        instanceData = json.deepCopy();
        setProperties(json);
    }

    public Instance(ObjectNode json, String isa) {
        this(json.get("name").asText(), json.get("desc").asText(), isa);
        instanceData = json.deepCopy();
        setProperties(json);
    }

    public Instance(ObjectNode json, Type isa,
                    String uuid) {
        this(json.get("name").asText(), json.get("desc").asText(), isa, uuid);
        setProperties(json);
    }

    /******************/
    /* Public Methods */
    /******************/

    public Type getInstanceOf() {
        return instanceOf;
    }

    public void setInstanceOf(Type type) {
        this.instanceOf = type;
    }

    public String getProperty(String key) {
        Object property = data.getProperty(key);
        if (property == null) {
            return null;
        } else {
            return property.toString();
        }
    }

    /**
     * This method is transform JSON to DynamicProperties.
     * @param properties The JSON to use for the properties.
     * @return The Instance setProperties was called on.
     */
    protected Instance setProperties(ObjectNode properties) {
        if (data == null) {
            data = new DynamicPropertiesContainer();
        }
        boolean success = JsonHelper.validateToSchema(instanceOf.getSchema().getAsObjectNode(), properties);
        if (success) {
            // Name and desc are saved already, so we can remove those.
            properties.remove("name");
            properties.remove("desc");
            Iterator<String> iter = properties.fieldNames();
            while (iter.hasNext()) {
                String key = iter.next();
                if (properties.get(key).isObject()) {
                    data.setProperty(key, properties.get(key));
                } else {
                    data.setProperty(key, properties.get(key).asText());
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

    // Get all the startOfResultChain belonging to this Instance
    public Result getStartOfResultChain() {
        return startOfResultChain;
    }

    public Instance addResult(Result result) {
        if (startOfResultChain != null) {
            startOfResultChain.setNext(result);
        }
        startOfResultChain = result;

        return this;
    }

    public Instance save() {
        boolean newNode = getId() == null || getId() < 0;
        Transaction tx = Context.getGraphDatabase().beginTx();
        if (newNode) {
            if (getInstanceOf().getSchema().validateJson(instanceData)) {
                getInstanceOf().getKv().asMap().entrySet().stream()
                        .forEach(entry -> data.setProperty(entry.getKey(), entry.getValue()));
                INSTANCE_REPOSITORY.save(this);
                tx.success();
            } else {
                tx.failure();
                throw new RuntimeException("Can't create instanceNode, missing properties.");
            }
        } else {
            INSTANCE_REPOSITORY.save(this);
            tx.success();
        }
        tx.close();

        if (newNode && Context.getVertx() != null) {
            Context.getVertx().deployVerticle(
                    new InstanceActor(this));
        }

        return this;
    }

    public ObjectNode toJson() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode();

        result.put("name", getName());
        result.put("desc", getDesc());
        result.put("uuid", getUuid());
        result.put("_hierarchyPath", getInstanceOf().getHierarchyPath());
        data.asMap().forEach((key, val) -> result.put(key, val.toString()));

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
        final Object value = data.getProperty(key);
        final IllegalArgumentException exception = new IllegalArgumentException(
                "Value belonging to \"" + key + "\" is not a JSON array.");
        if (value == null) {
            return Optional.empty();
        } else if (!(value instanceof String)) {
            throw exception;
        }

        try {
            ArrayNode array = (ArrayNode) mapper.readTree((String)value);
            return Optional.of(StreamSupport.stream(array.spliterator(), false)
                    .map(map)
                    .collect(Collectors.toList()));
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
        final Object value = data.getProperty(key);
        final IllegalArgumentException exception = new IllegalArgumentException(
                "Value belonging to \"" + key + "\" is not a JSON object.");
        if (value == null) {
            return Optional.empty();
        }
        else if (!(value instanceof String)) {
            throw exception;
        }

        try {
            return Optional.of(mapper.readTree((String) value));
        } catch (IOException e) {
            throw exception;
        }
    }

}
