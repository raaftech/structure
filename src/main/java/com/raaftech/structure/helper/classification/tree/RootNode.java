package com.raaftech.structure.helper.classification.tree;

import java.util.Set;

/**
 * Created by Boy van Duuren &lt;boy@raaftech.com&gt;.
 */
public class RootNode<T> extends InternalNode<T> {

    @Override
    public void passInput(Set<T> input) {
        children.parallelStream().forEach(node -> node.passInput(input));
    }

}
