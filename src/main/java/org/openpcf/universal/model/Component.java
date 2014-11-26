/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.model;

import org.springframework.data.neo4j.annotation.NodeEntity;

import java.util.Set;

/**
 * Component represents the most abstract part within the Domain Model.
 * Essentially, everything is a component and consists itself of various
 * specialized components. The application will mostly interact with
 * Components and more usefully, collections of components.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-01-06
 */
@NodeEntity
public class Component extends Node {

    public Component() {}

    public Component(String name, String desc, String uuid, Set<Node> isa) {
        super(name, desc, uuid, isa);
    }

    public Component(String name, String desc, Set<Node> isa) {
        super(name, desc, isa);
    }
}
