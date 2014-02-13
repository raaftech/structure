/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.scaffolding;

import java.awt.*;

/**
 * Represents a computer system.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-01-06
 */
public class ComputerSystem extends Device implements Virtualizable {
    private NetworkDevice networkDevice;
    private OperatingSystem operatingSystem;
    private StorageDevice storageDevice;
    private MemoryModule memoryModule;
    private ProcessingUnit processingUnit;
    private GraphicsDevice graphicsDevice;
    private ManagementConsole managementConsole;

    public ComputerSystem(String name, Image icon, String vendor, String model, String revision, NetworkDevice networkDevice, OperatingSystem operatingSystem, StorageDevice storageDevice, MemoryModule memoryModule, ProcessingUnit processingUnit, GraphicsDevice graphicsDevice, ManagementConsole managementConsole) {
        super(name, icon, vendor, model, revision);
        this.networkDevice = networkDevice;
        this.operatingSystem = operatingSystem;
        this.storageDevice = storageDevice;
        this.memoryModule = memoryModule;
        this.processingUnit = processingUnit;
        this.graphicsDevice = graphicsDevice;
        this.managementConsole = managementConsole;
    }
}
