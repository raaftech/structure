package tech.raaf.structure.graph.repositories;

import com.google.common.collect.ImmutableMap;
import tech.raaf.structure.graph.Graph;
import tech.raaf.structure.graph.GraphNode;
import tech.raaf.structure.graph.Node;
import tech.raaf.structure.graph.NodeMapper;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds the nodes with one label. The subclasses add their own queries.
 *
 * @param <T> The domain class of the nodes.
 */
public abstract class GenericRepository<T extends Node> {

    private final String label;

    protected GenericRepository(String label) {
        this.label = label;
    }

    public T findByUuid(String uuid) {
        return one("MATCH (n:" + label + " {uuid: $uuid}) RETURN n", ImmutableMap.of("uuid", uuid));
    }

    public Set<T> findByName(String name) {
        return set("MATCH (n:" + label + " {name: $name}) RETURN n", ImmutableMap.of("name", name));
    }

    public T findOneByName(String name) {
        return one("MATCH (n:" + label + " {name: $name}) RETURN n LIMIT 1", ImmutableMap.of("name", name));
    }

    public List<T> findAll() {
        return NodeMapper.mapAll(Graph.query("MATCH (n:" + label + ") RETURN n"), "n");
    }

    public long count() {
        return number("MATCH (n:" + label + ") RETURN count(n) AS n", ImmutableMap.of());
    }

    public void delete(T node) {
        Graph.query("MATCH (n:Node {uuid: $uuid}) DETACH DELETE n", ImmutableMap.of("uuid", node.getUuid()));
    }

    // The nodes in column n, as a set
    protected static <N extends Node> Set<N> set(String cypher, Map<String, Object> params) {
        return NodeMapper.mapAllToSet(Graph.query(cypher, params), "n");
    }

    // The node in column n of the first row, or null
    protected static <N extends Node> N one(String cypher, Map<String, Object> params) {
        final List<Map<String, Object>> rows = Graph.query(cypher, params);
        return rows.isEmpty() ? null : NodeMapper.map((GraphNode) rows.get(0).get("n"));
    }

    // The value in column n of the first row, or null
    @SuppressWarnings("unchecked")
    protected static <V> V value(String cypher, Map<String, Object> params) {
        final List<Map<String, Object>> rows = Graph.query(cypher, params);
        return rows.isEmpty() ? null : (V) rows.get(0).get("n");
    }

    // The number in column n of the first row; the driver returns Long
    protected static long number(String cypher, Map<String, Object> params) {
        final Number n = value(cypher, params);
        return n == null ? 0 : n.longValue();
    }
}
