/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal;

import java.awt.*;

/**
 * Represents a Hypervisor service.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-01-06
 */
public class Hypervisor extends LocalService {
    private String vmhome;
    private String vmdata;

    public Hypervisor(String name, Image icon, String vendor, String version, String path, String image, String vmhome, String vmdata) {
        super(name, icon, vendor, version, path, image);
        this.vmhome = vmhome;
        this.vmdata = vmdata;
    }
}
