package com.raaftech.structure.graph;

import com.raaftech.structure.base.ConverterConfig;
import com.raaftech.structure.base.SpringTestConfig;
import com.raaftech.structure.graph.repositories.ResultRepository;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class,
        classes = {SpringTestConfig.class, ConverterConfig.class})
public class ResultTest {

    @Autowired
    ResultRepository resultRepository;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    @Transactional
    @Rollback
    public void save() throws Exception {
        // save the thing
        String taskId = UUID.randomUUID().toString();
        Result result = new Result("test.exe", taskId, "stdout", "stderr", 0);
        result.save();

        // retrieve the thing
        Result retrieved = resultRepository.findByUuid(result.getUuid());
        assertNotNull(retrieved);
        assertEquals(taskId, retrieved.getTaskId());
        assertEquals(result.getDate(), retrieved.getDate());
    }

    @Test
    public void getThisAndNewer() throws Exception {
        Result r1 = new Result("1", UUID.randomUUID().toString(), "1", "1", 1);
        Result r2 = new Result("2", UUID.randomUUID().toString(), "2", "2", 2);
        Result r3 = new Result("3", UUID.randomUUID().toString(), "3", "3", 3);
        Result r4 = new Result("4", UUID.randomUUID().toString(), "4", "4", 4);

        r1.setNext(r2);
        r2.setNext(r3);
        r3.setNext(r4);

        // r1AndNewer should contain r1 -> r2 -> r3 -> r4
        List<Result> r1AndNewer = r1.getThisAndNewer();
        IntStream.range(1, 5).forEach(i -> {
            assertEquals(i, r1AndNewer.get(i-1).getRetcode());
        });

        // next LinkedList should just contain r4
        assertEquals(4, r4.getThisAndNewer().get(0).getRetcode());
    }

    @Test
    public void getThisAndOlder() throws Exception {
        Result r1 = new Result("1", UUID.randomUUID().toString(), "1", "1", 1);
        Result r2 = new Result("2", UUID.randomUUID().toString(), "2", "2", 2);
        Result r3 = new Result("3", UUID.randomUUID().toString(), "3", "3", 3);
        Result r4 = new Result("4", UUID.randomUUID().toString(), "4", "4", 4);

        r1.setNext(r2);
        r2.setNext(r3);
        r3.setNext(r4);

        // next LinkedList should contain r1
        assertEquals(1, r1.getThisAndNewer().get(0).getRetcode());

        // r4AndOlder should contain r4, r3, r2, r1
        List<Result> r4AndOlder = r4.getThisAndOlder();
        IntStream.range(1, 5).forEach(i -> {
            assertEquals(5-i, r4AndOlder.get(i-1).getRetcode());
        });
    }

    @Test
    public void testSetNextException() {
        Result r1 = new Result("1", UUID.randomUUID().toString(), "1", "1", 1);
        Result r2 = new Result("2", UUID.randomUUID().toString(), "2", "2", 2);
        Result r3 = new Result("3", UUID.randomUUID().toString(), "3", "3", 3);

        r1.setNext(r2);
        thrown.expect(IllegalStateException.class);
        r1.setNext(r3);
    }
}