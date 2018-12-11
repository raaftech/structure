package tech.raaf.structure.graph;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.RelatedTo;
import tech.raaf.structure.exceptions.JsonValidationException;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Group extends Instance implements Groupable {

    @RelatedTo(type="MEMBER_OF", direction= Direction.INCOMING)
    @Fetch
    private Set<Groupable> groupees;

    @RelatedTo(type="MEMBER_OF", direction= Direction.OUTGOING)
    @Fetch
    private Set<Group> memberOf;

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

    public Set<Groupable> getGroupees() {
        if (groupees == null) {
            groupees = new HashSet<>();
        }
        return groupees;
    }

    public Set<Group> getMemberOf() {
        if (memberOf == null) {
            memberOf = new HashSet<>();
        }
        return memberOf;
    }

    public void addMember(Groupable groupable) {
        /*
        If we're already a member of this group return. This is done because we
        call `addMember` from `addToGroup` and `addToGroup` from `addMember`.
         */
        if (getMemberOf().contains(groupable)) {
            return;
        }
        getGroupees().add(groupable);
        groupable.addToGroup(this);
    }


//    /**
//     * Used to create a JSON representation of a single Instance that
//     * can be used to as an argument for an instrument.
//     * @return JSON representation that can be used as an argument for a instrument.
//     */
//    public Set<JsonNode> toArguments() {
//        Set<JsonNode> arguments = new HashSet<>();
//
//        arguments.add(toJson());
//        BiConsumer<BiConsumer, Component> consumeArgsHelper = (f, node) -> {
//            Set<Component> groupees = node.getGroupees();
//            if (groupees.size() > 0) {
//                groupees.parallelStream().forEach(n -> f.accept(f, n));
//            }
//            arguments.add(node.toJson());
//        };
//
//        // Lambda's can't refer to themselves using their variable name. That means consumeArgsHelper defined
//        // above cannot refer to itself by name. This sucks, because it's a recursive function.
//        // However, it can refer to itself if it's passed a reference to itself as an argument, so that's what's
//        // happening in consumeArgs. After calling consumeArgsHelper below, consumeArgsHelper is bound to 'f' in the lambda.
//        Consumer<Component> consumeArgs = node -> consumeArgsHelper.accept(consumeArgsHelper, node);
//
//        getGroupees().stream().forEach(consumeArgs);
//
//        return arguments;
//    }
    /**
     * Used to create a JSON representation of a single Instance that
     * can be used to as an argument for an instrument.
     * @return JSON representation that can be used as an argument for a instrument.
     */
    @SuppressWarnings("unchecked")
    public Set<JsonNode> toArguments() {
        Set<JsonNode> arguments = new HashSet<>();

        arguments.add(super.toJson());
        BiFunction<BiFunction, Groupable, Set<JsonNode>> consumeArgsHelper = (f, node) -> {
            final Set<Component> components;
            if (node instanceof Component) {
                components = ((Component)node).getComponents();
            } else {
                components = Collections.emptySet();
            }
            final Set<JsonNode> childSet;
            if (!components.isEmpty()) {
                childSet = components.stream()
                        .flatMap(instance -> ((Set<JsonNode>)f.apply(f, instance)).stream())
                        .collect(Collectors.toSet());
//                Stream.concat(groupees.stream(), configurations.stream()).forEach(n -> f.accept(f, n));
            } else {
                childSet = Collections.emptySet();
            }
            return Sets.union(childSet, ImmutableSet.of(((Instance)node).toJson()));
        };

        // Lambda's can't refer to themselves using their variable name. That means consumeArgsHelper defined
        // above cannot refer to itself by name. This sucks, because it's a recursive function.
        // However, it can refer to itself if it's passed a reference to itself as an argument, so that's what's
        // happening in consumeArgs. After calling consumeArgsHelper below, consumeArgsHelper is bound to 'f' in the lambda.
        Function<Groupable, Set<JsonNode>> consumeArgs = node -> consumeArgsHelper.apply(consumeArgsHelper, node);

        return getGroupees().stream().map(consumeArgs).flatMap(Collection::stream).collect(Collectors.toSet());
    }

}
