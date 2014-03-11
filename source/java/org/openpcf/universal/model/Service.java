/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.model;

/**
 * Represents a generic service.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-01-06
 */
abstract class Service extends Software implements Checkable, Discoverable, Stoppable, Startable, Restartable, Createable, Destroyable {
    private String path;
    private String image;

    public Service(String name, String desc, String icon, String vendor, String version, String path, String image) {
        super(name, desc, icon, vendor, version);
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
