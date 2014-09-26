// Copyright © 2008-2014 RAAF Technology bv

package org.openpcf.universal.actor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openpcf.universal.base.BaseActor;

/**
 * The frame management actor.
 *
 * @author Maurice Schillemans <maurice.schillemans@raaftech.com>
 * @since 2014-09-24
 */
public class FrameHelper extends BaseActor {

    private String setName(JsonNode args, String pFrame) {
        if(args.has("name")) {
            return args.get("name").asText();
        } else {
            return pFrame;
        }
    }

    public MessageHandler component() {
        return new MessageHandler() {
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                JsonNode args = request.get("args");
                String name = setName(args, "Component");

                result.put("name", name);
                result.put("cypher", cypherQuery("MATCH (p:Component)<-[:ISA]-(c) WHERE p.name = '" + name + "' RETURN c"));

                return response(request, result, 0);
            }

            public JsonNode help(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                result.put("name", "component");
                result.put("description", "Component gets the children of a component type object.");
                return result;
            }
        };
    }

    public MessageHandler node() {
        return new MessageHandler() {
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                JsonNode args = request.get("args");
                String name = setName(args, "Component");

                result.put("name", name);
                result.put("cypher", cypherQuery("MATCH (p) WHERE p.name = '" + name + "' RETURN p, labels(p)"));

                return response(request, result, 0);
            }

            public JsonNode help(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                result.put("name", "node");
                result.put("description", "Node gets all properties and labels of a Neo4j node.");
                return result;
            }
        };
    }

    public MessageHandler search() {
        return new MessageHandler() {
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                JsonNode args = request.get("args");
                String name = setName(args, "Component");

                result.put("name", name);
                result.put("cypher", cypherQuery("MATCH (p) WHERE p.name =~ '.*(?i)" + name + ".*' RETURN p"));

                return response(request, result, 0);
            }

            public JsonNode help(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                result.put("name", "search");
                result.put("description", "Search Neo4j database with a given parameter.");
                return result;
            }
        };
    }
}
