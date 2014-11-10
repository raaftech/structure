/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.model;

/**
 * Represents a generic service.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-01-06
 */
public class Service extends Software implements Stoppable, Startable, Restartable, Installable, Removable {

    public Service() {
    };

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

    public void install() {
    }

    public void remove() {
    }

}
