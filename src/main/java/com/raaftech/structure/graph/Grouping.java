package com.raaftech.structure.graph;

import com.fasterxml.jackson.databind.JsonNode;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @Deprecated We need to redesign the domain classes to use Entity.
 */
@Deprecated
public class Grouping extends Instance {

    @RelatedTo(type="GROUPS", direction= Direction.OUTGOING)
    @Fetch
    private Set<Component> components;

    public Set<Component> getComponents() {
        return components;
    }

    /**
     * Used to create a JSON representation of a single Instance that
     * can be used to as an argument for an instrument.
     * @return JSON representation that can be used as an argument for a instrument.
     */
    public Set<JsonNode> toArguments() {
        Set<JsonNode> arguments = new HashSet<>();

        arguments.add(toJson());
        BiConsumer<BiConsumer, Component> consumeArgsHelper = (f, node) -> {
            Set<Component> components = node.getComponents();
            if (components.size() > 0) {
                components.parallelStream().forEach(n -> f.accept(f, n));
            }
            arguments.add(node.toJson());
        };

        // Lambda's can't refer to themselves using their variable name. That means consumeArgsHelper defined
        // above cannot refer to itself by name. This sucks, because it's a recursive function.
        // However, it can refer to itself if it's passed a reference to itself as an argument, so that's what's
        // happening in consumeArgs. After calling consumeArgsHelper below, consumeArgsHelper is bound to 'f' in the lambda.
        Consumer<Component> consumeArgs = node -> consumeArgsHelper.accept(consumeArgsHelper, node);

        getComponents().stream().forEach(consumeArgs);

        return arguments;
    }

}
