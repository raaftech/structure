/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.model;

/**
 * Represents a memory module in a computer system.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-01-06
 */
public class MemoryModule extends Device {
    int size;

    public MemoryModule(String name, String desc, String icon, String vendor, String model, String revision, int size) {
        super(name, desc, icon, vendor, model, revision);
        this.size = size;
    }
}
