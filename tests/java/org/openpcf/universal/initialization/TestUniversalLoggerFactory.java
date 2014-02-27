/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.initialization;

import org.junit.Test;
import org.openpcf.universal.initialization.UniversalLoggerFactory;
import org.slf4j.Logger;

/**
 * A quick test of the UniversalLoggerFactory Singleton
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-02-25
 */
public class TestUniversalLoggerFactory {

    @Test
    public void testLoggingObjectCreation() {

        Logger logger = UniversalLoggerFactory.getLogger(this.getClass());

        logger.info(logger.toString());
    }
}
