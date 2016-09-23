package com.raaftech.structure.graph;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.repositories.TypeNodeRepository;
import com.raaftech.structure.helper.JsonSchemaBuilder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.fieldaccess.DynamicProperties;
import org.springframework.data.neo4j.fieldaccess.DynamicPropertiesContainer;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Give a description of TypeNode here.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-11-27
 */
@NodeEntity
public class TypeNode extends Node {

    final static Logger log = LoggerFactory.getLogger(TypeNode.class);

    @RelatedTo(type="ISA", direction = Direction.OUTGOING, elementClass = TypeNode.class, enforceTargetType = true)
    private TypeNode isa;

    @RelatedTo(type="ISA", direction = Direction.INCOMING, elementClass = TypeNode.class, enforceTargetType = true)
    @SuppressWarnings("unused")
    private Set<TypeNode> childIsaSet;

    private int glyphId;

    @RelatedTo(type="HAS_INSTRUMENT", direction = Direction.OUTGOING)
    @Fetch
    private Set<InstanceNode> instruments;

    @RelatedTo(type="IMPLEMENTS", direction = Direction.OUTGOING)
    @Fetch
    private Set<TypeNode> implementsProtocols;

    public JsonSchemaBuilder schema = new JsonSchemaBuilder();
    public DynamicProperties kv = new DynamicPropertiesContainer();

    // isaUuid exists only to temporarily save a UUID of a TypeNode's parent
    // in case the parent doesn't exist in the database yet.
    @Transient
    private String isaUuid;

    @Transient
    private TypeNodeRepository typeNodeRepository = Context.getTypeNodeRepository();

    /****************/
    /* Constructors */
    /****************/

    public TypeNode() {}

    public TypeNode(String name, String desc, int glyphId) {
        super(name, desc);
        this.glyphId = glyphId;
        initSchema();
    }

    public TypeNode(String name, String desc, String uuid) {
        super(name, desc, uuid);
        initSchema();
    }

    public TypeNode(String name, String desc, String uuid, int glyphId) {
        super(name, desc, uuid);
        this.glyphId = glyphId;
        initSchema();
    }

    /*******************/
    /* Private Methods */
    /*******************/

    private void initSchema() {
        schema.setType("object");
        schema.setAdditionalProperties(false);
        schema.addProperty("name", "string", true, "The name of this node.");
        schema.addProperty("desc", "string", true, "The description of this node.");
    }

    /******************/
    /* Public Methods */

    @Override
    public int hashCode() {
        if (getId() != null) {
            return getId().hashCode();
        } else {
            return super.hashCode();
        }
    }

    @Override
    public boolean equals(Object obj) {
        return hashCode() == obj.hashCode();
    }

    /******************/

    public Set<TypeNode> getChildIsaSet() {
        GraphDatabase graphDatabase = Context.getGraphDatabase();
        Transaction tx = graphDatabase.beginTx();
        Context.getNeo4jTemplate().fetch(childIsaSet);
        tx.success();
        tx.close();
        return childIsaSet;
    }

    public TypeNode setDescriptor(String key, String valueType, boolean required, String description, String defaultValue,
                                  String pattern) {
        TypeNode parentNode = getIsa();
        if (parentNode != null && parentNode.getSchema().containsProperty(key)) {
            throw new RuntimeException("Can't override parent schema.");
        } else {
            schema.addProperty(key, valueType, required, description)
                    .setDefault(defaultValue)
                    .setPattern(pattern);
        }

        return this;
    }

    public int getGlyphId() {
        return glyphId;
    }

    public String getHierarchyPath() {
        String path = typeNodeRepository.getHierarchyPath(getUuid());
        if (path != null) {
            return path.toLowerCase();
        } else {
            return null;
        }
    }

    public TypeNode getIsa() {
        // Sometimes the parent isn't properly loaded from the database
        // I don't know why exactly, and don't have time to find out. This is a workaround.
        // TODO: find out what's going on here
        if (isa == null || isa.getName() == null) {
            GraphDatabase graphDatabase = Context.getGraphDatabase();
            Transaction tx = graphDatabase.beginTx();
            Context.getNeo4jTemplate().fetch(isa);
            tx.success();
            tx.close();
        }
        return isa;
    }

    public TypeNode setIsa(TypeNode isa) {
        if (isa != null) {
            this.isa = isa;
            JsonSchemaBuilder parentConstraints = isa.getSchema();
            if (glyphId == 0) {
                glyphId = isa.glyphId;
            }
            if (parentConstraints != null) {
                try {
                    if (schema == null) {
                        schema = new JsonSchemaBuilder(parentConstraints.getAsObjectNode());
                    } else {
                        Iterator<String> propertyIterator = parentConstraints.getProperties().fieldNames();
                        while (propertyIterator.hasNext()) {
                            String propertyName = propertyIterator.next();
                            if (!schema.containsProperty(propertyName)) {
                                JsonNode propertyJson = parentConstraints.getProperties().get(propertyName);
                                schema.addProperty(propertyName, propertyJson.get("type").asText(),
                                        parentConstraints.isRequired(propertyName), propertyJson.get("description").asText());
                            }
                        }
                    }
                } catch (ProcessingException e) {
                	log.error(e.getMessage());

        			if (log.isTraceEnabled()) {
        				e.printStackTrace();
        			}
        		}
            }
        }
        return this;
    }

    public TypeNode setIsa(String isa) {
        // We can't assume our parentNode has been persisted to the database yet at this point.
        // If the parentNode doesn't exist in the database yet, we just save it's UUID to a
        // transient field for later use (at createNode time).
        // If it does exist, we create the relations and inherit constraints in setIsa(TypeNode isa).

        TypeNode parentNode = typeNodeRepository.findByUuid(isa);
        if (parentNode != null) {
            setIsa(parentNode);
        } else {
            // This means our parent node hasn't been persisted to DB yet. Save UUID for later.
            setIsaUuid(isa);
        }

        return this;
    }

    public String getIsaUuid() {
        return isaUuid;
    }

    public void setIsaUuid(String isaUuid) {
        this.isaUuid = isaUuid;
    }

    public DynamicProperties getKv() {
        return kv;
    }

    public JsonSchemaBuilder getSchema() {
        return schema;
    }

    public TypeNode addInstrument(InstanceNode instrument) {
        if (instruments == null) {
            instruments = new HashSet<>();
        }

        if (instrument.getInstanceOf().getName().equals("Instrument")) {
            instruments.add(instrument);
        }

        return this;
    }

    public TypeNode addImplementedProtocol(TypeNode protocol) {
        if (protocol == null) {
            log.error("Provided argument to addImplementedProtocol cannot be null.");
            return this;
        }
        if (!protocol.isOfType("Protocol")) {
            log.error("Can't add non-protocol type " + protocol.getName() + " as implemented protocol.");
            return this;
        }
        if (!isOfType("Software")) {
            log.error("Can't add an implemented protocol to a non-software type.");
            return this;
        }

        if (implementsProtocols == null) {
            implementsProtocols = new HashSet<>();
        }
        implementsProtocols.add(protocol);

        return this;
    }

    public boolean implementsProtocol(TypeNode protocol) {
        return implementsProtocols == null ? false : implementsProtocols.contains(protocol);
    }

    // fillDescriptor is used to set the value of a descriptor. For example: when modelling a particular CPU model
    // you know that it'll always have 4 cores, n bytes of cache, etc. This method is used to register that for a TypeNode.
    public TypeNode fillDescriptor(String key, JsonNode data) {
        ObjectNode propertySchema = (ObjectNode) schema.getProperties().get(key);
        // Check if we actually have this property.
        if (propertySchema != null) {
            try {
                JsonSchemaBuilder propertySchemaBuilder = new JsonSchemaBuilder((ObjectNode) schema.getProperties().get(key));
                // If the data we got validates against that specific part of the schema
                // we save it as a DynamicProperty and remove that property from the schema.
                if (propertySchemaBuilder.validateJson(data.get(key))) {
                    kv.setProperty(key, data.get(key).asText());
                    schema.removeProperty(key);
                } else {
                    throw new IllegalArgumentException("Couldn't fill descriptor of " + getName() + " with data, didn't match the schema.");
                }
            } catch (ProcessingException e) {
            	log.error(e.getMessage());

    			if (log.isTraceEnabled()) {
    				e.printStackTrace();
    			}
    			
                throw new RuntimeException("Couldn't create JsonSchemaBuilder");
            }
        } else {
            throw new IllegalArgumentException("TypeNode " + getName() + " has no descriptor '" + key + "'.");
        }

        return this;
    }

    public boolean isOfType(String type) {
        return typeNodeRepository.isOfType(getUuid(), type) > 0;
    }

    public TypeNode save() {
        typeNodeRepository.save(this);
        return this;
    }

    public ObjectNode toJson() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = mapper.createObjectNode();

        List<String> childIsaUuids = null;
        if (getChildIsaSet() != null) {
            getChildIsaSet().stream()
                    .map(Node::getUuid)
                    .collect(Collectors.toList());
        }

        json.put("name", getName());
        json.put("uuid", getUuid());
        json.put("glyphId", Integer.toHexString(glyphId));
        if (getIsa() != null) {
            json.put("isa", getIsa().getUuid());
        }
        json.put("desc", getDesc());
        try {
            if (getSchema() != null) {
                json.set("schema", getSchema().getAsObjectNode());
            }
            if (childIsaUuids != null) {
                json.put("children", mapper.writeValueAsString(childIsaUuids));
            }
        } catch (JsonProcessingException e) {
        	log.error(e.getMessage());

			if (log.isTraceEnabled()) {
				e.printStackTrace();
			}
		}

        json.put("_nodeType", getNodeType());

        return json;
    }
}
