/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.model;

/**
 * Tells a given class to implement methods that represent "restart"
 * functionality.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-01-08
 */
public interface Restartable {
    public void restart();
}
