package org.openpcf.universal.model;

import java.util.Set;

/**
 * Give a description of Capability here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-11-26
 */
public class Capability extends Node {

    public Capability() {}

    public Capability(String name, String desc, String uuid, Set<Node> isa) {
        super(name, desc, uuid, isa);
    }

    public Capability(String name, String desc, Set<Node> isa) {
        super(name, desc, isa);
    }
}
