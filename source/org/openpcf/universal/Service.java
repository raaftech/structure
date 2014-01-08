/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal;

/**
 * Represents a generic service.
 *
 * @author      Rubin Simons <rubin.simons@raaftech.com>
 * @since       2014-01-06
 * @version     0.6
 */
public class Service extends Software implements Checkable, Discoverable, Stopable, Startable, Restartable, Createable, Destroyable {
    private String[] paths;
    private String[] images;

    public Service() {}

    public void check() {}
    public void discover() {}
    public void stop() {}
    public void start() {}
    public void restart() {}
    public void create() {}
    public void destroy() {}

}
