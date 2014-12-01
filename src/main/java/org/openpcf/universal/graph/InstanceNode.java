package org.openpcf.universal.graph;

import org.springframework.data.neo4j.annotation.NodeEntity;

/**
 * Give a description of InstanceNode here.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-11-27
 */
@NodeEntity
public class InstanceNode extends Node {

    public InstanceNode() {}

    public InstanceNode(String name, String desc) {
        super(name, desc);
    }

    public InstanceNode(String name, String desc, String uuid) {
        super(name, desc, uuid);
    }
}
