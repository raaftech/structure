package org.openpcf.universal.graph;

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

    private HashMap<String, Constraint> constraints;

    public TypeNode() {}

    public TypeNode(String name, String desc) {
        super(name, desc);
    }

    public TypeNode(String name, String desc, String uuid) {
        super(name, desc, uuid);
    }

    public TypeNode setKvs(String kvName, String kvType, boolean required) {
        if (constraints == null) {
            constraints = new HashMap<>();
        }
        constraints.put(kvName, new Constraint(kvType, required));
        return this;
    }

    public TypeNode setIsa(TypeNode isa) {
        super.setIsa(isa);
        return this;
    }

    public HashMap<String, Constraint> getConstraints() {
        return constraints;
    }
}
