package com.raaftech.structure.graph;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.repositories.TemplateNodeRepository;
import com.raaftech.structure.graph.repositories.TypeNodeRepository;
import com.raaftech.structure.helper.JsonSchemaBuilder;
import com.raaftech.structure.node.TemplateNodeActor;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.rx.java.ObservableHandler;
import io.vertx.rx.java.RxHelper;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import rx.Observable;

import java.util.*;
import java.util.stream.Collectors;

import static io.vertx.core.json.Json.mapper;

/**
 * Give a description of TemplateNode here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2015-03-09
 */
@NodeEntity
public class TemplateNode extends Node {

    final static Logger log = LoggerFactory.getLogger(TemplateNode.class);

    @RelatedTo(type = "TEMPLATE_FOR", direction = Direction.OUTGOING)
    @Fetch
    TypeNode isa;

    @RelatedTo(type = "HAS_COMPONENT", direction = Direction.OUTGOING)
    @Fetch
    Set<TemplateNode> hasComponents;

    @RelatedTo(type = "HAS_COMPONENT", direction = Direction.INCOMING)
    @Fetch
    TemplateNode parentComponent;

    boolean startOfTemplate;
    boolean required;
    boolean unique;

    @Transient
    TemplateNodeRepository templateNodeRepository = Context.INSTANCE.getTemplateNodeRepository();
    @Transient
    TypeNodeRepository typeNodeRepository = Context.INSTANCE.getTypeNodeRepository();

    /****************/
    /* Constructors */
    /****************/

    public TemplateNode() {}

    public TemplateNode(String isa) {
        super();
        TypeNode typeNode = typeNodeRepository.findByUuid(isa);
        if (typeNode != null) {
            this.name = typeNode.getName();
            this.desc = typeNode.getDesc();
            setIsa(typeNode);
        } else {
            throw new RuntimeException("Couldn't find ISA used to create new Template Node.");
        }
    }

    public TemplateNode(String name, String desc) {
        super(name, desc);
    }

    public TemplateNode(String name, String desc, String uuid) {
        super(name, desc, uuid);
    }

    /*******************/
    /* Private Methods */
    /*******************/

    private static void cascadeRemoval(TemplateNode child) {
        TemplateNodeRepository templateNodeRepository = Context.INSTANCE.getTemplateNodeRepository();
        child.hasComponents.stream()
        .forEach(TemplateNode::cascadeRemoval);
        templateNodeRepository.delete(child);
    }

    private JsonSchemaBuilder getArraySchema() {
        JsonSchemaBuilder arraySchema = new JsonSchemaBuilder()
                .setType("object")
                .setAdditionalProperties(false);
        arraySchema.addProperty("templateNodeUuid", "string", true, "UUID of the TemplateNode.").setPattern(getUuid());
        JsonSchemaBuilder data = arraySchema.addProperty("data", "object", true, "Data object containing the wrapping array.");
        JsonSchemaBuilder wrappingArray = data.addProperty("instances", "array", required, "Wrapping array.");

        if (required) {
            wrappingArray.setMinItems(1);
        }
        if (unique) {
            wrappingArray.setMaxItems(1);
        }

        return arraySchema;
    }

    // This is not directly called, use instantiate(Map<String, JsonNode> dataMap) in stead
    private String instantiate(TemplateNode template, InstanceNode parent, Map<String, JsonNode> dataMap, int depth) {
        if (!startOfTemplate && parent == null) {
            throw new IllegalArgumentException("Cannot instantiate a TemplateNode that is not the start of a template without a parent.");
        }

        JsonNode json = dataMap.get(template.getUuid());
        ArrayNode instances = (ArrayNode)json.get("instances");

        for (JsonNode instance : instances) {
            ObjectNode instanceData = (ObjectNode) instance;
            String typeUuid = null;
            final String typeOverrideKey = "_typeUuid";
            TypeNode typeNode = null;
            if (instanceData.has(typeOverrideKey)) {
                typeUuid = instanceData.get(typeOverrideKey).asText();
                instanceData.remove(typeOverrideKey);
                if (typeNodeRepository.isSubtype(template.isa.getUuid(), typeUuid) >= 1) {
                    typeNode = typeNodeRepository.findByUuid(typeUuid);
                } else {
                    throw new IllegalArgumentException("Type with UUID " + typeUuid + " is not a subtype of " + template.isa.getName() + ".");
                }
            }
            // Get the appropriate schema and validate
            JsonSchemaBuilder schema = typeNode != null ? typeNode.getSchema() : template.isa.getSchema();
            if (!schema.validateJson(instanceData)) {
                throw new IllegalArgumentException("Tried instantiating a template with invalid instantation data.");
            }

            // write back typeUuid, because we need it for instantiation
            if (typeUuid != null) {
                instanceData.put("typeUuid", typeUuid);
            }
        }

        String instanceUuid = null;
        JsonNode instanceDataArray = json.get("instances");

        if (instanceDataArray != null) {
            if (startOfTemplate) {
                ObjectNode instanceData = (ObjectNode)instanceDataArray.get(0);
                String typeUuid = instanceData.has("typeUuid") ? instanceData.get("typeUuid").asText() : isa.getUuid();
                instanceData.remove("typeUuid");
                final InstanceNode instanceNode = new InstanceNode((ObjectNode)instanceDataArray.get(0), typeUuid);
                instanceUuid = instanceNode.getUuid();

                template.hasComponents.stream()
                .forEach(t -> t.instantiate(t, instanceNode, dataMap, depth+1));
                instanceNode.save();
            }
            // If this TemplateNode has components (implies required and unique)
            else if (hasComponents.size() > 0) {
                ObjectNode instanceData = (ObjectNode)instanceDataArray.get(0);
                String typeUuid = instanceData.has("typeUuid") ? instanceData.get("typeUuid").asText() : isa.getUuid();
                instanceData.remove("typeUuid");
                final InstanceNode instanceNode = new InstanceNode((ObjectNode)instanceDataArray.get(0), typeUuid);
                instanceUuid = instanceNode.getUuid();
                if (parent != null) parent.addComponent(instanceNode);

                template.hasComponents.stream()
                .forEach(t -> t.instantiate(t, instanceNode, dataMap, depth+1));
                instanceNode.save();
            } else {
                instanceDataArray.forEach(data -> {
                    String typeUuid = data.has("typeUuid") ? data.get("typeUuid").asText() : isa.getUuid();
                    ((ObjectNode)data).remove("typeUuid");
                    InstanceNode instanceNode = new InstanceNode((ObjectNode)data, typeUuid);
                    if (parent != null) parent.addComponent(instanceNode);
                    instanceNode.save();
                });
            }
        }

        return instanceUuid;
    }

    private void recurseForArraySchemas(ArrayNode schema, Set<TemplateNode> templateNodes) {
        templateNodes.stream()
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .forEach(node -> {
                    schema.add(node.getArraySchema().getAsObjectNode());
                    if (node.hasComponents.size() > 0) {
                        recurseForArraySchemas(schema, node.hasComponents);
                    }
                });
    }

    /******************/
    /* Public Methods */
    /******************/

    public TemplateNode addComponent(TemplateNode component) {
        if (hasComponents == null) {
            hasComponents = new HashSet<>();
        }
        if (required && unique) {
            hasComponents.add(component);
            component.setParentComponent(this);
        } else {
            throw new RuntimeException("Cannot add a child TemplateNode to " + getName() + " (" + getUuid()
            + ") because " + getName() + " isn't set to required and unique.");
        }

        return this;
    }

    public TemplateNode discover(JsonNode args) {
        if (!isStartOfTemplate()) {
            throw new RuntimeException("Discovery can only be started on the start of a template");
        }

        EventBus eventBus = Context.INSTANCE.getVertx().eventBus();
        ObjectMapper mapper = new ObjectMapper();

        // OK, so this probably won't be readable for anyone (not even me five minutes from now)
        // Goal of the following routine is to create a Map that consists of an ObservableHandler,
        // an address of a Discovery Handler and the UUID of the TemplateNode connected to the DiscoHandler
        Transaction tx = Context.INSTANCE.getGraphDatabase().beginTx();
        Iterator<Map<String, String>> discoveryData = templateNodeRepository.getDiscoveryData(this.getUuid()).iterator();
        // Fairly certain there's a better type for this map.
        Map<ObservableHandler<AsyncResult<Message<JsonObject>>>, List<String>>
        discoveryHandlerMap = new HashMap<>();
        // Iterate over discoveryData (UUID + JSON containing Discovery Handler address)
        while(discoveryData.hasNext()) {
            Map<String, String> entry = discoveryData.next();
            // We'll use this map to collect the UUID + address
            HashMap<String, String> collect = new HashMap<>();
            entry.entrySet().stream()
            .forEach((e) -> {
                // If this entry is the kv entry parse it as JSON and get the plugin field
                if (e.getKey().equals("kv")) {
                    try {
                        collect.put("address", mapper.readTree(e.getValue()).get("plugin").asText());
                    } catch (Exception e1) {
            			log.error(e1.getMessage());

            			if (log.isTraceEnabled()) {
            				e1.printStackTrace();
            			}
            			
            			throw new RuntimeException("Error while building address map.");
                    }
                    // Else we're dealing with the uuid field which is already a string
                } else {
                    collect.put("uuid", e.getValue());
                }
            });
            // Put a new ObservableHandler, the UUID and the address in a map.
            discoveryHandlerMap.put(RxHelper.observableHandler(),
                    Arrays.asList(collect.get("uuid"), collect.get("address")));
        }
        tx.close();

        // Zip all observables to a JSON object that we can use to (probably) instantiate the template.
        // The JSON object won't always be able to instantiate a particular template, in that case the
        // discovery failed.
        Observable<JsonNode> zippedHandlers = Observable.zip(
                discoveryHandlerMap.entrySet().stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toList()),
                results -> {
                    ArrayNode instanceJson = mapper.createArrayNode();

                    for (Object obj: results) {
                        AsyncResult<Message<Object>> result = (AsyncResult<Message<Object>>) obj;
                        if (result.succeeded()) {
                            try {
                                instanceJson.add(mapper.readTree(result.result().body().toString()));
                            } catch (Exception e) {     
                    			log.error(e.getMessage());

                    			if (log.isTraceEnabled()) {
                    				e.printStackTrace();
                    			}
                    			
                            	throw new RuntimeException("Error while doing ArrayNode.add: " + e.getMessage());
                            }
                        } else {
                            throw new RuntimeException("Error while calling one of the discovery handlers.");
                        }
                    }

                    return instanceJson;
                });

        // Instantiate template using data from our zippedHandlers Observable
        zippedHandlers.subscribe(this::instantiate, fail -> log.error(fail.getMessage()));

        // Send  messages to all the DiscoveryHandlers using an ObservableHandler for callbacks
        discoveryHandlerMap.forEach((observableHandler, uuidAddressList) -> {
            JsonObject message = new JsonObject()
                    .put("msgh", "execute")
                    .put("args", new JsonObject(args.toString()).put("node", uuidAddressList.get(0)));
            eventBus.send("com.raaftech.structure.plugins." + uuidAddressList.get(1),
                    message, observableHandler.toHandler());
        });


        return this;
    }

    // Create a form layout for the generated form.
    // For now this will only create a form when a TemplateNode
    // isn't unique (else it'll just contain [ "*" ]).
    // TODO: Add functionality to create custom form layouts
    public ArrayNode getForm() {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode form = mapper.createArrayNode();

        ObjectNode formItems = form.addObject();
        ArrayNode fields = formItems.putArray("items");

        formItems.put("key", "instances");
        formItems.put("add", "New");
        if (unique) {
            formItems.putObject("style").put("add", "hidden");
        } else {
            formItems.putObject("style").put("add", "btn-success");
        }

        JsonSchemaBuilder isaSchema = isa.getSchema();
        Iterator<String> fieldIterator = isaSchema.getProperties().fieldNames();

        while (fieldIterator.hasNext()) {
            String fieldname = fieldIterator.next();
            fields.add("instances[]." + fieldname);
        }

        return form;
    }

    public Set<TemplateNode> getHasComponents() {
        return hasComponents;
    }

    public TemplateNode getParentComponent() {
        return parentComponent;
    }

    public TemplateNode setParentComponent(TemplateNode parentComponent) {
        this.parentComponent = parentComponent;

        return this;
    }

    public JsonSchemaBuilder getSchema() {
        // We're going to wrap our schema in another schema. This is done to allow for
        // multiple instantiations of one TypeNode (if the template allows it).
        // The reason we wrap all the schema's this way is so we have uniform JSON schema
        // for all types of TypeNodes. We'll just set different min/maxItems on it.
        JsonSchemaBuilder wrappingSchema;
        try {
            wrappingSchema = new JsonSchemaBuilder()
                    .setType("object")
                    .setAdditionalProperties(false);
            JsonSchemaBuilder wrappingArray = wrappingSchema.addProperty("instances", "array", required, "Wrapping array.")
                    .setItemToSchema(isa.getSchema().getAsObjectNode());

            if (required) {
                wrappingArray.setMinItems(1);
            }
            if (unique) {
                wrappingArray.setMaxItems(1);
            }
        } catch (ProcessingException e) {
            // Shouldn't be possible.
        	log.error(e.getMessage());

			if (log.isTraceEnabled()) {
				e.printStackTrace();
			}
			
        	throw new RuntimeException("TypeNode didn't contain a valid JSON schema.");
        }
        return wrappingSchema;
    }

    public String instantiate(JsonNode instanceData) {
        // Initialize schema validation data
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "array");
        ArrayNode schemaArrays = schema.putObject("items").putArray("oneOf");
        schemaArrays.add(getArraySchema().getAsObjectNode());
        recurseForArraySchemas(schemaArrays, hasComponents);
        schema.put("additionalItems", false);
        JsonSchemaBuilder jsonSchema;

        try {
            jsonSchema = new JsonSchemaBuilder(schema);
            if (!jsonSchema.validateJson(instanceData)) {
                throw new RuntimeException("Instantiating template failed on first validation.");
            }
        } catch (ProcessingException e) {
            log.error("Error constructing schema: " + e.getMessage());
            
            if (log.isTraceEnabled()) {
                e.printStackTrace();
            }
            
            throw new RuntimeException("Error while constructing schema, aborting.");
        }
        // Map stuff
        HashMap<String, JsonNode> uuidToDataMap = new HashMap<>();
        Iterator<JsonNode> elements = instanceData.elements();
        while (elements.hasNext()) {
            JsonNode element = elements.next();
            uuidToDataMap.put(element.get("templateNodeUuid").asText(), element.get("data"));
        }

        Transaction tx = Context.INSTANCE.getGraphDatabase().beginTx();
        String uuid = null;
        try {
            uuid = instantiate(this, null, uuidToDataMap, 0);
            tx.success();
        } catch (Exception e) {
            tx.failure();
            
            log.error(e.getMessage());

			if (log.isTraceEnabled()) {
				e.printStackTrace();
			}
			
            throw new RuntimeException("Transaction failed.");
        } finally {
            tx.close();
        }
        return uuid;
    }

    public boolean isRequired() {
        return required;
    }

    public TemplateNode setRequired(boolean required) {
        this.required = required;

        return this;
    }

    public boolean isStartOfTemplate() {
        return startOfTemplate;
    }

    public TemplateNode setStartOfTemplate(boolean startOfTemplate) {
        this.startOfTemplate = startOfTemplate;

        return this;
    }

    public boolean isUnique() {
        return unique;
    }

    public TemplateNode setUnique(boolean unique) {
        this.unique = unique;

        return this;
    }

    /**
     * Removes all components from a TemplateNode.
     * @return The number of components removed.
     */
    public long removeAllComponents() {
        Set<TemplateNode> copy = hasComponents.stream().collect(Collectors.toSet());
        long count = copy.stream()
                .peek(this::removeComponent)
                .count();
        hasComponents.clear();

        return count;
    }

    public boolean removeComponent(TemplateNode component) {
        // UUID will is unique, so we can safely assume
        // the resulting list will have only 1 element.
        TemplateNodeRepository templateNodeRepository = Context.INSTANCE.getTemplateNodeRepository();
        List<TemplateNode> placeholder = hasComponents.stream()
                .filter(c -> c.getUuid().equals(component.getUuid()))
                .collect(Collectors.toList());
        TemplateNode found = placeholder.get(0);

        found.hasComponents.stream()
        .forEach(TemplateNode::cascadeRemoval);

        templateNodeRepository.delete(found);

        return hasComponents.remove(found);
    }

    public TemplateNode save() {
        Transaction tx = Context.INSTANCE.getGraphDatabase().beginTx();
        TemplateNodeRepository templateNodeRepository = Context.INSTANCE.getTemplateNodeRepository();
        boolean newNode = getId() == null || getId() < 0;

        try {
            if (startOfTemplate && !(required && unique)) {
                throw new RuntimeException("Can't save TemplateNode " + getName() + " as the start of a template "
                        + "because the start of a template should also be unique and required.");
            }
            templateNodeRepository.save(this);
            tx.success();
        } catch (Exception e) {
            tx.failure();
            
            log.error(e.getMessage());

			if (log.isTraceEnabled()) {
				e.printStackTrace();
			}
			
            throw new RuntimeException("Transaction failed.");
        } finally {
            tx.close();
        }

        if (newNode) {
            Context.INSTANCE.getVertx().deployVerticle(
                    new TemplateNodeActor(this));
        }

        return this;
    }

    public TemplateNode setIsa(TypeNode isa) {
        this.isa = isa;

        return this;
    }

    public TemplateNode setIsa(String isa) {
        TypeNode found = typeNodeRepository.findByUuid(isa);
        if (found != null) {
            setIsa(found);
        } else {
            throw new RuntimeException("Could not find TypeNode " + isa + " to set ISA relation.");
        }

        return this;
    }

    public ObjectNode toJson() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = mapper.createObjectNode();

        json.put("name", getName());
        json.put("desc", getDesc());
        json.put("uuid", getUuid());
        json.put("startOfTemplate", startOfTemplate);
        json.put("required", required);
        json.put("unique", unique);
        json.put("isa", isa.getUuid());
        json.set("form", getForm());
        try {
            json.put("schema", mapper.writeValueAsString(getSchema().getAsObjectNode()));
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());

			if (log.isTraceEnabled()) {
				e.printStackTrace();
			}	
        }

        if (hasComponents != null && hasComponents.size() > 0) {
            ArrayNode children = json.putArray("children");
            hasComponents.stream()
                    .sorted((a, b) -> a.getId().compareTo(b.getId()))
                    .forEach(component -> children.add(component.toJson()));
        }

        json.put("_nodeType", getNodeType());

        return json;
    }
}
