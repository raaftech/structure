/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.model;

/**
 * Represents a management interface to a computer system or device.
 * Think ILO's, DRAC's, HPMP's, Areca management GUIs, etc.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-01-06
 */
public class ManagementConsole extends Device {
    private NetworkDevice networkDevice;
    private OperatingSystem operatingSystem;

    public ManagementConsole(String name, String icon, String vendor, String model, String revision, NetworkDevice networkDevice, OperatingSystem operatingSystem) {
        super(name, icon, vendor, model, revision);
        this.networkDevice = networkDevice;
        this.operatingSystem = operatingSystem;
    }
}
