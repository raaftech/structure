/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal;

import java.awt.*;

/**
 * The GraphicalAccessor class represents specialized NetworkedService classes
 * that give Graphical-Mode access to an underlying system.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-01-06
 */
public class GraphicalAccessor extends NetworkedService implements Accessable {

    public GraphicalAccessor(String name, Image icon, String vendor, String version, String path, String image, Listener listener) {
        super(name, icon, vendor, version, path, image, listener);
    }

    public void access() {
    }
}
