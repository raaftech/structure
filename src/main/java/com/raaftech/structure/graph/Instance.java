package com.raaftech.structure.graph;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.raaftech.structure.base.*;
import com.raaftech.structure.base.Configuration;
import com.raaftech.structure.exceptions.TypeUnknownException;
import com.raaftech.structure.graph.repositories.InstanceRepository;
import com.raaftech.structure.graph.repositories.TypeRepository;
import com.raaftech.structure.helper.JsonSchemaBuilder;
import com.raaftech.structure.node.InstanceActor;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.fieldaccess.DynamicProperties;
import org.springframework.data.neo4j.fieldaccess.DynamicPropertiesContainer;

import java.util.Iterator;

/**
 * Give a description of Instance here.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-11-27
 */
@NodeEntity
public abstract class Instance extends Node {

    private Logger log = LoggerFactory.getLogger(Instance.class);

    @RelatedTo(type="INSTANCE_OF", direction=Direction.OUTGOING)
    @Fetch
    protected Type instanceOf;

    protected DynamicProperties kv = new DynamicPropertiesContainer();

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
        Object property = kv.getProperty(key);
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
        try {
            JsonSchemaBuilder jsonSchema = instanceOf.getSchema();
            final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
            final JsonSchema schema = factory.getJsonSchema(jsonSchema.getAsObjectNode());
            ProcessingReport report;

            report = schema.validate(properties);

            if (report.isSuccess()) {
                // Name and desc are saved already, so we can remove those.
                properties.remove("name");
                properties.remove("desc");
                Iterator<String> iter = properties.fieldNames();
                while (iter.hasNext()) {
                    String key = iter.next();
                    kv.setProperty(key, properties.get(key).asText());
                }
            } else {
                log.debug(report.toString());
                throw new RuntimeException("Supplied JSON did not match Type schema.");
            }
        } catch (ProcessingException e) {
			log.error(e.getMessage());

			if (Configuration.isTraceModeOn()) {
				e.printStackTrace();
			}
			
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
                    .forEach(entry -> kv.setProperty(entry.getKey(), entry.getValue()));
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
        kv.asMap().forEach((key, val) -> result.put(key, val.toString()));

        return result;
    }

}
