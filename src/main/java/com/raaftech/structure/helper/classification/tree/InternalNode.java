package com.raaftech.structure.helper.classification.tree;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Boy van Duuren &lt;boy@raaftech.com&gt;.
 */
public abstract class InternalNode<T> extends Node<T> {

    protected Set<Node<T>> children = new HashSet<>();

    public InternalNode<T> addChild(Node<T> child) {
        children.add(child);
        return this;
    }

}
