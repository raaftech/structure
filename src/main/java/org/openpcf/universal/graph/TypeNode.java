package org.openpcf.universal.graph;

import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.fieldaccess.DynamicProperties;
import org.springframework.data.neo4j.fieldaccess.DynamicPropertiesContainer;

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

    @Transient
    private Set<String> allowedKvs = new HashSet<>();

    @Transient
    private Set<String> requiredKvs = new HashSet<>();

    private DynamicProperties kv = new DynamicPropertiesContainer();

    public Node addKv(String k, String v) {
        if (allowedKvs.contains(k)) {
            kv.setProperty(k, v);
        }
        return this;
    }
}
