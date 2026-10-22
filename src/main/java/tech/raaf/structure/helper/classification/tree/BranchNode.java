package tech.raaf.structure.helper.classification.tree;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A BranchNode has both a parent and children.
 * They filter a set of data entities based on a predicate and pass
 * this filtered set to their children.
 * @param <T> Type of objects we want to handle.
 */
public class BranchNode<T> extends InternalNode<T> {

  protected Predicate<T> predicate;
  InternalNode<T> parent;

  /**
   * The constructor.
   * @param parent The parent of this BranchNode.
   * @param predicate The predicate used for filtering.
   */
  @SuppressWarnings("unchecked")
  public BranchNode(InternalNode parent, Predicate<T> predicate) {
    this.parent = parent;
    this.predicate = predicate;
    parent.addChild(this);
  }

  /**
   * Consumes input given to it by its parent, then filters it and passes
   * the filtered data to its children.
   * @param input Data entities contained in a set.
   */
  @Override
  public void consumeInput(Set<T> input) {
    if (input != null && input.size() > 0) {
      Set<T> result = input.stream().filter(predicate).collect(Collectors.toSet());
      children.parallelStream().forEach(child -> child.consumeInput(result));
    }
  }
}
