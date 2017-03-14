package com.raaftech.structure.graph;

import com.fasterxml.jackson.databind.JsonNode;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.repositories.InstanceRepository;
import com.raaftech.structure.graph.repositories.ResultRepository;
import com.raaftech.structure.graph.repositories.TypeRepository;
import com.raaftech.structure.helper.JsonHelper;
import mockit.Expectations;
import mockit.Mocked;
import mockit.StrictExpectations;
import mockit.Verifications;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ResultTest {

    @Mocked
    Context context;
    @Mocked
    JsonHelper jsonHelper;
    @Mocked
    InstanceRepository instanceRepository;
    @Mocked
    TypeRepository typeRepository;
    @Mocked
    Type resultType;

    @Before
    public void setUp() throws Exception {
        new Expectations() {{
            // Validation of generated JSON to Result type's schema is always true
            JsonHelper.validateToSchema((JsonNode) any, (JsonNode) any);
            result = true;

            typeRepository.findOneByName("Result");
            result = resultType;
        }};

    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void save(
            @Mocked ResultRepository resultRepository,
            @Mocked TypeRepository typeRepository,
            @Mocked Type type) throws Exception {
        // save the thing
        String taskId = UUID.randomUUID().toString();

        Result res = new Result.Builder().process("test.exe").taskId(taskId)
                .stdout("stdout").stderr("stderr").returnCode(0).build();
        res.save();

        // Make sure the result was actually saved to the repository.
        new Verifications() {{
            resultRepository.save(res);
        }};


        // Return the created result from the repository.
        new StrictExpectations() {{
            resultRepository.findByUuid(res.getUuid());
            result = res;
        }};

        // retrieve the result
        Result retrieved = resultRepository.findByUuid(res.getUuid());
        assertNotNull(retrieved);
        assertEquals(taskId, retrieved.getTaskId());
        assertEquals(res.getDate(), retrieved.getDate());
    }

    @Test
    public void getThisAndNewer() throws Exception {
        Result r1 = new Result.Builder()
                .process("1")
                .taskId(UUID.randomUUID().toString())
                .stdout("1")
                .stderr("1")
                .returnCode(1)
                .build();
        Result r2 = new Result.Builder()
                .process("2")
                .taskId(UUID.randomUUID().toString())
                .stdout("2")
                .stderr("2")
                .returnCode(2)
                .build();
        Result r3 = new Result.Builder()
                .process("3")
                .taskId(UUID.randomUUID().toString())
                .stdout("3")
                .stderr("3")
                .returnCode(3)
                .build();
        Result r4 = new Result.Builder()
                .process("4")
                .taskId(UUID.randomUUID().toString())
                .stdout("4")
                .stderr("4")
                .returnCode(4)
                .build();

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
        Result r1 = new Result.Builder()
                .process("1")
                .taskId(UUID.randomUUID().toString())
                .stdout("1")
                .stderr("1")
                .returnCode(1)
                .build();
        Result r2 = new Result.Builder()
                .process("2")
                .taskId(UUID.randomUUID().toString())
                .stdout("2")
                .stderr("2")
                .returnCode(2)
                .build();
        Result r3 = new Result.Builder()
                .process("3")
                .taskId(UUID.randomUUID().toString())
                .stdout("3")
                .stderr("3")
                .returnCode(3)
                .build();
        Result r4 = new Result.Builder()
                .process("4")
                .taskId(UUID.randomUUID().toString())
                .stdout("4")
                .stderr("4")
                .returnCode(4)
                .build();

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
    public void testSetNextException() {
        Result r1 = new Result.Builder()
                .process("1")
                .taskId(UUID.randomUUID().toString())
                .stdout("1")
                .stderr("1")
                .returnCode(1)
                .build();
        Result r2 = new Result.Builder()
                .process("2")
                .taskId(UUID.randomUUID().toString())
                .stdout("2")
                .stderr("2")
                .returnCode(2)
                .build();
        Result r3 = new Result.Builder()
                .process("3")
                .taskId(UUID.randomUUID().toString())
                .stdout("3")
                .stderr("3")
                .returnCode(3)
                .build();
        Result r4 = new Result.Builder()
                .process("4")
                .taskId(UUID.randomUUID().toString())
                .stdout("4")
                .stderr("4")
                .returnCode(4)
                .build();

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
        Result result = new Result.Builder()
                .process("1")
                .taskId(UUID.randomUUID().toString())
                .stdout("1")
                .stderr("1")
                .returnCode(1)
                .state(new Result.State(0, "OK", "Test"))
                .build();

        assertTrue(result.getState().isPresent());
        assertFalse(result.getUser().isPresent());

        final Result.State state = result.getState().get();
        assertEquals(0, state.getCode());
        assertEquals("OK", state.getInterpretation());
        assertEquals("Test", state.getDescription());
    }
}