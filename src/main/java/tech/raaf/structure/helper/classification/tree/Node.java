package tech.raaf.structure.helper.classification.tree;

import java.util.Set;

/**
 * The base class for classification trees.
 * Nodes pass data between each other using consumeInput, which takes an object of type T as argument.
 * @param <T> Type of objects we want to handle.
 */
public abstract class Node<T> {

  public abstract void consumeInput(Set<T> input);
}
