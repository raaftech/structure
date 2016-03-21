package com.raaftech.structure.helper.classification.tree;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by Boy van Duuren &lt;boy@raaftech.com&gt;.
 */
public class BranchNode<T> extends InternalNode<T> {

    protected Predicate<T> predicate;
    InternalNode<T> parent;

    public BranchNode(InternalNode parent, Predicate<T> predicate) {
        this.parent = parent;
        this.predicate = predicate;
        parent.addChild(this);
    }

    @Override
    public void passInput(Set<T> input) {
        if (input != null && input.size() > 0) {
            Set<T> result = input.stream()
                    .filter(predicate)
                    .collect(Collectors.toSet());
            children.parallelStream().forEach(child -> child.passInput(result));
        }
    }

}
