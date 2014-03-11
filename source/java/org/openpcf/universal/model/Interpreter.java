/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.model;

/**
 * Represents an interpreter of commands/languages/bytecode/native executables.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-01-06
 */
public class Interpreter extends LocalService implements Commandable {
    private Language language;

    public Interpreter(String name, String desc, String icon, String vendor, String version, String path, String image, Language language) {
        super(name, desc, icon, vendor, version, path, image);
        this.language = language;
    }

    public void command() {
    }
}
