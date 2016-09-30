package com.raaftech.structure.graph;

import com.raaftech.structure.base.Context;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.util.Set;

public class Marker extends Instrument {

    @RelatedTo(type="GIVES_CAPABILITY", direction = Direction.OUTGOING)
    private Type capability;

    @RelatedTo(type="HANDLES", direction=Direction.OUTGOING)
    @Fetch
    private Set<Type> appliesTo;

    @RelatedTo(type="HAS_INSTRUMENT", direction=Direction.INCOMING)
    private Type providedBy;

    public Marker() {}

    public Marker(String name, String desc, String filePath,
                  Type capability, Set<Type> appliesTo, Type providedBy) {
        super(name, desc, filePath);
        this.capability = capability;
        this.appliesTo = appliesTo;
        this.providedBy = providedBy;
    }

    public Marker(String name, String desc, String uuid, String filePath,
                  Type capability, Set<Type> appliesTo, Type providedBy) {
        super(name, desc, uuid, filePath);
        this.capability = capability;
        this.appliesTo = appliesTo;
        this.providedBy = providedBy;
    }

    public Marker(String name, String desc, String filePath,
                  String capabilityName, Set<Type> appliesTo, String providedBy) {
        super(name, desc, filePath);
        this.capability = Context.getTypeRepository().findOneByName(capabilityName);
        this.appliesTo = appliesTo;
        this.providedBy = Context.getTypeRepository().findOneByName(providedBy);
    }

    public Marker(String name, String desc, String uuid, String filePath,
                  String capabilityName, Set<Type> appliesTo, String providedBy) {
        super(name, desc, uuid, filePath);
        this.capability = Context.getTypeRepository().findOneByName(capabilityName);
        this.appliesTo = appliesTo;
        this.providedBy = Context.getTypeRepository().findOneByName(providedBy);
    }

}
