package org.openpcf.universal.graph;

import org.openpcf.universal.base.SpringContext;
import org.openpcf.universal.graph.repositories.TypeNodeRepository;
import org.springframework.data.neo4j.annotation.NodeEntity;

import java.util.HashMap;

/**
 * Give a description of TypeNode here.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-11-27
 */
@NodeEntity
public class TypeNode extends Node {

    private HashMap<String, Descriptor> schema;
    private TypeNodeRepository typeNodeRepository = SpringContext.INSTANCE.getTypeNodeRepository();

    public TypeNode() {}

    public TypeNode(String name, String desc) {
        super(name, desc);
    }

    public TypeNode(String name, String desc, String uuid) {
        super(name, desc, uuid);
    }

    public TypeNode setKvs(String kvName, String kvType, boolean required, String description, String defaultValue,
                           String example) {
        if (schema == null) {
            schema = new HashMap<>();
        }

        TypeNode parentNode = (TypeNode)getIsa();
        if (parentNode != null && parentNode.getSchema().containsKey(kvName)) {
            throw new RuntimeException("Can't override parent schema.");
        } else {
            schema.put(kvName, new Descriptor(kvType, required, description, defaultValue, example));
        }

        return this;
    }

    public TypeNode setIsa(TypeNode isa) {
        super.setIsa(isa);

        if (schema == null) {
            schema = new HashMap<>();
        }

        HashMap<String, Descriptor> parentConstraints = typeNodeRepository.findByUuid(isa.getUuid()).getSchema();
        if (parentConstraints != null) {
            // We can go around setKvs here, because we don't need
            // the checks setKvs does.
            schema.putAll(parentConstraints);
        }

        return this;
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
                s += "\n\t.setKvs(\"" + k + "\", \"" + constraint.getKvType() + "\", " + constraint.getRequired() + ", \"" +
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
