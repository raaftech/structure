package org.openpcf.universal.model;

import org.openpcf.universal.graph.Node;

import java.util.Set;

/**
 * Give a description of Handler here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-11-26
 */
public class Handler extends Node {

    public Handler() {}

    public Handler(String name, String desc, String uuid, Set<Node> isa) {
        super(name, desc, uuid, isa);
    }

    public Handler(String name, String desc, Set<Node> isa) {
        super(name, desc, isa);
    }
}
