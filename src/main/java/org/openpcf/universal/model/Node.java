package org.openpcf.universal.model;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
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
    private Set<Node> isa = new HashSet<>();

    @RelatedTo(type="HAS", direction = Direction.OUTGOING)
    private Set<Component> has = new HashSet<Component>();

    public Node() {}

    public Node(String name, String desc, String uuid, Set<Node> isa) {
        this.name = name;
        this.desc = desc;
        this.uuid = uuid;
        if (isa != null) {
            this.isa = isa;
        }
    }

    public Node(String name, String desc, Set<Node> isa) {
        this.name = name;
        this.desc = desc;
        this.uuid = UUID.randomUUID().toString();
        if (isa != null) {
            this.isa = isa;
        }
    }

    public String getUuid() {
        return uuid;
    }
}
