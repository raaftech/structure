/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.initialization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Give a description of UniversalLoggingFactory here.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-02-25
 */
public class UniversalLoggingFactory {
    public static Logger create(final Class clazz) {

        Logger logger = LoggerFactory.getLogger(clazz);
        return logger;
    }
}
