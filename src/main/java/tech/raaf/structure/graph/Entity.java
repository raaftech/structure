package tech.raaf.structure.graph;

import static io.vertx.core.json.Json.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.vertx.core.json.JsonObject;
import java.net.URI;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.bus.GenericActor;
import tech.raaf.structure.exceptions.IllegalEntityType;
import tech.raaf.structure.exceptions.ResultBuilderException;
import tech.raaf.structure.exceptions.TypeUnknownException;
import tech.raaf.structure.graph.repositories.NodeRepository;
import tech.raaf.structure.graph.repositories.TypeRepository;
import tech.raaf.structure.helper.JsonHelper;
import tech.raaf.structure.helper.Triple;

/**
 * A new root entity node with heavily simplified and structured construction.
 *
 * @author Rubin Simons <rubin@raaf.tech>
 * @since 2016-10-19
 */
@JsonDeserialize(using = EntityDeserializer.class)
public class Entity {

  /* Logging object. */
  private static final Logger log = LoggerFactory.getLogger(Entity.class);

  /* Entity repository. Used for querying the graph. */
  private NodeRepository nodeRepository = Context.getNodeRepository();
  private TypeRepository typeRepository = Context.getTypeRepository();
  /* Main key value/pairs: bundle, type, name, desc, uuid. */
  private String bundle;

  private String name;

  private String desc;

  private String uuid;

  /**
   * "Type" entities can be made abstract. This means no instance of them can be created.
   */
  private boolean isAbstract;

  /**
   * See {@link Type#identity}
   */
  private String[] identity;

  /* Generated key/value pairs: checksum, path. */
  private String checksum;
  private String path;

  /* Dynamic property containers for inline data and files. */
  private Map<String, Object> data = new HashMap<>();
  private Map<String, Object> file = new HashMap<>();

  /* A holder for relations to be written by persistRelations().
   * From left to right, the triple should contain the relationship name,
   * the direction (incoming or outgoing) and the set of UUIDs
   * of the nodes that should be related by this relationship. */
  private Set<Triple<String, String, Set<String>>> relations;

  /* Constraints are temporarily stored in this field. The field is a map from a string
   * to a list. The string of that map is the relation name that is constrainted. The list
   * contains tuples of Cardinality + UUID.
   */
  private Map<String, List<Pair<Constraint.Cardinality, UUID>>> constraints;

  /*
    The next two sets are used to see if a vertice incident to an edge can be
    queryable by name. This can only be guaranteed for types, so we constraint it
    to just types.
  */
  // Incoming relations that origin from a type
  private static final Set<String> incomingFromType = ImmutableSet.of("ISA", "CONSTRAINTS", "IMPLEMENTS");
  // Outgoing relations that point to a typ
  private static final Set<String> outgoingToType = ImmutableSet.of(
    "ISA",
    "CONSTRAINTS",
    "IMPLEMENTS",
    "PROVIDED_BY",
    "INSTANCE_OF",
    "APPLIES_TO",
    "GIVES_CAPABILITY"
  );

  public Entity() {}

  /* Builders */
  public static class ResultBuilder {

    // Required
    private String process;
    private String taskId;
    // Optional
    private String stdout;
    private String stderr;
    private Integer returnCode;
    private String user;
    private Result.State state;
    private String error;

    public ResultBuilder() {}

    public ResultBuilder process(String process) {
      this.process = process;
      return this;
    }

    public ResultBuilder taskId(String taskId) {
      this.taskId = taskId;
      return this;
    }

    // Optional fields
    public ResultBuilder stdout(String stdout) {
      this.stdout = stdout;
      return this;
    }

    public ResultBuilder stderr(String stderr) {
      this.stderr = stderr;
      return this;
    }

    public ResultBuilder returnCode(int returnCode) {
      this.returnCode = returnCode;
      return this;
    }

    public ResultBuilder user(String user) {
      this.user = user;
      return this;
    }

    public ResultBuilder state(Result.State state) {
      this.state = state;
      return this;
    }

    public ResultBuilder error(String error) {
      this.error = error;
      return this;
    }

    public Result buildAndStore() throws JsonProcessingException {
      if (process == null || taskId == null) {
        log.error("Error while building Result");
        throw new ResultBuilderException("Not all required parameters were set.");
      }
      Type resultType = Context.getTypeRepository().findOneByName("Result");
      Map<String, Object> data = new HashMap<>();
      data.put(Result.NAME, "Result");
      data.put(Result.DESC, "A result.");
      data.put(Result.PROCESS, process);
      data.put(Result.TASKID, taskId);
      data.put(Result.DATECREATED, System.currentTimeMillis());

      if (stdout != null) {
        data.put(Result.STDOUT, stdout);
      }
      if (stderr != null) {
        data.put(Result.STDERR, stderr);
      }
      if (returnCode != null) {
        data.put(Result.RETURNCODE, returnCode);
      }
      if (user != null) {
        data.put(Result.USER, user);
      }
      if (error != null) {
        data.put(Result.ERROR, error);
      }
      if (state != null) {
        data.put(Result.STATECODE, state.code);
        data.put(Result.STATEINTERPRETATION, state.interpretation);
        data.put(Result.STATEDESCRIPTION, state.description);
      }

      Entity entity = new Entity();
      entity.setUuid(UUID.randomUUID().toString());
      entity.setName("Result");
      entity.setDesc("A result.");
      entity.addRelations("INSTANCE_OF", Direction.OUTGOING, ImmutableSet.of(UUID.fromString(resultType.getUuid())));
      entity.setData(data);

      Graph.transaction(entity::toGraph);

      return Context.getResultRepository().findByUuid(entity.getUuid());
    }
  }

  /* Getters. */
  public String getBundle() {
    return bundle;
  }

  public String getName() {
    return name;
  }

  public String getDesc() {
    return desc;
  }

  public String getUuid() {
    return uuid;
  }

  public String getChecksum() {
    return checksum;
  }

  public String getPath() {
    return path;
  }

  public Map<String, Object> getData() {
    return data;
  }

  public Map<String, Object> getFiles() {
    return file;
  }

  public Map<String, Object> getFile() {
    return file;
  }

  public String[] getIdentity() {
    return identity;
  }

  /**
   * Get the triples describing this entity's relations.
   *
   * @return A triple containing left to right: relation name, direction, related UUIDs.
   */
  public Set<Triple<String, String, Set<String>>> getRelations() {
    return relations;
  }

  public Optional<Map<String, List<Pair<Constraint.Cardinality, UUID>>>> getConstraints() {
    return constraints == null ? Optional.empty() : Optional.of(constraints);
  }

  /**
   * Returns whether this entity is abstract. This is only used on "Type" entities.
   *
   * @return
   */
  public boolean isAbstract() {
    return isAbstract;
  }

  /* Setters. */
  public void setBundle(String bundle) {
    this.bundle = bundle;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setDesc(String desc) {
    this.desc = desc;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public void setChecksum(String checksum) {
    this.checksum = checksum;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public void setData(Map<String, Object> map) {
    this.data = new HashMap<>(map);
  }

  public void setFile(Map<String, Object> map) {
    this.file = new HashMap<>(map);
  }

  public void setRelations(Set<Triple<String, String, Set<String>>> relations) {
    this.relations = relations;
  }

  /**
   * Add a relation to or from a set of UUIDs to our relationships object. If the relationship
   * already exists, it checks if the direction is the same and adds the UUIDs to the set. If the direction
   * doesn't match, it throws an exception. If the relationship didn't exist yet, it'll just add it to the relations
   * object.
   *
   * @param relationName The relationship name.
   * @param direction    The direction of the relationship.
   * @param uuids        The UUIDs to add to the relationship.
   * @return Did the relations object change?
   */
  public boolean addRelations(String relationName, Direction direction, Set<UUID> uuids) {
    if (relations == null) {
      relations = new HashSet<>();
    }
    final Set<Triple<String, String, Set<String>>> relevantTriples = relations
      .stream()
      .filter(t -> t.getLeft().equals(relationName))
      .collect(Collectors.toSet());

    if (relevantTriples.size() > 1) {
      throw new IllegalStateException(
        "Multiple triples found for relationship \"" + relationName + "\" on node \"" + uuid + "\"."
      );
    }
    Set<String> uuidsAsStrings = uuids.stream().map(UUID::toString).collect(Collectors.toSet());
    final boolean changed;
    if (relevantTriples.isEmpty()) {
      relations.add(Triple.of(relationName, direction.toString().toLowerCase(), uuidsAsStrings));
      changed = true;
    } else {
      // relevantTriples contains just a single triple
      final Triple<String, String, Set<String>> relevantTriple = relevantTriples.stream().findFirst().get();
      changed = relevantTriple.getRight().addAll(uuidsAsStrings);
    }
    return changed;
  }

  public void setIdentity(String[] identity) {
    this.identity = identity;
  }

  public void addConstraint(String relName, Constraint.Cardinality cardinality, UUID to) {
    if (constraints == null) {
      constraints = new HashMap<>();
    }

    final List<Pair<Constraint.Cardinality, UUID>> constraintList;
    if (constraints.containsKey(relName)) {
      constraintList = constraints.get(relName);
    } else {
      constraintList = new ArrayList<>();
      constraints.put(relName, constraintList);
    }

    constraintList.add(Pair.of(cardinality, to));
  }

  /**
   * An entity can either be of the "type" or "instance" variety.
   *
   * @return True if this entity is of the "instance" variety.
   */
  public boolean isInstance() {
    if (getRelations() == null) {
      throw new RuntimeException(
        "Cannot determine type of entity " + "when entity (\"" + getUuid() + "\" has no relations"
      );
    }
    return getRelations()
      .stream()
      .anyMatch(triple -> "INSTANCE_OF".equals(triple.getLeft()));
  }

  /**
   * Sets whether an entity is abstract. Can only be used on "Type" entities.
   *
   * @param isAbstract True if this is an abstract entity.
   * @throws IllegalArgumentException Thrown when called on a non "Type" entity.
   */
  public void setAbstract(boolean isAbstract) {
    if (isInstance()) {
      throw new IllegalArgumentException(createErrorMessage(400, "", "Only a \"Type\" entity can be made abstract."));
    } else {
      this.isAbstract = isAbstract;
    }
  }

  // A uuid identifies an entity
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;

    if (uuid == null || !(other instanceof Entity)) return false;

    return uuid.equals(((Entity) other).uuid);
  }

  @Override
  public int hashCode() {
    return uuid == null ? System.identityHashCode(this) : uuid.hashCode();
  }

  public JsonNode toJson() {
    JsonNode entityJson = null;
    return entityJson;
  }

  private Pair<Boolean, Set<EntityValidationError>> validateAgainstConstraints() {
    Optional<Type> instanceOf = getInstanceOf();

    if (!instanceOf.isPresent()) {
      throw new TypeUnknownException(createErrorMessage(404, "", "Type for entity \"" + uuid + "\" doesn't exist"));
    }

    return instanceOf.get().validateEntityToConstraints(this);
  }

  /**
   * Get the the identity properties for the type of an instance, and the name of the
   * type these identity properties are defined on.
   *
   * @return Optionally the identity properties that must hold for an instance of a particular type,
   * and the type name those properties are defined on.
   */
  private Optional<Pair<String, Set<String>>> getIdentityProperties() {
    final Optional<Type> maybeInstanceOf = getInstanceOf();
    if (!maybeInstanceOf.isPresent()) {
      throw new TypeUnknownException(createErrorMessage(404, "", "Type for entity \"" + uuid + "\" doesn't exist"));
    }

    Type instanceOf = maybeInstanceOf.get();
    if (identity == null) {
      if (typeRepository.inheritsIdentity(instanceOf.getUuid())) {
        /*
          Retrieve first inherited identity. We already know the optional won't be empty
          because of the typeRepository#inheritsIdentity query.
        */
        return instanceOf.getFirstInheritedIdentity();
      } else {
        // No identity to adhere to, return true
        return Optional.empty();
      }
    } else {
      // Use identity field
      return Optional.of(Pair.of(instanceOf.getName(), Arrays.stream(identity).collect(Collectors.toSet())));
    }
  }

  /**
   * Given a label and some identity properties, find duplicates.
   * We do this by getting all available values for the identity properties, and returning
   * node UUIDs of nodes with the given label and the given values for the properties in the
   * graph.
   *
   * @param label              The label to use in the query.
   * @param identityProperties The properties that need to be used for the query.
   * @return A set of UUIDs of nodes that have the given label and the same values
   * for the given properties as this entity.
   */
  private Set<String> findDuplicates(String label, Set<String> identityProperties) {
    final Map<String, Object> params = new HashMap<>();
    final List<String> conditions = new ArrayList<>();
    // The keys go into the query text, the values are parameters
    identityProperties
      .stream()
      .filter(data::containsKey)
      .forEach(id -> {
        final String param = "p" + params.size();
        conditions.add("n." + Graph.escape("data-" + id) + " = $" + param);
        params.put(param, toPropertyValue(data.get(id)));
      });
    // name is not part of the data map, but we can identify on it
    if (identityProperties.contains("name")) {
      final String param = "p" + params.size();
      conditions.add("n.name = $" + param);
      params.put(param, getName());
    }

    final String query =
      "MATCH (n:" +
      Graph.escape("type:" + label) +
      ") " +
      (conditions.isEmpty() ? "" : "WHERE " + String.join(" AND ", conditions) + " ") +
      "RETURN n.uuid AS uuid";
    final Set<String> duplicates = new HashSet<>();
    Graph.query(query, params).forEach(row -> duplicates.add((String) row.get("uuid")));
    return duplicates;
  }

  private String createErrorMessage(int code, String conflictUuid, String message) {
    final JsonObject msg = new JsonObject()
      .put("code", code)
      .put("uuid", getUuid())
      .put("typeName", getName())
      .put("conflict", conflictUuid)
      .put("message", message);
    return msg.toString();
  }

  public void toGraph() {
    final Optional<List<String>> maybeClassLabels;
    if (isInstance()) {
      final Optional<Type> maybeInstanceOf = getInstanceOf();
      if (!maybeInstanceOf.isPresent()) {
        throw new IllegalEntityType(createErrorMessage(404, "", "instance-of type of this instance was not found"));
      }
      final Type instanceOf = maybeInstanceOf.get();
      final Optional<String> maybeDomainClass = getDomainClassForPath(instanceOf.getHierarchyPath());

      if (!maybeDomainClass.isPresent()) {
        throw new IllegalEntityType(createErrorMessage(424, "", "No domain class found"));
      }
      maybeClassLabels = getClassLabels(maybeDomainClass.get());

      // See if the type we try to instantiate is abstract
      if (instanceOf.isAbstract()) {
        throw new IllegalEntityType(
          createErrorMessage(422, instanceOf.getUuid(), "Cannot instantiate based on an abstract type")
        );
      }

      // See if this instance validates against its type's constraints
      final Pair<Boolean, Set<EntityValidationError>> validation = validateAgainstConstraints();
      if (!validation.getLeft()) {
        throw new IllegalEntityType(validation.getRight().toString());
      }

      // Now check if this instance is unique per its type's identity array
      Optional<Pair<String, Set<String>>> maybeLabelAndIdentityProperties = getIdentityProperties();
      if (maybeLabelAndIdentityProperties.isPresent()) {
        final String label = maybeLabelAndIdentityProperties.get().getLeft();
        final Set<String> identityProperties = maybeLabelAndIdentityProperties.get().getRight();
        Set<String> duplicates = findDuplicates(label, identityProperties);
        if (duplicates.size() > 0) {
          IllegalEntityType e = new IllegalEntityType(
            createErrorMessage(409, duplicates.toArray()[0].toString(), "Duplicate based on " + identityProperties)
          );

          // The "duplicate" might be the same as this (in case of an update)
          if (duplicates.size() == 1) {
            // Make sure it isn't before throwing an exception
            if (!duplicates.toArray()[0].equals(getUuid())) {
              throw e;
            }
            // Duplicates > 2, so at least one won't be the same as this
          } else {
            throw e;
          }
        }
      }

      final String schemaUriPattern = "/schemas/%s.schema.json";
      final URI schemaUri = Context.getSchemaNamespace().resolve(
        String.format(schemaUriPattern, instanceOf.getName().toLowerCase())
      );
      final Map<String, Object> data = new HashMap(getData());
      data.put("name", getName());
      data.put("desc", getDesc());
      final boolean validated = JsonHelper.validateToSchema(schemaUri, mapper.valueToTree(data));
      if (!validated) {
        throw new IllegalEntityType(createErrorMessage(400, "", "Failed to validate against type schema"));
      }
    } else {
      maybeClassLabels = getClassLabels("Type");
      // This shouldn't happen
      if (!maybeClassLabels.isPresent()) {
        throw new IllegalEntityType(createErrorMessage(500, "", "Unable to retrieve labels for entity"));
      }
    }

    write(maybeClassLabels.get());
  }

  // Write this entity without validation; toGraph validates first
  void write(List<String> classLabels) {
    final Set<String> oldLabels = persist();
    persistRelations();
    persistLabels(oldLabels, classLabels);
  }

  // Property values must be primitives, strings or lists
  private static Object toPropertyValue(Object value) {
    return value instanceof JsonNode ? value.toString() : value;
  }

  /**
   * Write the properties of this entity to the node. Remove the
   * properties that the entity no longer has.
   *
   * @return The labels of the node before the write.
   */
  @SuppressWarnings("unchecked")
  private Set<String> persist() {
    final Map<String, Object> properties = new HashMap<>();
    data.forEach((key, value) -> properties.put("data-" + key, toPropertyValue(value)));
    file.forEach((key, value) -> properties.put("file-" + key, value));
    if (bundle != null) {
      properties.put("bundle", bundle);
    }
    if (name != null) {
      properties.put("name", name);
    }
    if (desc != null) {
      properties.put("desc", desc);
    }
    if (uuid != null) {
      properties.put("uuid", uuid);
    }
    if (checksum != null) {
      properties.put("checksum", checksum);
    }
    if (path != null) {
      properties.put("path", path);
    }
    if (identity != null) {
      properties.put("identity", Arrays.asList(identity));
    }
    if (!isInstance()) {
      properties.put("isAbstract", isAbstract);
      properties.put("glyphId", 0);
    }

    final Map<String, Object> params = new HashMap<>();
    params.put("uuid", uuid);
    final Set<String> oldLabels = new HashSet<>();
    for (Map<String, Object> row : Graph.query(
      "MATCH (n:Node {uuid: $uuid}) RETURN labels(n) AS labels, keys(n) AS keys",
      params
    )) {
      oldLabels.addAll((List<String>) row.get("labels"));
      // A null value removes the property
      for (String key : (List<String>) row.get("keys")) {
        if (!properties.containsKey(key) && !key.equals("created") && !key.startsWith("kv-")) {
          properties.put(key, null);
        }
      }
    }

    params.put("props", properties);
    Graph.query("MERGE (n:Node {uuid: $uuid}) " + "ON CREATE SET n.created = timestamp() " + "SET n += $props", params);
    return oldLabels;
  }

  /**
   * Given a hierarchical path for a type from the type tree returns the domain class.
   *
   * @param path The hierarchical path for a type in the type tree.
   * @return Optionally the domain class name.
   */
  private static Optional<String> getDomainClassForPath(String path) {
    // A map containing the domain class belonging to a particular type hierarchical path
    Map<String, String> domainClassPerPath = ImmutableMap.<String, String>builder()
      .put("type.instrument.controller", "Controller")
      .put("type.instrument.marker", "Marker")
      .put("type.instrument.parser", "Parser")
      .put("type.component", "Component")
      .put("type.profile", "Profile")
      .put("type.result", "Result")
      .put("type.group", "Group")
      .build();

    return domainClassPerPath
      .entrySet()
      .stream()
      .filter(entry -> path.startsWith(entry.getKey()))
      .map(Map.Entry::getValue)
      .findFirst();
  }

  /**
   * Given a name of a domain class that can be mapped to the graph, return the labels
   * that NodeMapper uses to map nodes onto objects of that class.
   *
   * @param nodeEntityName The name of the SDN mapped domain class.
   * @return Optionally a list of class labels.
   */
  static Optional<List<String>> getClassLabels(String nodeEntityName) {
    // A map containing the labels needed per nodeEntityName
    Map<String, List<String>> labelsPerType = ImmutableMap.<String, List<String>>builder()
      .put("Controller", ImmutableList.of("Controller", "Instrument", "Instance", "Node"))
      .put("Marker", ImmutableList.of("Marker", "Instrument", "Instance", "Node"))
      .put("Parser", ImmutableList.of("Parser", "Instrument", "Instance", "Node"))
      .put("Component", ImmutableList.of("Component", "Instance", "Node"))
      .put("Profile", ImmutableList.of("Profile", "Instance", "Node"))
      .put("Result", ImmutableList.of("Result", "Instance", "Node"))
      .put("Group", ImmutableList.of("Group", "Instance", "Node"))
      .put("Type", ImmutableList.of("Type", "Node"))
      .build();

    return labelsPerType.containsKey(nodeEntityName)
      ? Optional.of(labelsPerType.get(nodeEntityName))
      : Optional.empty();
  }

  /**
   * Replace the labels of the node with the class labels and the
   * "type:" labels of the ISA path of its type.
   *
   * @param oldLabels   The labels of the node before the write.
   * @param classLabels The class labels of this entity.
   */
  private void persistLabels(Set<String> oldLabels, List<String> classLabels) {
    final String typeUuid;
    if (isInstance()) {
      Optional<Type> maybeType = getInstanceOf();
      if (!maybeType.isPresent()) {
        throw new TypeUnknownException(createErrorMessage(404, "", "Type for entity \"" + uuid + "\" doesn't exist"));
      }
      typeUuid = maybeType.get().getUuid();
    } else {
      typeUuid = uuid;
    }
    final List<String> labels = new ArrayList<>(classLabels);
    nodeRepository.getTypeHierarchyForTypeUuid(typeUuid).forEach(name -> labels.add("type:" + name));

    final String remove = oldLabels
      .stream()
      .filter(label -> !labels.contains(label))
      .map(Graph::escape)
      .collect(Collectors.joining(":"));
    final String set = labels.stream().map(Graph::escape).collect(Collectors.joining(":"));
    Graph.query(
      "MATCH (n:Node {uuid: $uuid}) " + (remove.isEmpty() ? "" : "REMOVE n:" + remove + " ") + "SET n:" + set,
      ImmutableMap.of("uuid", uuid)
    );
  }

  /**
   * Called on instance entities to get the Type object the entity is an instance of.
   *
   * @return The found type.
   */
  private Optional<Type> getInstanceOf() {
    if (!isInstance()) {
      throw new IllegalArgumentException("Cannot get the type of a type entity.");
    }
    Optional<String> instanceOfUuid = getRelations()
      .stream()
      .filter(triple -> "INSTANCE_OF".equals(triple.getLeft()))
      .map(Triple::getRight)
      .flatMap(Set::stream)
      .findFirst();
    if (!instanceOfUuid.isPresent()) {
      throw new IllegalStateException("Instance " + getUuid() + " has no INSTANCE_OF relationship");
    }

    Type instanceOf = typeRepository.findByUuid(instanceOfUuid.get());
    if (instanceOf == null) {
      log.error("No type with UUID " + instanceOfUuid.get() + " exists in the database");
      return Optional.empty();
    } else {
      return Optional.of(instanceOf);
    }
  }

  private void persistRelations() {
    if (relations == null) {
      log.error(
        "Failed to persist relations for entity \"{}\". Are you sure the entity is in a persisted state?",
        name
      );
      return;
    }

    // Returns whether a relationship with a given direction implies the the entity can be queried by name
    BiPredicate<String, String> isQueryableByName = (direction, relName) ->
      ("outgoing".equals(direction) ? outgoingToType : incomingFromType).contains(relName);

    // Check that all peers of all relations exist
    final boolean peersExist = relations
      .stream()
      .flatMap(triple -> triple.<String>flatten())
      .allMatch(triple ->
        doesPeerExist(triple.getRight(), isQueryableByName.test(triple.getMiddle(), triple.getLeft()))
      );
    if (!peersExist) {
      log.error("One of the peers that entity \"{}\" would be related to, does not exist in the graph", name);
      return;
    }

    relations.forEach(relation -> {
      final String rname = relation.getLeft();
      final boolean outgoing = "outgoing".equals(relation.getMiddle());
      /*
        When a relation points towards a type it is possible to query that
        type by name as well as by uuid, which is the default. This is because
        types have a unique constraint on their names.
      */
      final boolean queryableByName = isQueryableByName.test(relation.getMiddle(), rname);
      final String query =
        "MATCH (a:Node {uuid: $from}), (b:Node {uuid: $to}) " + "MERGE (a)-[:" + Graph.escape(rname) + "]->(b)";

      relation.getRight().forEach(identifier -> {
        final String peer =
          queryableByName && !GenericActor.isUuid(identifier)
            ? typeRepository.findOneByName(identifier).getUuid()
            : identifier;
        log.debug("Merging {} \"{}\" relation between \"{}\" and \"{}\"", relation.getMiddle(), rname, uuid, peer);
        Graph.query(query, ImmutableMap.of("from", outgoing ? uuid : peer, "to", outgoing ? peer : uuid));
      });
    });
  }

  private boolean doesPeerExist(String peerIdentifier, boolean queryableByName) {
    if (GenericActor.isUuid(peerIdentifier)) {
      final String exists = "MATCH (n:Node {uuid: $uuid}) RETURN count(n) AS count";
      return (
        ((Number) Graph.query(exists, ImmutableMap.of("uuid", peerIdentifier)).get(0).get("count")).longValue() > 0
      );
    } else if (queryableByName) {
      return typeRepository.findOneByName(peerIdentifier) != null;
    } else {
      return false;
    }
  }
}
