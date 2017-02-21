package com.raaftech.structure.graph;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.base.Context;

/**
 * @Deprecated We need to redesign the domain classes to use Entity.
 */
@Deprecated
public abstract class Instrument extends Instance {

    private static  final String WEIGHT = "weight";

    public Instrument() {}

    public Instrument(ObjectNode json, Type type) {
        super(json, type);
    }

    public Instrument(ObjectNode json, String isa) {
        super(json, isa);
    }

    public double getWeight() {
        return data.hasProperty(WEIGHT) ? (double) data.getProperty(WEIGHT) : 0;
    }

    public Instrument save() {
        Context.getNodeRepository().save(this);
        return this;
    }

    public ObjectNode toJson() {
        ObjectNode json = super.toJson();
        if (getWeight() > 0) {
            json.put("weight", getWeight());
        }
        return json;
    }

}
