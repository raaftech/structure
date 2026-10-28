package tech.raaf.structure.graph;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.graph.repositories.NodeRepository;
import tools.jackson.databind.annotation.JsonSerialize;

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
@JsonSerialize(using = ConstraintSerializer.class)
public class Constraint {

  // The order in this enum matters. Cardinalities are placed loosest to strictest so we can compare them
  public enum Cardinality {
    OPTIONAL_TO_MANY,
    OPTIONAL_TO_ONE,
    REQUIRED_TO_MANY,
    REQUIRED_TO_ONE,
  }

  // The columns that fromRows reads, for (a)-[r:CONSTRAINTS]->(b)
  static final String COLUMNS = "RETURN a, b, r.forRelationship AS forRelationship, r.cardinality AS cardinality";

  private Type startOfConstraint;
  private Type endOfConstraint;
  private Cardinality cardinality;
  private String forRelationship;

  private final NodeRepository nodeRepository = Context.getNodeRepository();
  static final Logger log = LoggerFactory.getLogger(Constraint.class);

  public Constraint(Type startOfConstraint, Type endOfConstraint, Cardinality cardinality, String forRelationship) {
    Validate.notNull(startOfConstraint, "A constraint cannot be created with null types.");
    Validate.notNull(endOfConstraint, "A constraint cannot be created with null types.");
    Validate.notNull(cardinality, "A constraint cannot be created without a cardinality.");
    Validate.notEmpty(forRelationship, "A relationship name must be given when creating a constraint.");

    this.startOfConstraint = startOfConstraint;
    this.endOfConstraint = endOfConstraint;
    this.cardinality = cardinality;
    this.forRelationship = forRelationship;
  }

  // Make constraints from rows with the columns in COLUMNS
  public static Set<Constraint> fromRows(List<Map<String, Object>> rows) {
    final Set<Constraint> constraints = new HashSet<>();
    for (Map<String, Object> row : rows) {
      constraints.add(
        new Constraint(
          NodeMapper.map((GraphNode) row.get("a")),
          NodeMapper.map((GraphNode) row.get("b")),
          Cardinality.valueOf((String) row.get("cardinality")),
          (String) row.get("forRelationship")
        )
      );
    }
    return constraints;
  }

  /**
   * This class holds the data that results from the validation of a constraint on an entity.
   */
  public class ValidationReport {

    private boolean isSuccess;
    private final String forRelationship = Constraint.this.getForRelationship();
    private Optional<EntityValidationError> error = Optional.empty();
    private Set<String> validatedUuids;

    /**
     * Did the validation succeed?
     * @return True if it did, false if not.
     */
    public boolean isSuccess() {
      return isSuccess;
    }

    /**
     * The name of the relationship that was validated.
     * @return
     */
    public String getForRelationship() {
      return forRelationship;
    }

    /**
     * We might have encountered some errors that stem from the validation.
     * @return An optional wrapping possible errors.
     */
    public Optional<EntityValidationError> getError() {
      return error;
    }

    /**
     * The UUIDs that were validated.
     * @return A set of UUIDs,
     */
    public Set<String> getValidatedUuids() {
      return validatedUuids;
    }
  }

  /**
   * Check if a particular amount is valid according to the cardinality of this constraint.
   * @param amount An amount. Usually the amount of nodes incident to relationships that are constrained by this
   *               Constraint.
   * @return True when the amount is valid according to the cardinality, invalid if not.
   */
  private boolean requiredAmount(int amount) {
    if (amount == 0) {
      return cardinality == Cardinality.OPTIONAL_TO_MANY || cardinality == Cardinality.OPTIONAL_TO_ONE;
    } else if (amount == 1) {
      return cardinality.ordinal() >= Cardinality.OPTIONAL_TO_MANY.ordinal();
    } else {
      return cardinality == Cardinality.OPTIONAL_TO_MANY || cardinality == Cardinality.REQUIRED_TO_MANY;
    }
  }

  /**
   * Given a non-type entity that is about to be persisted return whether it validates against the constraints
   * set on its type, and the relationships this constraint validates.
   * @param entity The entity that needs to be checked against this constraint.
   * @return An object containing all the data of the validation.
   * @throws IllegalArgumentException Thrown when we pass an entity that represents a {@link Type type}.
   */
  public ValidationReport validateConstraint(Entity entity) {
    if (!entity.isInstance()) {
      throw new IllegalArgumentException("Constraints can only be validated for non-type entities.");
    }

    ValidationReport validationReport = new ValidationReport();
    validationReport.validatedUuids = ImmutableSet.of();

    final Set<UUID> relatedNodes = entity
      .getRelations()
      .stream()
      .filter(triple -> Objects.equals(triple.getLeft(), forRelationship))
      .filter(triple -> triple.getMiddle().toLowerCase().equals(Direction.OUTGOING.name().toLowerCase()))
      .flatMap(triple -> triple.getRight().stream())
      .map(UUID::fromString)
      .collect(Collectors.toSet());

    // Relationships now contains zero to many UUIDs, all of these should be validated against the constraint.

    // If we found 0 nodes we only need to check against cardinality
    if (relatedNodes.isEmpty()) {
      validationReport.isSuccess = requiredAmount(0);
      if (!validationReport.isSuccess) {
        validationReport.error = Optional.of(
          new EntityValidationError(
            400,
            entity.getUuid(),
            entity.getName(),
            "Zero relationships found for " +
              "constraint " +
              validationReport.forRelationship +
              " to " +
              endOfConstraint.getName() +
              ", this violates cardinality " +
              cardinality.toString()
          )
        );

        // validationReport.error = Optional.of("Zero relationships found for " +
        //     "constraint " + validationReport.forRelationship + " to " + endOfConstraint.getName() +
        //     ", this violates cardinality " + cardinality.toString());
      }
      return validationReport;
    }

    /*
      Now we need to check if all the found UUIDs are of the expected type. This assumes that all those UUIDs
      are present in the database. This is indeed the case when a set of entities is persisted in the right order.
      If any of these UUIDs do not exist in the database we'll fail the validation as well.

      Note that we're only interested in Instances.
    */

    // This function maps a UUID to an optional Node
    final Function<UUID, Pair<UUID, Optional<Node>>> uuidToNode = uuid -> {
      Node maybeNode = nodeRepository.findByUuid(uuid.toString());
      if (maybeNode == null) {
        return Pair.of(uuid, Optional.empty());
      }
      return Pair.of(uuid, Optional.of(maybeNode));
    };

    final Set<Pair<UUID, Optional<Node>>> foundRelatedNodes = relatedNodes
      .stream()
      .map(uuidToNode)
      .collect(Collectors.toSet());

    // Make sure there aren't any empty optionals in the set
    final Set<UUID> nonexistantUuids = foundRelatedNodes
      .stream()
      .filter(pair -> !pair.getRight().isPresent())
      .map(Pair::getLeft)
      .collect(Collectors.toSet());
    if (!nonexistantUuids.isEmpty()) {
      validationReport.isSuccess = false;
      validationReport.error = Optional.of(
        new EntityValidationError(
          404,
          entity.getUuid(),
          entity.getName(),
          EntityValidationError.uuidSetToHashSet(nonexistantUuids),
          "Related nodes not found"
        )
      );

      return validationReport;
    }

    /*
      Filter all the Instance nodes we found, cast them to Instance, and check whether they're of the expected
      types. This will return a set containing all the nodes that fall under this constraint.
    */
    final Set<String> validateAgainstConstraint = foundRelatedNodes
      .stream()
      .map(Pair::getRight)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .filter(node -> node instanceof Instance)
      .map(node -> (Instance) node)
      // now we need to check if we're an instance of the type allowed by this constraint
      .filter(instance -> instance.isOfType(endOfConstraint))
      .map(Instance::getUuid)
      .collect(Collectors.toSet());

    // Return whether the amount of nodes we found in the previous step is valid according to the cardinality.
    validationReport.validatedUuids = validateAgainstConstraint;
    validationReport.isSuccess = requiredAmount(validateAgainstConstraint.size());
    if (!validationReport.isSuccess) {
      validationReport.error = Optional.of(
        new EntityValidationError(
          500,
          entity.getUuid(),
          entity.getName(),
          validateAgainstConstraint,
          "Cardinality violations of constraint: " +
            validationReport.forRelationship +
            " to " +
            endOfConstraint.getName() +
            ", this violates cardinality " +
            cardinality.toString()
        )
      );
    }
    return validationReport;
  }

  /**
   * Check whether a given constraint specializes the constraint the method is called on.
   * A constraint is a specialization of another constraint if it constraints the same relationship,
   * has the same or stricter cardinality, and the type it points to is a subtype of the other one.
   * @param possibleSpecialization The constraint that might be a specialization.
   * @return True when the given argument is indeed a specialization, false if not.
   */
  public boolean isSpecialization(Constraint possibleSpecialization) {
    return (
      Objects.equals(getForRelationship(), possibleSpecialization.getForRelationship()) &&
      cardinality.ordinal() <= possibleSpecialization.cardinality.ordinal() &&
      possibleSpecialization.endOfConstraint.isOfType(endOfConstraint.getName())
    );
  }

  /**
   * Return the relationships constrained by this constraint.
   * @return The relationship name.
   */
  public String getForRelationship() {
    return forRelationship;
  }

  /**
   * The beginning of this constraint.
   * @return The type this constraint points from.
   */
  public Type getStartOfConstraint() {
    return startOfConstraint;
  }

  /**
   * The end of this constraint.
   * @return The type this constraints points to.
   */
  public Type getEndOfConstraint() {
    return endOfConstraint;
  }

  /**
   * The cardinality.
   * @return See {@link Cardinality}
   */
  public Cardinality getCardinality() {
    return cardinality;
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
