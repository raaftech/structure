package tech.raaf.structure.helper.classification.tree;

import java.util.Set;

/**
 * A type of node that is always the beginning of a classification tree.
 * RootNodes don't have a parent, but do have children, which are always contained
 * in a Set. They are responsible for passing their input unfiltered to their children.
 * @param <T> Type of objects we want to handle.
 */
public class RootNode<T> extends InternalNode<T> {

  /**
   * Pass input to RootNode's children.
   * @param input Data entities contained in a set.
   */
  @Override
  public void consumeInput(Set<T> input) {
    children.forEach(node -> node.consumeInput(input));
  }
}
