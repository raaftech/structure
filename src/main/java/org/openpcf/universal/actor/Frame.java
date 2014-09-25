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
public class Frame extends BaseActor {

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
                ObjectNode queryObject = JsonNodeFactory.instance.objectNode();
                JsonNode args = request.get("args");

                String name = setName(args, "Component");
                String query = "MATCH (p:Component)<-[:ISA]-(c) WHERE p.name = '" + name + "' RETURN c";

                queryObject.put("query", query);

                result.put("name", name);
                result.put("cypher", cypherQuery(queryObject));

                return response(request, result, 0);
            }

            public JsonNode help(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                result.put("name", "component");
                result.put("description", "component gets the children of the component.");
                return result;
            }
        };
    }

    public MessageHandler search() {
        return new MessageHandler() {
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                ObjectNode queryObject = JsonNodeFactory.instance.objectNode();
                JsonNode args = request.get("args");

                String name = setName(args, "Component");
                String query = "MATCH (p) WHERE p.name =~ '.*(?i)" + name + ".*' RETURN p";

                queryObject.put("query", query);

                result.put("name", name);
                result.put("cypher", cypherQuery(queryObject));

                return response(request, result, 0);
            }

            public JsonNode help(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                result.put("name", "search");
                result.put("description", "search Neo4j database with given parameter.");
                return result;
            }
        };
    }
}
