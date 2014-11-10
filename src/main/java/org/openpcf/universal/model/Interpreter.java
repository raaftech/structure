/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.model;

/**
 * Represents an interpreter of commands/languages/bytecode/native executables.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-01-06
 */
public class Interpreter extends Software implements Commandable {
    private Language language;

    public Interpreter() {};

    public void check() {
    }

    public void discover() {
    }

    public void command() {
    }
}
