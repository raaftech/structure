package org.openpcf.universal.model;

import org.openpcf.universal.graph.Node;

import java.util.Set;

/**
 * Give a description of Actor here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-11-26
 */
public class Actor extends Node {

    public Actor() {}

    public Actor(String name, String desc, String uuid, Set<Node> isa) {
        super(name, desc, uuid, isa);
    }

    public Actor(String name, String desc, Set<Node> isa) {
        super(name, desc, isa);
    }
}
