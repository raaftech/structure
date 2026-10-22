package tech.raaf.structure.helper.classification.tree;

import java.util.HashSet;
import java.util.Set;

/**
 * InternalNodes are nodes that have children.
 * @param <T> Type of objects we want to handle.
 */
public abstract class InternalNode<T> extends Node<T> {

  protected Set<Node<T>> children = new HashSet<>();

  /**
   * Add a child to the children set.
   * @param child The child to be added.
   * @return The object addChild is called on.
   */
  public InternalNode<T> addChild(Node<T> child) {
    children.add(child);
    return this;
  }
}
