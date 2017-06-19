package com.raaftech.structure.graph;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.raaftech.structure.base.Configuration;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.repositories.TypeRepository;
import joptsimple.internal.Strings;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.neo4j.graphdb.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
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

    public String asDetectionConsumable() {
        return asDetectionConsumable(providedBy.getName());
    }

    public String asDetectionConsumable(String prefix) {
        final StringBuilder out = new StringBuilder();
        final String dirPostfix = "Directories";
        final String filePostfix = "Files";

        /**
         * Given a key "FOO" and value "bar", return a string prefix_FOO="bar"
         */
        BiConsumer<String, String> writeKeyValue = (key, value) -> out
                .append(prefix.toUpperCase() + "_")
                .append(key)
                .append("=\"")
                .append(StringEscapeUtils.escapeJava(value))
                .append("\"\n");

        /**
         * Given a key "logDirectories" and a postfix "Directories", transform the key like so:
         * logDirectories -> LOG_DIRECTORIES
         */
        BiFunction<String, String, String> jsonKeyToSourceKey = (key, postfix) -> {
            final int idx = key.lastIndexOf(postfix);
            if (idx == -1) {
                return key;
            }
            return key.split(postfix)[0].toUpperCase() + "_" + postfix.toUpperCase();
        };

        /**
         * Iterates over all possible entries in a file or directory map and writes an entry in the string
         */
        BiConsumer<Map<String, List<String>>, String> writeFilesOrDirectories =
                (filesOrDirs, postfix) -> {
                    Map<String, String> entriesPerType = DIR_FILE_PREFIXES.stream()
                            .map(prfx -> prfx + postfix)
                            .filter(filesOrDirs::containsKey)
                            .collect(Collectors.toMap(type -> type,
                                    type -> Strings.join(filesOrDirs.get(type), ",")));
                    entriesPerType.entrySet().forEach(entry -> writeKeyValue.accept(
                            jsonKeyToSourceKey.apply(entry.getKey(), postfix), entry.getValue()));
        };

        // write packages
        final Optional<List<String>> maybePackages = getPackageNames();
        if (maybePackages.isPresent()) {
            writeKeyValue.accept("PACKAGES", Strings.join(maybePackages.get(), ","));
        }
        // write services
        final Optional<List<String>> maybeServices = getServiceNames();
        if (maybeServices.isPresent()) {
            writeKeyValue.accept("SERVICES", Strings.join(maybeServices.get(), ","));
        }
        // write ports
        final Optional<List<Integer>> maybePorts = getPorts();
        if (maybePorts.isPresent()) {
            final String portsAsStrings = maybePorts.get().stream()
                    .map(integer -> integer.toString())
                    .collect(Collectors.joining(","));
            writeKeyValue.accept("PORTS", portsAsStrings);
        }

        // write directories
        final Map<String, List<String>> directories = getPossibleDirectories();
        writeFilesOrDirectories.accept(directories, dirPostfix);
        // write files
        final Map<String, List<String>> files = getPossibleFiles();
        writeFilesOrDirectories.accept(files, filePostfix);

        // write commands
        Optional<Map<String, Map<String, String>>> maybeCommands = getCommands();
        if (maybeCommands.isPresent()) {
            Map<String, Map<String, String>> commands = maybeCommands.get();
            // first we need to write an index of all commands contained in this string
            writeKeyValue.accept("COMMANDS_INDEX", commands.keySet().stream()
                    .collect(Collectors.joining(",")));

            commands.entrySet().stream()
                    .map(entry -> Pair.of(
                            "COMMANDS_" + entry.getKey() + "_" + COMMANDS_COMMAND.toUpperCase(),
                            entry.getValue().get(COMMANDS_COMMAND)))
                    .forEach(pair -> writeKeyValue.accept(pair.getLeft(), pair.getRight()));
            commands.entrySet().stream()
                    .map(entry -> Pair.of(
                            "COMMANDS_" + entry.getKey() + "_" + COMMANDS_MATCHES.toUpperCase(),
                            entry.getValue().get(COMMANDS_MATCHES)))
                    .forEach(pair -> writeKeyValue.accept(pair.getLeft(), pair.getRight()));
        }

        return out.toString();
    }
}
