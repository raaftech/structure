package com.raaftech.structure.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.annotations.SchemaPath;
import com.raaftech.structure.graph.Component;
import com.raaftech.structure.graph.repositories.ComponentRepository;

public class Components extends Repository<Component> {

    private final ComponentRepository repository;

    public Components(ComponentRepository repository) {
        super(repository);
        this.repository = repository;
    }

    /**
     * Query the last N Instances, filtering out Instances that are instruments.
     *
     * @return A data array containing the last N created Instances. If we don't have N nodes, then return
     * as many as we have.
     */
    @SchemaPath("schema/repository/instances/nLastCreated.schema.json")
    @SuppressWarnings("unused")
    public MessageHandler nLastCreated() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();
                ObjectNode args = (ObjectNode) request.get("args");
                int n = args.get("amount").asInt();

                ArrayNode data = result.putArray("data");
                repository.getNLastCreated(n).stream()
                        .map(Component::toJson)
                        .forEach(data::add);

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }
}

