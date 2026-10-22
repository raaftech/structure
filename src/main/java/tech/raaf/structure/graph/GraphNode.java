package tech.raaf.structure.graph;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A node as a query returns it: copies of its labels and properties.
 * It stays valid after the transaction closes.
 */
public final class GraphNode {

  private final Set<String> labels;
  private final Map<String, Object> properties;

  public GraphNode(Set<String> labels, Map<String, Object> properties) {
    this.labels = Collections.unmodifiableSet(new HashSet<>(labels));
    this.properties = Collections.unmodifiableMap(new HashMap<>(properties));
  }

  public Set<String> getLabels() {
    return labels;
  }

  public Map<String, Object> getProperties() {
    return properties;
  }
}
