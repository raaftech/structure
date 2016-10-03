package com.raaftech.structure.graph;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.base.Context;

public abstract class Instrument extends Instance {

    public Instrument() {}

    public Instrument(ObjectNode json, Type type) {
        super(json, type);
    }

    public Instrument(ObjectNode json, String isa) {
        super(json, isa);
    }

    public int getWeight() {
        return (Integer)kv.getProperty("weight");
    }

    public String getFilePath() {
        return (String)kv.getProperty("fileLocation");
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
