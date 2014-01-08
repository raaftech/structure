/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal;

/**
 * Represents a service with a network listener component.
 *
 * @author      Rubin Simons <rubin.simons@raaftech.com>
 * @since       2014-01-06
 * @version     0.6
 */
public class NetworkedService extends Service {
    private Listener[] listeners;

    public NetworkedService() {}
}
