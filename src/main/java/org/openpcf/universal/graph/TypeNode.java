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

    private HashMap<String, Constraint> kvs;
    private TypeNodeRepository typeNodeRepository = SpringContext.INSTANCE.getTypeNodeRepository();

    public TypeNode() {}

    public TypeNode(String name, String desc) {
        super(name, desc);
    }

    public TypeNode(String name, String desc, String uuid) {
        super(name, desc, uuid);
    }

    public TypeNode setKvs(String kvName, String kvType, boolean required) {
        if (kvs == null) {
            kvs = new HashMap<>();
        }

        TypeNode parentNode = (TypeNode)getIsa();
        if (parentNode != null && parentNode.getKvs().containsKey(kvName)) {
            throw new RuntimeException("Can't override parent kvs.");
        } else {
            kvs.put(kvName, new Constraint(kvType, required));
        }

        return this;
    }

    public TypeNode setIsa(TypeNode isa) {
        super.setIsa(isa);

        if (kvs == null) {
            kvs = new HashMap<>();
        }

        HashMap<String, Constraint> parentConstraints = typeNodeRepository.findByUuid(isa.getUuid()).getKvs();
        if (parentConstraints != null) {
            // We can go around setKvs here, because we don't need
            // the checks setKvs does.
            kvs.putAll(parentConstraints);
        }

        return this;
    }

    public HashMap<String, Constraint> getKvs() {
        return kvs;
    }
}
