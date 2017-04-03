package com.raaftech.structure.graph;

import com.google.common.collect.ImmutableSet;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.repositories.NodeRepository;
import com.raaftech.structure.graph.repositories.TypeRepository;
import com.raaftech.structure.helper.Triple;
import com.raaftech.structure.next.Entity;
import mockit.Expectations;
import mockit.Mocked;
import mockit.StrictExpectations;
import mockit.Verifications;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.graphdb.Direction;

import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.*;

public class ConstraintTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mocked
    Context context;
    @Mocked
    NodeRepository nodeRepository;

    /*
     * Verify the cases where constraint verification fails or succeeds because of
     * the cardinality.
     */
    @Test
    public void testCardinalities(
            @Mocked Type start,
            @Mocked Type end,
            @Mocked Entity entity
    ) throws Exception {

        final String relationshipName = "TEST_REL";

        Constraint constraintOptionalToOne = new Constraint(start, end,
                Constraint.Cardinality.OPTIONAL_TO_ONE, relationshipName);
        Constraint constraintOptionalToMany = new Constraint(start, end,
                Constraint.Cardinality.OPTIONAL_TO_MANY, relationshipName);
        Constraint constraintRequiredToMany = new Constraint(start, end,
                Constraint.Cardinality.REQUIRED_TO_MANY, relationshipName);
        Constraint constraintRequiredToOne = new Constraint(start, end,
                Constraint.Cardinality.REQUIRED_TO_ONE, relationshipName);

        new Expectations() {{
            entity.getType();
            result = "Component";

            entity.getRelations();
            /*
            This set represents two "SOME_OTHER_REL" relationships to random UUIDs,
            but no relationships to the relationship under constraint.
             */
            result = ImmutableSet.of(
                    new Triple<String, String, Set<String>>("SOME_OTHER_REL", Direction.OUTGOING.toString(), ImmutableSet.of(
                            UUID.randomUUID().toString(),
                            UUID.randomUUID().toString()
                    ))
            );
        }};

        assertTrue(constraintOptionalToOne.verifyConstraint(entity));
        assertTrue(constraintOptionalToMany.verifyConstraint(entity));
        assertFalse(constraintRequiredToMany.verifyConstraint(entity));
        assertFalse(constraintRequiredToOne.verifyConstraint(entity));


        // Now test the case where we only have one related node
        new Expectations() {{
            entity.getRelations();
            // This set represents one node related by the relationship under constraint
            result = ImmutableSet.of(
                    new Triple<String, String, Set<String>>(relationshipName, Direction.OUTGOING.toString(), ImmutableSet.of(
                            UUID.randomUUID().toString()
                    ))
            );
        }};

        assertTrue(constraintOptionalToOne.verifyConstraint(entity));
        assertFalse(constraintOptionalToMany.verifyConstraint(entity));
        assertFalse(constraintRequiredToMany.verifyConstraint(entity));
        assertTrue(constraintRequiredToOne.verifyConstraint(entity));
    }

    /**
     * Verify the case where the cardinality check passes, and we want to check
     * whether the related nodes are of the proper types.
     */
    @Test
    public void testTypeConstraint(
            @Mocked Type start,
            @Mocked Type end,
            @Mocked Entity entity,
            @Mocked Instance node1,
            @Mocked Instance node2
    ) throws Exception {

        final String relationshipName = "TEST_REL";

        final UUID firstUuid = UUID.randomUUID();
        final UUID secondUuid = UUID.randomUUID();

        Constraint constraintRequiredToMany = new Constraint(start, end,
                Constraint.Cardinality.REQUIRED_TO_MANY, relationshipName);

        new Expectations() {{
            entity.getType();
            result = "Component";

            entity.getRelations();
            /*
            This set represents two relationships to previously generated UUIDs using the
            relationship under constraint.
             */
            result = ImmutableSet.of(
                    new Triple<String, String, Set<String>>(relationshipName, Direction.OUTGOING.toString(), ImmutableSet.of(
                            firstUuid.toString(), secondUuid.toString()
                    ))
            );

            // Return mocked nodes from the mocked repository
            nodeRepository.findByUuid(firstUuid.toString());
            result = node1;
            nodeRepository.findByUuid(secondUuid.toString());
            result = node2;

            node1.isOfType(end);
            result = true;
            node2.isOfType(end);
            result = true;
        }};

        // Both related nodes are of the proper (sub)type, so verification is true
        assertTrue(constraintRequiredToMany.verifyConstraint(entity));


        // Now the second node will be of the wrong type, which result in a false verification
        new Expectations() {{
            node2.isOfType(end);
            result = false;
        }};

        assertFalse(constraintRequiredToMany.verifyConstraint(entity));
    }

    /*
     * Test the case where an entity has relationships to a node that doesn't
     * exist in the graph. This should fail the verification.
     */
    @Test
    public void testNonExistingNode(
            @Mocked Type start,
            @Mocked Type end,
            @Mocked Entity entity,
            @Mocked Instance node
    ) throws Exception {

        final String relationshipName = "TEST_REL";

        final UUID firstUuid = UUID.randomUUID();
        final UUID secondUuid = UUID.randomUUID();

        Constraint constraintRequiredToMany = new Constraint(start, end,
                Constraint.Cardinality.REQUIRED_TO_MANY, relationshipName);

        new Expectations() {{
            entity.getType();
            result = "Component";

            entity.getRelations();
            /*
            This set represents two relationships to previously generated UUIDs using the
            relationship under constraint.
             */
            result = ImmutableSet.of(
                    new Triple<String, String, Set<String>>(relationshipName, Direction.OUTGOING.toString(), ImmutableSet.of(
                            firstUuid.toString(), secondUuid.toString()
                    ))
            );

            // One of these will be null, meaning no node with that UUID exists in the db
            nodeRepository.findByUuid(firstUuid.toString());
            result = node;
            nodeRepository.findByUuid(secondUuid.toString());
            result = null;
        }};

        // Because one node can't be found, the verification fails
        assertFalse(constraintRequiredToMany.verifyConstraint(entity));
    }

    // Test whether specialization method works
    @Test
    public void isSpecialization(
            @Mocked Type start,
            @Mocked Type end01,
            @Mocked Type end02,
            @Mocked TypeRepository typeRepository
    ) throws Exception {
        Constraint constraint1 = new Constraint(start, end01, Constraint.Cardinality.OPTIONAL_TO_MANY, "SOME_REL");
        Constraint constraint2 = new Constraint(start, end02, Constraint.Cardinality.OPTIONAL_TO_MANY, "SOME_REL");

        // end02 subtypes end01
        new StrictExpectations() {{
            end02.isOfType(anyString);
            result = true;
        }};
        assertTrue(constraint1.isSpecialization(constraint2));

        // end02 doesn't subtype end01
        new StrictExpectations() {{
            end02.isOfType(anyString);
            result = false;
        }};
        assertFalse(constraint1.isSpecialization(constraint2));

        // Check with a stricter cardinality on constraint1. This fails before type checks, so no need for expectations
        constraint1 = new Constraint(start, end01, Constraint.Cardinality.REQUIRED_TO_ONE, "SOME_REL");
        assertFalse(constraint1.isSpecialization(constraint2));

        // Check with a different rel name. This fails before type checks, so no need for expectations
        constraint1 = new Constraint(start, end01, Constraint.Cardinality.OPTIONAL_TO_MANY, "OTHER_REL");
        assertFalse(constraint1.isSpecialization(constraint2));

        // Make constraint2 the same, but with a stricter cardinality
        constraint2 = new Constraint(start, end02, Constraint.Cardinality.REQUIRED_TO_MANY, "OTHER_REL");
        // end02 does subtype end01
        new StrictExpectations() {{
            end02.isOfType(anyString);
            result = true;
        }};
        assertTrue(constraint1.isSpecialization(constraint2));
    }

    /*
     * Make sure cardinality enum is ordered properly
     */
    @Test
    public void testCardinalityOrdering() throws Exception {
        assertTrue(Constraint.Cardinality.OPTIONAL_TO_MANY.ordinal() < Constraint.Cardinality.OPTIONAL_TO_ONE.ordinal());
        assertTrue(Constraint.Cardinality.OPTIONAL_TO_ONE.ordinal() < Constraint.Cardinality.REQUIRED_TO_MANY.ordinal());
        assertTrue(Constraint.Cardinality.REQUIRED_TO_MANY.ordinal() < Constraint.Cardinality.REQUIRED_TO_ONE.ordinal());
    }

    /*
     * Test running a constraint verification on a type, this results in an exception.
     */
    @Test
    public void testVerificationOnType(@Mocked Type type, @Mocked Entity entity) throws Exception {
        Constraint constraint = new Constraint(type, type, Constraint.Cardinality.OPTIONAL_TO_MANY, "TEST_REL");

        new Expectations() {{
            entity.getType();
            result = "Type";
        }};

        thrown.expect(IllegalArgumentException.class);
        constraint.verifyConstraint(entity);
    }

    @Test
    public void testEqualsAndHashCode(@Mocked Type start, @Mocked Type end) {
        // These constraints are the same
        Constraint constraint1 = new Constraint(start, end, Constraint.Cardinality.OPTIONAL_TO_MANY, "SOME_REL");
        Constraint constraint2 = new Constraint(start, end, Constraint.Cardinality.OPTIONAL_TO_MANY, "SOME_REL");
        assertEquals(constraint1, constraint2);
        assertEquals(constraint1.hashCode(), constraint2.hashCode());

        // Constraint2 now differs from constraint1 by relationship name
        constraint2 = new Constraint(start, end, Constraint.Cardinality.OPTIONAL_TO_MANY, "SOME_OTHER_REL");
        assertNotEquals(constraint1, constraint2);

        // Constraint2 now different from constraint1 because the begin node is different
        constraint2 = new Constraint(end, end, Constraint.Cardinality.OPTIONAL_TO_MANY, "SOME_REL");
        assertNotEquals(constraint1, constraint2);

        // Constraint2 now different from constraint1 because the end node is different
        constraint2 = new Constraint(start, start, Constraint.Cardinality.OPTIONAL_TO_MANY, "SOME_REL");
        assertNotEquals(constraint1, constraint2);

        // Constraint2 now different from constraint1 because the cardinality different
        constraint2 = new Constraint(start, end, Constraint.Cardinality.OPTIONAL_TO_ONE, "SOME_REL");
        assertNotEquals(constraint1, constraint2);

        assertNotEquals(constraint1.hashCode(), constraint2.hashCode());
    }
}