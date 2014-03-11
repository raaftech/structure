/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.model;

/**
 * Represents a graphics device.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-01-06
 */
public class GraphicsDevice extends Device {
    private int clockspeed;
    private int memspeed;
    private int memsize;
    private int fillrate;

    public GraphicsDevice() {};

    public GraphicsDevice(String name, String desc, String icon, String vendor, String model, String revision, int clockspeed, int memspeed, int memsize, int fillrate) {
        super(name, desc, icon, vendor, model, revision);
        this.clockspeed = clockspeed;
        this.memspeed = memspeed;
        this.memsize = memsize;
        this.fillrate = fillrate;
    }

    public int getClockspeed() {
        return clockspeed;
    }

    public void setClockspeed(int clockspeed) {
        this.clockspeed = clockspeed;
    }

    public int getMemspeed() {
        return memspeed;
    }

    public void setMemspeed(int memspeed) {
        this.memspeed = memspeed;
    }

    public int getMemsize() {
        return memsize;
    }

    public void setMemsize(int memsize) {
        this.memsize = memsize;
    }

    public int getFillrate() {
        return fillrate;
    }

    public void setFillrate(int fillrate) {
        this.fillrate = fillrate;
    }
}
