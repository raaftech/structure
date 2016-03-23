package com.raaftech.structure.helper.classification.tree;

import java.util.Set;

/**
 * Created by Boy van Duuren &lt;boy@raaftech.com&gt;.
 */
public abstract class Node<T> {

    public abstract void passInput(Set<T> input);

}
