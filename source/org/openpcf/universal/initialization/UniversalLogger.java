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
public enum UniversalLogger {
    INSTANCE;

    private Logger logger;

    private UniversalLogger(){
        logger = LoggerFactory.getLogger(UniversalLogger.class);
    }

    public Logger getLogger(){
        return logger;
    }
}
