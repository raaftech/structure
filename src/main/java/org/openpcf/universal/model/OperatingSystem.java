/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.model;

import java.util.Set;

/**
 * Represents an Operating System running on a computer or an embedded
 * device like a management console.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-01-06
 */
public class OperatingSystem extends Software implements Stoppable, Restartable {

    public OperatingSystem() {
    };

    public void check() {
    }

    public void discover() {
    }

    public void stop() {
    }

    public void start() {
    }

    public void restart() {
    }
}
