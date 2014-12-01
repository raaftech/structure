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

    private DynamicProperties optionalKv = new DynamicPropertiesContainer();

    private DynamicProperties requiredKv = new DynamicPropertiesContainer();

    public TypeNode() {}

    public TypeNode(String name, String desc) {
        super(name, desc);
    }

    public TypeNode(String name, String desc, String uuid) {
        super(name, desc, uuid);
    }

    public TypeNode setOptionalKv(String k, String v) {
        optionalKv.setProperty(k, v);
        return this;
    }

    public TypeNode setRequiredKv(String k, String v) {
        optionalKv.setProperty(k, v);
        return this;
    }

    public TypeNode setIsa(TypeNode isa) {
        super.setIsa(isa);
        return this;
    }
}
