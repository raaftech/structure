/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.model;

/**
 * Represents a configured network device.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-01-08
 */
public class ConfiguredNetworkDevice extends Configuration {
    private NetworkDevice assignedNetworkDevice;
    private String assignedInetAddress;

    public ConfiguredNetworkDevice() {};

    public ConfiguredNetworkDevice(String name, NetworkDevice assignedNetworkDevice, String assignedInetAddress) {
        super(name);
        this.assignedNetworkDevice = assignedNetworkDevice;
        this.assignedInetAddress = assignedInetAddress;
    }

    public NetworkDevice getAssignedNetworkDevice() {
        return assignedNetworkDevice;
    }

    public void setAssignedNetworkDevice(NetworkDevice assignedNetworkDevice) {
        this.assignedNetworkDevice = assignedNetworkDevice;
    }

    public String getAssignedInetAddress() {
        return assignedInetAddress;
    }

    public void setAssignedInetAddress(String assignedInetAddress) {
        this.assignedInetAddress = assignedInetAddress;
    }
}
