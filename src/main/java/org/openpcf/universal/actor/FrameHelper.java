// Copyright © 2008-2014 RAAF Technology bv

package org.openpcf.universal.actor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openpcf.universal.base.BaseActor;

/**
 * The FrameHelper actor.
 *
 * @author Maurice Schillemans <maurice.schillemans@raaftech.com>
 * @since 2014-09-24
 */
public class FrameHelper extends BaseActor {

    private String setName(JsonNode args, String pFrame) {
        if (args.has("name")) {
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

                return response(request, result);
            }

            public JsonNode help(JsonNode request) {
                return jsonFromResource("help/framehelper.component.json");
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

                return response(request, result);
            }

            public JsonNode help(JsonNode request) {
                return jsonFromResource("help/framehelper.node.json");
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

                return response(request, result);
            }

            public JsonNode help(JsonNode request) {
                return jsonFromResource("help/framehelper.search.json");
            }
        };
    }
}
