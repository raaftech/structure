package org.openpcf.universal.graph;

import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.support.index.IndexType;

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

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }
}
