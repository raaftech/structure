/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.scaffolding;

import java.awt.*;

/**
 * Represents a generic service.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-01-06
 */
public class Service extends Software implements Checkable, Discoverable, Stopable, Startable, Restartable, Createable, Destroyable {
    private String path;
    private String image;

    public Service(String name, String icon, String vendor, String version, String path, String image) {
        super(name, icon, vendor, version);
        this.path = path;
        this.image = image;
    }

    public void check() {
    }

    public void discover() {
    }

    public void stop() {
    }

    public void start() {
    }

    public void restart() {
    }

    public void create() {
    }

    public void destroy() {
    }

}
