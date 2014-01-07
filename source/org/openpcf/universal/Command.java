/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal;

import java.util.Date;

/**
 * Give a description of Command here.
 *
 * @author      Rubin Simons <rubin.simons@raaftech.com>
 * @since       2014-01-06
 * @version     0.6
 */
public class Command {
    private Interpreter interpreter;
    private String content;
    private Date started;
    private Date finished;
    private int exitcode;
}
