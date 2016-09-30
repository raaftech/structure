package com.raaftech.structure.graph;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.util.Set;

public class Result extends Instance {

    @RelatedTo(type="RESULTED_IN", direction= Direction.OUTGOING)
    private Set<Instance> resultedIn;

}
