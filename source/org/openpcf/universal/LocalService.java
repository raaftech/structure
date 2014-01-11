/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal;

import java.awt.*;

/**
 * LocalService represents a local service without a network connection.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-01-06
 */
public class LocalService extends Service {

    public LocalService(String name, Image icon, String vendor, String version, String path, String image) {
        super(name, icon, vendor, version, path, image);
    }
}
