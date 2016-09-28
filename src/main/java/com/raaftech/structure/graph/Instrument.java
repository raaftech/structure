package com.raaftech.structure.graph;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.base.Context;

public abstract class Instrument extends Node {

    protected String filePath;
    protected int weight;

    public Instrument() {}

    public Instrument(String name, String desc, String filePath) {
        super(name, desc);
        this.filePath = filePath;
    }

    public Instrument(String name, String desc, String filePath, int weight) {
        this(name, desc, filePath);
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }

    public Instrument save() {
        Context.getNodeRepository().save(this);
        return this;
    }

    public ObjectNode toJson() {
        ObjectNode json = super.toJson();
        json.put("filePath", filePath);
        if (weight > 0) {
            json.put("weight", weight);
        }
        return json;
    }

}
