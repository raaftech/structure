/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Give a description of DataPool here.
 *
 * @author Your name <freek.alleman@raaftech.com>
 * @version 0.1
 * @since 2014-03-11
 */
public class DataPool {

    public Map<String, Command> commands = new HashMap();
    public Map<String, AssembledDevice> computerSystems = new HashMap<>();
    public Map<String, ConsoleDevice> consoleDevices = new HashMap<>();
    public Map<String, Credential> credentials = new HashMap<>();
    public Map<String, GraphicalAccessor> graphicalAccessors = new HashMap<>();
    public Map<String, GraphicsDevice> graphicsDevices = new HashMap<>();
    public Map<String, Handler> handlers = new HashMap<>();
    public Map<String, Hypervisor> hypervisors = new HashMap<>();
    public Map<String, Interpreter> interpreters = new HashMap<>();
    public Map<String, Listener> listeners = new HashMap<>();
    public Map<String, LocalService> localServices = new HashMap<>();
    public Map<String, MemoryModule> memoryModules = new HashMap<>();
    public Map<String, NetworkDevice> networkDevices = new HashMap<>();
    public Map<String, NetworkedService> networkedServices = new HashMap<>();
    public Map<String, OperatingSystem> operatingSystems = new HashMap<>();
    public Map<String, ProcessingUnit> processingUnits = new HashMap<>();
    public Map<String, TextualAccessor> textualAccessors = new HashMap<>();

}
