package tech.raaf.structure.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.raaf.structure.helper.classification.ArgumentsToClassificationTreeParser.matchesTypePath;

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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tech.raaf.structure.base.Configuration;
import tech.raaf.structure.base.Configuration.Setting;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.helper.InstrumentRunner;
import tech.raaf.structure.helper.loader.NaiveSorter;
import tech.raaf.structure.schema.TypeSchemaTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public class BundleGraphTest extends TypeSchemaTest {

  private static final Path BUNDLES = Configuration.get(Setting.HOME).resolve("src/main/resources/bundles");
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final List<JsonNode> entities = new ArrayList<>();

  @BeforeAll
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
    assertTrue(!entities.isEmpty(), "No bundle entities found in " + BUNDLES);
  }

  @Test
  public void allBundleEntitiesAreStored() {
    List<Map<String, Object>> rows = Graph.query("MATCH (n) WHERE n.path IS NOT NULL RETURN n.uuid AS uuid");
    assertEquals(entities.size(), rows.size(), "Number of stored bundle entities");
    Set<String> expected = entities
      .stream()
      .map(entity -> entity.path("meta").path("uuid").asString())
      .collect(Collectors.toSet());
    Set<Object> actual = rows
      .stream()
      .map(row -> row.get("uuid"))
      .collect(Collectors.toSet());
    assertEquals(expected, actual, "Stored bundle uuids");
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
          assertTrue(file.path("content").isString(), describe(node) + ": missing content in " + property.getKey());
        }
      }
    }
    for (JsonNode entity : entities) {
      String uuid = entity.path("meta").path("uuid").asString();
      GraphNode node = stored.get(uuid);
      assertNotNull(node, "Missing bundle entity " + uuid);
      entity
        .path("file")
        .propertyNames()
        .forEach(name ->
          assertTrue(node.getProperties().containsKey("file-" + name), describe(node) + ": missing file-" + name)
        );
      if (node.getLabels().contains("Type")) {
        assertTrue(node.getProperties().containsKey("file-dataSchema"), describe(node) + ": missing data schema");
      }
      if (node.getLabels().contains("Controller")) {
        assertTrue(
          node.getProperties().containsKey("file-templateFile") || node.getProperties().containsKey("file-script"),
          describe(node) + ": missing template or script"
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
        .properties()
        .forEach(argument -> {
          JsonNode value = argument.getValue().path("value");
          if (!"static".equals(value.path("from").asString())) {
            String typePath = value.path("type").asString();
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
    assertTrue(errors.isEmpty(), String.join("\n", errors));
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
      assertNotNull(appliedPath, describe(instrument) + ": missing hierarchy path for " + row.get("type"));
      arguments(instrument)
        .properties()
        .forEach(argument -> {
          JsonNode value = argument.getValue().path("value");
          if ("origin".equals(value.path("from").asString())) {
            String typePath = value.path("type").asString();
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
    assertTrue(errors.isEmpty(), String.join("\n", errors));
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
      Component listener = new Component(
        mapper
          .createObjectNode()
          .put("name", "ssh")
          .put("desc", "A listener.")
          .put("addr", "192.0.2.10")
          .put("port", 22),
        Context.getTypeRepository().findOneByName("Ipv4Listener")
      );
      assertJob(
        listener,
        "NmapClientVerifyListener",
        "sh -c 'nmap -Pn -n -p \"$2\" -oG - \"$1\" | grep -q \"Ports: $2/open/\"' nmap '192.0.2.10' '22'",
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
      .map(entity -> entity.path("name").asString() + " (" + entity.path("meta").path("uuid").asString() + ")")
      .collect(Collectors.toList());
    assertTrue(updates.isEmpty(), "Unchanged bundle entities need updates: " + names);
  }

  private static void assertDiscoveryCommand(Component origin, String instrument, String target) {
    String home = Configuration.get(Setting.HOME).toString();
    assertJob(
      origin,
      instrument,
      "node '" + home + "/src/main/resources/bundles/nmap/payload/main.js' " + target,
      60000
    );
  }

  private static void assertJob(Component origin, String instrument, String command, long timeAllocated) {
    origin.save();
    Controller controller = (Controller) Context.getInstrumentRepository().findOneByName(instrument);
    assertNotNull(controller);
    ObjectNode arguments = mapper.createObjectNode();
    InstrumentRunner runner = new InstrumentRunner(origin, controller, arguments, "discovery");
    assertEquals(1, runner.getJobs().size());
    assertEquals(command, runner.getJobs().get(0).getLeft());
    assertEquals(Long.valueOf(timeAllocated), runner.getJobs().get(0).getRight().get());
  }

  // Ping runs only on an address of something that has an ICMP server
  @Test
  public void pingNeedsIcmpServerWithAddress() {
    Group agent = new Group(
      mapper.createObjectNode().put("name", "Structure").put("desc", "The Structure host."),
      Context.getTypeRepository().findOneByName("Group")
    );
    Component host = software("Linux");
    host.addComponent(software("Iputils"));
    agent.addMember(host);
    agent.save();
    Context.setSelfGroup(agent);
    Context.setSelfIdentifier(agent.getUuid());
    try {
      for (boolean icmp : Arrays.asList(false, true)) {
        Component address = new Component(
          mapper
            .createObjectNode()
            .put("name", "host")
            .put("desc", "An address.")
            .put("addr", "192.0.2.20")
            .put("reachable", false),
          Context.getTypeRepository().findOneByName("Ipv4Address")
        );
        Component os = software("Linux").addComponent(address);
        if (icmp) {
          os.addComponent(software("IcmpServer"));
        }
        os.save();
        Controller ping = (Controller) Context.getInstrumentRepository().findOneByName("IputilsPingCheckable");
        InstrumentRunner runner = new InstrumentRunner(address, ping, mapper.createObjectNode(), "ping");
        assertEquals(icmp ? 1 : 0, runner.getJobs().size(), "Jobs with an IcmpServer: " + icmp);
      }
    } finally {
      Context.setSelfGroup(null);
      Context.setSelfIdentifier(null);
    }
  }

  // Invoke takes the shell with the most weight: the most recent shell
  @Test
  public void invokeTakesHeaviestShell() {
    assertEquals("ZshInvoke", invokeWith("Sh", "Ksh", "Csh", "Tcsh", "Bash", "Zsh"));
    assertEquals("BashInvoke", invokeWith("Tcsh", "Bash"));
    assertEquals("TcshInvoke", invokeWith("Sh", "Tcsh"));
    assertEquals("KshInvoke", invokeWith("Ksh", "Sh"));
  }

  // The discovery script runs on each Python, thus also on Python 2
  @Test
  public void pythonDiscoverNeedsPython() {
    for (boolean structure : Arrays.asList(false, true)) {
      Component agentHost = software("Linux");
      if (structure) {
        agentHost.addComponent(software("Structure"));
      }
      Group agent = subject(agentHost);
      for (boolean python : Arrays.asList(false, true)) {
        Component host = software("Linux");
        if (python) {
          host.addComponent(software("Python2x"));
        }
        subject(host);
        List<String> names = Context.getInstrumentRepository()
          .findAvailableControllers(host.getUuid(), agent.getUuid(), "Detect")
          .stream()
          .map(Controller::getName)
          .collect(Collectors.toList());
        assertEquals(structure && python, names.contains("PythonDiscover"));
      }
    }
  }

  private static String invokeWith(String... shells) {
    Component host = software("Linux");
    for (String shell : shells) {
      host.addComponent(software(shell));
    }
    return Context.getInstrumentRepository().getInvokableInterpreter(subject(host).getUuid()).getName();
  }

  private static Group subject(Component host) {
    Group group = new Group(
      mapper.createObjectNode().put("name", "subject").put("desc", "A subject."),
      Context.getTypeRepository().findOneByName("Group")
    );
    group.addMember(host);
    group.save();
    return group;
  }

  private static Component software(String type) {
    return new Component(
      mapper
        .createObjectNode()
        .put("name", type)
        .put("desc", "A " + type + ".")
        .put("shortName", type.toLowerCase())
        .put("longName", type)
        .put("ver", "unknown"),
      Context.getTypeRepository().findOneByName(type)
    );
  }

  // Ping, an ICMP client, finds its server through the ICMP protocol
  @Test
  public void pingAndIcmpServerImplementIcmp() {
    List<Object> protocols = Graph.query(
      "MATCH (:Type {name: 'Iputils'})-[:ISA*0..]->()-[:IMPLEMENTS]->(protocol)" +
        "<-[:IMPLEMENTS]-()<-[:ISA*0..]-(:Type {name: 'IcmpServer'}) RETURN DISTINCT protocol.name AS name"
    )
      .stream()
      .map(row -> row.get("name"))
      .collect(Collectors.toList());
    assertEquals(Collections.singletonList("ICMP"), protocols);
  }

  // Detect gets all profiles of a type tree, the deepest types last
  @Test
  public void profilesOfTypeTreeAreSortedByDepth() {
    Type component = Context.getTypeRepository().findOneByName("Component");
    List<Profile> profiles = Context.getProfileRepository().findAllProfilesForType(component.getUuid());
    long expected = entities
      .stream()
      .filter(entity ->
        entity.path("meta").path("relations").path("INSTANCE_OF").path("to").path(0).asString().equals("Profile")
      )
      .count();
    assertEquals(expected, profiles.size(), "Profiles under Component");
    List<Integer> depths = profiles
      .stream()
      .map(profile -> profile.getAppliesTo().getHierarchyPath().split("\\.").length)
      .collect(Collectors.toList());
    List<Integer> sorted = new ArrayList<>(depths);
    Collections.sort(sorted);
    assertEquals(sorted, depths, "Depths of the profile types");
  }

  private static Map<String, String> typePaths() {
    Map<String, String> paths = new LinkedHashMap<>();
    for (Type type : Context.getTypeRepository().getAll()) {
      String path = type.getHierarchyPath();
      assertNotNull(path, type.getName() + ": missing hierarchy path");
      paths.put(type.getUuid(), path);
    }
    assertTrue(!paths.isEmpty(), "No types loaded from bundles");
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
