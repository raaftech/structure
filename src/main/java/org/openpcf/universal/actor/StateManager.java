package org.openpcf.universal.actor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openpcf.universal.base.BaseActor;

import java.util.Iterator;

/**
 * The StateManager actor.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-10-15
 */
public class StateManager extends BaseActor {

    public void printAll(JsonNode node) {
        Iterator<String> fieldNames = node.fieldNames();
        while(fieldNames.hasNext()){
            String fieldName = fieldNames.next();
            JsonNode fieldValue = node.get(fieldName);
            if (fieldValue.isObject()) {
                System.out.println(fieldName + " :");
                printAll(fieldValue);
            } else {
                String value = fieldValue.asText();
                System.out.println(fieldName + " : " + value);
            }
        }
    }

    public MessageHandler check() {
        return new MessageHandler() {
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                JsonNode args = request.get("args");
                String name = getArgs(args, "name", "Default");

                result.put("cypher", cypherQuery("MATCH (p:Instance)-[r:HAS*]->(c) WHERE p.name = '" + name + "' RETURN p,r,c"));

                return response(request, result);
            }

            public JsonNode help(JsonNode request) {
                return jsonFromResource("help/statemanager.check.json");
            }
        };
    }
}
