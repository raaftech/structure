/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.scaffolding;

/**
 * Represents a listener (ip + portnumber) address for a NetworkedService.
 * Notice: needs to handle UDP and TCP!
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-01-06
 */
public class Listener {
    private String addr;
    private int port;

    public Listener(String addr, int port) {
        this.addr = addr;
        this.port = port;
    }
}
