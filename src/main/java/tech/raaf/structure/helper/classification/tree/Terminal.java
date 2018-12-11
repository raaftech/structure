package tech.raaf.structure.helper.classification.tree;

import java.util.Set;
import java.util.function.Consumer;

/**
 * Terminals are terminating entities implementing failure or success actions.
 * @param <T> Type of objects we want to handle.
 */
public class Terminal<T> extends Node<T> {

    private Consumer<Set<T>> consumer;

    /**
     * Our constructor.
     * @param consumer The action to execute when consuming input.
     */
    public Terminal(Consumer<Set<T>> consumer) {
        this.consumer = consumer;
    }

    /**
     * Run consumer on the input.
     * @param input
     */
    @Override
    public void consumeInput(Set<T> input) {
        consumer.accept(input);
    }

}
