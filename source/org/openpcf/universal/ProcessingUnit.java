/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal;

import java.awt.*;

/**
 * Represents a processor.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-01-06
 */
public class ProcessingUnit extends Device {
    private int hertz;
    private int cache;
    private int cores;

    public ProcessingUnit(String name, Image icon, String vendor, String model, String revision, int hertz, int cache, int cores) {
        super(name, icon, vendor, model, revision);
        this.hertz = hertz;
        this.cache = cache;
        this.cores = cores;
    }
}
