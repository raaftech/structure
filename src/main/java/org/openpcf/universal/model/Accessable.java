/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.model;

/**
 * An interface that gives access to an underlying interpreter/os/service/etc.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-01-07
 */
public interface Accessable {
    public void access();
}
