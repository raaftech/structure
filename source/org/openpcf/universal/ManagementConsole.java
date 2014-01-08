/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal;

/**
 * Represents a management interface to a computer system or device.
 * Think ILO's, DRAC's, HPMP's, Areca management GUIs, etc.
 *
 * @author      Rubin Simons <rubin.simons@raaftech.com>
 * @since       2014-01-06
 * @version     0.6
 */
public class ManagementConsole extends Device {
    private NetworkDevice[] networkDevices;
    private OperatingSystem[] operatingSystems;

    public ManagementConsole() {}
}
