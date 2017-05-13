package com.raaftech.structure.graph;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.exceptions.JsonValidationException;
import com.raaftech.structure.graph.repositories.InstanceRepository;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Component extends Instance {

    @RelatedTo(
            type="HAS",
            direction = Direction.OUTGOING,
            elementClass = Component.class,
            enforceTargetType = true
    )
    private Set<Component> components;

    @RelatedTo(
            type="HAS",
            direction = Direction.INCOMING,
            elementClass = Component.class,
            enforceTargetType = true
    )
    @Fetch
    private Component inComponent;

    @RelatedTo(
            type="HAS",
            direction = Direction.OUTGOING,
            elementClass = Configuration.class,
            enforceTargetType = true
    )
    @Fetch
    Set<Configuration> configurations;

    @Transient
    private static final InstanceRepository instanceRepository = Context.getInstanceRepository();

    @Transient
    private Logger log = LoggerFactory.getLogger(Component.class);

    /**
     * No-arg constructor for Spring
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
            throw new JsonValidationException("Cannot create component of type \"" + type.getName()
                    + "\", JSON doesn't validate against it.");
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

    private Instance getFirstComponent() {
        if (components == null) {
            throw new IllegalArgumentException("Components set is empty.");
        }

        return components.iterator().next();
    }

    public Set<Component> getComponents() {
        if (components == null || components.isEmpty() || getFirstComponent().getName() == null) {
            Transaction tx = Context.getGraphDatabase().beginTx();
            Context.getNeo4jTemplate().fetch(components);
            tx.close();
        }
        return components;
    }

    public Set<Configuration> getConfigurations() {
        if (configurations == null || configurations.isEmpty()) {
            Transaction tx = Context.getGraphDatabase().beginTx();
            Context.getNeo4jTemplate().fetch(components);
            tx.close();
        }
        return configurations;
    }

    /**
     * Get all the startOfResultChain belonging to this Instance while filtering on the task ID.
     * @param task The ID of the task.
     * @return All the startOfResultChain belonging to a specific task.
     */
    public Set<Result> getResults(String task) {

        return getStartOfResultChain().getThisAndNewer().stream()
                .filter(r -> r.getTaskId().equals(task))
                .collect(Collectors.toSet());
    }

    /**
     * Add another component as child of this component.
     * @param component The component to add as a child component.
     * @return this
     */
    public Component addComponent(Component component) {
        if (components == null) {
            components = new HashSet<>();
        }
        components.add(component);

        return this;
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
                addComponent((Component)component);
            }
        }
    }

    /**
     * Used to create a JSON representation of a single Instance that
     * can be used to as an argument for an instrument.
     * @return JSON representation that can be used as an argument for a instrument.
     */
    public Set<JsonNode> toArguments() {
        Set<JsonNode> arguments = new HashSet<>();

        arguments.add(super.toJson());
        BiFunction<BiFunction, Instance, Set<JsonNode>> consumeArgsHelper = (f, node) -> {
            final Set<Component> components;
            final Set<Configuration> configurations;
            if (node instanceof Component) {
                components = ((Component)node).getComponents();
                configurations = ((Component)node).getConfigurations();
            } else {
                components = Collections.emptySet();
                configurations = Collections.emptySet();
            }
            final Set<JsonNode> childSet;
            if (!components.isEmpty() || !configurations.isEmpty()) {
                childSet = Stream.concat(components.stream(), configurations.stream())
                        .flatMap(instance -> ((Set<JsonNode>)f.apply(f, instance)).stream())
                        .collect(Collectors.toSet());
//                Stream.concat(components.stream(), configurations.stream()).forEach(n -> f.accept(f, n));
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

        return Sets.union(getComponents().stream().map(consumeArgs).flatMap(Collection::stream).collect(Collectors.toSet()),
                getConfigurations().stream().map(consumeArgs).flatMap(Collection::stream).collect(Collectors.toSet()));
    }

    public ObjectNode deepToJson(int dimension) {
        if (dimension <= 0) {
            return null;
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode();
        ObjectNode kvJson = result.putObject("data");
        data.asMap().forEach((key, val) -> kvJson.put(key, val.toString()));
        ObjectNode componentsJson = mapper.createObjectNode();
        if (dimension > 1) {
            getComponents().forEach(component -> {
                componentsJson.set(component.getName(), component.deepToJson(dimension - 1));
            });
        }

        result.put("name", getName());
        result.put("uuid", getUuid());
        result.put("desc", getDesc());
        result.put("instanceof", instanceOf.getUuid());
        result.put("glyphId", Integer.toHexString(instanceOf.getGlyphId()));
        if (componentsJson.size() > 0) {
            result.set("components", componentsJson);
        }
        if (inComponent != null) {
            result.put("incomponent", inComponent.getUuid());
        }

        result.put("_nodeType", getNodeType());

        return result;
    }

    public ObjectNode toJson() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode();
//        ObjectNode kvJson = result.putObject("data");
        data.asMap().forEach((key, val) -> result.put(key, val.toString()));
        ArrayNode componentsJson = mapper.createArrayNode();
        components.forEach(component -> {
            ObjectNode componentNameUuid = mapper.createObjectNode();
            componentNameUuid.put("name", component.getName());
            componentNameUuid.put("uuid", component.getUuid());
            componentsJson.add(componentNameUuid);
        });

        result.put("name", getName());
        result.put("uuid", getUuid());
        result.put("desc", getDesc());
        result.put("instanceof", instanceOf.getUuid());
        result.put("glyphId", Integer.toHexString(instanceOf.getGlyphId()));
        if (componentsJson.size() > 0) {
            result.set("components", componentsJson);
        }
        if (inComponent != null) {
            result.put("incomponent", inComponent.getUuid());
        }

        result.put("_nodeType", getNodeType());
        result.put("_hierarchyPath", getInstanceOf().getHierarchyPath());

        return result;
    }

}
