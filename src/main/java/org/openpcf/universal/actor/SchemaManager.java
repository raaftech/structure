// Copyright © 2008-2014 RAAF Technology bv

package org.openpcf.universal.actor;

import org.openpcf.universal.base.BaseActor;
import org.vertx.java.core.json.JsonArray;

/**
 * The schema management verticle.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-09-11
 */
public class SchemaManager extends BaseActor {

    public MessageHandler initSchema() {
        return new MessageHandler() {
            public String exec(JsonArray args) {
                String msg = "I am initSchema";
                log("info", msg);
                return msg;
            }
        };
    }
}
