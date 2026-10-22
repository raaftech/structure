package tech.raaf.structure.graph;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.exceptions.JsonValidationException;
import tech.raaf.structure.graph.repositories.InstanceRepository;

public class Component extends Instance implements Groupable {

  // The child components of a new component. A persisted component reads them from the graph.
  private Set<Component> components;

  private static final InstanceRepository instanceRepository = Context.getInstanceRepository();

  private Logger log = LoggerFactory.getLogger(Component.class);

  /**
   * No-arg constructor for NodeMapper
   */
  public Component() {}

  /**
   * Construct a component based on a type and the data that represents it.
   * @param instanceData The data needed to instantiate the type.
   * @param type The type to instantiate.
   */
  public Component(ObjectNode instanceData, Type type) {
    ObjectNode instanceDataCopy = instanceData.deepCopy();
    if (type == null) {
      throw new IllegalArgumentException("Component constructor called with null as type.");
    }
    if (!type.getSchema().validateJson(instanceDataCopy)) {
      throw new JsonValidationException(
        "Cannot create component of type \"" + type.getName() + "\", JSON doesn't validate against it."
      );
    }
    final String name = instanceDataCopy.get("name").asText();
    final String desc = instanceDataCopy.get("desc").asText();

    this.name = name;
    this.desc = desc;
    this.instanceOf = type;
    this.instanceData = instanceData.deepCopy();
    setProperties(instanceDataCopy);
  }

  /**
   * See Component(ObjectNode instanceData, Type type).
   * @param instanceData The data needed to instantiate the type.
   * @param typeUuid The UUID of the type to instantiate.
   */
  public Component(ObjectNode instanceData, String typeUuid) {
    this(instanceData, TYPE_REPOSITORY.findByUuid(typeUuid));
  }

  public Set<Component> getComponents() {
    if (persisted) {
      return related("-[:HAS]->(b:Component)");
    }
    if (components == null) {
      components = new HashSet<>();
    }
    return components;
  }

  // The components that have this component
  public Set<Component> getInComponent() {
    return persisted ? related("<-[:HAS]-(b:Component)") : new HashSet<>();
  }

  /**
   * Get all the startOfResultChain belonging to this Instance while filtering on the task ID.
   * @param task The ID of the task.
   * @return All the startOfResultChain belonging to a specific task.
   */
  public Set<Result> getResults(String task) {
    return getStartOfResultChain()
      .getThisAndNewer()
      .stream()
      .filter(r -> r.getTaskId().equals(task))
      .collect(Collectors.toSet());
  }

  public Set<Group> getMemberOf() {
    return persisted ? related("-[:MEMBER_OF]->(b:Group)") : new HashSet<>();
  }

  /**
   * Add another component as child of this component.
   * @param component The component to add as a child component.
   * @return this
   */
  public Component addComponent(Component component) {
    getComponents().add(component);

    return this;
  }

  @Override
  protected Entity toEntity() {
    final Entity entity = super.toEntity();
    if (!persisted && components != null && !components.isEmpty()) {
      entity.addRelations(
        "HAS",
        Direction.OUTGOING,
        components
          .stream()
          .map(c -> UUID.fromString(c.getUuid()))
          .collect(Collectors.toSet())
      );
    }
    return entity;
  }

  @Override
  protected Set<Instance> getNewPeers() {
    return !persisted && components != null ? new HashSet<>(components) : new HashSet<>();
  }

  /**
   * Add another component as child of this component.
   * @param componentUuid The UUID of the component to add as a child component.
   * @return this
   */
  public void addComponent(String componentUuid) {
    Instance component;

    if (componentUuid != null) {
      component = instanceRepository.findByUuid(componentUuid);
      if (component != null) {
        addComponent((Component) component);
      }
    }
  }

  /**
   * Used to create a JSON representation of a single Instance that
   * can be used to as an argument for an instrument.
   * @return JSON representation that can be used as an argument for a instrument.
   */
  @SuppressWarnings("unchecked")
  public Set<JsonNode> toArguments() {
    Set<JsonNode> arguments = new HashSet<>();

    arguments.add(super.toJson());
    BiFunction<BiFunction, Instance, Set<JsonNode>> consumeArgsHelper = (f, node) -> {
      final Set<Component> components;
      if (node instanceof Component) {
        components = ((Component) node).getComponents();
      } else {
        components = Collections.emptySet();
      }
      final Set<JsonNode> childSet;
      if (!components.isEmpty()) {
        childSet = components
          .stream()
          .flatMap(instance -> ((Set<JsonNode>) f.apply(f, instance)).stream())
          .collect(Collectors.toSet());
        // Stream.concat(components.stream(), configurations.stream()).forEach(n -> f.accept(f, n));
      } else {
        childSet = Collections.emptySet();
      }
      return Sets.union(childSet, ImmutableSet.of(node.toJson()));
    };

    // Lambda's can't refer to themselves using their variable name. That means consumeArgsHelper defined
    // above cannot refer to itself by name. This sucks, because it's a recursive function.
    // However, it can refer to itself if it's passed a reference to itself as an argument, so that's what's
    // happening in consumeArgs. After calling consumeArgsHelper below, consumeArgsHelper is bound to 'f' in the lambda.
    Function<Instance, Set<JsonNode>> consumeArgs = node -> consumeArgsHelper.apply(consumeArgsHelper, node);

    return getComponents().stream().map(consumeArgs).flatMap(Collection::stream).collect(Collectors.toSet());
  }

  public ObjectNode deepToJson(int dimension) {
    if (dimension <= 0) {
      return null;
    }

    ObjectMapper mapper = new ObjectMapper();
    ObjectNode result = mapper.createObjectNode();
    ObjectNode kvJson = result.putObject("data");
    data.forEach((key, val) -> kvJson.put(key, val.toString()));
    ObjectNode componentsJson = mapper.createObjectNode();
    if (dimension > 1) {
      getComponents().forEach(component -> {
        componentsJson.set(component.getName(), component.deepToJson(dimension - 1));
      });
    }

    result.put("name", getName());
    result.put("uuid", getUuid());
    result.put("desc", getDesc());
    result.put("instanceof", getInstanceOf().getUuid());
    result.put("glyphId", Integer.toHexString(getInstanceOf().getGlyphId()));
    if (componentsJson.size() > 0) {
      result.set("components", componentsJson);
    }
    final Set<Component> inComponent = getInComponent();
    if (!inComponent.isEmpty()) {
      result.put("incomponent", inComponent.toArray().toString());
    }

    result.put("_nodeType", getNodeType());

    return result;
  }

  public ObjectNode toJson() {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode result = mapper.createObjectNode();
    // ObjectNode kvJson = result.putObject("data");
    data.forEach((key, val) -> result.put(key, val.toString()));
    ArrayNode componentsJson = mapper.createArrayNode();
    getComponents().forEach(component -> {
      ObjectNode componentNameUuid = mapper.createObjectNode();
      componentNameUuid.put("name", component.getName());
      componentNameUuid.put("uuid", component.getUuid());
      componentsJson.add(componentNameUuid);
    });

    result.put("name", getName());
    result.put("uuid", getUuid());
    result.put("desc", getDesc());
    result.put("instanceof", getInstanceOf().getUuid());
    result.put("glyphId", Integer.toHexString(getInstanceOf().getGlyphId()));
    if (componentsJson.size() > 0) {
      result.set("components", componentsJson);
    }
    final Set<Component> inComponent = getInComponent();
    if (!inComponent.isEmpty()) {
      result.put("incomponent", inComponent.toArray().toString());
    }

    result.put("_nodeType", getNodeType());
    result.put("_hierarchyPath", getInstanceOf().getHierarchyPath());

    return result;
  }
}
