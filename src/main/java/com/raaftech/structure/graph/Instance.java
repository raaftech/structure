package com.raaftech.structure.graph;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.repositories.InstanceRepository;
import com.raaftech.structure.graph.repositories.TypeRepository;
import com.raaftech.structure.helper.JsonHelper;
import com.raaftech.structure.node.InstanceActor;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.fieldaccess.DynamicPropertiesContainer;

import java.util.Iterator;

/**
 * Give a description of Instance here.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-11-27
 *
 * @Deprecated We need to redesign the domain classes to use Entity.
 */
@NodeEntity
@Deprecated
public abstract class Instance extends Node {

    private Logger log = LoggerFactory.getLogger(Instance.class);

    @RelatedTo(type="INSTANCE_OF", direction=Direction.OUTGOING)
    @Fetch
    protected Type instanceOf;

    @Transient
    protected static final InstanceRepository INSTANCE_REPOSITORY = Context.getInstanceRepository();

    @Transient
    protected static final TypeRepository TYPE_REPOSITORY = Context.getTypeRepository();

    @Transient
    JsonNode instanceData;

    /****************/
    /* Constructors */
    /****************/

    // TODO: Move to builder pattern, just too many constructors

    public Instance() {}

    public Instance(String name, String desc, Type instanceOf) {
        super(name, desc);
        this.instanceOf = instanceOf;
    }

    public Instance(String name, String desc, Type instanceOf, String uuid) {
        super(name, desc, uuid);
        this.instanceOf = instanceOf;
    }

    public Instance(String name, String desc, String instanceOfUuid) {
        super(name, desc);

        Type instanceOf = TYPE_REPOSITORY.findByUuid(instanceOfUuid);
        if (instanceOf != null) {
            this.instanceOf = TYPE_REPOSITORY.findByUuid(instanceOfUuid);
        } else {
            throw new RuntimeException("ERROR: can't create Instance, UUID " + instanceOfUuid + " unknown.");
        }
    }

    public Instance(String name, String desc, String instanceOfUuid, String uuid) {
        super(name, desc, uuid);

        Type instanceOf = TYPE_REPOSITORY.findByUuid(instanceOfUuid);
        if (instanceOf != null) {
            this.instanceOf = TYPE_REPOSITORY.findByUuid(instanceOfUuid);
        } else {
            throw new RuntimeException("ERROR: can't create Instance, UUID " + instanceOfUuid + " unknown.");
        }
    }

    public Instance(ObjectNode json, Type isa) {
        this(json.get("name").asText(), json.get("desc").asText(), isa);
        instanceData = json.deepCopy();
        setProperties(json);
    }

    public Instance(ObjectNode json, String isa) {
        this(json.get("name").asText(), json.get("desc").asText(), isa);
        instanceData = json.deepCopy();
        setProperties(json);
    }

    public Instance(ObjectNode json, Type isa,
                    String uuid) {
        this(json.get("name").asText(), json.get("desc").asText(), isa, uuid);
        setProperties(json);
    }

    /******************/
    /* Public Methods */
    /******************/

    public Type getInstanceOf() {
        return instanceOf;
    }

    public String getProperty(String key) {
        Object property = data.getProperty(key);
        if (property == null) {
            return null;
        } else {
            return property.toString();
        }
    }

    /**
     * This method is transform JSON to DynamicProperties.
     * @param properties The JSON to use for the properties.
     * @return The Instance setProperties was called on.
     */
    protected Instance setProperties(ObjectNode properties) {
        if (data == null) {
            data = new DynamicPropertiesContainer();
        }
        boolean success = JsonHelper.validateToSchema(instanceOf.getSchema().getAsObjectNode(), properties);
        if (success) {
            // Name and desc are saved already, so we can remove those.
            properties.remove("name");
            properties.remove("desc");
            Iterator<String> iter = properties.fieldNames();
            while (iter.hasNext()) {
                String key = iter.next();
                if (properties.get(key).isObject()) {
                    data.setProperty(key, properties.get(key));
                } else {
                    data.setProperty(key, properties.get(key).asText());
                }
            }
        } else {
            throw new RuntimeException("Supplied JSON did not match Type schema.");
        }

        return this;
    }

    public boolean isOfType(String type) {
        return INSTANCE_REPOSITORY.isOfType(getUuid(), type) > 0;
    }

    public Instance save() {
        boolean newNode = getId() == null || getId() < 0;
        Transaction tx = Context.getGraphDatabase().beginTx();
        if (getInstanceOf().getSchema().validateJson(instanceData)) {
            getInstanceOf().getKv().asMap().entrySet().stream()
                    .forEach(entry -> data.setProperty(entry.getKey(), entry.getValue()));
            INSTANCE_REPOSITORY.save(this);
            tx.success();
            tx.close();
        } else {
            tx.failure();
            tx.close();
            throw new RuntimeException("Can't create instanceNode, missing properties.");
        }

        if (newNode && Context.getVertx() != null) {
            Context.getVertx().deployVerticle(
                    new InstanceActor(this));
        }

        return this;
    }

    public ObjectNode toJson() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode();

        result.put("name", getName());
        result.put("desc", getDesc());
        result.put("uuid", getUuid());
        result.put("_hierarchyPath", getInstanceOf().getHierarchyPath());
        data.asMap().forEach((key, val) -> result.put(key, val.toString()));

        return result;
    }

}
