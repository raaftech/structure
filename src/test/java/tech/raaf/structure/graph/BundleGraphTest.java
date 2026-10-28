package tech.raaf.structure.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static tech.raaf.structure.helper.classification.ArgumentsToClassificationTreeParser.matchesTypePath;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.BeforeClass;
import org.junit.Test;
import tech.raaf.structure.base.Configuration;
import tech.raaf.structure.base.Configuration.Setting;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.helper.InstrumentRunner;
import tech.raaf.structure.helper.loader.NaiveSorter;
import tech.raaf.structure.schema.TypeSchemaTest;

public class BundleGraphTest extends TypeSchemaTest {

  private static final Path BUNDLES = Configuration.get(Setting.HOME).resolve("src/main/resources/bundles");
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final List<JsonNode> entities = new ArrayList<>();

  @BeforeClass
  public static void readBundleFiles() throws IOException {
    List<Path> files;
    try (Stream<Path> paths = Files.walk(BUNDLES)) {
      files = paths
        .filter(Files::isRegularFile)
        .filter(path -> path.toString().endsWith(".type.json") || path.toString().endsWith(".inst.json"))
        .sorted()
        .collect(Collectors.toList());
    }
    for (Path path : files) {
      JsonNode document = mapper.readTree(path.toFile());
      for (JsonNode entity : document.isArray() ? document : Arrays.asList(document)) {
        entities.add(entity);
      }
    }
    assertTrue("No bundle entities found in " + BUNDLES, !entities.isEmpty());
  }

  @Test
  public void allBundleEntitiesAreStored() {
    List<Map<String, Object>> rows = Graph.query("MATCH (n) WHERE n.path IS NOT NULL RETURN n.uuid AS uuid");
    assertEquals("Number of stored bundle entities", entities.size(), rows.size());
    Set<String> expected = entities
      .stream()
      .map(entity -> entity.path("meta").path("uuid").asText())
      .collect(Collectors.toSet());
    Set<Object> actual = rows
      .stream()
      .map(row -> row.get("uuid"))
      .collect(Collectors.toSet());
    assertEquals("Stored bundle uuids", expected, actual);
  }

  @Test
  public void bundleFilesHaveContent() throws IOException {
    Map<String, GraphNode> stored = new LinkedHashMap<>();
    for (Map<String, Object> row : Graph.query("MATCH (n) WHERE n.path IS NOT NULL RETURN n")) {
      GraphNode node = (GraphNode) row.get("n");
      stored.put((String) node.getProperties().get("uuid"), node);
      for (Map.Entry<String, Object> property : node.getProperties().entrySet()) {
        if (property.getKey().startsWith("file-")) {
          JsonNode file = mapper.readTree((String) property.getValue());
          assertTrue(describe(node) + ": missing content in " + property.getKey(), file.path("content").isTextual());
        }
      }
    }
    for (JsonNode entity : entities) {
      String uuid = entity.path("meta").path("uuid").asText();
      GraphNode node = stored.get(uuid);
      assertNotNull("Missing bundle entity " + uuid, node);
      entity
        .path("file")
        .fieldNames()
        .forEachRemaining(name ->
          assertTrue(describe(node) + ": missing file-" + name, node.getProperties().containsKey("file-" + name))
        );
      if (node.getLabels().contains("Type")) {
        assertTrue(describe(node) + ": missing data schema", node.getProperties().containsKey("file-dataSchema"));
      }
      if (node.getLabels().contains("Controller")) {
        assertTrue(
          describe(node) + ": missing template or script",
          node.getProperties().containsKey("file-templateFile") || node.getProperties().containsKey("file-script")
        );
      }
    }
  }

  @Test
  public void instrumentArgumentTypesExist() throws IOException {
    Map<String, String> paths = typePaths();
    List<String> errors = new ArrayList<>();
    for (Map<String, Object> row : Graph.query("MATCH (n:Instrument) RETURN n ORDER BY n.uuid")) {
      GraphNode instrument = (GraphNode) row.get("n");
      arguments(instrument)
        .fields()
        .forEachRemaining(argument -> {
          JsonNode value = argument.getValue().path("value");
          if (!"static".equals(value.path("from").asText())) {
            String typePath = value.path("type").asText();
            if (
              paths
                .values()
                .stream()
                .noneMatch(path -> matchesTypePath(typePath, path))
            ) {
              errors.add(
                describe(instrument) + ": argument " + argument.getKey() + " has no matching type: " + typePath
              );
            }
          }
        });
    }
    assertTrue(String.join("\n", errors), errors.isEmpty());
  }

  @Test
  public void originArgumentsAgreeWithAppliedTypes() throws IOException {
    Map<String, String> paths = typePaths();
    List<String> errors = new ArrayList<>();
    for (Map<String, Object> row : Graph.query(
      "MATCH (n:Instrument)-[:APPLIES_TO]->(t:Type) RETURN n, t.uuid AS type ORDER BY n.uuid, t.uuid"
    )) {
      GraphNode instrument = (GraphNode) row.get("n");
      String appliedPath = paths.get(row.get("type"));
      assertNotNull(describe(instrument) + ": missing hierarchy path for " + row.get("type"), appliedPath);
      arguments(instrument)
        .fields()
        .forEachRemaining(argument -> {
          JsonNode value = argument.getValue().path("value");
          if ("origin".equals(value.path("from").asText())) {
            String typePath = value.path("type").asText();
            if (!matchesTypePath(typePath, appliedPath) && !matchesTypePath(appliedPath, typePath)) {
              errors.add(
                describe(instrument) +
                  ": origin argument " +
                  argument.getKey() +
                  " uses " +
                  typePath +
                  ", APPLIES_TO uses " +
                  appliedPath
              );
            }
          }
        });
    }
    assertTrue(String.join("\n", errors), errors.isEmpty());
  }

  @Test
  public void discoveryUsesTheOriginAndInstalledTool() throws IOException {
    Group agent = new Group(
      mapper.createObjectNode().put("name", "Structure").put("desc", "The Structure host."),
      Context.getTypeRepository().findOneByName("Group")
    );
    Component host = new Component(
      mapper.createObjectNode().put("name", "host").put("desc", "The Structure host."),
      Context.getTypeRepository().findOneByName("Component")
    );
    host.addComponent(
      new Component(
        mapper
          .createObjectNode()
          .put("name", "Structure")
          .put("desc", "The Structure application.")
          .put("shortName", "structure")
          .put("longName", "Structure")
          .put("ver", "unknown"),
        Context.getTypeRepository().findOneByName("Structure")
      )
    );
    host.addComponent(
      new Component(
        mapper
          .createObjectNode()
          .put("name", "NmapClient")
          .put("desc", "The nmap network scanner.")
          .put("shortName", "nmap")
          .put("longName", "Nmap")
          .put("ver", "unknown"),
        Context.getTypeRepository().findOneByName("NmapClient")
      )
    );
    agent.addMember(host);
    agent.save();
    Context.setSelfGroup(agent);
    Context.setSelfIdentifier(agent.getUuid());
    try {
      Component address = new Component(
        mapper
          .createObjectNode()
          .put("name", "host")
          .put("desc", "An existing address.")
          .put("addr", "192.0.2.10")
          .put("reachable", false),
        Context.getTypeRepository().findOneByName("Ipv4Address")
      );
      assertDiscoveryCommand(address, "IpAddressDiscover", "-i '192.0.2.10'");
      assertJob(
        address,
        "NmapClientVerify",
        "sh -c 'nmap -sn -n -oG - \"$1\" | grep -q \"Status: Up\"' nmap '192.0.2.10'",
        10000
      );
      for (String cidr : Arrays.asList("192.0.2.0/24", "192.0.2.10/32")) {
        Component network = new Component(
          mapper.createObjectNode().put("name", "network").put("desc", "A network.").put("cidr", cidr),
          Context.getTypeRepository().findOneByName("Ipv4Network")
        );
        assertDiscoveryCommand(network, "IpNetworkDiscover", "-c '" + cidr + "'");
      }
    } finally {
      Context.setSelfGroup(null);
      Context.setSelfIdentifier(null);
    }
  }

  @Test
  public void unchangedBundlesNeedNoUpdates() throws Exception {
    NaiveSorter sorter = new NaiveSorter(Collections.singleton(BUNDLES));
    List<JsonNode> updates = sorter.getUpdateList(sorter.sortIt(sorter.readAllJson()));
    List<String> names = updates
      .stream()
      .map(entity -> entity.path("name").asText() + " (" + entity.path("meta").path("uuid").asText() + ")")
      .collect(Collectors.toList());
    assertTrue("Unchanged bundle entities need updates: " + names, updates.isEmpty());
  }

  private static void assertDiscoveryCommand(Component origin, String instrument, String target) {
    String home = Configuration.get(Setting.HOME).toString();
    assertJob(origin, instrument, "node '" + home + "'/ext/util/net-discovery/main.js " + target + " -t 'test-token'", 60000);
  }

  private static void assertJob(Component origin, String instrument, String command, long timeAllocated) {
    origin.save();
    Controller controller = (Controller) Context.getInstrumentRepository().findOneByName(instrument);
    assertNotNull(controller);
    ObjectNode arguments = mapper.createObjectNode().put("token", "test-token");
    InstrumentRunner runner = new InstrumentRunner(origin, controller, arguments, "discovery");
    assertEquals(1, runner.getJobs().size());
    assertEquals(command, runner.getJobs().get(0).getLeft());
    assertEquals(Long.valueOf(timeAllocated), runner.getJobs().get(0).getRight().get());
  }

  // Detect gets all profiles of a type tree, the deepest types last
  @Test
  public void profilesOfTypeTreeAreSortedByDepth() {
    Type component = Context.getTypeRepository().findOneByName("Component");
    List<Profile> profiles = Context.getProfileRepository().findAllProfilesForType(component.getUuid());
    long expected = entities
      .stream()
      .filter(entity -> entity.path("meta").path("relations").path("INSTANCE_OF").path("to").path(0).asText().equals("Profile"))
      .count();
    assertEquals("Profiles under Component", expected, profiles.size());
    List<Integer> depths = profiles
      .stream()
      .map(profile -> profile.getAppliesTo().getHierarchyPath().split("\\.").length)
      .collect(Collectors.toList());
    List<Integer> sorted = new ArrayList<>(depths);
    Collections.sort(sorted);
    assertEquals("Depths of the profile types", sorted, depths);
  }

  private static Map<String, String> typePaths() {
    Map<String, String> paths = new LinkedHashMap<>();
    for (Type type : Context.getTypeRepository().getAll()) {
      String path = type.getHierarchyPath();
      assertNotNull(type.getName() + ": missing hierarchy path", path);
      paths.put(type.getUuid(), path);
    }
    assertTrue("No types loaded from bundles", !paths.isEmpty());
    return paths;
  }

  private static JsonNode arguments(GraphNode instrument) throws IOException {
    String json = (String) instrument.getProperties().get("data-arguments");
    return json == null ? mapper.createObjectNode() : mapper.readTree(json);
  }

  private static String describe(GraphNode node) {
    Map<String, Object> properties = node.getProperties();
    return properties.get("path") + " (" + properties.get("name") + ", " + properties.get("uuid") + ")";
  }
}
