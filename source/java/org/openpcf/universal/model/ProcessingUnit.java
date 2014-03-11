/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.model;

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

    public ProcessingUnit() {};
    
    public ProcessingUnit(String name, String desc, String icon, String vendor, String model, String revision, int hertz, int cache, int cores) {
        super(name, desc, icon, vendor, model, revision);
        this.hertz = hertz;
        this.cache = cache;
        this.cores = cores;
    }

    public int getHertz() {
        return hertz;
    }

    public void setHertz(int hertz) {
        this.hertz = hertz;
    }

    public int getCache() {
        return cache;
    }

    public void setCache(int cache) {
        this.cache = cache;
    }

    public int getCores() {
        return cores;
    }

    public void setCores(int cores) {
        this.cores = cores;
    }
}
