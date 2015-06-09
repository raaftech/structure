package com.raaftech.structure.graph;

import java.util.UUID;

import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.support.index.IndexType;

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

    @Indexed(indexType = IndexType.FULLTEXT, indexName = "name")
    private String name;

    @Indexed(indexType = IndexType.FULLTEXT, indexName = "description")
    private String desc;

    @Indexed(indexType = IndexType.FULLTEXT, unique=true, indexName = "uuid")
    private String uuid;

    public Node() {}

    public Node(String name, String desc, String uuid) {
        UUID.fromString(uuid); // throws exception on invalid uuid
        if (name.length() == 0 || desc.length() == 0) {
            throw new RuntimeException("Cannot create node with an empty name or description.");
        }
        this.name = name;
        this.desc = desc;
        this.uuid = uuid;
    }

    public Node(String name, String desc) {
        this(name, desc, UUID.randomUUID().toString());
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }
}
