/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.model;

/**
 * The TextualAccessor class represents specialized NetworkedService classes
 * that give Text-Mode access to an underlying system.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-01-06
 */
public class TextualAccessor extends NetworkedService implements Accessable {

    public TextualAccessor(String name, String desc, String icon, String vendor, String version, String path, String image, Listener listener) {
        super(name, desc, icon, vendor, version, path, image, listener);
    }

    public void access() {
    }
}
