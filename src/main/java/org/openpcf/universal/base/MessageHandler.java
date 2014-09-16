// Copyright © 2008-2014 RAAF Technology bv

package org.openpcf.universal.base;

import org.vertx.java.core.json.JsonArray;

/**
 * Give a description of MessageHandler here.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-09-15
 */
public interface MessageHandler {
    public String exec(JsonArray args);
}
