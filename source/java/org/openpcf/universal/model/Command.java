/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.model;

import java.util.Date;

/**
 * A command is an object which represents a runnable command, invoked using
 * a specific interpreter.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-01-06
 */
public class Command {
    private Interpreter interpreter;
    private String content;
    private Date started;
    private Date finished;
    private String result;
    private int exitcode;

    public Command(Interpreter interpreter, String content, Date started, Date finished, String result, int exitcode) {
        this.interpreter = interpreter;
        this.content = content;
        this.started = started;
        this.finished = finished;
        this.result = result;
        this.exitcode = exitcode;
    }
}
