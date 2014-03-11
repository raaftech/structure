/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.model;

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
    private ConsoleDevice consoleDevice;
    private ManagementConsole managementConsole;

    public ComputerSystem() {};

    public ComputerSystem(String name, String desc, String icon, String vendor, String model, String revision, NetworkDevice networkDevice, OperatingSystem operatingSystem, StorageDevice storageDevice, MemoryModule memoryModule, ProcessingUnit processingUnit, GraphicsDevice graphicsDevice, ConsoleDevice consoleDevice, ManagementConsole managementConsole) {
        super(name, desc, icon, vendor, model, revision);
        this.networkDevice = networkDevice;
        this.operatingSystem = operatingSystem;
        this.storageDevice = storageDevice;
        this.memoryModule = memoryModule;
        this.processingUnit = processingUnit;
        this.graphicsDevice = graphicsDevice;
        this.consoleDevice = consoleDevice;
        this.managementConsole = managementConsole;
    }

    public NetworkDevice getNetworkDevice() {
        return networkDevice;
    }

    public void setNetworkDevice(NetworkDevice networkDevice) {
        this.networkDevice = networkDevice;
    }

    public OperatingSystem getOperatingSystem() {
        return operatingSystem;
    }

    public void setOperatingSystem(OperatingSystem operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    public StorageDevice getStorageDevice() {
        return storageDevice;
    }

    public void setStorageDevice(StorageDevice storageDevice) {
        this.storageDevice = storageDevice;
    }

    public MemoryModule getMemoryModule() {
        return memoryModule;
    }

    public void setMemoryModule(MemoryModule memoryModule) {
        this.memoryModule = memoryModule;
    }

    public ProcessingUnit getProcessingUnit() {
        return processingUnit;
    }

    public void setProcessingUnit(ProcessingUnit processingUnit) {
        this.processingUnit = processingUnit;
    }

    public GraphicsDevice getGraphicsDevice() {
        return graphicsDevice;
    }

    public void setGraphicsDevice(GraphicsDevice graphicsDevice) {
        this.graphicsDevice = graphicsDevice;
    }

    public ConsoleDevice getConsoleDevice() {
        return consoleDevice;
    }

    public void setConsoleDevice(ConsoleDevice consoleDevice) {
        this.consoleDevice = consoleDevice;
    }

    public ManagementConsole getManagementConsole() {
        return managementConsole;
    }

    public void setManagementConsole(ManagementConsole managementConsole) {
        this.managementConsole = managementConsole;
    }
}
