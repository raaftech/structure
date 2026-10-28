package tech.raaf.structure.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.exceptions.JsonValidationException;
import tech.raaf.structure.graph.repositories.InstanceRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public final class Component extends Instance implements Groupable {

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

  // The results of this component, newest first; a null task gives all
  public List<Result> getResults(String task) {
    final Map<String, Object> params = new HashMap<>();
    params.put("uuid", getUuid());
    params.put("task", task);
    return NodeMapper.mapAll(
      Graph.query(
        "MATCH (:Component {uuid: $uuid})-[:HAS_RESULT]->(n:Result) " +
          "WHERE $task IS NULL OR n.`data-task` = $task " +
          "RETURN n ORDER BY n.created DESC, n.uuid DESC",
        params
      ),
      "n"
    );
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
  public Set<JsonNode> toArguments() {
    return getComponents()
      .stream()
      .flatMap(c -> arguments(c).stream())
      .collect(Collectors.toSet());
  }

  // The JSON of an instance and of its components, recursively
  static Set<JsonNode> arguments(Instance instance) {
    final Set<JsonNode> result = new HashSet<>();
    result.add(instance.toJson());
    if (instance instanceof Component component) {
      component.getComponents().forEach(c -> result.addAll(arguments(c)));
    }
    return result;
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
