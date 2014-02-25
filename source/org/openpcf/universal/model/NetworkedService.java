/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.model;

/**
 * Represents a service with a network listener component.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-01-06
 */
public class NetworkedService extends Service {
    private Listener listener;

    public NetworkedService(String name, String icon, String vendor, String version, String path, String image, Listener listener) {
        super(name, icon, vendor, version, path, image);
        this.listener = listener;
    }
}
