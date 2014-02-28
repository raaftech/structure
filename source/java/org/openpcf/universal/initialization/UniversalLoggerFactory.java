package org.openpcf.universal.initialization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A preliminary Universal Logger Factory.
 *
 * @author Freek Alleman <freek.alleman@raaftech.com>
 * @since 2014-02-25
 * @version 0.6
 */
public class UniversalLoggerFactory {

    public static Logger getLogger(Class clazz) {
        Logger logger = LoggerFactory.getLogger(clazz);

        return logger;
    }
}
