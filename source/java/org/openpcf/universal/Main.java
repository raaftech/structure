/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal;

import org.openpcf.universal.initialization.UniversalLoggerFactory;
import org.slf4j.Logger;

/**
 * The main class and method, possibly referenced in Ant or Maven.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-01-06
 */
public class Main {

    public static void main(final String[] args) {

        Logger logger = UniversalLoggerFactory.getLogger(Main.class);
        logger.info("Main loop");
    }
}
