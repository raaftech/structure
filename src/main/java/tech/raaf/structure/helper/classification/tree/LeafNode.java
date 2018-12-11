package tech.raaf.structure.helper.classification.tree;

import java.util.Set;
import java.util.function.Function;

/**
 * LeafNodes are the last nodes used for filtering in the classification tree.
 * They don't filter using a predicate, but filter (rather, select) using a function that
 * takes one argument and produces output.
 * When the selection doesn't produce output (null or an empty set), the original input
 * is passed to a failure terminal.
 * If the selection does produce output, that output is passed to a success terminal.
 * @param <T> Type of objects we want to handle.
 */
public class LeafNode<T> extends Node<T> {

    private InternalNode<T> parent;
    private Function<Set<T>, Set<T>> selection;
    private Terminal success;
    private Terminal failure;

    /**
     * Our constructor.
     * @param parent The parent of this LeafNode.
     * @param success Terminal used when the selection produced results.
     * @param failure Terminal used when the selection produces null or an empty set.
     * @param selection A function implementing the selection.
     */
    @SuppressWarnings("unchecked")
    public LeafNode(InternalNode<T> parent, Terminal success, Terminal failure, Function<Set<T>, Set<T>> selection) {
        this.parent = parent;
        this.selection = selection;
        this.success = success;
        this.failure = failure;
        parent.addChild(this);
    }

    /**
     * Apply our selection to data passed to us.
     * If this results in null or an empty set, pass the original input to
     * the failure terminal, else pass the selection to the success terminal.
     * @param input
     */
    @Override
    @SuppressWarnings("unchecked")
    public void consumeInput(Set<T> input) {
        Set<T> selectedSet = selection.apply(input);

        if (selectedSet == null || selectedSet.size() == 0) {
            failure.consumeInput(input);
        } else {
            success.consumeInput(selectedSet);
        }
    }

}
