package com.raaftech.structure.graph;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Transaction;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.core.GraphDatabase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.repositories.TypeNodeRepository;
import com.raaftech.structure.helper.JsonSchemaBuilder;

/**
 * Give a description of TypeNode here.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-11-27
 */
@NodeEntity
public class TypeNode extends Node {

    @RelatedTo(type="ISA", direction = Direction.OUTGOING, elementClass = TypeNode.class, enforceTargetType = true)
    private TypeNode isa;

    @RelatedTo(type="ISA", direction = Direction.INCOMING, elementClass = TypeNode.class, enforceTargetType = true)
    private Set<TypeNode> childIsaSet;

    @RelatedTo(type="HAS_CAPABILITY", direction = Direction.OUTGOING)
    private Set<InstanceNode> capabilities = new HashSet<>();

    // isaUuid exists only to temporarily save a UUID of a TypeNode's parent
    // in case the parent doesn't exist in the database yet.
    @Transient
    private String isaUuid;


    public JsonSchemaBuilder schema = new JsonSchemaBuilder();
    @Transient
    private TypeNodeRepository typeNodeRepository = Context.INSTANCE.getTypeNodeRepository();

    public TypeNode() {}

    public TypeNode(String name, String desc) {
        super(name, desc);
        initSchema();
    }

    public TypeNode(String name, String desc, String uuid) {
        super(name, desc, uuid);
        initSchema();
    }

    private void initSchema() {
        schema.setType("object");
        schema.setAdditionalProperties(false);
        schema.addProperty("name", "string", true, "The name of this node.");
        schema.addProperty("desc", "string", true, "The description of this node.");
    }

    public TypeNode setDescriptor(String key, String valueType, boolean required, String description, String defaultValue,
                                  String pattern) {
        TypeNode parentNode = getIsa();
        if (parentNode != null && parentNode.getSchema().containsProperty(key)) {
            throw new RuntimeException("Can't override parent schema.");
        } else {
            schema.addProperty(key, valueType, required, description)
                .setDefault(defaultValue)
                .setPattern(pattern);
        }

        return this;
    }

    public TypeNode getIsa() {
        GraphDatabase graphDatabase = Context.INSTANCE.getGraphDatabase();
        Transaction tx = graphDatabase.beginTx();
        Context.INSTANCE.getNeo4jTemplate().fetch(isa);
        tx.success();
        tx.close();
        return isa;
    }

    public TypeNode setIsa(TypeNode isa) {
        if (isa != null) {
            this.isa = isa;
            JsonSchemaBuilder parentConstraints = isa.getSchema();
            if (parentConstraints != null) {
                try {
                    if (schema == null) {
                        schema = new JsonSchemaBuilder(parentConstraints.getAsObjectNode());
                    } else {
                        Iterator<String> propertyIterator = parentConstraints.getProperties().fieldNames();
                        while (propertyIterator.hasNext()) {
                            String propertyName = propertyIterator.next();
                            if (!schema.containsProperty(propertyName)) {
                                JsonNode propertyJson = parentConstraints.getProperties().get(propertyName);
                                schema.addProperty(propertyName, propertyJson.get("type").asText(),
                                        parentConstraints.isRequired(propertyName), propertyJson.get("description").asText());
                            }
                        }
                    }
                } catch (ProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
        return this;
    }

    public TypeNode setIsa(String isa) {
        // We can't assume our parentNode has been persisted to the database yet at this point.
        // If the parentNode doesn't exist in the database yet, we just save it's UUID to a
        // transient field for later use (at createNode time).
        // If it does exist, we create the relations and inherit constraints in setIsa(TypeNode isa).

        TypeNode parentNode = typeNodeRepository.findByUuid(isa);
        if (parentNode != null) {
            setIsa(parentNode);
        } else {
            // This means our parent node hasn't been persisted to DB yet. Save UUID for later.
            setIsaUuid(isa);
        }

        return this;
    }

    public String getIsaUuid() {
        return isaUuid;
    }

    public void setIsaUuid(String isaUuid) {
        this.isaUuid = isaUuid;
    }

    public Set<TypeNode> getChildIsaSet() {
        GraphDatabase graphDatabase = Context.INSTANCE.getGraphDatabase();
        Transaction tx = graphDatabase.beginTx();
        Context.INSTANCE.getNeo4jTemplate().fetch(childIsaSet);
        tx.success();
        tx.close();
        return childIsaSet;
    }

    public TypeNode addCapability(InstanceNode capability) {
        if (capability == null) {
            throw new RuntimeException("Method can't be called with a null argument.");
        }
        if (capabilities == null) {
            capabilities = new HashSet<>();
        }

        TypeNode rootCapability = typeNodeRepository.findByUuid("916e04a9-c773-4eaf-9c08-450016426e5a");
        boolean isCapability = rootCapability.getChildIsaSet().stream()
                .anyMatch(c -> c.getUuid().equals(capability.getInstanceOf().getUuid()));

        if (isCapability) {
            capabilities.add(capability);
        } else {
            throw new RuntimeException("Method can't be called with an argument that's not part of the Capability tree.");
        }

        return this;
    }

    public JsonNode getCapabilitiesAndHandlers() {
        GraphDatabase graphDatabase = Context.INSTANCE.getGraphDatabase();
        Transaction tx = graphDatabase.beginTx();
        Context.INSTANCE.getNeo4jTemplate().fetch(capabilities);
        tx.success();
        tx.close();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode capabilitiesAndHandlers = mapper.createObjectNode();

        capabilities.stream()
            .filter(c -> c.getHandlers().size() > 0)
            .forEach(c -> {
                ObjectNode handlers = capabilitiesAndHandlers.putObject(c.getName());
                c.getHandlers().stream()
                    .forEachOrdered(h -> {
                        handlers.put(h.getName(), h.getProperty("plugin"));
                    });
            });
        return capabilitiesAndHandlers;
    }

    public JsonSchemaBuilder getSchema() {
        return schema;
    }

    public ObjectNode toJson() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = mapper.createObjectNode();

        List<String> childIsaUuids = getChildIsaSet().stream()
                .map(childIsa -> childIsa.getUuid())
                .collect(Collectors.toList());

        json.put("name", getName());
        json.put("uuid", getUuid());
        if (getIsa() != null) {
            json.put("isa", getIsa().getUuid());
        }
        json.put("desc", getDesc());
        try {
            if (getSchema() != null) {
                json.put("schema", getSchema().getAsObjectNode());
            }
            json.put("children", mapper.writeValueAsString(childIsaUuids));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return json;
    }

    public String getHierarchyPath() {
        LinkedList<String> reversePath = new LinkedList<>();
        TypeNode pointer = getIsa();

        // Get path in reverse order by traversing parent ISA's
        reversePath.add(getName());
        if (pointer != null) {
            reversePath.add(pointer.getName());
            while (pointer.getIsa() != null) {
                pointer = pointer.getIsa();
                reversePath.add(pointer.getName());
            }
        }

        // Change "type" to "typenode" for uniform addressing
        reversePath.set(reversePath.size()-1, "typenode");
        // Get a stream from the reverse order of the reversePath
        Iterable<String> reversePathIterable = () -> reversePath.descendingIterator();
        Stream<String> reversePathStream = StreamSupport.stream(reversePathIterable.spliterator(), false);
        // Collect every element of the path, joining on "." and casting to lower case
        String path = reversePathStream
            .map(s -> s.toLowerCase())
            .collect(Collectors.joining("."));

        return path;
    }
}
