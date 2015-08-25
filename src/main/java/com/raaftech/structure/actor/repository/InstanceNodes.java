package com.raaftech.structure.actor.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.annotations.SchemaPath;
import com.raaftech.structure.graph.InstanceNode;
import com.raaftech.structure.graph.repositories.InstanceNodeRepository;

public class InstanceNodes extends Repository<InstanceNode> {

    private Logger log = LoggerFactory.getLogger(InstanceNodes.class);

    private InstanceNodeRepository repository;

    public InstanceNodes(InstanceNodeRepository repository) {
        super(repository);
        this.repository = repository;
    }

    /**
     * Persist an InstanceNode to the database.
     * @return A JSON object containing the UUID of the new InstanceNode
     * or an error field if something went wrong.
     */
    @SchemaPath("schema/repository/instancenodes/putNode.schema.json")
    public MessageHandler putNode() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();
                ObjectNode args = (ObjectNode)request.get("args");
                String isa = args.get("node").asText();
                // Remove the node field so it won't interfere with the TypeNode schema.
                args.remove("node");

                try {
                    InstanceNode newInstanceNode = new InstanceNode(args, isa);
                    newInstanceNode.save();
                    result.put("data", newInstanceNode.getUuid());
                } catch (RuntimeException e) {
                    log.error(e.getMessage());
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
