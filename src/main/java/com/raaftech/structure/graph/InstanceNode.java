package com.raaftech.structure.graph;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.repositories.InstanceNodeRepository;
import com.raaftech.structure.graph.repositories.TypeNodeRepository;
import com.raaftech.structure.helper.JsonSchemaBuilder;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

/**
 * Give a description of InstanceNode here.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-11-27
 */
@NodeEntity
public class InstanceNode extends Node {

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
    private Set<InstanceNode> handlers = new HashSet<>();

    @RelatedTo(type="HAS_PROTOCOL", direction = Direction.OUTGOING)
    private Set<InstanceNode> protocols = new HashSet<>();

    @RelatedTo(type="HAS_CAPABILITY", direction = Direction.OUTGOING)
    private Set<InstanceNode> capabilities = new HashSet<>();

    @RelatedTo(type="HAS_ACTOR", direction = Direction.OUTGOING)
    private Set<InstanceNode> actors = new HashSet<>();

    @Fetch
    @RelatedTo(type="INSTANCE_OF", direction=Direction.OUTGOING)
    private TypeNode instanceOf;

    @RelatedTo(type="HAS_COMPONENT", direction=Direction.OUTGOING)
    private Set<InstanceNode> hasComponents;

    InstanceNodeRepository instanceNodeRepository = Context.INSTANCE.getInstanceNodeRepository();

    private LinkedHashMap<String, String> kv = new LinkedHashMap<>();

    private TypeNodeRepository typeNodeRepository = Context.INSTANCE.getTypeNodeRepository();

    public InstanceNode() {}

    public InstanceNode(String name, String desc, TypeNode instanceOf) {
        super(name, desc);
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

    public InstanceNode(ObjectNode json, String isa) {
        this(json.get("name").asText(), json.get("desc").asText(), isa);
        setProperties(json);
    }

    public void addResult(InstanceNode result) {
        if (results == null) {
            results = new HashSet<>();
        }
        results.add(result);
    }

    public void addResult(String resultUuid) {
        InstanceNode result = null;

        if (resultUuid != null) {
            result = instanceNodeRepository.findByUuid(resultUuid);
            if (result != null) {
                addResult(result);
            }
        }
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

    public InstanceNode setProperties(ObjectNode properties) {
        try {
            JsonSchemaBuilder jsonSchema = instanceOf.getSchema();
            final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
            final JsonSchema schema = factory.getJsonSchema(jsonSchema.getAsObjectNode());
            ProcessingReport report;

            report = schema.validate(properties);
            System.out.println(jsonSchema.getAsObjectNode());
            System.out.println(properties);

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
            e.printStackTrace();
            throw new RuntimeException("Supplied JSON did not match TypeNode schema.");
        }

        return this;
    }

    public InstanceNode save() {
        if (instanceNodeRepository.findByUuid(getUuid()) == null) {
            // -2 for the name and desc property.
            if (instanceOf.getSchema().getProperties().size()-2 == kv.size()) {
                instanceNodeRepository.save(this);
            } else {
                throw new RuntimeException("Can't create instanceNode, missing properties.");
            }
        }
        return this;
    }

    public ObjectNode toJson() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode();
        ArrayNode kvJson = mapper.createArrayNode();
        kv.forEach((key, val) -> {
            ObjectNode kvObj = mapper.createObjectNode();
            kvObj.put(key, val);
            kvJson.add(kvObj.toString());
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
        result.put("kv", kvJson);
        if (componentsJson.size() > 0) {
            result.put("components", componentsJson);
        }
        if (inComponent != null) {
            result.put("incomponent", inComponent.getUuid());
        }

        return result;
    }
}
