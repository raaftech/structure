package tech.raaf.structure.graph;

import java.util.Set;

public class Parser extends Instrument {

  public Set<Result> getRawResults() {
    return related("-[:PARSED]->(b:Result)");
  }

  public Set<Instance> getProducedNodes() {
    return related("-[:PRODUCED]->(b:Instance)");
  }
}
