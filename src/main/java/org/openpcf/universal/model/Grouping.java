package org.openpcf.universal.model;

import org.openpcf.universal.graph.Node;

import java.util.Set;

/**
 * Give a description of Grouping here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-11-26
 */
public class Grouping extends Node {

    public Grouping() {}

    public Grouping(String name, String desc, String uuid, Set<Node> isa) {
        super(name, desc, uuid, isa);
    }

    public Grouping(String name, String desc, Set<Node> isa) {
        super(name, desc, isa);
    }
}
