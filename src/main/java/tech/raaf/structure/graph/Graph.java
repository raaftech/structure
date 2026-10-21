package tech.raaf.structure.graph;

import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.TransactionFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.exceptions.GraphException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Access to the graph database. Binds a transaction to the current
 * thread. Nested calls use the bound transaction.
 */
public final class Graph {

    private static final Logger log = LoggerFactory.getLogger(Graph.class);
    private static final ThreadLocal<Bound> bound = new ThreadLocal<>();

    // The state of the transaction of a thread, and its commit actions.
    private static final class Bound {
        final List<Runnable> afterCommit = new ArrayList<>();
        boolean failed;
    }

    private Graph() {}

    // Run work in a transaction on this thread; nested calls use it.
    public static <T> T transaction(Supplier<T> work) {
        Bound outer = bound.get();
        if (outer != null) {
            try {
                return work.get();
            } catch (RuntimeException e) {
                outer.failed = true;
                throw e;
            }
        }
        T result;
        Bound b = new Bound();
        bound.set(b);
        try (Transaction tx = Context.getGraphDatabase().beginTx()) {
            result = work.get();
            if (b.failed) {
                throw new GraphException("Nested transaction failed");
            }
            tx.success();
        } catch (TransactionFailureException e) {
            throw new GraphException(e);
        } finally {
            bound.remove();
        }
        for (Runnable action : b.afterCommit) {
            try {
                action.run();
            } catch (RuntimeException e) {
                log.error("Action after commit failed", e);
            }
        }
        return result;
    }

    public static void transaction(Runnable work) {
        transaction(() -> {
            work.run();
            return null;
        });
    }

    // Run action after the commit, or now if there is no transaction.
    public static void afterCommit(Runnable action) {
        Bound b = bound.get();
        if (b == null) {
            action.run();
        } else {
            b.afterCommit.add(action);
        }
    }
}
