package com.raaftech.structure.graph;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.util.Set;

public class Parser extends Instrument {

    @RelatedTo(type="PARSED", direction=Direction.OUTGOING)
    private Set<Result> rawResults;

    @RelatedTo(type="PRODUCED", direction=Direction.OUTGOING)
    private Set<Instance> producedNodes;

}
