package org.openpcf.universal.graph;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Transaction;
import org.openpcf.universal.base.Context;
import org.openpcf.universal.graph.repositories.TypeNodeRepository;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.core.GraphDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Give a description of TypeNode here.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-11-27
 */
@NodeEntity
public class TypeNode extends Node implements Template {

    @RelatedTo(type="ISA", direction = Direction.OUTGOING)
    private TypeNode isa;

    @RelatedTo(type="ISA", direction = Direction.INCOMING)
    private Set<Template> childIsaSet;

    // isaUuid exists only to temporarily save a UUID of a TypeNode's parent
    // in case the parent doesn't exist in the database yet.
    @Transient
    private String isaUuid;


    private HashMap<String, Descriptor> schema;
    private TypeNodeRepository typeNodeRepository = Context.INSTANCE.getTypeNodeRepository();

    public TypeNode() {}

    public TypeNode(String name, String desc) {
        super(name, desc);
    }

    public TypeNode(String name, String desc, String uuid) {
        super(name, desc, uuid);
    }

    public TypeNode setDescriptor(String key, String valueType, boolean required, String description, String defaultValue,
                                  String example) {
        if (schema == null) {
            schema = new HashMap<>();
        }

        TypeNode parentNode = getIsa();
        if (parentNode != null && parentNode.getSchema().containsKey(key)) {
            throw new RuntimeException("Can't override parent schema.");
        } else {
            schema.put(key, new Descriptor(valueType, required, description, defaultValue, example));
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
        if (schema == null) {
            schema = new HashMap<>();
        }

        if (isa != null) {
            this.isa = isa;
            HashMap<String, Descriptor> parentConstraints = isa.getSchema();
            if (parentConstraints != null) {
                // We can go around setDescriptor here, because we don't need
                // the checks setDescriptor does.
                schema.putAll(parentConstraints);
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

    public Set<Template> getChildIsaSet() {
        GraphDatabase graphDatabase = Context.INSTANCE.getGraphDatabase();
        Transaction tx = graphDatabase.beginTx();
        Context.INSTANCE.getNeo4jTemplate().fetch(childIsaSet);
        tx.success();
        tx.close();
        return childIsaSet;
    }

    public HashMap<String, Descriptor> getSchema() {
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
            json.put("schema", mapper.writeValueAsString(getSchema()));
            json.put("children", mapper.writeValueAsString(childIsaUuids));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return json;
    }

    @Override
    public String toString() {
        String s = "createNode(new TypeNode(\"" + getName() + "\", \"" + getDesc() + "\", \"" + getUuid() + "\")";
        boolean set = false;
        if (schema != null) {
            for (String k : schema.keySet()) {
                Descriptor constraint = schema.get(k);
                s += "\n\t.setDescriptor(\"" + k + "\", \"" + constraint.getValueType() + "\", " + constraint.getRequired() + ", \"" +
                        constraint.getDescription() + "\", \"" + constraint.getDefaultValue() + "\", \"" +
                        constraint.getExampleValue() + "\")";
            }
            set = true;
        }
        if (getIsa() != null) {
            s += "\n\t.setIsa(typeNodeRepository.findByUuid(\"" + getIsa().getUuid() + "\")";
            set = true;
        }
        s += set ? "));" : ");";
        return s;
    }
}
