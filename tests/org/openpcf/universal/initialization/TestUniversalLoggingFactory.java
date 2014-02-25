/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.initialization;

import org.junit.Test;
import org.slf4j.Logger;

/**
 * Give a description of TestUniversalLoggingFactory here.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-02-25
 */
public class TestUniversalLoggingFactory {

    @Test
    public void testLoggingObjectCreation() {

        Logger logger = UniversalLoggingFactory.create(TestUniversalLoggingFactory.class);

        logger.info(logger.toString());
    }
}
