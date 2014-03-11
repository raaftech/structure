/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.model;

/**
 * Represents a (serial) Console device.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-01-06
 */
public class ConsoleDevice extends Device {
    private int baudRate;
    private int dataBits;
    private String parity;
    private int stopBits;

    public ConsoleDevice(String name, String desc, String icon, String vendor, String model, String revision, int baudRate, int dataBits, String parity, int stopBits) {
        super(name, desc, icon, vendor, model, revision);
        this.baudRate = baudRate;
        this.dataBits = dataBits;
        this.parity = parity;
        this.stopBits = stopBits;
    }
}
