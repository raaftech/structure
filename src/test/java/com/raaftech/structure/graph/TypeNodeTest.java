package com.raaftech.structure.graph;

import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.repositories.TypeNodeRepository;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

public class TypeNodeTest {

    private TypeNodeRepository typeNodeRepository = Context.INSTANCE.getTypeNodeRepository();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @BeforeClass
    public static void setUp() throws Exception {
        Context.INSTANCE.getClass();
    }

    @AfterClass
    public static void tearDown() throws Exception {
    }

    @Test
    public void testTypeNodeSetDescriptor() {
        // WARNING! THIS IS RUN ON THE DEVELOPMENT DATABASE!
        TypeNode type01 = new TypeNode("TypeNode 01", "First TypeNode", 0);
        TypeNode type02 = new TypeNode("TypeNode 02", "Second TypeNode", 0);
        TypeNode type03 = new TypeNode("TypeNode 03", "Third TypeNode", 0);
        TypeNode type04 = new TypeNode("TypeNode 04", "Fourth TypeNode", 0);

        type01.setDescriptor("foo", "boolean", true, "All typenodes should inherit this", null, null);
        type02.setDescriptor("bar", "string", true, "Only type02..04 should have this.", null, null);

        type02.setIsa(type01);
        type03.setIsa(type02);
        type04.setIsa(type03);

        // Save all nodes to the database.
        typeNodeRepository.save(type04);

        assertNotNull(type01.getSchema().getProperties().get("foo"));
        assertNull(type01.getSchema().getProperties().get("bar"));
        assertNotNull(type02.getSchema().getProperties().get("foo"));
        assertNotNull(type03.getSchema().getProperties().get("bar"));
        assertNull(type04.getSchema().getProperties().get("baz"));

        type04.setDescriptor("baz", "number", false, "Only type04 should have this", null, null);

        assertNull(type03.getSchema().getProperties().get("baz"));
        assertNotNull(type04.getSchema().getProperties().get("baz"));

        // Can't use except.expect() here because we still need to cleanup.
        String exc = "";
        try {
            type02.setDescriptor("foo", "string", false, "Should be disallowed.", null, null);
        } catch (RuntimeException e) {
            exc = e.getMessage();
        }
        assertTrue(exc.equals("Can't override parent schema."));

        typeNodeRepository.delete(type01);
        typeNodeRepository.delete(type02);
        typeNodeRepository.delete(type03);
        typeNodeRepository.delete(type04);
    }

}
