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

    public enum TypeConstraint {
        REQUIRED, OPTIONAL,
        STRING, INTEGER, DATETIME, BOOLEAN
    };

    private DynamicProperties kvs = new DynamicPropertiesContainer();

    public TypeNode() {}

    public TypeNode(String name, String desc) {
        super(name, desc);
    }

    public TypeNode(String name, String desc, String uuid) {
        super(name, desc, uuid);
    }

    public TypeNode setKvs(String k, String type, Boolean required) {
        TypeConstraint[] constraints = new TypeConstraint[2];
        TypeConstraint realType = null;

        switch (type) {
            case "String":
            case "Integer":
            case "DateTime":
            case "Boolean":
                realType = TypeConstraint.valueOf(type.toUpperCase());
        }
        if (realType != null) {
            constraints[0] = realType;
            constraints[1] = required ? TypeConstraint.REQUIRED : TypeConstraint.OPTIONAL;
        } else {
            // TODO: Throw exception?
            return this;
        }
        kvs.setProperty(k, constraints);
        return this;
    }

    public TypeNode setIsa(TypeNode isa) {
        super.setIsa(isa);
        return this;
    }
}
