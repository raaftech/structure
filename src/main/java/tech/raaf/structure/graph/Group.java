package tech.raaf.structure.graph;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import tech.raaf.structure.exceptions.JsonValidationException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

public final class Group extends Instance implements Groupable {

  // The members of a new group. A persisted group reads them from the graph.
  private Set<Groupable> members;

  public Group() {}

  /**
   * Construct a component based on a type and the data that represents it.
   * @param instanceData The data needed to instantiate the type.
   * @param type The type to instantiate.
   */
  public Group(ObjectNode instanceData, Type type) {
    ObjectNode instanceDataCopy = instanceData.deepCopy();
    if (type == null) {
      throw new IllegalArgumentException("Component constructor called with null as type.");
    }
    final List<String> errors = type.getSchema().validationErrors(instanceDataCopy);
    if (!errors.isEmpty()) {
      throw new JsonValidationException(
        "Cannot create component of type \"" + type.getName() + "\", JSON doesn't validate against it: " + errors
      );
    }
    final String name = instanceDataCopy.get("name").asString();
    final String desc = instanceDataCopy.get("desc").asString();

    this.name = name;
    this.desc = desc;
    this.instanceOf = type;
    this.instanceData = instanceData.deepCopy();
    setProperties(instanceDataCopy);
  }

  public Set<Groupable> getMembers() {
    if (persisted) {
      final Set<Groupable> result = new HashSet<>();
      this.<Node>related("<-[:MEMBER_OF]-(b)").forEach(n -> result.add((Groupable) n));
      return Collections.unmodifiableSet(result);
    }
    if (members == null) {
      members = new HashSet<>();
    }
    return members;
  }

  public Set<Group> getMemberOf() {
    return persisted ? related("-[:MEMBER_OF]->(b:Group)") : new HashSet<>();
  }

  // Add a member to a new group; save() writes the membership
  public void addMember(Groupable groupable) {
    getMembers().add(groupable);
  }

  @Override
  protected Entity toEntity() {
    final Entity entity = super.toEntity();
    if (!persisted && members != null && !members.isEmpty()) {
      entity.addRelations(
        "MEMBER_OF",
        Direction.INCOMING,
        members
          .stream()
          .map(g -> UUID.fromString(((Node) g).getUuid()))
          .collect(Collectors.toSet())
      );
    }
    return entity;
  }

  @Override
  protected Set<Instance> getNewPeers() {
    final Set<Instance> peers = new HashSet<>();
    if (!persisted && members != null) {
      members.forEach(g -> peers.add((Instance) g));
    }
    return peers;
  }

  // /**
  //  * Used to create a JSON representation of a single Instance that
  //  * can be used to as an argument for an instrument.
  //  * @return JSON representation that can be used as an argument for a instrument.
  //  */
  // public Set<JsonNode> toArguments() {
  //   Set<JsonNode> arguments = new HashSet<>();
  //
  //   arguments.add(toJson());
  //   BiConsumer<BiConsumer, Component> consumeArgsHelper = (f, node) -> {
  //     Set<Component> members = node.getMembers();
  //     if (members.size() > 0) {
  //       members.parallelStream().forEach(n -> f.accept(f, n));
  //     }
  //     arguments.add(node.toJson());
  //   };
  //
  //   // Lambda's can't refer to themselves using their variable name. That means consumeArgsHelper defined
  //   // above cannot refer to itself by name. This sucks, because it's a recursive function.
  //   // However, it can refer to itself if it's passed a reference to itself as an argument, so that's what's
  //   // happening in consumeArgs. After calling consumeArgsHelper below, consumeArgsHelper is bound to 'f' in
  //   // the lambda.
  //   Consumer<Component> consumeArgs = node -> consumeArgsHelper.accept(consumeArgsHelper, node);
  //
  //   getMembers().stream().forEach(consumeArgs);
  //
  //   return arguments;
  // }
  /**
   * Used to create a JSON representation of a single Instance that
   * can be used to as an argument for an instrument.
   * @return JSON representation that can be used as an argument for a instrument.
   */
  public Set<JsonNode> toArguments() {
    return getMembers()
      .stream()
      .flatMap(g -> Component.arguments((Instance) g).stream())
      .collect(Collectors.toSet());
  }
}
