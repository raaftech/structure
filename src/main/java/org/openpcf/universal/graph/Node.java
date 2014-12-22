package org.openpcf.universal.graph;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.*;
import org.springframework.data.neo4j.support.index.IndexType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Give a description of Node here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-11-26
 */
@NodeEntity
public class Node {

    @GraphId
    private Long id;

    @Indexed
    private String name;

    @Indexed(indexType = IndexType.FULLTEXT, indexName = "descriptions")
    private String desc;

    @Indexed(unique=true)
    private String uuid;

    @RelatedTo(type="ISA", direction = Direction.OUTGOING)
    @Fetch
    private Node isa;

    @RelatedTo(type="ISA", direction = Direction.INCOMING)
    private Set<Node> childIsaSet;

    @RelatedTo(type="HAS", direction = Direction.OUTGOING)
    private Set<Node> has = new HashSet<>();

    public Node() {}

    public Node(String name, String desc) {
        this.name = name;
        this.desc = desc;
        this.uuid = UUID.randomUUID().toString();
    }

    public Node(String name, String desc, String uuid) {

        this.name = name;
        this.desc = desc;
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public Node getIsa() {
        return isa;
    }

    public Node setIsa(Node isa) {
        this.isa = isa;
        return this;
    }

    public Set<Node> getChildIsaSet() {
        return childIsaSet;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }
}
