package org.openpcf.universal.graph;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.util.HashSet;
import java.util.Set;

/**
 * Give a description of InstanceNode here.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-11-27
 */
@NodeEntity
public class InstanceNode extends Node {

    @RelatedTo(type="HAS_RESULT", direction = Direction.OUTGOING)
    private Set<InstanceNode> results = new HashSet<>();

    @RelatedTo(type="HAS_COMPONENT", direction = Direction.OUTGOING)
    private Set<InstanceNode> components = new HashSet<>();

    @RelatedTo(type="HAS_GROUPING", direction = Direction.OUTGOING)
    private Set<InstanceNode> groupings = new HashSet<>();

    @RelatedTo(type="HAS_OPERATION", direction = Direction.OUTGOING)
    private Set<InstanceNode> operations = new HashSet<>();

    @RelatedTo(type="HAS_HANDLER", direction = Direction.OUTGOING)
    private Set<InstanceNode> handlers = new HashSet<>();

    @RelatedTo(type="HAS_PROTOCOL", direction = Direction.OUTGOING)
    private Set<InstanceNode> protocols = new HashSet<>();

    @RelatedTo(type="HAS_CAPABILITY", direction = Direction.OUTGOING)
    private Set<InstanceNode> capabilities = new HashSet<>();

    @RelatedTo(type="HAS_ACTOR", direction = Direction.OUTGOING)
    private Set<InstanceNode> actors = new HashSet<>();

    public InstanceNode() {}

    public InstanceNode(String name, String desc) {
        super(name, desc);
    }

    public InstanceNode(String name, String desc, String uuid) {
        super(name, desc, uuid);
    }
}
