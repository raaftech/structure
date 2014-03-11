/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.model;

/**
 * Represents an abstract Device.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-01-06
 */
abstract class Device extends Component {
    private String vendor;
    private String model;
    private String revision;

    public Device() {};
    public Device(String name, String desc, String icon, String vendor, String model, String revision) {
        super(name, desc, icon);
        this.vendor = vendor;
        this.model = model;
        this.revision = revision;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }
}
