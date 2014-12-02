package org.openpcf.universal.graph;

import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.fieldaccess.DynamicProperties;
import org.springframework.data.neo4j.fieldaccess.DynamicPropertiesContainer;

/**
 * Give a description of TypeNode here.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-11-27
 */
@NodeEntity
public class TypeNode extends Node {

    public enum Constraint {
        REQUIRED, OPTIONAL, NUMERIC
    };

    private DynamicProperties kvs = new DynamicPropertiesContainer();

    public TypeNode() {}

    public TypeNode(String name, String desc) {
        super(name, desc);
    }

    public TypeNode(String name, String desc, String uuid) {
        super(name, desc, uuid);
    }

    public TypeNode setKvs(String k, Constraint... constraint) {
        kvs.setProperty(k, constraint);
        return this;
    }

    public TypeNode setIsa(TypeNode isa) {
        super.setIsa(isa);
        return this;
    }
}
