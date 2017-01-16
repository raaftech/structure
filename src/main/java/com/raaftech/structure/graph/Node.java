package com.raaftech.structure.graph;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.fieldaccess.DynamicProperties;
import org.springframework.data.neo4j.support.index.IndexType;

import java.util.Map;
import java.util.UUID;

import static io.vertx.core.json.Json.mapper;

/**
 * Give a description of Node here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-11-26
 *
 * @Deprecated We need to redesign the domain classes to use Entity.
 */
@NodeEntity
@JsonSerialize(using = NodeSerializer.class)
@Deprecated
public abstract class Node {

    @Indexed(indexType = IndexType.FULLTEXT, indexName = "name")
    protected String name;
    @Indexed(indexType = IndexType.FULLTEXT, indexName = "description")
    protected String desc;
    protected String bundle;
    protected String type;
    protected String checksum;
    @GraphId
    @SuppressWarnings("unused")
    private Long id;
    @Indexed(indexType = IndexType.FULLTEXT, indexName = "uuid")
    private String uuid;

    DynamicProperties data;
    DynamicProperties file;

    /****************/
    /* Constructors */
    /****************/

    public Node() { this.uuid = UUID.randomUUID().toString(); }

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

    public String getType() {
        return type;
    }

    public String getBundle() {
        return bundle;
    }

    public Map<String, Object> getFiles() {
        return file.asMap();
    }

    public Map<String, Object> getData() {
        return data.asMap();
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public void setBundle(String bundle) {
        this.bundle = bundle;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Node node = (Node) o;

        if (id != null ? !id.equals(node.id) : node.id != null) return false;
        return uuid != null ? uuid.equals(node.uuid) : node.uuid == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (uuid != null ? uuid.hashCode() : 0);
        return result;
    }
}
