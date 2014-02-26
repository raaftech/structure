/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.initialization;

import org.junit.Test;
import org.slf4j.Logger;

/**
 * A quick test of the UniversalLogger Singleton
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-02-25
 */
public class TestUniversalLoggerSingleton {

    @Test
    public void testLoggingObjectCreation() {

        Logger logger = UniversalLogger.INSTANCE.getLogger();

        logger.info(logger.toString());
    }
}
