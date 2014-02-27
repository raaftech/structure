package org.openpcf.universal.initialization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A preliminary Universal Logger Singleton.
 *
 * @author Freek Alleman <freek.alleman@raaftech.com>
 * @version 0.1
 * @since 2014-02-25
 */
public class UniversalLoggerFactory {

    public static Logger getLogger(Class clazz) {
        Logger logger = LoggerFactory.getLogger(clazz);

        return logger;
    }
}
