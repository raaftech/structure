package com.raaftech.structure.graph;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/**
 * Created by blackorchid on 30-5-17.
 */
public class EntityValidationError {
    private int code;
    private String uuid;
    private String name;
    private Set<String> conflictUuids;
    private String message;

    public EntityValidationError(int code, String uuid, String name, Set<String> conflicts, String message) {
        this.code = code;
        this.uuid = uuid;
        this.name = name;
        this.conflictUuids = conflicts;
        this.message = message;
    }

    public EntityValidationError(int code, String uuid, String name, String message) {
        this(code, uuid, name, new HashSet<String>(), message);
    }

    public static Set<String> uuidSetToHashSet(Set<UUID> list) {
        Set<String> result = new HashSet<String>();
        Iterator<UUID> iter = list.iterator();
        while(iter.hasNext()) {
            result.add(iter.next().toString());
        }
        return result;
    }

    private JsonArray hashSetToJsonArrayString(Set<String> set) {
        final JsonArray a = new JsonArray();
        Iterator<String> iter = set.iterator();
        while(iter.hasNext()) {
            a.add(iter.next());
        }
        return a;
    }

    @Override
    public String toString() {
        final JsonObject o = new JsonObject()
                .put("code", this.code)
                .put("uuid", this.uuid)
                .put("name", this.name)
                .put("conflict", hashSetToJsonArrayString(this.conflictUuids))
                .put("message", this.message);
        return o.toString();
    }
}
