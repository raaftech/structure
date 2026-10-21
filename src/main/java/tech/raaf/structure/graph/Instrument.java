package tech.raaf.structure.graph;

import com.fasterxml.jackson.databind.node.ObjectNode;
import tech.raaf.structure.base.Context;

public abstract class Instrument extends Instance {

    private static  final String WEIGHT = "weight";

    public Instrument() {}

    public Instrument(ObjectNode json, Type type) {
        super(json, type);
    }

    public int getWeight() {
        return data.hasProperty(WEIGHT) ? (int)data.getProperty(WEIGHT) : 0;
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
