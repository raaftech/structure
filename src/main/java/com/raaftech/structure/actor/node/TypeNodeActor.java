package com.raaftech.structure.actor.node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.base.NodeActor;
import com.raaftech.structure.graph.TypeNode;
import com.raaftech.structure.graph.repositories.TypeNodeRepository;

/**
 * Give a description of TypeNodeActor here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2015-08-12
 */
public class TypeNodeActor extends NodeActor<TypeNode> {

    private Logger log = LoggerFactory.getLogger(TypeNodeActor.class);

    private TypeNodeRepository typeNodeRepository = Context.INSTANCE.getTypeNodeRepository();
    private String hierarchyPath;

    public TypeNodeActor(TypeNode node) {
        super(node);
        this.hierarchyPath = node.getHierarchyPath();
    }

    @Override
    protected String getSecondaryAddress() {
        return hierarchyPath;
    }

    // Next methods are exposed on the eventbus

    // Get the hierarchical path of the TypeNode
    public MessageHandler getPath() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();
                result.put("data", hierarchyPath);
                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

    // Get the TypeNode's schema
    public MessageHandler getSchema() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();
                result.set("data", getNode().getSchema().getAsObjectNode());
                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }
}
