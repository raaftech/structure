/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.model;

import java.util.Set;

/**
 * Represents an abstract Device.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-01-06
 */
public class Device extends Component implements Virtualizable {

    private String rev;

    public Device() {};

    public Device(String name, String desc, String uuid, String rev, Set<Node> isa) {
        super(name, desc, uuid, isa);
        this.rev = rev;
    }

    public Device(String name, String desc, String rev, Set<Node> isa) {
        super(name, desc, isa);
        this.rev = rev;
    }
}
