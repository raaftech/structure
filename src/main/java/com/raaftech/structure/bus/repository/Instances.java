package com.raaftech.structure.bus.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.annotations.SchemaPath;
import com.raaftech.structure.base.Configuration;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.Component;
import com.raaftech.structure.graph.Instance;
import com.raaftech.structure.graph.Type;
import com.raaftech.structure.graph.repositories.InstanceRepository;
import com.raaftech.structure.graph.repositories.TypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Instances extends Repository<Instance> {

    private InstanceRepository repository;
    private final static TypeRepository TYPE_REPOSITORY = Context.getTypeRepository();

    private Logger log = LoggerFactory.getLogger(Instances.class);

    public Instances(InstanceRepository repository) {
        super(repository);
        this.repository = repository;
    }


    /**
     * Query the last N Instances, filtering out Instances that are instruments.
     * @return A data array containing the last N created Instances. If we don't have N nodes, then return
     * as many as we have.
     */
    @SchemaPath("resource:/schema/repository/instances/nLastCreated.schema.json")
    @SuppressWarnings("unused")
    public MessageHandler nLastCreated() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();
                ObjectNode args = (ObjectNode)request.get("args");
                int n = args.get("amount").asInt();

                ArrayNode data = result.putArray("data");
                repository.getNLastCreated(n).stream()
                        .map(Instance::toJson)
                        .forEach(data::add);

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }
    /**
     * Persist an Instance to the database.
     * @return A JSON object containing the UUID of the new Instance
     * or an error field if something went wrong.
     */
    @SchemaPath("resource:/schema/repository/instances/putNode.schema.json")
    @SuppressWarnings("unused")
    public MessageHandler putNode() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();
                ObjectNode args = (ObjectNode)request.get("args");
                String isa = args.get("node").asText();
                Type type = TYPE_REPOSITORY.findByUuid(isa);
                // Remove the node field so it won't interfere with the Type schema.
                args.remove("node");

                try {
                    if (type.isOfType("Component")) {
                        Instance newInstance = new Component(args, type);
                        newInstance.save();
                        result.put("data", newInstance.getUuid());
                    } else {
                        // TODO: add support for other Instances
                    }
                } catch (RuntimeException e) {
                	log.error(e.getMessage());

        			if (Configuration.isTraceModeOn()) {
        				e.printStackTrace();
        			}
        			
        			result.put("error", e.getMessage());
                }

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

}
