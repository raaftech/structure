/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.model;

/**
 * Represents an Operating System running on a computer or an embedded
 * device like a management console.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-01-06
 */
public class OperatingSystem extends Software implements Checkable, Discoverable, Stoppable, Restartable {
    private Boolean dormant;
    private Service assignedService;
    private Configuration configuredDevice;

    public OperatingSystem(String name, String desc, String icon, String vendor, String version, Boolean dormant, Service assignedService, Configuration configuredDevice) {
        super(name, desc, icon, vendor, version);
        this.dormant = dormant;
        this.assignedService = assignedService;
        this.configuredDevice = configuredDevice;
    }

    public void check() {
    }

    public void discover() {
    }

    public void stop() {
    }

    public void restart() {
    }
}
