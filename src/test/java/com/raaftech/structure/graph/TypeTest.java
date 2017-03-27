package com.raaftech.structure.graph;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.base.SpringTestConfig;
import com.raaftech.structure.graph.repositories.InstanceRepository;
import com.raaftech.structure.graph.repositories.NodeRepository;
import com.raaftech.structure.graph.repositories.TypeRepository;
import com.raaftech.structure.next.Entity;
import mockit.Expectations;
import mockit.Mocked;
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

import java.util.UUID;

import static org.junit.Assert.*;

public class TypeTest {

    @Mocked
    Context context;
    @Mocked
    InstanceRepository instanceRepository;
    ObjectMapper mapper = new ObjectMapper();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    /**
     * SetDescriptor used to be used before everything was entity backed.
     * When we are going to allow modifications from other places we might
     * need it again, so I'll leave this test here, but commented.
     */
//    @Test
//    @Transactional
//    @Rollback
//    public void testTypeNodeSetDescriptor() {
//        Type type01 = new Type("Type 01", "First Type", 0);
//        Type type02 = new Type("Type 02", "Second Type", 0);
//        Type type03 = new Type("Type 03", "Third Type", 0);
//        Type type04 = new Type("Type 04", "Fourth Type", 0);
//
//        type01.setDescriptor("foo", "boolean", true, "All typenodes should inherit this", null, null);
//        type02.setDescriptor("bar", "string", true, "Only type02..04 should have this.", null, null);
//
//        type02.setIsa(type01);
//        type03.setIsa(type02);
//        type04.setIsa(type03);
//
//        // Save all nodes to the database.
//        typeRepository.save(type04);
//
//        assertNotNull(type01.getSchema().getProperties().get("foo"));
//        assertNull(type01.getSchema().getProperties().get("bar"));
//        assertNotNull(type02.getSchema().getProperties().get("foo"));
//        assertNotNull(type03.getSchema().getProperties().get("bar"));
//        assertNull(type04.getSchema().getProperties().get("baz"));
//
//        type04.setDescriptor("baz", "number", false, "Only type04 should have this", null, null);
//
//        assertNull(type03.getSchema().getProperties().get("baz"));
//        assertNotNull(type04.getSchema().getProperties().get("baz"));
//
//        // Can't use except.expect() here because we still need to cleanup.
//        exception.expectMessage("Can't override parent schema.");
//        type02.setDescriptor("foo", "string", false, "Should be disallowed.", null, null);
//    }

    /**
     * This tests the case where an entity has relationships that aren't covered by any
     * constraints defined on type. This shouldn't validate.
     */
    @Test
    public void testEntityValidation01(
            @Mocked NodeRepository nodeRepository, @Mocked Instance instance) throws Exception {
        Type start = new Type("Start", "test", UUID.randomUUID().toString());
        Type end = new Type("End", "test", UUID.randomUUID().toString());
        start.addConstraint(end, Constraint.Cardinality.REQUIRED_TO_ONE, "TEST");

        /*
        A simple entity, without any relations except for the INSTANCE_OF, which
        is never validated against a constraint.
         */
        JsonNode entityJson = mapper.readTree("  {" +
                "  \"type\": \"Component\"," +
                "  \"name\": \"NVMe\"," +
                "  \"desc\": \"An NVMe solid state disk.\"," +
                "  \"meta\": {" +
                "    \"uuid\": \"4f84bd11-e108-4d98-8f03-0c4c6855e692\"," +
                "    \"relations\": {" +
                "      \"INSTANCE_OF\": {" +
                "        \"direction\": \"outgoing\"," +
                "        \"uuids\": [" +
                "          \"7a60ec32-0a9a-40b9-bf1f-d69674d4a233\"" +
                "        ]" +
                "      }" +
                "    }" +
                "  }," +
                "  \"data\": {" +
                "    \"memSizeKb\": 268435456" +
                "  }" +
                "}");

        // Add an outgoing relationship that validates against the constraint
        ObjectNode relations = (ObjectNode) entityJson.get("meta").get("relations");
        ObjectNode testRel = relations.putObject("TEST");
        testRel.put("direction", "outgoing");
        ArrayNode uuidArray = testRel.putArray("uuids");
        UUID testRelTo = UUID.randomUUID();
        uuidArray.add(testRelTo.toString());

        // The only relationship we have validates against the constraint
        new Expectations() {{
            nodeRepository.findByUuid(anyString);
            result = instance;

            instance.isOfType(end);
            result = true;
        }};

        // ... so this will be true
        assertTrue(start.verifyEntityToConstraints(mapper.treeToValue(entityJson, Entity.class)));

        // We'll add an __incoming__ relationship that doesn't have a constraint, this shouldn't invalidate anything
        ObjectNode incoming = relations.putObject("INCOMING_NOT_UNDER_CONSTRAINT");
        incoming.put("direction", "incoming");
        uuidArray = incoming.putArray("uuids");
        UUID incomingUuid = UUID.randomUUID();
        uuidArray.add(incomingUuid.toString());

        // The new relationship is incoming, validation should be alright
        assertTrue(start.verifyEntityToConstraints(mapper.treeToValue(entityJson, Entity.class)));

        // Now we'll introduce a relationship that is not under a constraint
        ObjectNode notUnderConstraint = relations.putObject("NOT_CONSTRAINED");
        notUnderConstraint.put("direction", "outgoing");
        uuidArray = notUnderConstraint.putArray("uuids");
        UUID nucTo = UUID.randomUUID();
        uuidArray.add(nucTo.toString());

        // This shouldn't validate. Unknown outgoing relationship added.
        assertFalse(start.verifyEntityToConstraints(mapper.treeToValue(entityJson, Entity.class)));
    }

}
