package com.raaftech.structure.graph;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.repositories.InstanceRepository;
import com.raaftech.structure.graph.repositories.NodeRepository;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
            @Mocked NodeRepository nodeRepository, @Mocked Instance instance1, @Mocked Instance instance2) throws Exception {
        Type start = new Type("Start", "test", UUID.randomUUID().toString());
        Type end = new Type("End", "test", UUID.randomUUID().toString());
        start.addConstraint(end, Constraint.Cardinality.REQUIRED_TO_ONE, "TEST");

        /*
        A simple entity, without any relations except for the INSTANCE_OF, which
        is never validated against a constraint.
         */
        JsonNode entityJson = mapper.readTree("  {" +
                "  \"name\": \"NVMe\"," +
                "  \"desc\": \"An NVMe solid state disk.\"," +
                "  \"meta\": {" +
                "    \"uuid\": \"4f84bd11-e108-4d98-8f03-0c4c6855e692\"," +
                "    \"relations\": {" +
                "      \"INSTANCE_OF\": {" +
                "        \"direction\": \"outgoing\"," +
                "        \"to\": [" +
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
        ArrayNode uuidArray = testRel.putArray("to");
        UUID testRelTo = UUID.randomUUID();
        uuidArray.add(testRelTo.toString());

        // The only relationship we have validates against the constraint
        new Expectations() {{
            nodeRepository.findByUuid(testRelTo.toString());
            result = instance1;

            instance1.isOfType(end);
            result = true;
            instance1.getUuid();
            result = testRelTo.toString();
        }};

        // ... so this will be true
        assertTrue(start.validateEntityToConstraints(mapper.treeToValue(entityJson, Entity.class)).getLeft());

        // We'll add an __incoming__ relationship that doesn't have a constraint, this shouldn't invalidate anything
        ObjectNode incoming = relations.putObject("INCOMING_NOT_UNDER_CONSTRAINT");
        incoming.put("direction", "incoming");
        uuidArray = incoming.putArray("to");
        UUID incomingUuid = UUID.randomUUID();
        uuidArray.add(incomingUuid.toString());

        // The new relationship is incoming, validation should be alright
        assertTrue(start.validateEntityToConstraints(mapper.treeToValue(entityJson, Entity.class)).getLeft());

        // Now we'll introduce a relationship that is not under a constraint
        ObjectNode notUnderConstraint = relations.putObject("NOT_CONSTRAINED");
        notUnderConstraint.put("direction", "outgoing");
        uuidArray = notUnderConstraint.putArray("to");
        UUID nucTo = UUID.randomUUID();
        uuidArray.add(nucTo.toString());

        new Expectations() {{
            nodeRepository.findByUuid(nucTo.toString());
            result = instance2;
            instance2.isOfType(end);
            result = true;
            instance2.getUuid();
            result = nucTo.toString();
        }};

        // This shouldn't validate. Unknown outgoing relationship added.
        assertFalse(start.validateEntityToConstraints(mapper.treeToValue(entityJson, Entity.class)).getLeft());

        // Just to go full circle, add a constraint for "NOT_CONSTRAINED" and see if it validates
        start.addConstraint(end, Constraint.Cardinality.OPTIONAL_TO_ONE, "NOT_CONSTRAINED");
        assertTrue(start.validateEntityToConstraints(mapper.treeToValue(entityJson, Entity.class)).getLeft());
    }

    /**
     * Test the case where we have two constraints with the same name and cardinality, but a different type.
     */
    @Test
    public void testEntityValidation02(
            @Mocked NodeRepository nodeRepository, @Mocked Instance instance1, @Mocked Instance instance2) throws Exception {
        Type start = new Type("Start", "test", UUID.randomUUID().toString());
        Type end01 = new Type("End01", "test", UUID.randomUUID().toString());
        Type end02 = new Type("End02", "test", UUID.randomUUID().toString());
        start.addConstraint(end01, Constraint.Cardinality.REQUIRED_TO_ONE, "TEST");
        start.addConstraint(end02, Constraint.Cardinality.REQUIRED_TO_ONE, "TEST");

        /*
        A simple entity, without any relations except for the INSTANCE_OF, which
        is never validated against a constraint.
         */
        JsonNode entityJson = mapper.readTree("  {" +
                "  \"name\": \"NVMe\"," +
                "  \"desc\": \"An NVMe solid state disk.\"," +
                "  \"meta\": {" +
                "    \"uuid\": \"4f84bd11-e108-4d98-8f03-0c4c6855e692\"," +
                "    \"relations\": {" +
                "      \"INSTANCE_OF\": {" +
                "        \"direction\": \"outgoing\"," +
                "        \"to\": [" +
                "          \"7a60ec32-0a9a-40b9-bf1f-d69674d4a233\"" +
                "        ]" +
                "      }" +
                "    }" +
                "  }," +
                "  \"data\": {" +
                "    \"memSizeKb\": 268435456" +
                "  }" +
                "}");

        // Add an outgoing relationship that validates against one of the constraints
        ObjectNode relations = (ObjectNode) entityJson.get("meta").get("relations");
        ObjectNode testRel = relations.putObject("TEST");
        testRel.put("direction", "outgoing");
        ArrayNode uuidArray = testRel.putArray("to");
        UUID testRelTo01 = UUID.randomUUID();
        UUID testRelTo02 = UUID.randomUUID();
        uuidArray.add(testRelTo01.toString());
        uuidArray.add(testRelTo02.toString());

        // The relationships we have validate against their constraints
        new Expectations() {{
            nodeRepository.findByUuid(testRelTo01.toString());
            result = instance1;

            instance1.isOfType(end01);
            result = true;

            instance1.getUuid();
            result = testRelTo01.toString();

            nodeRepository.findByUuid(testRelTo02.toString());
            result = instance2;

            instance2.isOfType(end02);
            result = true;

            instance2.getUuid();
            result = testRelTo02.toString();
        }};

        // ... so this should be true
        assertTrue(start.validateEntityToConstraints(mapper.treeToValue(entityJson, Entity.class)).getLeft());

        // First rel is OK, second one isn't
        new Expectations() {{
            nodeRepository.findByUuid(testRelTo01.toString());
            result = instance1;

            instance1.isOfType(end01);
            result = true;

            instance1.getUuid();
            result = testRelTo01.toString();

            nodeRepository.findByUuid(testRelTo02.toString());
            result = instance2;

            instance2.isOfType(end02);
            result = false;
        }};

        // ... so this should be false
        assertFalse(start.validateEntityToConstraints(mapper.treeToValue(entityJson, Entity.class)).getLeft());
    }

    /**
     * Test the case where we have two constraints with the same name, but a different type and cardinality.
     */
    @Test
    public void testEntityValidation03(
            @Mocked NodeRepository nodeRepository, @Mocked Instance instance01,
            @Mocked Instance instance02, @Mocked Instance instance03) throws Exception {
        Type start = new Type("Start", "test", UUID.randomUUID().toString());
        Type end01 = new Type("End01", "test", UUID.randomUUID().toString());
        Type end02 = new Type("End02", "test", UUID.randomUUID().toString());
        start.addConstraint(end01, Constraint.Cardinality.REQUIRED_TO_ONE, "TEST");
        start.addConstraint(end02, Constraint.Cardinality.OPTIONAL_TO_ONE, "TEST");

        /*
        A simple entity, without any relations except for the INSTANCE_OF, which
        is never validated against a constraint.
         */
        JsonNode entityJson = mapper.readTree("  {" +
                "  \"name\": \"NVMe\"," +
                "  \"desc\": \"An NVMe solid state disk.\"," +
                "  \"meta\": {" +
                "    \"uuid\": \"4f84bd11-e108-4d98-8f03-0c4c6855e692\"," +
                "    \"relations\": {" +
                "      \"INSTANCE_OF\": {" +
                "        \"direction\": \"outgoing\"," +
                "        \"to\": [" +
                "          \"7a60ec32-0a9a-40b9-bf1f-d69674d4a233\"" +
                "        ]" +
                "      }" +
                "    }" +
                "  }," +
                "  \"data\": {" +
                "    \"memSizeKb\": 268435456" +
                "  }" +
                "}");

        // Add an outgoing relationship that validates against one of the constraints
        ObjectNode relations = (ObjectNode) entityJson.get("meta").get("relations");
        ObjectNode testRel = relations.putObject("TEST");
        testRel.put("direction", "outgoing");
        ArrayNode uuidArray = testRel.putArray("to");
        UUID testRelTo01 = UUID.randomUUID();
        uuidArray.add(testRelTo01.toString());

        // The relationships we have validate against their constraints
        new Expectations() {{
            nodeRepository.findByUuid(testRelTo01.toString());
            result = instance01;

            instance01.isOfType(end01);
            result = true;

            instance01.getUuid();
            result = testRelTo01.toString();
        }};

        // ... so this should be true
        assertTrue(start.validateEntityToConstraints(mapper.treeToValue(entityJson, Entity.class)).getLeft());


        // Now add the second relationship
        UUID testRelTo02 = UUID.randomUUID();
        uuidArray.add(testRelTo02.toString());

        new Expectations() {{
            // first constraint check
            nodeRepository.findByUuid(testRelTo01.toString());
            result = instance01;
            nodeRepository.findByUuid(testRelTo02.toString());
            result = instance02;

            instance01.isOfType(end01);
            result = true;
            instance01.getUuid();
            result = testRelTo01.toString();
            instance02.isOfType(end01);
            result = false;
            instance02.getUuid();
            result = testRelTo02.toString();

            // second constraint check
            nodeRepository.findByUuid(testRelTo01.toString());
            result = instance01;
            nodeRepository.findByUuid(testRelTo02.toString());
            result = instance02;

            instance01.isOfType(end02);
            result = false;
            instance01.getUuid();
            result = testRelTo01.toString();
            instance02.isOfType(end02);
            result = true;
            instance02.getUuid();
            result = testRelTo02.toString();
        }};

        // ... so this should be true
        assertTrue(start.validateEntityToConstraints(mapper.treeToValue(entityJson, Entity.class)).getLeft());


        // Now add a third rel
        UUID testRelTo03 = UUID.randomUUID();
        uuidArray.add(testRelTo03.toString());

        new Expectations() {{
            // first constraint check should fail because cardinality is wrong
            nodeRepository.findByUuid(testRelTo01.toString());
            result = instance01;
            nodeRepository.findByUuid(testRelTo02.toString());
            result = instance02;
            nodeRepository.findByUuid(testRelTo03.toString());
            result = instance03;

            instance01.isOfType(end01);
            result = true;
            instance01.getUuid();
            result = testRelTo01.toString();
            instance02.isOfType(end01);
            instance02.getUuid();
            result = testRelTo02.toString();
            result = false;
            instance03.isOfType(end01);
            result = true;
            instance03.getUuid();
            result = testRelTo03.toString();
        }};

        // ... so this should be false
        assertFalse(start.validateEntityToConstraints(mapper.treeToValue(entityJson, Entity.class)).getLeft());
    }

    /**
     * Test the case where we have one constraints, and two relationships with the constraint's name,
     * but just one of them validates against that constraint.
     */
    @Test
    public void testEntityValidation04(
            @Mocked NodeRepository nodeRepository, @Mocked Instance instance01,
            @Mocked Instance instance02) throws Exception {
        Type start = new Type("Start", "test", UUID.randomUUID().toString());
        Type end = new Type("End01", "test", UUID.randomUUID().toString());
        start.addConstraint(end, Constraint.Cardinality.OPTIONAL_TO_MANY, "TEST");

        /*
        A simple entity, without any relations except for the INSTANCE_OF, which
        is never validated against a constraint.
         */
        JsonNode entityJson = mapper.readTree("  {" +
                "  \"name\": \"NVMe\"," +
                "  \"desc\": \"An NVMe solid state disk.\"," +
                "  \"meta\": {" +
                "    \"uuid\": \"4f84bd11-e108-4d98-8f03-0c4c6855e692\"," +
                "    \"relations\": {" +
                "      \"INSTANCE_OF\": {" +
                "        \"direction\": \"outgoing\"," +
                "        \"to\": [" +
                "          \"7a60ec32-0a9a-40b9-bf1f-d69674d4a233\"" +
                "        ]" +
                "      }" +
                "    }" +
                "  }," +
                "  \"data\": {" +
                "    \"memSizeKb\": 268435456" +
                "  }" +
                "}");

        // Add two outgoing relationship of which one will validate against the constraint
        ObjectNode relations = (ObjectNode) entityJson.get("meta").get("relations");
        ObjectNode testRel = relations.putObject("TEST");
        testRel.put("direction", "outgoing");
        ArrayNode uuidArray = testRel.putArray("to");
        UUID testRelTo01 = UUID.randomUUID();
        uuidArray.add(testRelTo01.toString());
        UUID testRelTo02 = UUID.randomUUID();
        uuidArray.add(testRelTo02.toString());

        new Expectations() {{
            // first constraint check
            nodeRepository.findByUuid(testRelTo01.toString());
            result = instance01;
            nodeRepository.findByUuid(testRelTo02.toString());
            result = instance02;

            instance01.isOfType(end);
            result = true;
            instance01.getUuid();
            result = testRelTo01.toString();
            instance02.isOfType(end);
            result = false;
        }};

        /*
        Because we have one outgoing rel that doesn't validate against the constraint
        it should either validate against another constraint, or the whole validation should fail.
         */
        assertFalse(start.validateEntityToConstraints(mapper.treeToValue(entityJson, Entity.class)).getLeft());
    }
}
