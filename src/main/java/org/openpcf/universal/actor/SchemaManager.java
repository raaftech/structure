// Copyright © 2008-2014 RAAF Technology bv

package org.openpcf.universal.actor;

import org.openpcf.universal.base.BaseActor;
import org.vertx.java.core.json.JsonObject;

/**
 * The schema management verticle.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-09-11
 */
public class SchemaManager extends BaseActor {

    public MessageHandler initSchema() {
        return new MessageHandler() {
            public JsonObject exec(JsonObject args) {
                JsonObject result = new JsonObject("\"result\": \"I am initSchema!\"");
                log("info", result.toString());
                return result;
            }

            public JsonObject help(JsonObject args) {
                JsonObject help = new JsonObject();
                help.putString("name", "initSchema");
                help.putObject("synopsis", new JsonObject("{ \"msgh\": \"initSchema\" }"));
                help.putString("description", "initSchema initializes a Neo4j database with OpenPCF schema.");
                return help;
            }
        };
    }
}
