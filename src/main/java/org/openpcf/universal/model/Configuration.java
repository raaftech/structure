/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.model;

import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.fieldaccess.DynamicProperties;
import org.springframework.data.neo4j.fieldaccess.DynamicPropertiesContainer;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Represents an abstract Device.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-01-06
 */
public class Configuration extends Component {

    @Transient
    private List<String> allowedProperties = Arrays.asList(
            "dnsname", "addr", "port", "map", "url"
    );
    private DynamicProperties config =
            new DynamicPropertiesContainer();

    public Configuration() {};

    public Configuration(String name, String desc, String uuid, Set<Node> isa) {
        super(name, desc, uuid, isa);
    }

    public Configuration(String name, String desc, Set<Node> isa) {
        super(name, desc, isa);
    }

    public Configuration addProperty(String k, String v) {
        if (allowedProperties.contains(k)) {
            config.setProperty(k, v);
        }
        return this;
    }
}
