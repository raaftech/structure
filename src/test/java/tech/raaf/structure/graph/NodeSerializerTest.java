package tech.raaf.structure.graph;

import static io.vertx.core.json.Json.mapper;
import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;
import org.junit.Test;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.graph.repositories.NodeRepository;
import tech.raaf.structure.helper.Triple;
import tech.raaf.structure.schema.TypeSchemaTest;

public class NodeSerializerTest extends TypeSchemaTest {

  private final NodeRepository nodeRepository = Context.getNodeRepository();

  /*
   * Test serialization of a node, test the resulting json according to schema and property values
   */
  @Test
  public void testDeserializationSerialization() throws Exception {
    Node componentNode = nodeRepository.findOneByName("Linux");
    assertNotNull(componentNode);

    String serializedJson = mapper.writeValueAsString(componentNode);
    Entity entity = mapper.readValue(serializedJson, Entity.class);
    assertNotNull(entity);
    assertEquals("Linux", entity.getName());
    assertEquals("The Linux Operating System.", entity.getDesc());
    assertEquals("a5613527-2f64-4481-83f2-80b0cb427e28", entity.getUuid());
    assertEquals("linux", entity.getBundle());
    assertEquals("src/main/resources/bundles/linux/types/linux.type.json", entity.getPath());

    // test relationships
    assertEquals(1, entity.getRelations().size());
    Triple<String, String, Set<String>> rel = entity.getRelations().iterator().next();
    assertEquals("ISA", rel.getLeft());
    assertEquals("outgoing", rel.getMiddle());
    Set<String> peers = rel.getRight();
    assertEquals(1, peers.size());
    assertEquals("6a1eea8b-1462-4439-8c60-12edbee6c397", peers.iterator().next());

    // test files
    assertNotNull(entity.getFiles());
    JsonNode dataSchema = mapper.readTree((String) entity.getFiles().get("dataSchema"));
    assertNotNull(dataSchema);
    assertEquals("text", dataSchema.get("encoding").asText());
    assertEquals("json", dataSchema.get("format").asText());
    assertEquals("../schemas/linux.schema.json", dataSchema.get("path").asText());
  }

  @Test
  public void testIncomingRelationshipSerialization() throws Exception {
    /*
      Bash and bashInvoke have outgoing relationships towards each other. This will
      cause issues during loading with NaiveSorter. For that reason we need to make sure
      one of the relationships is serialized as an incoming relationship.
    */
    Node bash = nodeRepository.findOneByName("Bash");
    Node bashInvoke = nodeRepository.findOneByName("BashInvoke");

    assertNotNull(bash);
    assertNotNull(bashInvoke);

    JsonNode bashObj = mapper.valueToTree(bash);
    JsonNode bashInvokeObj = mapper.valueToTree(bashInvoke);

    /*
      Bash has an outgoing "HAS_INSTRUMENT" relationship to BashInvoke, but we persist that
      as an incoming "HAS_INSTRUMENT" on BashInvoke.
    */
    assertTrue(!bashObj.get("meta").get("relations").has("PROVIDED_BY"));
    assertTrue(bashInvokeObj.get("meta").get("relations").has("PROVIDED_BY"));
    assertEquals("outgoing", bashInvokeObj.get("meta").get("relations").get("PROVIDED_BY").get("direction").asText());
  }
}
