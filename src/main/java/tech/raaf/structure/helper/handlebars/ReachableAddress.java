package tech.raaf.structure.helper.handlebars;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import java.io.IOException;
import java.util.Iterator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;

/**
 * ReachableAddress can be used to resolve a "reachable" address from an array containing IpAdress Instances.
 */
public class ReachableAddress implements Helper<ArrayNode> {

  public static final ReachableAddress INSTANCE = new ReachableAddress();

  private ReachableAddress() {}

  public CharSequence apply(ArrayNode arrayNode, Options options) throws IOException {
    Iterator<JsonNode> iter = arrayNode.values().iterator();
    // Iterate over the elements of the array returning the first reachable ip address.
    while (iter.hasNext()) {
      JsonNode el = iter.next();
      if (el.get("reachable").asBoolean() == true) {
        return el.get("addr").asString();
      }
    }
    return null;
  }
}
