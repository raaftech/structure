package tech.raaf.structure.helper.handlebars;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import java.io.IOException;
import java.util.Iterator;

/**
 * ReachableAddress can be used to resolve a "reachable" address from an array containing IpAdress Instances.
 */
public class ReachableAddress implements Helper<ArrayNode> {

  public static final ReachableAddress INSTANCE = new ReachableAddress();

  private ReachableAddress() {}

  public CharSequence apply(ArrayNode arrayNode, Options options) throws IOException {
    Iterator<JsonNode> iter = arrayNode.elements();
    // Iterate over the elements of the array returning the first reachable ip address.
    while (iter.hasNext()) {
      JsonNode el = iter.next();
      if (el.get("reachable").asBoolean() == true) {
        return el.get("addr").asText();
      }
    }
    return null;
  }
}
