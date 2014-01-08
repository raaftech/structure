/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal;

import java.net.InetAddress;

/**
 * Represents a listener (ip + portnumber) address for a NetworkedService.
 * Notice: needs to handle UDP and TCP!
 *
 * @author      Rubin Simons <rubin.simons@raaftech.com>
 * @since       2014-01-06
 * @version     0.6
 */
public class Listener {
    private InetAddress addr;
    private int port;

    public Listener() {}
}
