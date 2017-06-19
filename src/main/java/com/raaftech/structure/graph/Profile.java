package com.raaftech.structure.graph;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.raaftech.structure.base.Configuration;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.repositories.TypeRepository;
import org.neo4j.graphdb.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static io.vertx.core.json.Json.mapper;

public class Profile extends Instance {

    private static final String PACKAGENAMES = "packageNames";
    private static final String SERVICENAMES = "serviceNames";
    private static final String PORTS = "ports";
    private static final String COMMANDS = "commands";
    private static final String COMMANDS_COMMAND = "command";
    private static final String COMMANDS_MATCHES = "matches";
    private static final String DIRECTORIES = "directories";
    private static final String FILES = "files";
    private static final Set<String> DIR_FILE_PREFIXES = ImmutableSet.of(
            "log", "data", "binary", "library", "configuration"
    );

    private enum DirectoriesOrFiles {
        DIRECTORIES, FILES
    }

    private static final TypeRepository typeRepository = Context.getTypeRepository();
    private static final Logger log = LoggerFactory.getLogger(Profile.class);

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

    public Optional<Map<String, Map<String, String>>> getCommands() {
        Optional<JsonNode> maybeJson = dataObjectToJsonNode(COMMANDS);
        if (!maybeJson.isPresent()) {
            return Optional.empty();
        }
        JsonNode json = maybeJson.get();
        final Map<String, Map<String, String>> commands = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(json.fields(), Spliterator.ORDERED), false)
                .collect(Collectors.toMap(
                        (field) -> field.getKey(),
                        (field) -> ImmutableMap.of(
                                COMMANDS_COMMAND, field.getValue().get(COMMANDS_COMMAND).asText(),
                                COMMANDS_MATCHES, field.getValue().get(COMMANDS_MATCHES).asText()
                        )));
        return Optional.of(commands);
    }

    public Map<String, List<String>> getPossibleDirectories() {
        return getDirectoriesOrFiles(DirectoriesOrFiles.DIRECTORIES);
    }

    public Map<String, List<String>> getPossibleFiles() {
        return getDirectoriesOrFiles(DirectoriesOrFiles.FILES);
    }

    private Map<String, List<String>> getDirectoriesOrFiles(DirectoriesOrFiles directoriesOrFiles) {
        final String key = directoriesOrFiles == DirectoriesOrFiles.FILES ? FILES : DIRECTORIES;
        Optional<JsonNode> maybeDirectories = dataObjectToJsonNode(key);
        if (!maybeDirectories.isPresent()) {
            return ImmutableMap.of();
        }
        try {
            return mapper.treeToValue(maybeDirectories.get(), Map.class);
        } catch (JsonProcessingException e) {
            if (Configuration.isTraceModeOn()) {
                e.printStackTrace();
            }
            log.error(e.getMessage());
            throw new IllegalArgumentException("An error occurred while deserializing directories/files object to Map.");
        }
    }
}
