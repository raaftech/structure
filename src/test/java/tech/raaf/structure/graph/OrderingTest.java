package tech.raaf.structure.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.raaf.structure.helper.JsonHelper.mapper;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.helper.Triple;
import tech.raaf.structure.schema.TypeSchemaTest;
import tools.jackson.databind.JsonNode;

/**
 * Checks the order of hierarchy paths, of the last created instances
 * and of serialized relationships. The tests use many nodes, so that
 * an order that is correct by chance does not pass.
 */
public class OrderingTest extends TypeSchemaTest {

  private static final String GROUP = "5f1eb7c5-ec5e-4563-be6a-fc50774fc374";
  private static final String LINUX = "a5613527-2f64-4481-83f2-80b0cb427e28";

  @Test
  public void hierarchyPath() {
    int count = 0;
    // Only the bundle loader sets path, other tests make types too
    for (Map<String, Object> row : Graph.query("MATCH (t:Type) WHERE t.path IS NOT NULL RETURN t.uuid AS uuid")) {
      String uuid = (String) row.get("uuid");
      assertEquals(walk(uuid), Context.getTypeRepository().getHierarchyPath(uuid));
      count++;
    }
    assertTrue(count > 50, "Too few types: " + count);
  }

  @Test
  public void nLastCreated() {
    // Newer than the bundle instances, which also have created
    final long base =
      ((Number) Graph.query("MATCH (n:Node) RETURN max(n.created) AS max").get(0).get("max")).longValue() + 1000;
    // Three nodes for each value of created, so that the uuid decides
    List<String[]> created = new ArrayList<>();
    for (int i = 0; i < 30; i++) {
      Entity group = new Entity();
      group.setName("ordering-" + i);
      group.setDesc("A group for the ordering test.");
      group.setUuid(UUID.randomUUID().toString());
      group.setRelations(ImmutableSet.of(Triple.of("INSTANCE_OF", "outgoing", ImmutableSet.of(GROUP))));
      group.toGraph();
      long time = base + i / 3;
      Graph.query(
        "MATCH (n:Node {uuid: $uuid}) SET n.created = $created",
        ImmutableMap.of("uuid", group.getUuid(), "created", time)
      );
      created.add(new String[] { Long.toString(time), group.getUuid() });
    }
    // Newest first; the same time sorts by uuid, also descending
    created.sort(
      Comparator.<String[], Long>comparing(c -> Long.parseLong(c[0]))
        .thenComparing(c -> c[1])
        .reversed()
    );
    List<String> expected = created
      .stream()
      .limit(20)
      .map(c -> c[1])
      .collect(Collectors.toList());
    List<String> last = Context.getInstanceRepository()
      .nLastCreated(20)
      .stream()
      .map(Instance::getUuid)
      .collect(Collectors.toList());
    assertEquals(expected, last);
    List<String> filtered = Context.getInstanceRepository()
      .nLastCreated(20, "Group")
      .stream()
      .map(Instance::getUuid)
      .collect(Collectors.toList());
    assertEquals(expected, filtered);
  }

  @Test
  public void relationshipOrder() {
    // 60 peers, more than the dense node threshold of 50
    List<String> peers = new ArrayList<>();
    for (int i = 0; i < 60; i++) {
      peers.add(UUID.randomUUID().toString());
    }
    Graph.query(
      "MATCH (linux:Node {uuid: $linux}) UNWIND $peers AS peer " + "CREATE (linux)-[:ORDERED]->(:Node {uuid: peer})",
      ImmutableMap.of("linux", LINUX, "peers", peers)
    );
    Collections.sort(peers);
    JsonNode json = mapper.valueToTree(Context.getNodeRepository().findByUuid(LINUX));
    List<String> to = new ArrayList<>();
    json
      .get("meta")
      .get("relations")
      .get("ORDERED")
      .get("to")
      .forEach(n -> to.add(n.asString()));
    assertEquals(peers, to);
  }

  // The names from the root type down to the type, by single ISA steps
  private static String walk(String uuid) {
    List<String> names = new ArrayList<>();
    String current = uuid;
    while (current != null) {
      List<Map<String, Object>> rows = Graph.query(
        "MATCH (t:Node {uuid: $uuid}) " + "OPTIONAL MATCH (t)-[:ISA]->(p) RETURN t.name AS name, p.uuid AS parent",
        ImmutableMap.of("uuid", current)
      );
      names.add(0, (String) rows.get(0).get("name"));
      current = (String) rows.get(0).get("parent");
    }
    return String.join(".", names);
  }
}
