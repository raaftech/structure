package com.raaftech.structure.graph;

import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.repositories.NodeRepository;
import com.raaftech.structure.next.Entity;
import org.apache.commons.lang.Validate;
import org.neo4j.graphdb.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.annotation.*;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The constraint class is responsible for denoting constraints between (instances of) types.
 * When an instance of a type is created all the outgoing Constraint objects that apply will be validated, and only
 * when no constraint has been broken will the instance be persisted.
 *
 * The main reason for this is so we can avoid relationships between instances that cannot exist. Say we have
 * a CPU type and a motherboard type. We can create a constraint that will only allow a motherboard instance to be the
 * parent of a CPU instance in stead of the other way around.
 *
 */
@RelationshipEntity(type = "CONSTRAINTS")
public class Constraint {

    public enum Cardinality { REQUIRED_TO_ONE, OPTIONAL_TO_ONE, REQUIRED_TO_MANY, OPTIONAL_TO_MANY }

    @GraphId
    private Long id;

    @StartNode
    @Fetch
    private Type startOfConstraint;

    @EndNode
    @Fetch
    private Type endOfConstraint;

    @GraphProperty
    private Cardinality cardinality;

    @GraphProperty
    private String forRelationship;

    @Transient
    private final NodeRepository nodeRepository = Context.getNodeRepository();
    @Transient
    static final Logger log = LoggerFactory.getLogger(Constraint.class);

    @SuppressWarnings("unused")
    public Constraint() {
        // Empty constructor for SDN
    }

    public Constraint(Type startOfConstraint, Type endOfConstraint, Cardinality cardinality, String forRelationship) {
        Validate.notNull(startOfConstraint, "A constraint cannot be created with null types.");
        Validate.notNull(endOfConstraint, "A constraint cannot be created with null types.");
        Validate.notNull(cardinality, "A constraint cannot without a cardinality.");
        Validate.notEmpty(forRelationship, "A relationship name must be given when creating a constraint.");

        this.startOfConstraint = startOfConstraint;
        this.endOfConstraint = endOfConstraint;
        this.cardinality = cardinality;
        this.forRelationship = forRelationship;
    }

    /**
     * Check if a particular amount is valid according to the cardinality of this constraint.
     * @param amount An amount. Usually the amount of nodes incident to relationships that are constrained by this
     *               Constraint.
     * @return True when the amount if valid according to the cardinality, invalid if not.
     */
    private boolean requiredAmount(int amount) {
        if (amount == 0) {
            return cardinality == Cardinality.OPTIONAL_TO_MANY || cardinality == Cardinality.OPTIONAL_TO_ONE;
        } else if (amount == 1) {
            return cardinality == Cardinality.OPTIONAL_TO_ONE || cardinality == Cardinality.REQUIRED_TO_ONE;
        } else {
            return cardinality == Cardinality.OPTIONAL_TO_MANY || cardinality == Cardinality.REQUIRED_TO_MANY;
        }
    }

    /**
     * Given a non-type entity that is about to be persisted return whether it validates against the constraints
     * set on its type.
     * @param entity The entity that needs to be checked against this constraint.
     * @return Whether the entity validates against this constraint.
     * @throws IllegalArgumentException Thrown when we pass an entity that represents a {@link Type type}.
     */
    public boolean verifyConstraint(Entity entity) {
        if (Objects.equals("Type", entity.getType())) {
            throw new IllegalArgumentException("Constraints can only be validated for non-type entities.");
        }

        Set<UUID> relatedNodes = entity.getRelations().stream()
                .filter(triple -> Objects.equals(triple.getLeft(), forRelationship))
                .filter(triple -> triple.getMiddle().equals(Direction.OUTGOING.name()))
                .flatMap(triple -> triple.getRight().stream())
                .map(UUID::fromString)
                .collect(Collectors.toSet());

        // Relationships now contains zero to many UUIDs, all of these should be validated against the constraint.

        // Check if the amount of found nodes is OK
        if (!requiredAmount(relatedNodes.size())) {
            return false;
        } else {
            // In case we have 0 related nodes and a 0..1 or 0..* cardinality, we can return
            if (relatedNodes.isEmpty()) {
                return true;
            }
        }

        /*
        Now we need to check if all the found UUIDs are of the expected type. This assumes that all those UUIDs
        are present in the database. This is indeed the case when a set of entities is persisted in the right order.
        If any of these UUIDs do not exist in the database we'll fail the validation as well.

        Note that we're only interested in Instances.
         */

        // This function maps a UUID to an optional Node
        Function<UUID, Optional<Node>> uuidToNode = uuid -> {
            Node maybeNode = nodeRepository.findByUuid(uuid.toString());
            if (maybeNode == null) {
                log.error("Error while retrieving node with UUID \"{}\" from database, this fails " +
                        "the constraint validation", uuid);
                return Optional.empty();
            }
            return Optional.of(maybeNode);
        };

        Set<Optional<Node>> foundRelatedNodes = relatedNodes.stream()
                .map(uuidToNode)
                .collect(Collectors.toSet());

        // Make sure there aren't any empty optionals in the set
        if (foundRelatedNodes.stream()
                .anyMatch(maybeNode -> !maybeNode.isPresent())) {
            return false;
        }

        /*
        Filter all the Instance nodes we found, cast them to Instance, and check whether they're of the expected
        types. Return this value, because if they all match the expected type the validation succeeds.
         */
        return foundRelatedNodes.stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(node -> node instanceof Instance)
                .map(node -> (Instance) node)
                // now we need to check if we're an instance of the type allowed by this constraint
                .allMatch(instance -> instance.isOfType(endOfConstraint));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Constraint that = (Constraint) o;

        if (!startOfConstraint.equals(that.startOfConstraint)) return false;
        if (!endOfConstraint.equals(that.endOfConstraint)) return false;
        if (!cardinality.equals(that.cardinality)) return false;
        return forRelationship.equals(that.forRelationship);

    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(startOfConstraint);
        result = 31 * result + Objects.hashCode(endOfConstraint);
        result = 31 * result + Objects.hashCode(forRelationship);
        result = 31 * result + Objects.hashCode(cardinality);
        return result;
    }
}
