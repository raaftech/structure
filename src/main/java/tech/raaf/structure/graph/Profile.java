package tech.raaf.structure.graph;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import joptsimple.internal.Strings;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.raaf.structure.base.Configuration;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.graph.repositories.TypeRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

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
        // empty constructor for NodeMapper
    }

    private Profile(ObjectNode data, Type profileType) {
        super(data, profileType);
    }

    public Type getAppliesTo() {
        return relatedOne("-[:APPLIES_TO]->(b:Type)");
    }

    private Type getProvidedBy() {
        return relatedOne("-[:PROVIDED_BY]->(b:Type)");
    }

    /**
     * Profiles may contain a list of package names.
     * @return Return an {@link Optional} containing the package names if available, otherwise
     * an empty {@link Optional}.
     */
    public Optional<List<String>> getPackageNames() {
        return dataArrayStringToList(PACKAGENAMES, JsonNode::asText);
    }

    /**
     * Profiles may contain a list of service names.
     * @return Return an {@link Optional} containing the service names if available, otherwise
     * an empty {@link Optional}.
     */
    public Optional<List<String>> getServiceNames() {
        return dataArrayStringToList(SERVICENAMES, JsonNode::asText);
    }

    /**
     * Profiles may contain a list of ports.
     * @return Return an {@link Optional} containing the ports if available, otherwise
     * an empty {@link Optional}.
     */
    public Optional<List<Integer>> getPorts() {
        return dataArrayStringToList(PORTS, JsonNode::asInt);
    }

    /**
     * Profiles may contain a list of commands.
     * A command may look like
     * { "arbitraryIdentifier": { "command" "actual command", "matches": "some regex"}}
     * and is simply deserialized to a map by Jackson, keeping the same structure.
     * @return Return a {@link Map} wrapped in an {@link Optional}. The map has
     * another {@link Map} as values.
     */
    public Optional<Map<String, Map<String, String>>> getCommands() {
        Optional<JsonNode> maybeJson = dataObjectToJsonNode(COMMANDS);
        if (!maybeJson.isPresent()) {
            return Optional.empty();
        }
        try {
            return Optional.of(mapper.treeToValue(maybeJson.get(), Map.class));
        } catch (JsonProcessingException e) {
            if (Configuration.isTraceModeOn()) {
                e.printStackTrace();
            }
            log.error(e.getMessage());
            throw new IllegalArgumentException("An error occurred while " +
                    "deserializing commands of profile \"" + getUuid() + "\".");
        }
    }

    /**
     * Profiles may contain a list of directories.
     * @return Return an {@link Optional} containing the directories if available, otherwise
     * an empty {@link Optional}.
     */
    public Map<String, List<String>> getPossibleDirectories() {
        return getDirectoriesOrFiles(DirectoriesOrFiles.DIRECTORIES);
    }

    /**
     * Profiles may contain a list of files.
     * @return Return an {@link Optional} containing the files if available, otherwise
     * an empty {@link Optional}.
     */
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
            throw new IllegalArgumentException("An error occurred while " +
                    "deserializing directories/files object to Map for profile \"" + getUuid() + "\".");
        }
    }

    /**
     * Same as {@link Profile#asDetectionConsumable(String)} but with the name
     * of the {@link Profile#providedBy} as prefix.
     * @return See {@link Profile#asDetectionConsumable(String)}
     */
    public String asDetectionConsumable() {
        return asDetectionConsumable(getProvidedBy().getName());
    }

    /**
     * Profiles can be ran against a target host which ultimately gives a probability
     * of a that target host being, or owning an instance of the type to which a profile belongs.
     * We need to format profiles to a particular format so these profiles can be consumed
     * by scripts that check those profiles.
     * This method formats a profile to a file that can be `source`'d on *nix machines
     * (and possibly batch/powershell?).
     * @param prefix The string to use as prefix for every key.
     * @return A formatted string, possible containing the following keys prefixed by {@code prefix_}:
     * <code>
     *     PACKAGES,
     *     SERVICES,
     *     PORTS,
     *     LOG_DIRECTORIES,
     *     LOG_FILES,
     *     COMMANDS_INDEX
     * </code>
     * All keys may have one or more values separated by comma's.
     *
     * <code>COMMANDS_INDEX</code> contains an index for other keys contained in this file.
     * For instance, if <code>COMMANDS_INDEX</code> has value <code>"foo,bar"</code>, this means
     * that the following keys exist in the string:
     * <code>
     *     COMMANDS_FOO_COMMAND,
     *     COMMANDS_FOO_MATCHES,
     *     COMMANDS_BAR_COMMAND,
     *     COMMANDS_BAR_MATCHES
     * </code>
     */
    public String asDetectionConsumable(String prefix) {
        final StringBuilder out = new StringBuilder();
        final String dirPostfix = "Directories";
        final String filePostfix = "Files";

        /*
         * Given a key "FOO" and value "bar", return a string prefix_FOO="bar"
         */
        BiConsumer<String, String> writeKeyValue = (key, value) -> out
                .append(prefix)
                .append("_")
                .append(key)
                .append("=\"")
                .append(StringEscapeUtils.escapeJava(value))
                .append("\"\n");

        /*
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

        /*
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
                    .map(Object::toString)
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
            writeKeyValue.accept("COMMAND_INDEX", commands.keySet().stream()
                    .collect(Collectors.joining(",")));

            commands.entrySet().stream()
                    .map(entry -> Pair.of(
                            "COMMAND_" + entry.getKey() + "_" + COMMANDS_COMMAND.toUpperCase(),
                            entry.getValue().get(COMMANDS_COMMAND)))
                    .forEach(pair -> writeKeyValue.accept(pair.getLeft(), pair.getRight()));
            commands.entrySet().stream()
                    .map(entry -> Pair.of(
                            "COMMAND_" + entry.getKey() + "_" + COMMANDS_MATCHES.toUpperCase(),
                            entry.getValue().get(COMMANDS_MATCHES)))
                    .forEach(pair -> writeKeyValue.accept(pair.getLeft(), pair.getRight()));
        }

        return out.toString();
    }
}
