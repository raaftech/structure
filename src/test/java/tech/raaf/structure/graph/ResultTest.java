package tech.raaf.structure.graph;

import com.fasterxml.jackson.databind.JsonNode;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import tech.raaf.structure.base.Configuration;
import tech.raaf.structure.base.SpringTestConfig;
import tech.raaf.structure.exceptions.IllegalEntityType;
import tech.raaf.structure.graph.repositories.InstanceRepository;
import tech.raaf.structure.graph.repositories.ResultRepository;
import tech.raaf.structure.graph.repositories.TypeRepository;
import tech.raaf.structure.helper.JsonHelper;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static io.vertx.core.json.Json.mapper;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = SpringTestConfig.class)
public class ResultTest {

    @Autowired
    GraphDatabase graphDatabase;
    @Autowired
    InstanceRepository instanceRepository;
    @Autowired
    TypeRepository typeRepository;
    @Autowired
    ResultRepository resultRepository;
    @Mocked
    JsonHelper jsonHelper;

    @Before
    public void setUp() throws Exception {
        new Expectations() {{
            JsonHelper.validateToSchema((URI)any, (JsonNode)any);
            result = true;
        }};

        String rootType = "  {" +
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

        String resultType = "{" +
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
                "        },  " +
                "        \"constraints\": {" +
                "            \"NEXT_RESULT\": [" +
                "                {   " +
                "                    \"cardinality\": \"OPTIONAL_TO_ONE\"," +
                "                    \"to\": \"f3498c02-58c6-486b-87c6-9d54607fdb12\"" +
                "                }   " +
                "            ]   " +
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

        Configuration.setSchemaNamespace(URI.create("http://localhost"));

        Transaction tx = graphDatabase.beginTx();
        rootEntity.toGraph();
        resultEntity.toGraph();
        tx.success();
        tx.close();
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void save() throws Exception {
        // save the thing
        String taskId = UUID.randomUUID().toString();

        Result res = new Entity.ResultBuilder().process("test.exe").taskId(taskId)
                .stdout("stdout").stderr("stderr").returnCode(0).buildAndStore();
        res.save();


        // retrieve the result
        Result retrieved = resultRepository.findByUuid(res.getUuid());
        assertNotNull(retrieved);
        assertEquals(taskId, retrieved.getTaskId());
        assertEquals(res.getDate(), retrieved.getDate());
    }

    @Test
    public void getThisAndNewer() throws Exception {
        Result r1 = new Entity.ResultBuilder()
                .process("1")
                .taskId(UUID.randomUUID().toString())
                .stdout("1")
                .stderr("1")
                .returnCode(1)
                .buildAndStore();
        Result r2 = new Entity.ResultBuilder()
                .process("2")
                .taskId(UUID.randomUUID().toString())
                .stdout("2")
                .stderr("2")
                .returnCode(2)
                .buildAndStore();
        Result r3 = new Entity.ResultBuilder()
                .process("3")
                .taskId(UUID.randomUUID().toString())
                .stdout("3")
                .stderr("3")
                .returnCode(3)
                .buildAndStore();
        Result r4 = new Entity.ResultBuilder()
                .process("4")
                .taskId(UUID.randomUUID().toString())
                .stdout("4")
                .stderr("4")
                .returnCode(4)
                .buildAndStore();

        r1.setNext(r2);
        r2.setNext(r3);
        r3.setNext(r4);

        // r1AndNewer should contain r1 -> r2 -> r3 -> r4
        List<Result> r1AndNewer = r1.getThisAndNewer();
        IntStream.range(1, 5).forEach(i -> {
            assertEquals(i, r1AndNewer.get(i-1).getReturnCode());
        });

        // next LinkedList should just contain r4
        assertEquals(4, r4.getThisAndNewer().get(0).getReturnCode());
    }

    @Test
    public void getThisAndOlder() throws Exception {
        Result r1 = new Entity.ResultBuilder()
                .process("1")
                .taskId(UUID.randomUUID().toString())
                .stdout("1")
                .stderr("1")
                .returnCode(1)
                .buildAndStore();
        Result r2 = new Entity.ResultBuilder()
                .process("2")
                .taskId(UUID.randomUUID().toString())
                .stdout("2")
                .stderr("2")
                .returnCode(2)
                .buildAndStore();
        Result r3 = new Entity.ResultBuilder()
                .process("3")
                .taskId(UUID.randomUUID().toString())
                .stdout("3")
                .stderr("3")
                .returnCode(3)
                .buildAndStore();
        Result r4 = new Entity.ResultBuilder()
                .process("4")
                .taskId(UUID.randomUUID().toString())
                .stdout("4")
                .stderr("4")
                .returnCode(4)
                .buildAndStore();

        r1.setNext(r2);
        r2.setNext(r3);
        r3.setNext(r4);

        // next LinkedList should contain r1
        assertEquals(1, r1.getThisAndNewer().get(0).getReturnCode());

        // r4AndOlder should contain r4, r3, r2, r1
        List<Result> r4AndOlder = r4.getThisAndOlder();
        IntStream.range(1, 5).forEach(i -> {
            assertEquals(5-i, r4AndOlder.get(i-1).getReturnCode());
        });
    }

    @Test
    public void testSetNextException() throws Exception {
        Result r1 = new Entity.ResultBuilder()
                .process("1")
                .taskId(UUID.randomUUID().toString())
                .stdout("1")
                .stderr("1")
                .returnCode(1)
                .buildAndStore();
        Result r2 = new Entity.ResultBuilder()
                .process("2")
                .taskId(UUID.randomUUID().toString())
                .stdout("2")
                .stderr("2")
                .returnCode(2)
                .buildAndStore();
        Result r3 = new Entity.ResultBuilder()
                .process("3")
                .taskId(UUID.randomUUID().toString())
                .stdout("3")
                .stderr("3")
                .returnCode(3)
                .buildAndStore();
        Result r4 = new Entity.ResultBuilder()
                .process("4")
                .taskId(UUID.randomUUID().toString())
                .stdout("4")
                .stderr("4")
                .returnCode(4)
                .buildAndStore();

        r1.setNext(r2);
        thrown.expect(IllegalStateException.class);
        r1.setNext(r3);
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

    @Test
    public void testAddResultToResult() throws Exception {
        // Adding a result to a result shouldn't be allowed.
        Result r1 = new Entity.ResultBuilder()
                .process("1")
                .taskId(UUID.randomUUID().toString())
                .stdout("1")
                .stderr("1")
                .returnCode(1)
                .buildAndStore();
        Result r2 = new Entity.ResultBuilder()
                .process("2")
                .taskId(UUID.randomUUID().toString())
                .stdout("2")
                .stderr("2")
                .returnCode(2)
                .buildAndStore();

        thrown.expect(IllegalEntityType.class);
        r1.addResult(r2);
    }
}