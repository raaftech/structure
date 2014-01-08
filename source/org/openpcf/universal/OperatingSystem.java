/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal;

/**
 * Represents an Operating System running on a computer or an embedded
 * device like a management console.
 *
 * @author      Rubin Simons <rubin.simons@raaftech.com>
 * @since       2014-01-06
 * @version     0.6
 */
public class OperatingSystem extends Software implements Checkable, Discoverable, Stopable, Restartable {
    private Boolean dormant;
    private Service[] assignedServices;
    private Configuration[] configuredDevices;

    public OperatingSystem() {}

    public void check() {}
    public void discover() {}
    public void stop() {}
    public void restart() {}
}
