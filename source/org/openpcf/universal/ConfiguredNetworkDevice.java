/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal;

import java.net.InetAddress;

/**
 * Represents a configured network device.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.1
 * @since 2014-01-08
 */
public class ConfiguredNetworkDevice extends Configuration {
    private NetworkDevice[] assignedNetworkDevices;
    private InetAddress[] assignedInetAddresses;

    public ConfiguredNetworkDevice() {}
}
