package com.raaftech.structure.helper.classification.tree.implementation;

import com.fasterxml.jackson.databind.JsonNode;
import com.raaftech.structure.helper.classification.tree.BranchNode;
import com.raaftech.structure.helper.classification.tree.InternalNode;

/**
 * Created by Boy van Duuren &lt;boy@raaftech.com&gt;.
 */
public class BranchNodeJson extends BranchNode<JsonNode> {

    public BranchNodeJson(InternalNode<JsonNode> parent, String key, String value) {
        super(parent,
                json -> {
                    if (json.has(key)) {
                        return json.get(key).asText().matches(value);
                    } else {
                        return false;
                    }
                });
    }

}
