/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal;

/**
 * Represents a computer system.
 *
 * @author      Rubin Simons <rubin.simons@raaftech.com>
 * @since       2014-01-06
 * @version     0.6
 */
public class ComputerSystem extends Device implements Virtualizable {
    private NetworkDevice[] networkDevices;
    private OperatingSystem[] operatingSystems;
    private StorageDevice[] storageDevices;
    private MemoryModule[] memoryModules;
    private ProcessingUnit[] processingUnits;
    private GraphicsDevice[] graphicsDevices;
    private ManagementConsole[] managementConsoles;

    public ComputerSystem() {}
}
