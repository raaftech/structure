// Copyright © 2008-2014 RAAF Technology bv

package org.openpcf.universal.actor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openpcf.universal.base.BaseActor;

/**
 * The schema management verticle.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-09-11
 */
public class SchemaManager extends BaseActor {

    public MessageHandler initSchema() {
        return new MessageHandler() {
            public JsonNode exec(JsonNode args) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                result.put("result", "I am initSchema!");
                log("info", result.toString());
                return result;
            }

            public JsonNode help(JsonNode args) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                result.put("name", "initSchema");
                result.put("description", "initSchema initializes a Neo4j database with OpenPCF schema.");
                return result;
            }
        };
    }
}
