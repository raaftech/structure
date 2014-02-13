/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.scaffolding;

import java.awt.*;

/**
 * Represents a memory module in a computer system.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-01-06
 */
public class MemoryModule extends Device {
    int size;

    public MemoryModule(String name, Image icon, String vendor, String model, String revision, int size) {
        super(name, icon, vendor, model, revision);
        this.size = size;
    }
}
