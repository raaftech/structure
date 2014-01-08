/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal;

/**
 * Represents an interpreter of commands/languages/bytecode/native executables.
 *
 * @author      Rubin Simons <rubin.simons@raaftech.com>
 * @since       2014-01-06
 * @version     0.6
 */
public class Interpreter extends LocalService implements Commandable {
    private Language language;

    public Interpreter() {}

    public void command () {}
}
