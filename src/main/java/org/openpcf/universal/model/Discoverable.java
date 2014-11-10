/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.model;

/**
 * Tells a given class to implement methods that represent "discover"
 * functionality.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-01-07
 */
public interface Discoverable {
    public void discover();
}
