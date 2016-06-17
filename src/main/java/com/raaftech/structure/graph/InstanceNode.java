package com.raaftech.structure.graph;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.repositories.InstanceNodeRepository;
import com.raaftech.structure.graph.repositories.TypeNodeRepository;
import com.raaftech.structure.helper.InstrumentRunner;
import com.raaftech.structure.helper.JsonSchemaBuilder;
import com.raaftech.structure.node.InstanceNodeActor;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.fieldaccess.DynamicProperties;
import org.springframework.data.neo4j.fieldaccess.DynamicPropertiesContainer;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Give a description of InstanceNode here.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-11-27
 */
@NodeEntity
public class InstanceNode extends Node {

    private Logger log = LoggerFactory.getLogger(InstanceNode.class);

    @RelatedTo(type="HAS_RESULT", direction = Direction.OUTGOING)
    private Set<InstanceNode> results = new HashSet<>();

    @RelatedTo(type="HAS_COMPONENT", direction = Direction.OUTGOING)
    private Set<InstanceNode> components;

    @RelatedTo(type="HAS_COMPONENT", direction = Direction.INCOMING)
    @Fetch
    private InstanceNode inComponent;

    @RelatedTo(type="HAS_GROUPING", direction = Direction.OUTGOING)
    private Set<InstanceNode> groupings = new HashSet<>();

    @RelatedTo(type="HAS_OPERATION", direction = Direction.OUTGOING)
    private Set<InstanceNode> operations = new HashSet<>();

    @RelatedTo(type="HAS_PROTOCOL", direction = Direction.OUTGOING)
    private Set<InstanceNode> protocols = new HashSet<>();

    @RelatedTo(type="HAS_ACTOR", direction = Direction.OUTGOING)
    private Set<InstanceNode> actors = new HashSet<>();

    @RelatedTo(type="INSTANCE_OF", direction=Direction.OUTGOING)
    @Fetch
    private TypeNode instanceOf;

    @RelatedTo(type="GIVES_CAPABILITY", direction=Direction.OUTGOING)
    private TypeNode capability;

    @RelatedTo(type="HANDLES", direction=Direction.OUTGOING)
    @Fetch
    private Set<TypeNode> handlesTypes;

    private DynamicProperties kv = new DynamicPropertiesContainer();

    @Transient
    InstanceNodeRepository instanceNodeRepository = Context.INSTANCE.getInstanceNodeRepository();

    @Transient
    private TypeNodeRepository typeNodeRepository = Context.INSTANCE.getTypeNodeRepository();

    @Transient
    JsonNode instanceData;

    /****************/
    /* Constructors */
    /****************/

    // TODO: Move to builder pattern, just too many constructors

    public InstanceNode() {}

    public InstanceNode(String name, String desc, TypeNode instanceOf) {
        super(name, desc);
        this.instanceOf = instanceOf;
    }

    public InstanceNode(String name, String desc, TypeNode instanceOf, String uuid) {
        super(name, desc, uuid);
        this.instanceOf = instanceOf;
    }

    public InstanceNode(String name, String desc, String instanceOfUuid) {
        super(name, desc);

        TypeNode instanceOf = typeNodeRepository.findByUuid(instanceOfUuid);
        if (instanceOf != null) {
            this.instanceOf = typeNodeRepository.findByUuid(instanceOfUuid);
        } else {
            throw new RuntimeException("ERROR: can't create InstanceNode, UUID " + instanceOfUuid + " unknown.");
        }
    }

    public InstanceNode(String name, String desc, String instanceOfUuid, String uuid) {
        super(name, desc, uuid);

        TypeNode instanceOf = typeNodeRepository.findByUuid(instanceOfUuid);
        if (instanceOf != null) {
            this.instanceOf = typeNodeRepository.findByUuid(instanceOfUuid);
        } else {
            throw new RuntimeException("ERROR: can't create InstanceNode, UUID " + instanceOfUuid + " unknown.");
        }
    }

    public InstanceNode(ObjectNode json, String isa) {
        this(json.get("name").asText(), json.get("desc").asText(), isa);
        instanceData = json.deepCopy();
        setProperties(json);
    }

    public InstanceNode(ObjectNode json, TypeNode isa,
            String uuid) {
        this(json.get("name").asText(), json.get("desc").asText(), isa, uuid);
        setProperties(json);
    }

    /*******************/
    /* Private Methods */
    /*******************/

    private InstanceNode getFirstComponent() {
        if (components == null) {
            throw new IllegalArgumentException("Components set is empty.");
        }

        return components.iterator().next();
    }

    private ObjectNode recurseForArguments(String pathKeyName, Set<InstanceNode> components, ObjectNode hierarchyPaths) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode();
        if (hierarchyPaths == null) {
            hierarchyPaths = result.putObject(pathKeyName);
        }

        final ObjectNode finalHierarchyPaths = hierarchyPaths;
        getComponents().stream()
                .forEach(component -> {
                    String componentTypeName = component.getInstanceOf().getName();
                    if (!result.has(componentTypeName)) {
                        result.putArray(componentTypeName);
                    }
                    JsonNode comp = component.toFlatJson();
                    if (comp.get(pathKeyName).isTextual()) {
                        finalHierarchyPaths.put(comp.get(pathKeyName).asText(), componentTypeName);
                    }
                    ((ObjectNode) comp).remove(pathKeyName);
                    ((ArrayNode) result.get(componentTypeName)).add(comp);

                    Set<InstanceNode> childComponents = component.getComponents();
                    if (childComponents.size() > 0) {
                        ObjectNode childResult = recurseForArguments(pathKeyName, childComponents, finalHierarchyPaths);
                        Iterator<String> fields = childResult.fieldNames();
                        while (fields.hasNext()) {
                            String fieldName = fields.next();
                            if (!result.has(fieldName)) {
                                result.set(fieldName, childResult.get(fieldName));
                            } else {
                                ArrayNode field = (ArrayNode) result.get(fieldName);
                                Iterator<JsonNode> elements = childResult.elements();
                                while (elements.hasNext()) {
                                    field.add(elements.next());
                                }
                            }
                        }
                    }
                });

        return result;
    }

    /******************/
    /* Public Methods */
    /******************/

    // Set the capability of a Handler instance. Should only be called on
    // a Handler.
    public void setCapability(TypeNode capability) {
        // Check if this is a Handler and if capability isn't null and of right type.
        if (getInstanceOf().getName().equals("Instrument") &&
                capability != null && capability.getIsa().getName().equals("Capability")) {
            this.capability = capability;
        } else {
            throw new RuntimeException("setCapability can only be run on an instance of type instrument.");
        }
    }

    // Set the capability of a Handler instance. Should only be called on
    // a Handler.
    public void setCapability(String capabilityUuid) {
        TypeNode capability = typeNodeRepository.findByUuid(capabilityUuid);
        setCapability(capability);
    }

    public Set<InstanceNode> getComponents() {
        if (components.size() < 1 || getFirstComponent().getName() == null) {
            Transaction tx = Context.INSTANCE.getGraphDatabase().beginTx();
            Context.INSTANCE.getNeo4jTemplate().fetch(components);
            tx.close();
        }
        return components;
    }

    public Set<TypeNode> getHandledTypes() {
        return handlesTypes;
    }

    public TypeNode getInstanceOf() {
        return instanceOf;
    }

    public String getProperty(String key) {
        Object property = kv.getProperty(key);
        if (property == null) {
            return null;
        } else {
            return property.toString();
        }
    }

    public InstanceNode setProperties(ObjectNode properties) {
        try {
            JsonSchemaBuilder jsonSchema = instanceOf.getSchema();
            final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
            final JsonSchema schema = factory.getJsonSchema(jsonSchema.getAsObjectNode());
            ProcessingReport report;

            report = schema.validate(properties);

            if (report.isSuccess()) {
                // Name and desc are saved already, so we can remove those.
                properties.remove("name");
                properties.remove("desc");
                Iterator<String> iter = properties.fieldNames();
                while (iter.hasNext()) {
                    String key = iter.next();
                    kv.setProperty(key, properties.get(key).asText());
                }
            } else {
                log.debug(report.toString());
                throw new RuntimeException("Supplied JSON did not match TypeNode schema.");
            }
        } catch (ProcessingException e) {
            log.error(e.getMessage());
            throw new RuntimeException("Supplied JSON did not match TypeNode schema.");
        }

        return this;
    }

    // Get all the results belonging to this InstanceNode
    public Set<InstanceNode> getResults() {
        // Fetch results from database
        GraphDatabase graphDatabase = Context.INSTANCE.getGraphDatabase();
        Transaction tx = graphDatabase.beginTx();
        Context.INSTANCE.getNeo4jTemplate().fetch(results);
        tx.success();
        tx.close();

        return results;
    }

    // Get all the results belonging to this InstanceNode
    // filtering on the task ID.
    public Set<InstanceNode> getResults(String task) {
        return getResults().stream()
                .filter(r -> r.getProperty("task").equals(task))
                .collect(Collectors.toSet());
    }

    public void addComponent(InstanceNode component) {
        if (components == null) {
            components = new HashSet<>();
        }
        components.add(component);
    }

    public void addComponent(String componentUuid) {
        InstanceNode component = null;

        if (componentUuid != null) {
            component = instanceNodeRepository.findByUuid(componentUuid);
            if (component != null) {
                addComponent(component);
            }
        }
    }

    // Add a handled type to an instrument.
    // Handled types are used on Instruments only.
    public InstanceNode addHandledType(TypeNode type) {
        if (getInstanceOf().getName().equals("Instrument")) {
            if (handlesTypes == null) {
                handlesTypes = new HashSet<>();
            }
            handlesTypes.add(type);
            return this;
        }
        throw new RuntimeException("Can't call addHandledType on non-instrument instance.");
    }

    public void addResult(InstanceNode result) {
        if (results == null) {
            results = new HashSet<>();
        }
        // Check if parameter isn't null and is of right type.
        if (result != null && result.getInstanceOf().getName().equals("Result")) {
            Transaction tx = Context.INSTANCE.getGraphDatabase().beginTx();
            results.add(result);
            tx.success();
            tx.close();
        } else {
            log.error("addResult called with non-result parameter.");
        }
    }

    public void addResult(String resultUuid) {
        InstanceNode result = instanceNodeRepository.findByUuid(resultUuid);
        addResult(result);
    }

    public Set<TypeNode> findCommonProtocols(InstanceNode subject) {
        return typeNodeRepository.getCommonProtocols(this.getUuid(), subject.getUuid());
    }

    public boolean isOfType(String type) {
        return instanceNodeRepository.isOfType(getUuid(), type) > 0;
    }

    // Run a instrument on this instance
    public void runInstrument(InstanceNode instrument, ObjectNode arguments, String taskId) {
        new InstrumentRunner(this, instrument, arguments, taskId).startInstrument();
    }

    public InstanceNode save() {
        boolean newNode = getId() == null || getId() < 0;
        Transaction tx = Context.INSTANCE.getGraphDatabase().beginTx();
        if (instanceData == null || getInstanceOf().getSchema().validateJson(instanceData)) {
            getInstanceOf().getKv().asMap().entrySet().stream()
                    .forEach(entry -> kv.setProperty(entry.getKey(), entry.getValue()));
            instanceNodeRepository.save(this);
            tx.success();
            tx.close();
        } else {
            tx.failure();
            tx.close();
            throw new RuntimeException("Can't create instanceNode, missing properties.");
        }

        if (newNode) {
            Context.INSTANCE.getVertx().deployVerticle(
                    new InstanceNodeActor(this));
        }

        return this;
    }

    /**
     * Used to create a JSON representation of a single InstanceNode that
     * can be used to as an argument for a instrument.
     * @return JSON representation that can be used as an argument for a instrument.
     */
    public Set<JsonNode> toArguments() {
        Set<JsonNode> arguments = new HashSet<>();

        arguments.add(toFlatJson());
        BiConsumer<BiConsumer, InstanceNode> consumeArgsHelper = (f, node) -> {
            Set<InstanceNode> components = node.getComponents();
            if (components.size() > 0) {
                components.parallelStream().forEach(n -> f.accept(f, n));
            }
            arguments.add(node.toFlatJson());
        };

        // Lambda's can't refer to themselves using their variable name. That means consumeArgsHelper defined
        // above cannot refer to itself by name. This sucks, because it's a recursive function.
        // However, it can refer to itself if it's passed a reference to itself as an argument, so that's what's
        // happening in consumeArgs. After calling consumeArgsHelper below, consumeArgsHelper is bound to 'f' in the lambda.
        Consumer<InstanceNode> consumeArgs = node -> consumeArgsHelper.accept(consumeArgsHelper, node);

        getComponents().stream().forEach(consumeArgs);

        return arguments;
    }

    public JsonNode toFlatJson() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode();

        result.put("name", getName());
        result.put("desc", getDesc());
        result.put("uuid", getUuid());
        result.put("_hierarchyPath", getInstanceOf().getHierarchyPath());
        kv.asMap().forEach((key, val) -> result.put(key, val.toString()));

        return result;
    }

    public ObjectNode deepToJson(int dimension) {
        if (dimension <= 0) {
            return null;
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode();
        ObjectNode kvJson = result.putObject("kv");
        kv.asMap().forEach((key, val) -> kvJson.put(key, val.toString()));
        ObjectNode componentsJson = mapper.createObjectNode();
        if (dimension > 1) {
            getComponents().forEach(component -> {
                componentsJson.set(component.getName(), component.deepToJson(dimension - 1));
            });
        }

        result.put("name", getName());
        result.put("uuid", getUuid());
        result.put("desc", getDesc());
        result.put("instanceof", instanceOf.getUuid());
        result.put("glyphId", Integer.toHexString(instanceOf.getGlyphId()));
        if (componentsJson.size() > 0) {
            result.set("components", componentsJson);
        }
        if (inComponent != null) {
            result.put("incomponent", inComponent.getUuid());
        }

        result.put("_nodeType", getNodeType());

        return result;
    }

    public ObjectNode toJson() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode();
        ObjectNode kvJson = result.putObject("kv");
        kv.asMap().forEach((key, val) -> kvJson.put(key, val.toString()));
        ArrayNode componentsJson = mapper.createArrayNode();
        getComponents().forEach(component -> {
            ObjectNode componentNameUuid = mapper.createObjectNode();
            componentNameUuid.put("name", component.getName());
            componentNameUuid.put("uuid", component.getUuid());
            componentsJson.add(componentNameUuid);
        });

        result.put("name", getName());
        result.put("uuid", getUuid());
        result.put("desc", getDesc());
        result.put("instanceof", instanceOf.getUuid());
        result.put("glyphId", Integer.toHexString(instanceOf.getGlyphId()));
        if (componentsJson.size() > 0) {
            result.set("components", componentsJson);
        }
        if (inComponent != null) {
            result.put("incomponent", inComponent.getUuid());
        }

        result.put("_nodeType", getNodeType());

        return result;
    }
}
