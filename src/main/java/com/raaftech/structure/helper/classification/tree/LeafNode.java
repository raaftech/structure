package com.raaftech.structure.helper.classification.tree;

import java.util.Set;
import java.util.function.Function;

/**
 * Created by Boy van Duuren &lt;boy@raaftech.com&gt;.
 */
public class LeafNode<T> extends Node<T> {

    private InternalNode<T> parent;
    private Function<Set<T>, Set<T>> selection;
    private Terminal success;
    private Terminal failure;

    public LeafNode(InternalNode<T> parent, Terminal success, Terminal failure, Function<Set<T>, Set<T>> selection) {
        this.parent = parent;
        this.selection = selection;
        this.success = success;
        this.failure = failure;
        parent.addChild(this);
    }

    @Override
    public void passInput(Set<T> input) {
        Set<T> selectedSet = selection.apply(input);

        if (selectedSet == null || selectedSet.size() == 0) {
            failure.passInput(selectedSet);
        } else {
            success.passInput(selectedSet);
        }
    }

}
