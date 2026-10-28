package tech.raaf.structure.graph;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static tech.raaf.structure.helper.JsonHelper.mapper;

import java.net.URI;
import java.util.UUID;
import java.util.stream.IntStream;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import tech.raaf.structure.GraphTest;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.graph.repositories.ResultRepository;
import tech.raaf.structure.helper.JsonHelper;
import tools.jackson.databind.JsonNode;

public class ResultTest extends GraphTest {

  private final ResultRepository resultRepository = Context.getResultRepository();

  @Mocked
  JsonHelper jsonHelper;

  @Before
  public void setUp() throws Exception {
    new Expectations() {
      {
        JsonHelper.validateToSchema((URI) any, (JsonNode) any);
        result = true;
      }
    };

    String rootType =
      "  {" +
      "    \"name\": \"Type\"," +
      "    \"desc\": \"The root type.\"," +
      "    \"meta\": {" +
      "      \"uuid\": \"b1bc963a-19cc-4518-aecb-a35d028c49e3\"," +
      "      \"abstract\": true," +
      "      \"relations\": {}" +
      "    },  " +
      "    \"file\": {" +
      "      \"dataSchema\": {" +
      "        \"encoding\": \"text\"," +
      "        \"format\": \"json\"," +
      "        \"content\": \"{}\"" +
      "      }   " +
      "    }   " +
      "  }";

    String resultType =
      "{" +
      "    \"name\": \"Result\"," +
      "    \"desc\": \"A result from an operation.\"," +
      "    \"meta\": {" +
      "        \"uuid\": \"f3498c02-58c6-486b-87c6-9d54607fdb12\"," +
      "        \"relations\": {" +
      "            \"ISA\": {" +
      "                \"direction\": \"outgoing\"," +
      "                \"to\": [" +
      "                    \"b1bc963a-19cc-4518-aecb-a35d028c49e3\"" +
      "                ]   " +
      "            }   " +
      "        }   " +
      "    },  " +
      "    \"file\": {" +
      "        \"dataSchema\": {" +
      "            \"encoding\": \"text\"," +
      "            \"format\": \"json\"," +
      "            \"content\": \"{}\"" +
      "        }   " +
      "    }   " +
      "}";

    Entity rootEntity = mapper.readValue(rootType, Entity.class);
    Entity resultEntity = mapper.readValue(resultType, Entity.class);

    Context.setSchemaNamespace(URI.create("http://localhost"));

    Graph.transaction(() -> {
      rootEntity.toGraph();
      resultEntity.toGraph();
    });
  }

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void save() throws Exception {
    // save the thing
    String taskId = UUID.randomUUID().toString();

    Result res = new Entity.ResultBuilder()
      .process("test.exe")
      .taskId(taskId)
      .stdout("stdout")
      .stderr("stderr")
      .returnCode(0)
      .buildAndStore();
    res.save();

    // retrieve the result
    Result retrieved = resultRepository.findByUuid(res.getUuid());
    assertNotNull(retrieved);
    assertEquals(taskId, retrieved.getTaskId());
    assertEquals(res.getDate(), retrieved.getDate());
  }

  @Test
  public void testInvalidStateConstructions() throws Exception {
    final String stateInterpretation = "OK";
    final String stateDescription = "Test";
    // 0, 1 and 2 should be fine, with non null strings
    IntStream.of(0, 1, 2).forEach(i -> new Result.State(i, stateInterpretation, stateDescription));

    // Anything else should result in an exception
    thrown.expect(IllegalArgumentException.class);
    new Result.State(-1, stateInterpretation, stateDescription);
    new Result.State(0, null, stateDescription);
    new Result.State(0, stateInterpretation, null);
  }

  @Test
  public void testOptionals() throws Exception {
    Result result = new Entity.ResultBuilder()
      .process("1")
      .taskId(UUID.randomUUID().toString())
      .stdout("1")
      .stderr("1")
      .returnCode(1)
      .state(new Result.State(0, "OK", "Test"))
      .buildAndStore();

    assertTrue(result.getState().isPresent());
    assertFalse(result.getUser().isPresent());

    final Result.State state = result.getState().get();
    assertEquals(0, state.getCode());
    assertEquals("OK", state.getInterpretation());
    assertEquals("Test", state.getDescription());
  }
}
