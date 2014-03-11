/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.model;

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

    public Hypervisor() {};
    public Hypervisor(String name, String desc, String icon, String vendor, String version, String path, String image, String vmhome, String vmdata) {
        super(name, desc, icon, vendor, version, path, image);
        this.vmhome = vmhome;
        this.vmdata = vmdata;
    }

    public String getVmhome() {
        return vmhome;
    }

    public void setVmhome(String vmhome) {
        this.vmhome = vmhome;
    }

    public String getVmdata() {
        return vmdata;
    }

    public void setVmdata(String vmdata) {
        this.vmdata = vmdata;
    }
}
