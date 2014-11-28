package org.openpcf.universal.model;

import org.openpcf.universal.graph.Node;

import java.util.Set;

/**
 * Give a description of Model here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-11-26
 */
public class Model extends Node {

    private String ns;
    private String ver;

    public Model() {}

    public Model(String name, String desc, String uuid, String ns, String ver, Set<Node> isa) {
        super(name, desc, uuid, isa);
        this.ns = ns;
        this.ver = ver;
    }

    public Model(String name, String desc, String ns, String ver, Set<Node> isa) {
        super(name, desc, isa);
        this.ns = ns;
        this.ver = ver;
    }
}
