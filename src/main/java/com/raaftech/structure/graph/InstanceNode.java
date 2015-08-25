package com.raaftech.structure.graph;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.stream.Collectors;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.core.GraphDatabase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.raaftech.structure.actor.node.InstanceNodeActor;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.repositories.InstanceNodeRepository;
import com.raaftech.structure.graph.repositories.TypeNodeRepository;
import com.raaftech.structure.helper.JsonSchemaBuilder;

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
    @Fetch
    private Set<InstanceNode> components = new HashSet<>();

    @RelatedTo(type="HAS_COMPONENT", direction = Direction.INCOMING)
    @Fetch
    private InstanceNode inComponent;

    @RelatedTo(type="HAS_GROUPING", direction = Direction.OUTGOING)
    private Set<InstanceNode> groupings = new HashSet<>();

    @RelatedTo(type="HAS_OPERATION", direction = Direction.OUTGOING)
    private Set<InstanceNode> operations = new HashSet<>();

    @RelatedTo(type="HAS_HANDLER", direction = Direction.OUTGOING)
    @Fetch
    private Set<InstanceNode> handlers;

    @RelatedTo(type="HAS_PROTOCOL", direction = Direction.OUTGOING)
    private Set<InstanceNode> protocols = new HashSet<>();

    @RelatedTo(type="HAS_ACTOR", direction = Direction.OUTGOING)
    private Set<InstanceNode> actors = new HashSet<>();

    @RelatedTo(type="INSTANCE_OF", direction=Direction.OUTGOING)
    @Fetch
    private TypeNode instanceOf;

    @RelatedTo(type="HAS_COMPONENT", direction=Direction.OUTGOING)
    private Set<InstanceNode> hasComponents;

    private LinkedHashMap<String, String> kv = new LinkedHashMap<>();

    @Transient
    InstanceNodeRepository instanceNodeRepository = Context.INSTANCE.getInstanceNodeRepository();

    @Transient
    private TypeNodeRepository typeNodeRepository = Context.INSTANCE.getTypeNodeRepository();

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
        setProperties(json);
    }

    public InstanceNode(ObjectNode json, TypeNode isa,
            String uuid) {
        this(json.get("name").asText(), json.get("desc").asText(), isa, uuid);
        setProperties(json);
    }

    public void addResult(InstanceNode result) {
        // TODO: check if result is an actual Result
        if (results == null) {
            results = new HashSet<>();
        }
        if (instanceNodeRepository.isOfType(result.getInstanceOf().getUuid(), "Result") > 0) {
            results.add(result);
        }
    }

    public void addResult(String resultUuid) {
        InstanceNode result = null;

        if (resultUuid != null) {
            // TODO: check if result is an actual Result
            result = instanceNodeRepository.findByUuid(resultUuid);
            if (result != null) {
                addResult(result);
            }
        }
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
        Set<InstanceNode> filteredResults = getResults().stream()
                .filter(r -> r.getProperty("task").equals(task))
                .collect(Collectors.toSet());

        return filteredResults;
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

    public Set<InstanceNode> getComponents() {
        return components;
    }

    public InstanceNode addHandler(InstanceNode handler) {
        if (handler == null) {
            throw new RuntimeException("Cannot add handler, null argument.");
        }
        if (handlers == null) {
            handlers = new HashSet<>();
        }
        TypeNode handlerType = typeNodeRepository.findByUuid("14dbfbfa-b50a-466f-8539-55d5d3f15e4a");
        TypeNode capabilityType = typeNodeRepository.findByUuid("916e04a9-c773-4eaf-9c08-450016426e5a");
        // Check if the given argument is part of the Handler tree
        boolean isHandler = handlerType.getChildIsaSet().stream()
                .anyMatch(h -> h.getUuid().equals(handler.instanceOf.getUuid()));
        // Check if this InstanceNode is an instance of a node in the Capability tree
        boolean isCapability = capabilityType.getChildIsaSet().stream()
                .anyMatch(c -> c.getUuid().equals(instanceOf.getUuid()));

        if (!isHandler) {
            throw new RuntimeException("Cannot add handler, it's not a handler type.");
        }
        if (!isCapability) {
            throw new RuntimeException("Cannot add handler to non Capability type.");
        }

        // Everything checked out, add handler.
        Transaction tx = Context.INSTANCE.getGraphDatabase().beginTx();
        try {
            handlers.add(handler);
            this.save();
            handler.save();
            tx.success();
        } catch (Exception e) {
            tx.failure();
            throw e;
        } finally {
            tx.close();
        }

        return handler;
    }

    public InstanceNode addHandler(String handlerUuid) {
        return addHandler(instanceNodeRepository.findByUuid(handlerUuid));
    }

    public Set<InstanceNode> getHandlers() {
        return handlers;
    }

    public TypeNode getInstanceOf() {
        return instanceOf;
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
                    kv.put(key, properties.get(key).asText());
                }
            } else {
                throw new RuntimeException("Supplied JSON did not match TypeNode schema.");
            }
        } catch (ProcessingException e) {
            log.error(e.getMessage());
            throw new RuntimeException("Supplied JSON did not match TypeNode schema.");
        }

        return this;
    }

    public String getProperty(String key) {
        return kv.get(key);
    }

    public InstanceNode save() {
        boolean newNode = getId() == null || getId() < 0;
        Transaction tx = Context.INSTANCE.getGraphDatabase().beginTx();
        if (instanceNodeRepository.findByUuid(getUuid()) == null) {
            // -2 for the name and desc property.
            if (instanceOf.getSchema().getProperties().size()-2 == kv.size()) {
                instanceNodeRepository.save(this);
                tx.success();
                tx.close();
            } else {
                tx.failure();
                tx.close();
                throw new RuntimeException("Can't create instanceNode, missing properties.");
            }
        }

        if (newNode) {
            Context.INSTANCE.getVertx().deployVerticle(
                    new InstanceNodeActor(this));
        }

        return this;
    }

    public ObjectNode toJson() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode();
        ObjectNode kvJson = result.putObject("kv");
        kv.forEach((key, val) -> {
            kvJson.put(key, val);
        });
        ArrayNode componentsJson = mapper.createArrayNode();
        components.forEach(component -> {
            ObjectNode componentNameUuid = mapper.createObjectNode();
            componentNameUuid.put("name", component.getName());
            componentNameUuid.put("uuid", component.getUuid());
            componentsJson.add(componentNameUuid);
        });

        result.put("name", getName());
        result.put("uuid", getUuid());
        result.put("desc", getDesc());
        result.put("instanceof", instanceOf.getUuid());
        result.put("capabilities", instanceOf.getCapabilitiesAndHandlers());
        if (componentsJson.size() > 0) {
            result.put("components", componentsJson);
        }
        if (inComponent != null) {
            result.put("incomponent", inComponent.getUuid());
        }

        return result;
    }

    public String getHierarchyPath() {
        return null;
    }
}
