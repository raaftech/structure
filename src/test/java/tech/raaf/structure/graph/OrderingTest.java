package tech.raaf.structure.graph;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.tooling.GlobalGraphOperations;
import org.springframework.beans.factory.annotation.Autowired;
import tech.raaf.structure.graph.repositories.InstanceRepository;
import tech.raaf.structure.graph.repositories.NodeRepository;
import tech.raaf.structure.graph.repositories.TypeRepository;
import tech.raaf.structure.helper.Triple;
import tech.raaf.structure.schema.TypeSchemaTest;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.vertx.core.json.Json.mapper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Checks the order of hierarchy paths, of the last created instances
 * and of serialized relationships. The tests use many nodes, so that
 * an order that is correct by chance does not pass. The tests do not
 * commit.
 */
public class OrderingTest extends TypeSchemaTest {

    private static final String GROUP = "5f1eb7c5-ec5e-4563-be6a-fc50774fc374";
    private static final String LINUX = "a5613527-2f64-4481-83f2-80b0cb427e28";
    private static final RelationshipType ISA = DynamicRelationshipType.withName("ISA");
    private static final RelationshipType ORDERED = DynamicRelationshipType.withName("ORDERED");

    @Autowired
    GraphDatabaseService graphDatabaseService;
    @Autowired
    TypeRepository typeRepository;
    @Autowired
    InstanceRepository instanceRepository;
    @Autowired
    NodeRepository nodeRepository;

    @Test
    public void hierarchyPath() {
        try (Transaction tx = graphDatabaseService.beginTx()) {
            int count = 0;
            for (Node type : GlobalGraphOperations.at(graphDatabaseService)
                    .getAllNodesWithLabel(DynamicLabel.label("Type"))) {
                // Only the bundle loader sets path, other tests make types too
                if (type.hasProperty("path")) {
                    assertEquals(walk(type), typeRepository.getHierarchyPath((String) type.getProperty("uuid")));
                    count++;
                }
            }
            assertTrue("Too few types: " + count, count > 50);
        }
    }

    @Test
    public void nLastCreated() {
        try (Transaction tx = graphDatabaseService.beginTx()) {
            List<String> created = new ArrayList<>();
            for (int i = 0; i < 30; i++) {
                Entity group = new Entity();
                group.setName("ordering-" + i);
                group.setDesc("A group for the ordering test.");
                group.setUuid(UUID.randomUUID().toString());
                group.setRelations(ImmutableSet.of(Triple.of("INSTANCE_OF", "outgoing", ImmutableSet.of(GROUP))));
                group.toGraph();
                created.add(group.getUuid());
            }
            Collections.reverse(created);
            List<String> last = instanceRepository.getNLastCreated(20).stream()
                    .map(Instance::getUuid)
                    .collect(Collectors.toList());
            assertEquals(created.subList(0, 20), last);
        }
    }

    @Test
    public void relationshipOrder() {
        try (Transaction tx = graphDatabaseService.beginTx()) {
            // 60 peers, more than the dense node threshold of 50
            List<String> peers = new ArrayList<>();
            for (int i = 0; i < 60; i++) {
                peers.add(UUID.randomUUID().toString());
            }
            Node linux = nodeRepository.findGraphNodeByUuid(LINUX);
            for (String uuid : peers) {
                Node peer = graphDatabaseService.createNode(DynamicLabel.label("Node"));
                peer.setProperty("uuid", uuid);
                linux.createRelationshipTo(peer, ORDERED);
            }
            Collections.sort(peers);
            JsonNode json = mapper.valueToTree(nodeRepository.findByUuid(LINUX));
            List<String> to = new ArrayList<>();
            json.get("meta").get("relations").get("ORDERED").get("to").forEach(n -> to.add(n.asText()));
            assertEquals(peers, to);
        }
    }

    private static String walk(Node type) {
        Deque<String> names = new ArrayDeque<>();
        for (Node n = type; n != null; n = parent(n)) {
            names.addFirst((String) n.getProperty("name"));
        }
        return String.join(".", names);
    }

    private static Node parent(Node type) {
        Relationship isa = type.getSingleRelationship(ISA, Direction.OUTGOING);
        return isa == null ? null : isa.getEndNode();
    }
}
