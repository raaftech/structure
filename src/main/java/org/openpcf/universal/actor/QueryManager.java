// Copyright © 2008-2014 RAAF Technology bv

package org.openpcf.universal.actor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openpcf.universal.base.BaseActor;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

/**
 * The query management verticle.
 *
 * @author Maurice Schillemans <maurice.schillemans@raaftech.com>
 * @since 2014-09-24
 */
public class QueryManager extends BaseActor {

    public MessageHandler runQuery() {
        return new MessageHandler() {
            public JsonNode exec(JsonNode request) {
                ObjectNode query = JsonNodeFactory.instance.objectNode();
                query.put("query", "match (c{name:'SSH'})<-[:ISA]-(a) return a");
                JsonObject jquery = new JsonObject(query.toString());

                vertx.eventBus().send("org.openpcf.neo4vertx.cypher.query", jquery, new Handler<Message<JsonObject>>() {
                    public void handle(Message<JsonObject> message) {
                        String meh = message.body().toString();
                        log("info", "I received a reply " + meh);
                    }
                });

                return response(request, query);
            }

            public JsonNode help(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                result.put("name", "runQuery");
                result.put("description", "runQuery executes a query in the Neo4j database.");
                return result;
            }
        };
    }
}
