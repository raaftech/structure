/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal;

/**
 * Tells a given class to implement methods that represent "restart"
 * functionality.
 *
 * @author      Rubin Simons <rubin.simons@raaftech.com>
 * @since       2014-01-08
 * @version     0.6
 */
public interface Restartable {
    public void restart();
}
