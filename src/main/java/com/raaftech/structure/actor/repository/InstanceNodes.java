package com.raaftech.structure.actor.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.actor.node.InstanceNodeActor;
import com.raaftech.structure.annotations.SchemaPath;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.InstanceNode;
import com.raaftech.structure.graph.Node;
import com.raaftech.structure.graph.repositories.GenericRepository;
import com.raaftech.structure.graph.repositories.InstanceNodeRepository;
import com.raaftech.structure.graph.repositories.NodeRepository;
import org.neo4j.graphdb.Transaction;

public class InstanceNodes extends Repository<InstanceNode> {

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
                    e.printStackTrace();
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
