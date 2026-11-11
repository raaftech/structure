package tech.raaf.structure.bus.manager;

import static org.junit.jupiter.api.Assertions.*;
import static tech.raaf.structure.helper.JsonHelper.mapper;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.bus.GenericActor;
import tech.raaf.structure.graph.Component;
import tech.raaf.structure.graph.Entity;
import tech.raaf.structure.graph.Graph;
import tech.raaf.structure.graph.Node;
import tech.raaf.structure.graph.repositories.NodeRepository;
import tech.raaf.structure.helper.loader.NaiveSorter;
import tech.raaf.structure.schema.TypeSchemaTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public class EntityManagerTest extends TypeSchemaTest {

  private final NodeRepository nodeRepository = Context.getNodeRepository();

  private static final String OPERATING_SYSTEM = "f05e325e-1aee-4892-96e4-8bcd6e97c528";
  private static final String UNIX_LIKE = "6a1eea8b-1462-4439-8c60-12edbee6c397";
  private static final String LINUX = "a5613527-2f64-4481-83f2-80b0cb427e28";
  private static final String COMPONENT_GROUP = "ee92c308-80f4-40b2-af3a-77d26cd99517";
  private static final String DNS_NAME = "99c0da37-3599-418d-8ecc-de36d37ae9f4";
  private static final String OS_DATA = "{\"ver\": \"1\", \"shortName\": \"os\", \"longName\": \"Operating system\"}";

  @Test
  public void testTraversal() throws Exception {
    Node operatingSystem = nodeRepository.findByUuid("f05e325e-1aee-4892-96e4-8bcd6e97c528");
    EntityManager entityManager = new EntityManager();

    List<Node> foundNodes = entityManager.labelBasedTraversal(ImmutableSet.of("type:Component"), operatingSystem, true);
    assertEquals(ImmutableSet.of("OperatingSystem", "Software", "Component", "Configuration"), names(foundNodes));

    foundNodes = entityManager.labelBasedTraversal(ImmutableSet.of("type:Component"), operatingSystem, false);
    assertEquals(ImmutableSet.of("Software", "Component", "Configuration"), names(foundNodes));

    Node ipAddress = nodeRepository.findByUuid("299001a6-1091-4c0d-87be-c512af03e179");
    foundNodes = entityManager.labelBasedTraversal(ImmutableSet.of("type:Configuration"), ipAddress, false);
    assertEquals(ImmutableSet.of("Configuration", "DnsName"), names(foundNodes));

    Node linuxType = nodeRepository.findByUuid("a5613527-2f64-4481-83f2-80b0cb427e28");
    foundNodes = entityManager.labelBasedTraversal(ImmutableSet.of("Type"), linuxType, false);
    assertEquals(
      ImmutableSet.of(
        "UnixLike",
        "OperatingSystem",
        "Software",
        "Component",
        "Type",
        "Result",
        "ComponentGroup",
        "Group",
        "Configuration"
      ),
      names(foundNodes)
    );
  }

  @Test
  public void testCreate() throws Exception {
    EntityManager entityManager = new EntityManager();

    // construct a message that creates a new entity
    String entityUuid = "94fa591f-375b-4fff-84f7-2c6018cc7707";
    ObjectNode message = mapper.createObjectNode();
    message.put("msgh", "create");
    ObjectNode args = message.putObject("args");
    ArrayNode entities = args.putArray("entities");
    entities.add(
      mapper.readTree(
        "  {\n" +
          "    \"name\": \"GenericOS\",\n" +
          "    \"desc\": \"Instance of a generic OS.\",\n" +
          "    \"meta\": {\n" +
          "      \"uuid\": \"" +
          entityUuid +
          "\",\n" +
          "      \"relations\": {\n" +
          "        \"INSTANCE_OF\": {\n" +
          "          \"direction\": \"outgoing\",\n" +
          "          \"to\": [\n" +
          "            \"f05e325e-1aee-4892-96e4-8bcd6e97c528\"\n" +
          "          ]\n" +
          "        }\n" +
          "      }\n" +
          "    },\n" +
          "    \"data\": {\n" +
          "      \"ver\": \"ver\",\n" +
          "      \"shortName\": \"shortName\",\n" +
          "      \"longName\": \"longName\"\n" +
          "    }\n" +
          "  }"
      )
    );

    // send the message constructed above and get the result
    JsonNode response = entityManager.create().exec(message);
    ObjectNode result = (ObjectNode) response.get("result");

    // the result should not have an error property
    assertFalse(result.has("error"), result.toString());
    // it should have a data array
    assertTrue(result.has("data"));
    assertTrue(result.get("data").isArray());

    // the data array should have one element
    ArrayNode data = (ArrayNode) result.get("data");
    assertEquals(1, data.size());

    // that element should be equal to the UUID of the entity we created
    assertEquals(entityUuid, data.get(0).asString());

    // We're going to try to create the same entity again, this shouldn't result in any changes
    // send the message again, unchanged
    response = entityManager.create().exec(message);
    result = (ObjectNode) response.get("result");

    // the result should not have an error property
    assertFalse(result.has("error"), result.toString());
    // because no new entity was created, it should have a data array, but it should be empty
    assertTrue(result.has("data"));
    assertEquals(0, result.get("data").size());
  }

  @Test
  public void testRead() throws Exception {
    EntityManager entityManager = new EntityManager();

    /*
      Construct a message that asks for every type that can be found by walking from
      the "Software" type following outgoing relationships.
    */

    ObjectNode message = mapper.createObjectNode();
    message.put("msgh", "read");
    ObjectNode args = message.putObject("args");
    ArrayNode labels = args.putArray("labels");
    labels.add("Type");
    args.put("start", "8db2b492-9a4a-41fe-97a5-bc625da042ea");

    // send the message constructed above and get the result
    JsonNode response = entityManager.read().exec(message);
    ObjectNode result = (ObjectNode) response.get("result");

    // the result should not have an error property
    assertFalse(result.has("error"), result.toString());
    // it should have a data array
    assertTrue(result.has("data"));
    assertTrue(result.get("data").isArray());

    ArrayNode data = (ArrayNode) result.get("data");

    // the traversal includes the start node
    Set<String> names = new HashSet<>();
    data.forEach(e -> names.add(e.get("name").asString()));
    assertEquals(ImmutableSet.of("Software", "Component", "Type", "Result", "ComponentGroup", "Group"), names);

    // all items in that array should be deserializable to an Entity
    Iterator<JsonNode> iterator = data.values().iterator();
    while (iterator.hasNext()) {
      mapper.treeToValue(iterator.next(), Entity.class);
    }
  }

  @Test
  public void testUpdate() throws Exception {
    EntityManager entityManager = new EntityManager();
    final String componentGroupTypeUuid = "ee92c308-80f4-40b2-af3a-77d26cd99517";
    final String linuxTypeUuid = "a5613527-2f64-4481-83f2-80b0cb427e28";

    // get two nodes, of which we are going to update only one
    Node computerGrouping = nodeRepository.findByUuid(componentGroupTypeUuid);
    Node linuxType = nodeRepository.findByUuid(linuxTypeUuid);

    // set the name of the Linux type to something else
    final String newName = "UpdatedName";
    linuxType.setName(newName);

    // construct a message that updates the name of an entity
    ObjectNode message = mapper.createObjectNode();
    message.put("msgh", "update");
    ObjectNode args = message.putObject("args");
    ArrayNode entities = args.putArray("entities");

    // add the two entities to the array
    entities.add(mapper.valueToTree(computerGrouping));
    entities.add(mapper.valueToTree(linuxType));

    // send the message constructed above and get the result
    JsonNode response = entityManager.update().exec(message);
    ObjectNode result = (ObjectNode) response.get("result");

    // the result should not have an error property
    assertFalse(result.has("error"), result.toString());
    // it should have a data array
    assertTrue(result.has("data"));
    assertTrue(result.get("data").isArray());

    ArrayNode data = (ArrayNode) result.get("data");

    // we should have 1 node contained in the array
    assertEquals(1, data.size());

    // data should contain the UUID of the "Linux" type
    assertEquals(linuxType.getUuid(), data.get(0).asString());

    // retrieve "Linux" from the graph again, check if it was indeed updated
    Assertions.assertEquals(newName, nodeRepository.findByUuid(linuxType.getUuid()).getName());
    // the update keeps the bundle and the path
    Assertions.assertEquals("linux", nodeRepository.findByUuid(linuxType.getUuid()).getBundle());
    Assertions.assertEquals(
      "linux/types/linux.type.json",
      nodeRepository.findByUuid(linuxType.getUuid()).getPath()
    );

    /*
      try an update again, this time a safe update, which means we send the old entities as well,
      and we compare the old versions to the graph version before we update.
    */
    computerGrouping = nodeRepository.findByUuid(componentGroupTypeUuid);
    linuxType = nodeRepository.findByUuid("a5613527-2f64-4481-83f2-80b0cb427e28");

    // add the "original" versions of our entities to the oldEntities array
    ArrayNode oldEntities = args.putArray("oldEntities");
    entities.removeAll();
    oldEntities.add(mapper.valueToTree(computerGrouping));
    oldEntities.add(mapper.valueToTree(linuxType));

    // change the "Computer" entity and add it to our updated entities array
    computerGrouping.setDesc("New description.");
    entities.add(mapper.valueToTree(computerGrouping));
    entities.add(mapper.valueToTree(linuxType));

    // now, edit the "Computer" entity outside of EntityManager and save it
    computerGrouping = nodeRepository.findByUuid(componentGroupTypeUuid);
    computerGrouping.setDesc("Edited outside of EntityManager.");
    Graph.query(
      "MATCH (n:Node {uuid: $uuid}) SET n.desc = $desc, n.checksum = $checksum",
      ImmutableMap.of(
        "uuid",
        componentGroupTypeUuid,
        "desc",
        computerGrouping.getDesc(),
        "checksum",
        NaiveSorter.calculateChecksum(mapper.valueToTree(computerGrouping), NaiveSorter.ChecksumAlgorithm.SHA1)
      )
    );

    // send the message constructed above and get the result
    response = entityManager.update().exec(message);
    result = (ObjectNode) response.get("result");

    // the result should not have an error property
    assertTrue(result.has("error"), result.toString());
  }

  // An entity that is not valid stops the whole update
  @Test
  public void testUpdateIsAllOrNothing() throws Exception {
    EntityManager entityManager = new EntityManager();
    String os = instance(OPERATING_SYSTEM, OS_DATA);
    ObjectNode changed = mapper.valueToTree(nodeRepository.findByUuid(os));
    changed.put("desc", "Changed.");
    ObjectNode broken = mapper.valueToTree(nodeRepository.findByUuid(instance(OPERATING_SYSTEM, OS_DATA)));
    broken.remove("name");
    ObjectNode args = mapper.createObjectNode();
    args.putArray("entities").add(changed).add(broken);

    ObjectNode result = exec(entityManager.update(), "update", args);
    assertTrue(result.has("error"), result.toString());
    assertFalse(result.has("data"), result.toString());
    assertEquals("A write path test node.", nodeRepository.findByUuid(os).getDesc());

    // An update without changes updates no entities
    args = mapper.createObjectNode();
    args.putArray("entities").add(mapper.valueToTree(nodeRepository.findByUuid(UNIX_LIKE)));
    result = exec(entityManager.update(), "update", args);
    assertFalse(result.has("error"), result.toString());
    assertEquals(mapper.createArrayNode(), result.get("data"));
  }

  // A relation to a node that does not exist stops the create. The
  // constraints do not check incoming relations, so use one.
  @Test
  public void testCreateWithUnknownPeer() throws Exception {
    EntityManager entityManager = new EntityManager();
    String uuid = java.util.UUID.randomUUID().toString();
    String unknown = java.util.UUID.randomUUID().toString();
    ObjectNode entity = (ObjectNode) mapper.readTree(entity(uuid, OPERATING_SYSTEM, OS_DATA));
    relations(entity).putObject("HAS").put("direction", "incoming").putArray("to").add(unknown);
    ObjectNode args = mapper.createObjectNode();
    args.putArray("entities").add(entity);

    ObjectNode result = exec(entityManager.create(), "create", args);
    assertTrue(result.path("error").asString().contains(unknown), result.toString());
    assertNull(nodeRepository.findByUuid(uuid));
  }

  @Test
  public void testUpdateDoesNotCreate() throws Exception {
    EntityManager entityManager = new EntityManager();
    String uuid = java.util.UUID.randomUUID().toString();
    ObjectNode unknown = (ObjectNode) mapper.readTree(entity(uuid, OPERATING_SYSTEM, OS_DATA));
    ObjectNode nameless = (ObjectNode) mapper.readTree(entity(uuid, OPERATING_SYSTEM, OS_DATA));
    ((ObjectNode) nameless.path("meta")).remove("uuid");
    // The UUID, or else the name, tells which entity it is
    for (ObjectNode entity : ImmutableList.of(unknown, nameless)) {
      ObjectNode result = exec(entityManager.update(), "update", entities(entity));
      assertEquals(
        "Update does not create entities; use create. Entity: " + (entity == unknown ? uuid : "write-" + uuid),
        result.path("error").asString()
      );
    }
    assertNull(nodeRepository.findByUuid(uuid));
  }

  @Test
  public void testUpdateDoesNotChangeRelations() throws Exception {
    EntityManager entityManager = new EntityManager();
    String os = instance(OPERATING_SYSTEM, OS_DATA);
    String group = instance(COMPONENT_GROUP, "{}");
    exec(entityManager.group(), "group", mapper.createObjectNode().put("group", group).put("member", os));
    List<String> relations = outgoing(os);

    ObjectNode added = mapper.valueToTree(nodeRepository.findByUuid(os));
    relations(added).putObject("HAS").put("direction", "outgoing").putArray("to").add(dns());
    ObjectNode removed = mapper.valueToTree(nodeRepository.findByUuid(os));
    relations(removed).remove("MEMBER_OF");
    ObjectNode retyped = mapper.valueToTree(nodeRepository.findByUuid(os));
    relations(retyped).putObject("INSTANCE_OF").put("direction", "outgoing").putArray("to").add(UNIX_LIKE);
    for (ObjectNode entity : ImmutableList.of(added, removed, retyped)) {
      entity.put("desc", "Changed.");
      ObjectNode result = exec(entityManager.update(), "update", entities(entity));
      assertTrue(result.path("error").asString().startsWith("Update does not change relations"), result.toString());
    }
    assertEquals(relations, outgoing(os));
    assertEquals("A write path test node.", nodeRepository.findByUuid(os).getDesc());

    // The same relations, with the type by its name, as the graph gives them
    ObjectNode same = mapper.valueToTree(nodeRepository.findByUuid(os));
    same.put("desc", "Changed.");
    ObjectNode result = exec(entityManager.update(), "update", entities(same));
    assertEquals(os, result.path("data").path(0).asString(), result.toString());
    assertEquals("Changed.", nodeRepository.findByUuid(os).getDesc());
    assertEquals(relations, outgoing(os));
  }

  @Test
  public void testRelateAndUnrelate() throws Exception {
    EntityManager entityManager = new EntityManager();
    String os = instance(OPERATING_SYSTEM, OS_DATA);
    String dns = dns();
    ObjectNode args = mapper.createObjectNode().put("from", os).put("relation", "HAS").put("to", dns);

    for (boolean changed : ImmutableList.of(true, false)) {
      ObjectNode result = exec(entityManager.relate(), "relate", args);
      assertEquals(changed, result.path("data").path("changed").asBoolean(), result.toString());
      assertEquals(ImmutableList.of("HAS -> " + dns, "INSTANCE_OF -> " + OPERATING_SYSTEM), outgoing(os));
    }
    for (boolean changed : ImmutableList.of(true, false)) {
      ObjectNode result = exec(entityManager.unrelate(), "unrelate", args);
      assertEquals(changed, result.path("data").path("changed").asBoolean(), result.toString());
      assertEquals(ImmutableList.of("INSTANCE_OF -> " + OPERATING_SYSTEM), outgoing(os));
    }
  }

  @Test
  public void testRelateErrors() throws Exception {
    EntityManager entityManager = new EntityManager();
    String os = instance(OPERATING_SYSTEM, OS_DATA);
    String dns = dns();
    String unknown = java.util.UUID.randomUUID().toString();
    // The relation, the node that it goes to, and the expected error
    String[][] rows = {
      { "INSTANCE_OF", UNIX_LIKE, "Change INSTANCE_OF with specialize." },
      { "MEMBER_OF", dns, "Change MEMBER_OF with group and ungroup." },
      { "ISA", UNIX_LIKE, "Change ISA with the bundles." },
      { "HAS", unknown, "Node with UUID \"" + unknown + "\" not found." },
    };
    for (String[] row : rows) {
      ObjectNode args = mapper.createObjectNode().put("from", os).put("relation", row[0]).put("to", row[1]);
      for (GenericActor.MessageHandler handler : ImmutableList.of(entityManager.relate(), entityManager.unrelate())) {
        assertEquals(row[2], exec(handler, "relate", args).path("error").asString());
      }
    }
    // No type constraint permits this relation
    ObjectNode args = mapper.createObjectNode().put("from", os).put("relation", "RUNS_ON").put("to", dns);
    ObjectNode result = exec(entityManager.relate(), "relate", args);
    assertTrue(result.path("error").asString().contains("\"code\":400"), result.toString());
    assertEquals(ImmutableList.of("INSTANCE_OF -> " + OPERATING_SYSTEM), outgoing(os));
  }

  // A relation that a type constraint requires stays
  @Test
  public void testUnrelateKeepsARequiredRelation() throws Exception {
    EntityManager entityManager = new EntityManager();
    String required = type("RequiredOs", OPERATING_SYSTEM);
    Graph.query(
      "MATCH (a:Type {uuid: $from}), (b:Type {uuid: $to}) " +
        "MERGE (a)-[c:CONSTRAINTS {forRelationship: 'HAS'}]->(b) SET c.cardinality = 'REQUIRED_TO_ONE'",
      ImmutableMap.of("from", required, "to", DNS_NAME)
    );
    String dns = dns();
    String os = java.util.UUID.randomUUID().toString();
    ObjectNode entity = (ObjectNode) mapper.readTree(entity(os, required, OS_DATA));
    relations(entity).putObject("HAS").put("direction", "outgoing").putArray("to").add(dns);
    mapper.treeToValue(entity, Entity.class).toGraph();

    ObjectNode args = mapper.createObjectNode().put("from", os).put("relation", "HAS").put("to", dns);
    ObjectNode result = exec(entityManager.unrelate(), "unrelate", args);
    assertTrue(result.path("error").asString().contains("REQUIRED_TO_ONE"), result.toString());
    assertEquals(ImmutableList.of("HAS -> " + dns, "INSTANCE_OF -> " + required), outgoing(os));
  }

  @Test
  public void testDelete() throws Exception {
    EntityManager entityManager = new EntityManager();

    final String operatingSystemUuid = "f05e325e-1aee-4892-96e4-8bcd6e97c528";
    // A new address is a leaf: nothing points to it
    final Component address = new Component(
      mapper
        .createObjectNode()
        .put("name", "leaf")
        .put("desc", "A leaf.")
        .put("addr", "192.0.2.30")
        .put("reachable", false),
      Context.getTypeRepository().findOneByName("Ipv4Address")
    );
    address.save();
    final String leafUuid = address.getUuid();

    /*
      construct a message that tries to delete the "OperatingSystem" node, but will fail because we have outgoing
      relations to entities with labels that are in the label array
    */
    ObjectNode message = mapper.createObjectNode();
    message.put("msgh", "delete");
    ObjectNode args = message.putObject("args");
    ArrayNode labels = args.putArray("labels");
    labels.add("type:Component");
    args.put("node", operatingSystemUuid);

    // send the message constructed above and get the result
    JsonNode response = entityManager.delete().exec(message);
    ObjectNode result = (ObjectNode) response.get("result");

    // the result should have an error property
    assertTrue(result.has("error"), result.toString());
    assertNotNull(nodeRepository.findByUuid(operatingSystemUuid));

    // Now try again, but pick a leaf node
    args.put("node", leafUuid);
    labels.removeAll();
    // We won't have an outgoing rel to anything with label Foo, so this will succeed
    labels.add("Foo");

    // send the message constructed above and get the result
    response = entityManager.delete().exec(message);
    result = (ObjectNode) response.get("result");

    // error should now be absent
    assertTrue(!result.has("error"), result.toString());
    assertNull(nodeRepository.findByUuid(leafUuid));
  }

  private static Set<String> names(List<Node> nodes) {
    return nodes.stream().map(Node::getName).collect(Collectors.toSet());
  }

  // The tests below record the write path before the migration rewrites it

  @Test
  public void testSpecialize() throws Exception {
    EntityManager entityManager = new EntityManager();
    String os = instance(OPERATING_SYSTEM, OS_DATA);
    Set<String> labels = labels(os);

    ObjectNode result = exec(
      entityManager.specialize(),
      "specialize",
      mapper.createObjectNode().put("instance", os).put("isa", UNIX_LIKE)
    );
    assertFalse(result.has("error"), result.toString());
    assertEquals("Instance [OperatingSystem] specialized to [UnixLike]", result.get("data").asString());
    assertEquals(ImmutableList.of("INSTANCE_OF -> " + UNIX_LIKE), outgoing(os));
    // The type labels follow the new type
    labels.add("type:UnixLike");
    assertEquals(labels, labels(os));
  }

  // A type four levels below, for example a release of a distribution
  @Test
  public void testSpecializeBelowChildren() throws Exception {
    EntityManager entityManager = new EntityManager();
    String archLinux = type("ArchLinux", LINUX);
    String archLinux2026 = type("ArchLinux2026", archLinux);
    String os = instance(OPERATING_SYSTEM, OS_DATA);

    ObjectNode result = exec(
      entityManager.specialize(),
      "specialize",
      mapper.createObjectNode().put("instance", os).put("isa", archLinux2026)
    );
    assertFalse(result.has("error"), result.toString());
    assertEquals("Instance [OperatingSystem] specialized to [ArchLinux2026]", result.get("data").asString());
    assertEquals(ImmutableList.of("INSTANCE_OF -> " + archLinux2026), outgoing(os));
  }

  @Test
  public void testSpecializeErrors() throws Exception {
    EntityManager entityManager = new EntityManager();
    String os = instance(OPERATING_SYSTEM, OS_DATA);
    String linux = instance(LINUX, OS_DATA);

    // A type above the current type, or the current type itself
    for (String isa : ImmutableList.of(UNIX_LIKE, LINUX)) {
      ObjectNode result = exec(
        entityManager.specialize(),
        "specialize",
        mapper.createObjectNode().put("instance", linux).put("isa", isa)
      );
      assertEquals(
        "Invalid specialization requested. Type [" +
          nodeRepository.findByUuid(isa).getName() +
          "] is not in the specialization set of: [Linux]",
        result.get("error").asString()
      );
    }
    assertEquals(ImmutableList.of("INSTANCE_OF -> " + LINUX), outgoing(linux));

    String unknown = java.util.UUID.randomUUID().toString();
    ObjectNode result = exec(
      entityManager.specialize(),
      "specialize",
      mapper.createObjectNode().put("instance", os).put("isa", unknown)
    );
    assertEquals("Could not find a type with uuid " + unknown, result.get("error").asString());

    // A type is not an instance
    result = exec(
      entityManager.specialize(),
      "specialize",
      mapper.createObjectNode().put("instance", LINUX).put("isa", OPERATING_SYSTEM)
    );
    assertEquals("Could not find an instance with uuid " + LINUX, result.get("error").asString());

    assertEquals(ImmutableList.of("INSTANCE_OF -> " + OPERATING_SYSTEM), outgoing(os));
  }

  @Test
  public void testGroupAndUngroup() throws Exception {
    EntityManager entityManager = new EntityManager();
    String host = instance(OPERATING_SYSTEM, OS_DATA);
    String other = instance(OPERATING_SYSTEM, OS_DATA);
    String group = instance(COMPONENT_GROUP, "{}");
    ObjectNode args = mapper.createObjectNode().put("group", group).put("member", host);
    List<String> member = ImmutableList.of("INSTANCE_OF -> " + OPERATING_SYSTEM, "MEMBER_OF -> " + group);

    ObjectNode result = exec(entityManager.group(), "group", args);
    assertEquals(true, result.path("data").path("changed").asBoolean(), result.toString());
    assertEquals(member, outgoing(host));

    // A second member does not remove the first
    exec(entityManager.group(), "group", mapper.createObjectNode().put("group", group).put("member", other));
    assertEquals(member, outgoing(host));
    assertEquals(member, outgoing(other));

    // A second group adds no relationship
    result = exec(entityManager.group(), "group", args);
    assertEquals(member, outgoing(host));
    assertEquals(false, result.path("data").path("changed").asBoolean(), result.toString());

    result = exec(entityManager.ungroup(), "ungroup", args);
    assertEquals(true, result.path("data").path("changed").asBoolean(), result.toString());
    assertEquals(ImmutableList.of("INSTANCE_OF -> " + OPERATING_SYSTEM), outgoing(host));
    assertEquals(ImmutableList.of("INSTANCE_OF -> " + COMPONENT_GROUP), outgoing(group));

    result = exec(entityManager.ungroup(), "ungroup", args);
    assertEquals(false, result.path("data").path("changed").asBoolean(), result.toString());
  }

  @Test
  public void testGroupErrors() throws Exception {
    EntityManager entityManager = new EntityManager();
    String host = instance(OPERATING_SYSTEM, OS_DATA);
    String group = instance(COMPONENT_GROUP, "{}");
    String unknown = java.util.UUID.randomUUID().toString();

    for (GenericActor.MessageHandler handler : ImmutableList.of(entityManager.group(), entityManager.ungroup())) {
      ObjectNode result = exec(
        handler,
        "group",
        mapper.createObjectNode().put("group", unknown).put("member", host)
      );
      assertEquals("Group with UUID \"" + unknown + "\" not found.", result.get("error").asString());
      result = exec(handler, "group", mapper.createObjectNode().put("group", group).put("member", unknown));
      assertEquals("Node with UUID \"" + unknown + "\" not found.", result.get("error").asString());
    }
    ObjectNode result = exec(
      entityManager.group(),
      "group",
      mapper.createObjectNode().put("group", group).put("member", OPERATING_SYSTEM)
    );
    assertEquals("Node with UUID \"" + OPERATING_SYSTEM + "\" is not groupable.", result.get("error").asString());
    assertEquals(ImmutableList.of("INSTANCE_OF -> " + OPERATING_SYSTEM), outgoing(host));
  }

  @Test
  public void testDeleteWithRelationships() throws Exception {
    EntityManager entityManager = new EntityManager();
    String host = instance(OPERATING_SYSTEM, OS_DATA);
    String peer = instance(OPERATING_SYSTEM, OS_DATA);
    Graph.query(
      "MATCH (a:Node {uuid: $peer}), (b:Node {uuid: $host}) CREATE (a)-[:RUNS_ON]->(b)",
      ImmutableMap.of("peer", peer, "host", host)
    );

    // Delete an instance with its Instance label.
    ObjectNode result = exec(entityManager.delete(), "delete", deleteArgs(host, "Instance"));
    assertEquals(host, result.path("data").asString(), result.toString());
    assertNull(nodeRepository.findByUuid(host));
    assertEquals(ImmutableList.of("INSTANCE_OF -> " + OPERATING_SYSTEM), outgoing(peer));
  }

  @Test
  public void testDeleteNonEmptyGroup() throws Exception {
    EntityManager entityManager = new EntityManager();
    String host = instance(OPERATING_SYSTEM, OS_DATA);
    String group = instance(COMPONENT_GROUP, "{}");
    exec(entityManager.group(), "group", mapper.createObjectNode().put("group", group).put("member", host));

    // A member without the Group label makes the delete unsafe.
    ObjectNode result = exec(entityManager.delete(), "delete", deleteArgs(group, "Group"));
    assertEquals("Couldn't delete entity.", result.get("error").asString());
    assertEquals(ImmutableList.of("INSTANCE_OF -> " + OPERATING_SYSTEM, "MEMBER_OF -> " + group), outgoing(host));
  }

  private static ObjectNode exec(GenericActor.MessageHandler handler, String msgh, ObjectNode args) {
    ObjectNode message = mapper.createObjectNode().put("msgh", msgh);
    message.set("args", args);
    return (ObjectNode) handler.exec(message).get("result");
  }

  private static ObjectNode deleteArgs(String uuid, String label) {
    ObjectNode args = mapper.createObjectNode().put("node", uuid).put("includeStart", true);
    args.putArray("labels").add(label);
    return args;
  }

  // Makes a type below the type isa and returns its uuid
  private static String type(String name, String isa) throws Exception {
    String uuid = java.util.UUID.randomUUID().toString();
    ObjectNode type = mapper.createObjectNode().put("name", name).put("desc", "A specialization test type.");
    ObjectNode meta = type.putObject("meta").put("uuid", uuid);
    meta.putObject("relations").putObject("ISA").put("direction", "outgoing").putArray("to").add(isa);
    type
      .putObject("file")
      .putObject("dataSchema")
      .put("encoding", "text")
      .put("format", "json")
      .put("content", "{\"type\": \"object\"}");
    mapper.treeToValue(type, Entity.class).toGraph();
    return uuid;
  }

  // Makes an instance of the type isa and returns its uuid
  private static String instance(String isa, String data) throws Exception {
    String uuid = java.util.UUID.randomUUID().toString();
    mapper.readValue(entity(uuid, isa, data), Entity.class).toGraph();
    return uuid;
  }

  // The entity of an instance of the type isa
  private static String entity(String uuid, String isa, String data) {
    return String.format(
      "{\"name\": \"write-%s\", \"desc\": \"A write path test node.\", " +
        "\"meta\": {\"uuid\": \"%s\", \"relations\": {\"INSTANCE_OF\": {\"direction\": \"outgoing\", " +
        "\"to\": [\"%s\"]}}}, \"data\": %s}",
      uuid,
      uuid,
      isa,
      data
    );
  }

  // Makes a DnsName with a name of its own, as the name is its identity
  private static String dns() throws Exception {
    return instance(DNS_NAME, "{\"dnsname\": \"" + java.util.UUID.randomUUID() + ".example\"}");
  }

  private static ObjectNode relations(ObjectNode entity) {
    return (ObjectNode) entity.path("meta").path("relations");
  }

  private static ObjectNode entities(ObjectNode... entities) {
    ObjectNode args = mapper.createObjectNode();
    ArrayNode array = args.putArray("entities");
    for (ObjectNode entity : entities) {
      array.add(entity);
    }
    return args;
  }

  // The outgoing relationships of a node in the raw graph, sorted
  private List<String> outgoing(String uuid) {
    List<String> outgoing = new ArrayList<>();
    for (Map<String, Object> r : Graph.query(
      "MATCH (n:Node {uuid: $uuid})-[r]->(b) " + "RETURN type(r) AS type, b.uuid AS peer",
      ImmutableMap.of("uuid", uuid)
    )) {
      outgoing.add(r.get("type") + " -> " + r.get("peer"));
    }
    Collections.sort(outgoing);
    return outgoing;
  }

  @SuppressWarnings("unchecked")
  private Set<String> labels(String uuid) {
    return new HashSet<>(
      (List<String>) Graph.query(
        "MATCH (n:Node {uuid: $uuid}) RETURN labels(n) AS labels",
        ImmutableMap.of("uuid", uuid)
      )
        .get(0)
        .get("labels")
    );
  }
}
