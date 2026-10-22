package tech.raaf.structure.graph;

import static io.vertx.core.json.Json.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.raaf.structure.base.Configuration;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.graph.repositories.TypeRepository;
import tech.raaf.structure.helper.JsonSchemaBuilder;
import tech.raaf.structure.helper.Triple;

/**
 * Give a description of Type here.
 *
 * @author Rubin Simons <rubin@raaf.tech>
 * @since 2014-11-27
 */
public class Type extends Node {

  static final Logger log = LoggerFactory.getLogger(Type.class);

  int glyphId;
  boolean isAbstract;

  // The constraints of a new type. A persisted type reads them from the graph.
  private Set<Constraint> outgoingConstraints;

  /*
    This field contains names of data properties that instances of this type can have.
    When creating an instance of a type, we'll check if another instance of that type
    already exists with the values for the properties that are stored in the `identity` field.
    This way we can define what makes instances of a particular type unique.
    An example would be the `addr` data property of an `IpAddress` type.
  */
  Set<String> identity;

  Map<String, Object> kv = new HashMap<>();

  // isaUuid exists only to temporarily save a UUID of a Type's parent
  // in case the parent doesn't exist in the database yet.
  private String isaUuid;

  private TypeRepository typeRepository = Context.getTypeRepository();

  /****************/
  /* Constructors */
  /****************/

  public Type() {}

  public Type(String name, String desc, int glyphId) {
    super(name, desc);
    this.glyphId = glyphId;
    initSchema();
  }

  public Type(String name, String desc, int glyphId, String checksum) {
    super(name, desc);
    this.glyphId = glyphId;
    this.checksum = checksum;
    initSchema();
  }

  public Type(String name, String desc, String uuid) {
    super(name, desc, uuid);
    initSchema();
  }

  public Type(String name, String desc, String uuid, int glyphId) {
    super(name, desc, uuid);
    this.glyphId = glyphId;
    initSchema();
  }

  public Type(String name, String desc, String uuid, int glyphId, String checksum) {
    super(name, desc, uuid, checksum);
    this.glyphId = glyphId;
    this.checksum = checksum;
    initSchema();
  }

  /*******************/
  /* Private Methods */
  /*******************/

  private void initSchema() {
    // TODO: Entity, this can go, i think
  }

  /******************/
  /* Public Methods */
  /******************/

  public Set<Type> getChildIsaSet() {
    return persisted ? related("<-[:ISA]-(b:Type)") : new HashSet<>();
  }

  /**
   * Add a constraint to this type. See {@link Constraint}.
   * @param to Add a constraint to the given type.
   * @param cardinality The cardinality of the constraint.
   * @param relationshipName What relationship between this and <code>to</code> should be constrained?
   * @return The type this method was called on.
   */
  public Type addConstraint(Type to, Constraint.Cardinality cardinality, String relationshipName) {
    Validate.notNull(to, "Cannot create constraint to null type.");
    Validate.notEmpty(relationshipName, "Cannot create constraint for empty relationship name.");

    if (outgoingConstraints == null) {
      outgoingConstraints = new LinkedHashSet<>();
    }

    outgoingConstraints.add(new Constraint(this, to, cardinality, relationshipName));

    return this;
  }

  /**
   * Return the constraints an instance of this type should validate against.
   * We might inherit some constraints which can be specialized deeper in the type tree
   * by some other constraint. We want to keep the most specific constraints only.
   * @param ownConstraints This type's own incoming or outgoing constraints.
   * @param inheritedConstraints All the incoming or outgoing inherited constraints.
   * @return A set containing constraints of which no generalization exists in the set.
   */
  private Set<Constraint> getApplicableConstraints(
    Set<Constraint> inheritedConstraints,
    Set<Constraint> ownConstraints
  ) {
    // This predicate is used to remove constraints that are specialized by other constraints
    BiPredicate<Set<Constraint>, Constraint> isNotSpecialized = (setToFilter, inheritedConstraint) ->
      setToFilter.stream().allMatch(ownConstraint -> !inheritedConstraint.isSpecialization(ownConstraint));

    // First filter the inherited constraints so we only keep "specialized" constraints
    final Set<Constraint> filteredInheritedConstraints = inheritedConstraints
      .stream()
      .filter(inheritedConstraint -> isNotSpecialized.test(inheritedConstraints, inheritedConstraint))
      .collect(Collectors.toSet());

    // Return the constraints we should actually validate to
    return Stream.concat(
      ownConstraints.stream(),
      filteredInheritedConstraints
        .stream()
        .filter(inheritedConstraint -> isNotSpecialized.test(ownConstraints, inheritedConstraint))
    ).collect(Collectors.toSet());
  }

  /**
   * Given an entity, verify it satisfies every (outward) constraint applied to this
   * type.
   * @param entity The entity to verify.
   * @return A tuple containing whether verification succeeded, and a set of that may
   * contain errors encountered during validation.
   */
  // The own constraints of this type, in one direction
  private Set<Constraint> getOwnConstraints(Direction direction) {
    if (!persisted) {
      return direction == Direction.OUTGOING && outgoingConstraints != null ? outgoingConstraints : new HashSet<>();
    }
    final String pattern =
      direction == Direction.OUTGOING
        ? "(a:Type {uuid: $uuid})-[r:CONSTRAINTS]->(b:Type)"
        : "(a:Type)-[r:CONSTRAINTS]->(b:Type {uuid: $uuid})";
    return Constraint.fromRows(
      Graph.query("MATCH " + pattern + " " + Constraint.COLUMNS, ImmutableMap.of("uuid", getUuid()))
    );
  }

  public Pair<Boolean, Set<EntityValidationError>> validateEntityToConstraints(Entity entity) {
    if (!entity.isInstance()) {
      throw new IllegalArgumentException(
        new EntityValidationError(
          500,
          getUuid(),
          getName(),
          "Constraints can only be validated for non-type entities."
        ).toString()
      );
    }

    // All errors that occur during validation are collected in `errors`
    Set<EntityValidationError> errors = new HashSet<>();

    /*
      We need to whitelist a few relationships. Constraints only work on relationships between instances or
      its subtypes. This whitelist contains all relationships that don't belong to this category and should
      be ignored during constraint checks.
    */
    final Set<String> whitelist = ImmutableSet.of("INSTANCE_OF", "PROVIDED_BY", "APPLIES_TO", "GIVES_CAPABILITY");

    /*
      The merged set of inherited constraints and outgoingConstraints might contain
      generalizations of other constraints in the set, we don't want those so we remove them.
    */
    Set<Constraint> constraintsToValidateTo = getApplicableConstraints(
      typeRepository.getInheritedOutgoingConstraints(getUuid()),
      getOwnConstraints(Direction.OUTGOING)
    );

    /*
      There are a few relationships that aren't inter-instance, but between instances and types.
      These don't fall under any constraint, so we need to filter them using the whitelist.
    */
    final Map<String, Set<String>> relationsToTest = entity
      .getRelations()
      .stream()
      .filter(t -> !whitelist.contains(t.getLeft()))
      .filter(t -> t.getMiddle().toLowerCase().equals(Direction.OUTGOING.name().toLowerCase()))
      .collect(Collectors.toMap(Triple::getLeft, Triple::getRight));

    // Make sure there's a constraint for every relationship, fail if there isn't
    final Predicate<String> constraintExists = relName ->
      constraintsToValidateTo
        .stream()
        .map(Constraint::getForRelationship)
        .anyMatch(constraintRel -> Objects.equals(constraintRel, relName));

    Set<String> relationsNotUnderConstraint = relationsToTest
      .keySet()
      .stream()
      .filter(relName -> !constraintExists.test(relName))
      .collect(Collectors.toSet());
    if (!relationsNotUnderConstraint.isEmpty()) {
      errors.add(
        new EntityValidationError(
          400,
          getUuid(),
          getName(),
          relationsNotUnderConstraint,
          "Found relationships that aren't under constraint"
        )
      );
    }

    // Now we can check every constraint
    final Set<Constraint.ValidationReport> validationOutput = constraintsToValidateTo
      .stream()
      .map(constraint -> constraint.validateConstraint(entity))
      .collect(Collectors.toSet());

    // Get all the constraints that were violated
    Set<Constraint.ValidationReport> violatedConstraints = validationOutput
      .stream()
      .filter(report -> !report.isSuccess())
      .collect(Collectors.toSet());

    // Get the errors from the violated constraints
    errors = Stream.concat(
      errors.stream(),
      violatedConstraints
        .stream()
        .map(Constraint.ValidationReport::getError)
        .filter(Optional::isPresent)
        .map(Optional::get)
    ).collect(Collectors.toSet());

    /*
      Create a map of every relation name verified by the constraints to the UUIDs
      that were verified.
      Sometimes there may be multiple constraints for the same relation name so we
      need to take care not to overwrite the values belonging to such a relation name.
    */
    final Map<String, Set<String>> validatedRelationships = validationOutput
      .stream()
      .collect(
        Collectors.toMap(
          Constraint.ValidationReport::getForRelationship,
          report -> report.getValidatedUuids(),
          (left, right) -> Stream.of(left, right).flatMap(Collection::stream).collect(Collectors.toSet())
        )
      );

    // Get all the relations that weren't validated by any constraint
    Set<UUID> missedRelations = Maps.difference(relationsToTest, validatedRelationships)
      .entriesDiffering()
      .keySet()
      .stream()
      .map(key -> Sets.difference(relationsToTest.get(key), validatedRelationships.get(key)))
      .map(Sets.SetView::immutableCopy)
      .flatMap(Set::stream)
      .map(UUID::fromString)
      .collect(Collectors.toSet());

    if (!missedRelations.isEmpty()) {
      errors.add(
        new EntityValidationError(
          400,
          getUuid(),
          getName(),
          EntityValidationError.uuidSetToHashSet(missedRelations),
          "Found relationships that weren't validated by any constraint"
        )
      );
    }

    /*
      Given that any error state of validation errors results in an error added to
      the errors collection, it is safe to assume that when errors is empty, validation
      was successful.
    */
    return Pair.of(errors.isEmpty(), ImmutableSet.copyOf(errors));
  }

  public Set<Type> findCommonProtocols(Instance subject) {
    return typeRepository.getCommonProtocols(this.getUuid(), subject.getUuid());
  }

  public int getGlyphId() {
    return glyphId;
  }

  public String getHierarchyPath() {
    String path = typeRepository.getHierarchyPath(getUuid());
    if (path != null) {
      return path.toLowerCase();
    } else {
      return null;
    }
  }

  /**
   * Get the applicable outgoing constraints of this type.
   * @return The applicable outgoing constraints.
   */
  public Set<Constraint> getOutgoingConstraints() {
    return getApplicableConstraints(
      typeRepository.getInheritedOutgoingConstraints(getUuid()),
      getOwnConstraints(Direction.OUTGOING)
    );
  }

  /**
   * Get the applicable incoming constraints of this type.
   * @return The applicable incoming constraints.
   */
  public Set<Constraint> getIncomingConstraints() {
    return getApplicableConstraints(
      typeRepository.getInheritedIncomingConstraints(getUuid()),
      getOwnConstraints(Direction.INCOMING)
    );
  }

  public Type getIsa() {
    return persisted ? relatedOne("-[:ISA]->(b:Type)") : null;
  }

  public Type setIsa(Type isa) {
    // TODO: Entity, This can go, i think
    return this;
  }

  public Type setIsa(String isa) {
    // We can't assume our parentNode has been persisted to the database yet at this point.
    // If the parentNode doesn't exist in the database yet, we just save it's UUID to a
    // transient field for later use (at createNode time).
    // If it does exist, we create the relations and inherit constraints in setIsa(Type isa).

    Type parentNode = typeRepository.findByUuid(isa);
    if (parentNode != null) {
      setIsa(parentNode);
    } else {
      // This means our parent node hasn't been persisted to DB yet. Save UUID for later.
      setIsaUuid(isa);
    }

    return this;
  }

  public void setIsaUuid(String isaUuid) {
    this.isaUuid = isaUuid;
  }

  public Map<String, Object> getKv() {
    return kv;
  }

  public JsonSchemaBuilder getSchema() {
    JsonSchemaBuilder schema = null;
    try {
      String dataSchemaContent = mapper
        .readTree((String) getFiles().get("dataSchema"))
        .get("content")
        .asText();
      schema = new JsonSchemaBuilder((ObjectNode) mapper.readTree(dataSchemaContent));
    } catch (ProcessingException | IOException e) {
      if (Configuration.isTraceModeOn()) {
        e.printStackTrace();
      }
    }
    return schema;
  }

  public Set<Instrument> getInstruments() {
    return persisted ? related("<-[:PROVIDED_BY]-(b:Instrument)") : new HashSet<>();
  }

  public Type addImplementedProtocol(Type protocol) {
    if (protocol == null) {
      log.error("Provided argument to addImplementedProtocol cannot be null.");
      return this;
    }
    if (!protocol.isOfType("Protocol")) {
      log.error("Can't add non-protocol type " + protocol.getName() + " as implemented protocol.");
      return this;
    }
    if (!isOfType("Software")) {
      log.error("Can't add an implemented protocol to a non-software type.");
      return this;
    }

    Graph.query(
      "MATCH (a:Type {uuid: $uuid}), (b:Type {uuid: $protocol}) MERGE (a)-[:IMPLEMENTS]->(b)",
      ImmutableMap.of("uuid", getUuid(), "protocol", protocol.getUuid())
    );

    return this;
  }

  /**
   * Get the first identity set of a linked list of types.
   * @return An optional of the first identity set found, or an empty optional.
   */
  public Optional<Pair<String, Set<String>>> getFirstInheritedIdentity() {
    return getFirstInheritedIndentity(this);
  }

  private Optional<Pair<String, Set<String>>> getFirstInheritedIndentity(Type type) {
    if (type.identity != null) {
      return Optional.of(Pair.of(getName(), type.identity));
    } else if (getIsa() == null) {
      return Optional.empty();
    } else {
      return getFirstInheritedIndentity(type.getIsa());
    }
  }

  public boolean implementsProtocol(Type protocol) {
    return protocol != null && related("-[:IMPLEMENTS]->(b:Type)").contains(protocol);
  }

  public boolean isOfType(String type) {
    return typeRepository.isOfType(getUuid(), type) > 0;
  }

  /**
   * See {@link Entity#isAbstract}
   * @return Whether the type is abstract or not.
   */
  public boolean isAbstract() {
    return isAbstract;
  }

  public ObjectNode toJson() {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode json = mapper.createObjectNode();

    List<String> childIsaUuids = null;
    if (getChildIsaSet() != null) {
      getChildIsaSet().stream().map(Node::getUuid).collect(Collectors.toList());
    }

    json.put("name", getName());
    json.put("uuid", getUuid());
    json.put("glyphId", Integer.toHexString(glyphId));
    if (getIsa() != null) {
      json.put("isa", getIsa().getUuid());
    }
    json.put("desc", getDesc());
    json.put("checksum", getChecksum());
    try {
      if (getSchema() != null) {
        json.set("schema", getSchema().getAsObjectNode());
      }
      if (childIsaUuids != null) {
        json.put("children", mapper.writeValueAsString(childIsaUuids));
      }
    } catch (JsonProcessingException e) {
      log.error(e.getMessage());

      if (Configuration.isTraceModeOn()) {
        e.printStackTrace();
      }
    }

    json.put("_nodeType", getNodeType());

    return json;
  }
}
