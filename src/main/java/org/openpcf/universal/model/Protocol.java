package org.openpcf.universal.model;

import java.util.Set;

/**
 * Give a description of Protocol here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-11-26
 */
public class Protocol extends Node {

    public Protocol() {}

    public Protocol(String name, String desc, String uuid, Set<Node> isa) {
        super(name, desc, uuid, isa);
    }

    public Protocol(String name, String desc, Set<Node> isa) {
        super(name, desc, isa);
    }
}
