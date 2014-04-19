/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.model;

import java.util.Set;

/**
 * Represents an assembly of devices, like an enclosure, a
 * computer system, a network switch or a management
 * console, etc.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-01-06
 */
public class AssembledDevice extends Device implements Virtualizable {

    // What the assembly consists of, device-wise.
    private Set<ProcessingUnit> processingUnits;
    private Set<MemoryModule> memoryModules;
    private Set<StorageDevice> storageDevices;
    private Set<NetworkDevice> networkDevices;
    private Set<GraphicsDevice> graphicsDevices;
    private Set<ConsoleDevice> consoleDevices;

    // What the assembly runs
    private OperatingSystem installedOperatingSystem;

    // Further embedded devices (that also might run something)
    private Set<AssembledDevice> embeddedDevices;

    public AssembledDevice() {}
}
