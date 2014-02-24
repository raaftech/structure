/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.scaffolding;

import java.awt.*;

/**
 * Represents a storage device, like a harddisk. Needs work!
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-01-06
 */
public class StorageDevice extends Device {
    private int size;

    public StorageDevice(String name, String icon, String vendor, String model, String revision, int size) {
        super(name, icon, vendor, model, revision);
        this.size = size;
    }
}
