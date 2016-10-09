package com.raaftech.structure.graph;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.support.index.IndexType;

import java.util.UUID;

import static io.vertx.core.json.Json.mapper;

/**
 * Give a description of Node here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-11-26
 */
@NodeEntity
public abstract class Node {

    @Indexed(indexType = IndexType.FULLTEXT, indexName = "name")
    protected String name;
    @Indexed(indexType = IndexType.FULLTEXT, indexName = "description")
    protected String desc;

    protected String checksum;
    @GraphId
    @SuppressWarnings("unused")
    private Long id;
    @Indexed(indexType = IndexType.FULLTEXT, indexName = "uuid")
    private String uuid;

    /****************/
    /* Constructors */
    /****************/

    public Node() {
        this.uuid = UUID.randomUUID().toString();
    }

    public Node(String name, String desc) {
        this(name, desc, UUID.randomUUID().toString());
    }

    @SuppressWarnings("unused")
    public Node(String name, String desc, String uuid) {
        UUID check = UUID.fromString(uuid); // throws exception on invalid uuid
        if (name.length() == 0 || desc.length() == 0) {
            throw new RuntimeException("Cannot create node with an empty name or description.");
        }
        this.name = name;
        this.desc = desc;
        this.uuid = uuid;
    }

    @SuppressWarnings("unused")
    public Node(String name, String desc, String uuid, String checksum) {
        UUID check = UUID.fromString(uuid); // throws exception on invalid uuid
        if (name.length() == 0 || desc.length() == 0) {
            throw new RuntimeException("Cannot create node with an empty name or description.");
        }
        this.name = name;
        this.desc = desc;
        this.uuid = uuid;
        this.checksum = checksum;
    }


    /******************/
    /* Public Methods */
    /******************/

    public String getDesc() {
        return desc;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNodeType() {
        return getClass().getSimpleName();
    }

    public String getUuid() {
        return uuid;
    }

    public String getChecksum() {
        return checksum;
    }

    public ObjectNode toJson() {
        ObjectNode json = mapper.createObjectNode();
        json
                .put("name", name)
                .put("desc", desc)
                .put("uuid", uuid)
                .put("checksum", checksum);

        return json;
    }
}
