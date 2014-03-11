/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.initialization;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.Properties;

/**
 * A quick test of the UniversalSettingsSingleton Singleton
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-02-25
 */
public class TestUniversalSettingsSingleton {

    @Test
    public void testGetResource() {
        Assert.assertNotNull("Test file missing", getClass().getResource("/universal.properties"));
    }

    @Test
    public void testGetStream() {
        Assert.assertNotNull("Test file missing", getClass().getResourceAsStream("/universal.properties"));
    }

    @Test
    public void testSettingsObjectCreation() {

        Logger logger = UniversalLoggerFactory.getLogger(this.getClass());
        Properties settings = UniversalSettingsSingleton.INSTANCE.getSettings();

        logger.info("universal.home: " + settings.getProperty("universal.home"));
        logger.info("universal.graph.dbtype: " + settings.getProperty("universal.graph.dbtype"));
        logger.info("universal.graph.dbhome: " + settings.getProperty("universal.graph.dbhome"));
        logger.info("universal.graph.dbindex: " + settings.getProperty("universal.graph.dbindex"));

    }
}
