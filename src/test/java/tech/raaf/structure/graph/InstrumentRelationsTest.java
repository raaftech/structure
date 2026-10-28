package tech.raaf.structure.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.raaf.structure.helper.JsonHelper.mapper;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.exceptions.IllegalEntityType;
import tech.raaf.structure.schema.TypeSchemaTest;
import tools.jackson.databind.JsonNode;

public class InstrumentRelationsTest extends TypeSchemaTest {

  private Parser parser;
  private Controller controller;
  private Set<Result> rawResults;
  private Set<Instance> producedNodes;

  @BeforeEach
  public void createInstrumentsAndResults() throws Exception {
    rawResults = ImmutableSet.of(result("first"), result("second"));
    Component component = new Component(
      mapper.createObjectNode().put("name", "ParsedComponent").put("desc", "A parsed component."),
      Context.getTypeRepository().findOneByName("Component")
    );
    component.save();
    producedNodes = ImmutableSet.of(component, result("typed"));

    Entity entity = parserEntity("TestParser");
    entity.addRelations("PARSED", Direction.OUTGOING, uuids(rawResults));
    entity.addRelations("PRODUCED", Direction.OUTGOING, uuids(producedNodes));
    Graph.transaction(entity::toGraph);
    parser = (Parser) Context.getInstrumentRepository().findByUuid(entity.getUuid());

    controller = (Controller) Context.getInstrumentRepository().findOneByName("BashInvoke");
    Graph.query(
      "MATCH (c:Controller {uuid: $controller}), (r:Result) " + "WHERE r.uuid IN $results MERGE (c)-[:PRODUCED]->(r)",
      ImmutableMap.of(
        "controller",
        controller.getUuid(),
        "results",
        rawResults.stream().map(Node::getUuid).collect(Collectors.toList())
      )
    );
  }

  @Test
  public void parserReadsOutgoingResultsAndProducedInstances() throws Exception {
    Result incoming = result("incoming");
    Graph.query(
      "MATCH (p:Parser {uuid: $parser}), (r:Result {uuid: $result}) " +
        "MERGE (r)-[:PARSED]->(p) MERGE (r)-[:PRODUCED]->(p)",
      ImmutableMap.of("parser", parser.getUuid(), "result", incoming.getUuid())
    );

    assertEquals(rawResults, parser.getRawResults());
    assertEquals(producedNodes, parser.getProducedNodes());
    assertTrue(parser.getProducedNodes().stream().anyMatch(Component.class::isInstance));
    assertTrue(parser.getProducedNodes().stream().anyMatch(Result.class::isInstance));
  }

  @Test
  public void controllerReadsItsOutgoingResults() throws Exception {
    Result incoming = result("incoming");
    Graph.query(
      "MATCH (c:Controller {uuid: $controller}), (r:Result {uuid: $result}) MERGE (r)-[:PRODUCED]->(c)",
      ImmutableMap.of("controller", controller.getUuid(), "result", incoming.getUuid())
    );

    assertEquals(rawResults, controller.getResults());
  }

  @Test
  public void missingRelationsReturnEmptySets() {
    Entity entity = parserEntity("EmptyParser");
    Graph.transaction(entity::toGraph);
    Parser emptyParser = (Parser) Context.getInstrumentRepository().findByUuid(entity.getUuid());
    Controller emptyController = (Controller) Context.getInstrumentRepository().findOneByName(
      "OpenSshClientSendString"
    );

    assertTrue(emptyParser.getRawResults().isEmpty());
    assertTrue(emptyParser.getProducedNodes().isEmpty());
    assertTrue(emptyController.getResults().isEmpty());
  }

  @Test
  public void rejectsInputsThatAreNotResults() {
    Entity entity = parserEntity("InvalidParser");
    entity.addRelations("PARSED", Direction.OUTGOING, uuids(producedNodes));
    assertThrows(IllegalEntityType.class, () -> Graph.transaction(entity::toGraph));
  }

  @Test
  public void rejectsTypeNodesAsOutputs() {
    Entity entity = parserEntity("InvalidParser");
    entity.addRelations(
      "PRODUCED",
      Direction.OUTGOING,
      ImmutableSet.of(UUID.fromString(Context.getTypeRepository().findOneByName("Component").getUuid()))
    );
    assertThrows(IllegalEntityType.class, () -> Graph.transaction(entity::toGraph));
  }

  @Test
  public void savingAndSerializingKeepTheRelations() {
    parser.setDesc("An updated parser.");
    parser.save();
    controller.setDesc("An updated controller.");
    controller.save();
    parser = (Parser) Context.getInstrumentRepository().findByUuid(parser.getUuid());
    controller = (Controller) Context.getInstrumentRepository().findByUuid(controller.getUuid());

    assertEquals("An updated parser.", parser.getDesc());
    assertEquals("An updated controller.", controller.getDesc());
    assertEquals(rawResults, parser.getRawResults());
    assertEquals(producedNodes, parser.getProducedNodes());
    assertEquals(rawResults, controller.getResults());

    JsonNode parserJson = mapper.valueToTree(parser);
    assertRelation(parserJson, "PARSED", rawResults);
    assertRelation(parserJson, "PRODUCED", producedNodes);
    assertRelation(mapper.valueToTree(controller), "PRODUCED", rawResults);
  }

  private Result result(String process) throws Exception {
    return new Entity.ResultBuilder().process(process).taskId("parser-test").buildAndStore();
  }

  private Entity parserEntity(String name) {
    Entity entity = new Entity();
    entity.setUuid(UUID.randomUUID().toString());
    entity.setName(name);
    entity.setDesc("A test parser.");
    entity.setData(ImmutableMap.of("instrumentType", "parser", "subjectIsLocal", true));
    entity.addRelations(
      "INSTANCE_OF",
      Direction.OUTGOING,
      ImmutableSet.of(UUID.fromString(Context.getTypeRepository().findOneByName("Parser").getUuid()))
    );
    return entity;
  }

  private Set<UUID> uuids(Set<? extends Node> nodes) {
    return nodes
      .stream()
      .map(node -> UUID.fromString(node.getUuid()))
      .collect(Collectors.toSet());
  }

  private void assertRelation(JsonNode json, String name, Set<? extends Node> peers) {
    JsonNode relation = json.path("meta").path("relations").path(name);
    assertEquals("outgoing", relation.path("direction").asString());
    Set<UUID> targets = new HashSet<>();
    relation.path("to").forEach(target -> targets.add(UUID.fromString(target.asString())));
    assertEquals(uuids(peers), targets);
  }
}
