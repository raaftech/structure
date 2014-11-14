/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.model;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.support.index.IndexType;

import java.util.HashSet;
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
public class Component {

    @GraphId
    private Long id;

    @Indexed
    private String name;

    @Indexed(indexType = IndexType.FULLTEXT, indexName = "descriptions")
    private String desc;

    @Indexed(unique=true)
    private String uuid;

    @RelatedTo(type="ISA", direction = Direction.OUTGOING)
    private Set<Component> isa = new HashSet<Component>();

    @RelatedTo(type="HAS", direction = Direction.OUTGOING)
    private Set<Component> has = new HashSet<Component>();

    public Component() {}
}
