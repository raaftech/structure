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
        return (double) data.getProperty(WEIGHT);
    }

    public String getFilePath() {
        return (String) data.getProperty("fileLocation");
    }

    public Instrument save() {
        Context.getNodeRepository().save(this);
        return this;
    }

    public ObjectNode toJson() {
        ObjectNode json = super.toJson();
        json.put("fileLocation", getFilePath());
        if (getWeight() > 0) {
            json.put("weight", getWeight());
        }
        return json;
    }

}
