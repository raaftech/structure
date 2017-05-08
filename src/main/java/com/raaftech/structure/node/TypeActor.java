package com.raaftech.structure.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.base.NodeActor;
import com.raaftech.structure.graph.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Give a description of TypeActor here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2015-08-12
 */
public class TypeActor extends NodeActor<Type> {

    private String hierarchyPath;

    private Logger log = LoggerFactory.getLogger(TypeActor.class);

    public TypeActor(Type node) {
        super(node);
        this.hierarchyPath = node.getHierarchyPath();
    }

    @Override
    protected String getSecondaryAddress() {
        return hierarchyPath;
    }

    // Next methods are exposed on the eventbus

    // Get the hierarchical path of the Type
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

    // Get the Type's schema
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

    // Get an array of all the types one level below
    @SuppressWarnings("unused")
    public MessageHandler getSubtypes() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();
                ArrayNode data = result.putArray("data");

                getNode().getChildIsaSet().stream()
                        .forEach(child -> data.add(child.toJson()));

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

     // Get all the incoming and outgoing constraints for a type.
    public MessageHandler getConstraints() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();
                ObjectNode data = result.putObject("data");
                data.set("incoming", mapper.valueToTree(getNode().getIncomingConstraints()));
                data.set("outgoing", mapper.valueToTree(getNode().getOutgoingConstraints()));

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }
}
