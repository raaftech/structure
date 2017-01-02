// Copyright © 2008-2016 RAAF Technology bv

package com.raaftech.structure.base;

/**
 * The Configuration contains application wide configuration options for structure.
 *
 * @author Freek Alleman <freek.alleman@raaftech.com>
 * @since 2016-09-30
 */
public enum Configuration {
    INSTANCE;

    private static boolean traceMode = false;        // enable printStackTrace()
    private static boolean unsafeMode = false;       // enable "unsafe" mode

    public static boolean isTraceModeOn() { return traceMode; }
    public static void setTraceMode(boolean b) { traceMode = b; }

    public static boolean isUnsafeModeOn() { return unsafeMode; }
    public static void setUnsafeMode(boolean b) { unsafeMode = b; }
}

