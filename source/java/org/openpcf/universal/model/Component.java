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
abstract class Component {
    private String name;
    private String desc;
    private String icon;
    private Coordinate location;

    Properties settings = UniversalSettingsSingleton.INSTANCE.getSettings();
    Logger logger = UniversalLoggerFactory.getLogger(this.getClass());
    TitanGraph graph = UniversalGraphSingleton.INSTANCE.getGraph();

    public Component() {}

    public Component(String name, String desc, String icon) {
        this.name = name;
        this.desc = desc;
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Coordinate getLocation() {
        return location;
    }

    public void setLocation(Coordinate location) {
        this.location = location;
    }
}
