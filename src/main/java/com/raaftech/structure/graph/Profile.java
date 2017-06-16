package com.raaftech.structure.graph;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.raaftech.structure.base.Configuration;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.repositories.TypeRepository;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static io.vertx.core.json.Json.mapper;

public class Profile extends Instance {

    private static final String PACKAGENAMES = "packageNames";
    private static final String SERVICENAMES = "serviceNames";
    private static final String PORTS = "ports";
    private static final String COMMANDS = "commands";
    private static final String DIRECTORIES = "directories";
    private static final String FILES = "files";
    private static final Set<String> DIR_FILE_PREFIXES = ImmutableSet.of(
            "log", "data", "binary", "library", "configuration"
    );

    private static final TypeRepository typeRepository = Context.getTypeRepository();

    @RelatedTo(type="APPLIES_TO", direction= Direction.OUTGOING)
    @Fetch
    private Set<Type> appliesTo;

    @RelatedTo(type="PROVIDED_BY", direction=Direction.OUTGOING)
    @Fetch
    private Type providedBy;

    public static class Builder {
        ObjectNode data;
        public Builder(ObjectNode data) {
            this.data = data;
        }
        public Profile build() {
            Type profileType = typeRepository.findOneByName("Profile");
            return new Profile(data, profileType);

        }
    }

    public Profile() {
        // empty constructor for Spring Data Neo4j
    }

    private Profile(ObjectNode data, Type profileType) {
        super(data, profileType);
    }

    public void setAppliesTo(Set<Type> appliesTo) {
        if (this.appliesTo == null) {
            this.appliesTo = appliesTo;
        } else {
            throw new IllegalStateException("Can't set appliesTo, already set.");
        }
    }

    public void setProvidedBy(Type providedBy) {
        if (this.providedBy == null) {
            this.providedBy = providedBy;
        } else {
            throw new IllegalStateException("Can't set providedBy, already set.");
        }
    }

    public Optional<List<String>> getPackageNames() {
        return dataArrayStringToList(PACKAGENAMES, (el) -> el.asText());
    }

    public Optional<List<String>> getServiceNames() {
        return dataArrayStringToList(SERVICENAMES, (el) -> el.asText());
    }

    public Optional<List<Integer>> getPorts() {
        return dataArrayStringToList(PORTS, (el) -> el.asInt());
    }
}
