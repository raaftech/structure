/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.model;

/**
 * Represents software in the most generic sense of the word.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-01-06
 */
abstract class Software extends Component {
    private String vendor;
    private String version;

    public Software() {};

    public Software(String name, String desc, String icon, String vendor, String version) {
        super(name, desc, icon);
        this.vendor = vendor;
        this.version = version;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
