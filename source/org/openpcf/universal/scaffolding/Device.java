/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.scaffolding;

import java.awt.*;

/**
 * Represents an abstract Device.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-01-06
 */
public class Device extends Component {
    private String vendor;
    private String model;
    private String revision;

    public Device(String name, Image icon, String vendor, String model, String revision) {
        super(name, icon);
        this.vendor = vendor;
        this.model = model;
        this.revision = revision;
    }
}
