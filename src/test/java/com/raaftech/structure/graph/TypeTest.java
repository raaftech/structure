package com.raaftech.structure.graph;

import com.raaftech.structure.base.SpringTestConfig;
import com.raaftech.structure.graph.repositories.TypeRepository;
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = SpringTestConfig.class)
public class TypeTest {

    @Autowired
    private TypeRepository typeRepository;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    @Transactional
    @Rollback
    public void testTypeNodeSetDescriptor() {
        Type type01 = new Type("Type 01", "First Type", 0);
        Type type02 = new Type("Type 02", "Second Type", 0);
        Type type03 = new Type("Type 03", "Third Type", 0);
        Type type04 = new Type("Type 04", "Fourth Type", 0);

        type01.setDescriptor("foo", "boolean", true, "All typenodes should inherit this", null, null);
        type02.setDescriptor("bar", "string", true, "Only type02..04 should have this.", null, null);

        type02.setIsa(type01);
        type03.setIsa(type02);
        type04.setIsa(type03);

        // Save all nodes to the database.
        typeRepository.save(type04);

        assertNotNull(type01.getSchema().getProperties().get("foo"));
        assertNull(type01.getSchema().getProperties().get("bar"));
        assertNotNull(type02.getSchema().getProperties().get("foo"));
        assertNotNull(type03.getSchema().getProperties().get("bar"));
        assertNull(type04.getSchema().getProperties().get("baz"));

        type04.setDescriptor("baz", "number", false, "Only type04 should have this", null, null);

        assertNull(type03.getSchema().getProperties().get("baz"));
        assertNotNull(type04.getSchema().getProperties().get("baz"));

        // Can't use except.expect() here because we still need to cleanup.
        exception.expectMessage("Can't override parent schema.");
        type02.setDescriptor("foo", "string", false, "Should be disallowed.", null, null);
    }

}
