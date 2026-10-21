package tech.raaf.structure.graph.repositories;

import com.google.common.collect.ImmutableMap;
import tech.raaf.structure.graph.Graph;
import tech.raaf.structure.graph.Node;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Give a description of NodeRepository here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-11-26
 */
public class NodeRepository extends GenericRepository<Node> {

    public NodeRepository() {
        super("Node");
    }

    public void purge() {
        Graph.query("MATCH (n) DETACH DELETE n");
    }

    /**
     * Get all the ISA parents' names + a node's own name.
     * This is used to query all the names of a type so we know what it specializes.
     * @param uuid The uuid of the node.
     * @return The found names.
     */
    public Set<String> getTypeHierarchyForTypeUuid(String uuid) {
        final Set<String> names = new HashSet<>();
        Graph.query("MATCH (n:Node {uuid: $uuid})-[:ISA*0..]->(t) RETURN DISTINCT t.name AS name",
                ImmutableMap.of("uuid", uuid)).forEach(row -> names.add((String) row.get("name")));
        return names;
    }

    public static class RelationshipTuple {
        public String name;
        public String adjacentUuid;
    }

    public Set<RelationshipTuple> getIncomingRelationships(String uuid) {
        return tuples(Graph.query("MATCH (entity:Node {uuid: $uuid})<-[r]-(adjacent) " +
                "RETURN type(r) AS name, adjacent.uuid AS adjacentUuid", ImmutableMap.of("uuid", uuid)));
    }

    public Set<RelationshipTuple> getOutgoingRelationships(String uuid) {
        return tuples(Graph.query("MATCH (adjacent)<-[r]-(entity:Node {uuid: $uuid}) " +
                "RETURN type(r) AS name, adjacent.uuid AS adjacentUuid", ImmutableMap.of("uuid", uuid)));
    }

    private static Set<RelationshipTuple> tuples(List<Map<String, Object>> rows) {
        final Set<RelationshipTuple> tuples = new HashSet<>();
        for (Map<String, Object> row : rows) {
            final RelationshipTuple tuple = new RelationshipTuple();
            tuple.name = (String) row.get("name");
            tuple.adjacentUuid = (String) row.get("adjacentUuid");
            tuples.add(tuple);
        }
        return tuples;
    }
}
