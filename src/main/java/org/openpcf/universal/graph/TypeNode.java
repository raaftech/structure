package org.openpcf.universal.graph;

import org.openpcf.universal.base.Context;
import org.openpcf.universal.graph.repositories.TypeNodeRepository;
import org.springframework.data.neo4j.annotation.NodeEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Give a description of TypeNode here.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-11-27
 */
@NodeEntity
public class TypeNode extends Node {

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

        TypeNode parentNode = (TypeNode)getIsa();
        if (parentNode != null && parentNode.getSchema().containsKey(key)) {
            throw new RuntimeException("Can't override parent schema.");
        } else {
            schema.put(key, new Descriptor(valueType, required, description, defaultValue, example));
        }

        return this;
    }

    public TypeNode getIsa() {
        return (TypeNode)super.getIsa();
    }

    public TypeNode setIsa(String isa) {
        // We can't assume our parentNode has been persisted to the database yet at this point.
        // If the parentNode doesn't exist in the database yet, we just save it's UUID to a
        // transient field for later use (at createNode time).
        // If it does exist, we create the relations and inherit constraints.
        if (schema == null) {
            schema = new HashMap<>();
        }

        TypeNode parentNode = typeNodeRepository.findByUuid(isa);
        if (parentNode != null) {
            setIsa(parentNode);
            HashMap<String, Descriptor> parentConstraints = parentNode.getSchema();
            if (parentConstraints != null) {
                // We can go around setDescriptor here, because we don't need
                // the checks setDescriptor does.
                schema.putAll(parentConstraints);
            }
        } else {
            // This means our parent node hasn't been persisted to DB yet. Save UUID for later.
            super.setIsaUuid(isa);
        }

        return this;
    }

    public Set<TypeNode> getChildIsaSetAsTypeNodes() {
        HashSet<TypeNode> childIsaSet = new HashSet<>();

        Context.INSTANCE.getNeo4jTemplate().fetch(getChildIsaSet());
        getChildIsaSet().stream()
                .filter(node -> node instanceof TypeNode)
                .map(node -> (TypeNode)node)
                .forEach(node -> childIsaSet.add(node));

        return childIsaSet;
    }

    public HashMap<String, Descriptor> getSchema() {
        return schema;
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
