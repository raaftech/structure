/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal;

import java.awt.*;

/**
 * Represents a network device (possibly with a MAC address).
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-01-06
 */
public class NetworkDevice extends Device {
    private String macaddr;

    public NetworkDevice(String name, Image icon, String vendor, String model, String revision, String macaddr) {
        super(name, icon, vendor, model, revision);
        this.macaddr = macaddr;
    }
}
