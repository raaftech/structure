/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.model;

import com.thinkaurelius.titan.core.TitanGraph;
import org.openpcf.universal.initialization.UniversalGraphSingleton;
import org.openpcf.universal.initialization.UniversalLoggerFactory;
import org.openpcf.universal.initialization.UniversalSettingsSingleton;
import org.slf4j.Logger;

import java.util.Properties;

/**
 * Component represents the most abstract part within the Domain Model.
 * Essentially, everything is a component and consists itself of various
 * specialized components. The application will mostly interact with
 * Components and more usefully, collections of components.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-01-06
 */
abstract class Component implements Persistable {
    private String name;
    private String icon;

    Properties settings = UniversalSettingsSingleton.INSTANCE.getSettings();
    Logger logger = UniversalLoggerFactory.getLogger(this.getClass());
    TitanGraph graph = UniversalGraphSingleton.INSTANCE.getGraph();

    public Component(String name, String icon) {

        //make sure connection object exists, reuse
        // query for name try catch

    }

    public void init() {
    }

    ;

    public void load() {
    }

    ;

    public void save() {
    }

    ;

}
