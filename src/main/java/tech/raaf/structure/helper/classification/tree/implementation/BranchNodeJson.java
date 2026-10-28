package tech.raaf.structure.helper.classification.tree.implementation;

import tech.raaf.structure.helper.classification.tree.BranchNode;
import tech.raaf.structure.helper.classification.tree.InternalNode;
import tools.jackson.databind.JsonNode;

/**
 * BranchNodeJson extends BranchNode in a way that will occur
 * very often when validating instrument arguments.
 * Its predicate takes a JsonNode, and matches the value of one of its
 * keys against a given pattern. If the match fails or the key doesn't
 * exist, we return false. Else we return true.
 */
public class BranchNodeJson extends BranchNode<JsonNode> {

  public BranchNodeJson(InternalNode<JsonNode> parent, String key, String value) {
    super(parent, json -> {
      if (json.has(key)) {
        return json.get(key).asText().matches(value);
      } else {
        return false;
      }
    });
  }
}
