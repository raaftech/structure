package com.raaftech.structure.helper.classification.tree;

import java.util.Set;
import java.util.function.Consumer;

/**
 * Created by Boy van Duuren &lt;boy@raaftech.com&gt;.
 */
public class Terminal<T> extends Node<T> {

    private Consumer<Set<T>> consumer;

    public Terminal(Consumer<Set<T>> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void passInput(Set<T> input) {
        consumer.accept(input);
    }

}
