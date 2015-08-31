package com.raaftech.structure.node;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.annotations.SchemaPath;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.base.NodeActor;
import com.raaftech.structure.graph.InstanceNode;
import com.raaftech.structure.graph.repositories.InstanceNodeRepository;

public class InstanceNodeActor extends NodeActor<InstanceNode> {

    private InstanceNodeRepository instanceNodeRepository = Context.INSTANCE.getInstanceNodeRepository();

    private Logger log = LoggerFactory.getLogger(InstanceNodeActor.class);

    public InstanceNodeActor(InstanceNode node) {
        super(node);
    }

    @Override
    protected String getSecondaryAddress() {
        return null;
    }

    /**
     * Query the instancenode for results. Field "task" may
     * be given as an argument with a specific task ID to
     * filter on a particular task.
     * @return An array containing the queried results.
     */
    @SchemaPath("schema/node/instancenode/getresults.schema.json")
    public MessageHandler getResults() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();
                JsonNode args = request.get("args");
                InstanceNode instanceNode = getNode();

                ArrayNode allResults = result.putArray("data");
                if (instanceNode != null) {
                    if (args.get("task") == null) {
                        instanceNode.getResults().stream()
                        .forEachOrdered(r -> allResults.add(r.toJson()));
                    } else {
                        instanceNode.getResults(args.get("task").asText()).stream()
                        .forEachOrdered(r -> allResults.add(r.toJson()));
                    }
                }

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

    /**
     * This method takes an InstanceNode as argument and finds
     * child components of that InstanceNode of a particular type.
     * @return An array containing the found components.
     */
    public MessageHandler getComponentsByType() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();
                JsonNode args = request.get("args");
                String type = args.get("type").asText();

                Set<InstanceNode> found = instanceNodeRepository.findComponentsByType(uuid, type);
                if (found != null && found.size() > 0) {
                    ArrayNode foundComponents = result.putArray("data");
                    found.stream()
                    .map(n -> n.toJson())
                    .forEach(json -> foundComponents.add(json));
                } else {
                    result.put("error", "Couldn't find components of type \"" + type + "\".");
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
