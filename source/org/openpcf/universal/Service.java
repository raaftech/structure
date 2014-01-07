/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal;

/**
 * Give a description of Service here.
 *
 * @author      Rubin Simons <rubin.simons@raaftech.com>
 * @since       2014-01-06
 * @version     0.6
 */
public class Service extends Software implements Checkable, Startable, Stopable, Createable, Destroyable {
    private String[] paths;
    private String image;
}
