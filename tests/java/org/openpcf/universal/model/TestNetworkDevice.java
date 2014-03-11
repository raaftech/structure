/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.model;

import org.junit.Test;
import org.openpcf.universal.initialization.UniversalLoggerFactory;
import org.slf4j.Logger;

/**
 * Give a description of TestNetworkDevice here.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.1
 * @since 2014-03-11
 */
public class TestNetworkDevice {

    @Test
    public void testComputerSystemObjectCreation() {

        Logger logger = UniversalLoggerFactory.getLogger(this.getClass());

        NetworkDevice someComponent = new NetworkDevice();
        someComponent.setMacaddr("00:CC:00:FF:EE:EE");

        logger.info(someComponent.getMacaddr());


    }
}
