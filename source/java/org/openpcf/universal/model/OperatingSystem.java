/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.model;

import java.util.Set;

/**
 * Represents an Operating System running on a computer or an embedded
 * device like a management console.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-01-06
 */
public class OperatingSystem extends Software implements Checkable, Discoverable, Stoppable, Restartable {

    private Set<Device> configuredDevices;
    private Set<Service> configuredServices;

    public OperatingSystem() {};

    public void check() {
    }

    public void discover() {
    }

    public void stop() {
    }

    public void restart() {
    }
}
