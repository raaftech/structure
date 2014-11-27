/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.model;

import java.util.Set;

/**
 * Represents software in the most generic sense of the word.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-01-06
 */
public class Software extends Component implements Checkable, Discoverable {

    String ver;
    String shortName;
    String longName;

    public Software() {};

    public Software(String name, String desc, String uuid, String ver,
                    String shortName, String longName, Set<Node> isa) {
        super(name, desc, uuid, isa);
        this.ver = ver;
        this.shortName = shortName;
        this.longName = longName;
    }

    public Software(String name, String desc, String ver, String shortName,
                    String longName, Set<Node> isa) {
        super(name, desc, isa);
        this.ver = ver;
        this.shortName = shortName;
        this.longName = longName;
    }

    @Override
    public void check() {

    }

    @Override
    public void discover() {

    }
}
