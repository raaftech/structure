package tech.raaf.structure.graph;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Logging;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.exceptions.Neo4jException;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.raaf.structure.exceptions.GraphException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;

/**
 * Access to the graph database. Binds a transaction to the current
 * thread. Nested calls use the bound transaction. Only this class
 * uses the API of the database.
 */
public final class Graph {

    private static final Logger log = LoggerFactory.getLogger(Graph.class);
    private static Driver driver;
    private static final ThreadLocal<Bound> bound = new ThreadLocal<>();

    // The transaction of a thread, its state and its commit actions.
    private static final class Bound {
        final Transaction tx;
        final List<Runnable> afterCommit = new ArrayList<>();
        boolean failed;

        Bound(Transaction tx) {
            this.tx = tx;
        }
    }

    private Graph() {}

    public static void connect(String uri, String user, String password) {
        // The driver logs only warnings and errors
        final Config config = Config.builder()
                .withLogging(Logging.javaUtilLogging(java.util.logging.Level.WARNING))
                .build();
        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password), config);
        try {
            driver.verifyConnectivity();
        } catch (Neo4jException e) {
            driver.close();
            throw new GraphException(e);
        }
    }

    public static void close() {
        driver.close();
    }

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
        List<Runnable> actions;
        try (Session session = driver.session();
             Transaction tx = session.beginTransaction()) {
            Bound b = new Bound(tx);
            bound.set(b);
            result = work.get();
            if (b.failed) {
                throw new GraphException("Nested transaction failed");
            }
            tx.commit();
            actions = b.afterCommit;
        } catch (Neo4jException e) {
            throw new GraphException(e);
        } finally {
            bound.remove();
        }
        for (Runnable action : actions) {
            try {
                action.run();
            } catch (RuntimeException e) {
                log.error("Action after commit failed", e);
            }
        }
        return result;
    }

    // Eclipse picks this overload for () -> expr; use { return expr; }
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

    // Use the bound transaction if there is one, else auto-commit.
    public static List<Map<String, Object>> query(String cypher, Map<String, Object> params) {
        Bound b = bound.get();
        try {
            if (b != null) {
                return rows(b.tx.run(cypher, params));
            }
            try (Session session = driver.session()) {
                return rows(session.run(cypher, params));
            }
        } catch (Neo4jException e) {
            throw new GraphException(e);
        }
    }

    public static List<Map<String, Object>> query(String cypher) {
        return query(cypher, Collections.emptyMap());
    }

    // Put a label, relationship type or property key in backticks
    public static String escape(String name) {
        return "`" + name.replace("`", "``") + "`";
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> rows(Result result) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Record record : result.list()) {
            rows.add((Map<String, Object>) plain(record.asMap()));
        }
        return rows;
    }

    // Convert driver values to plain Java values and GraphNode.
    @SuppressWarnings("unchecked")
    private static Object plain(Object value) {
        if (value instanceof Node) {
            Node node = (Node) value;
            Set<String> labels = new HashSet<>();
            node.labels().forEach(labels::add);
            return new GraphNode(labels, node.asMap());
        }
        if (value instanceof Relationship || value instanceof Path) {
            throw new IllegalArgumentException("Return nodes or values only");
        }
        if (value instanceof List) {
            return ((List<?>) value).stream().map(Graph::plain).collect(toList());
        }
        if (value instanceof Map) {
            Map<String, Object> map = new LinkedHashMap<>();
            ((Map<String, ?>) value).forEach((k, v) -> map.put(k, plain(v)));
            return map;
        }
        return value;
    }
}
