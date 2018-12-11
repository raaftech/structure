package com.raaftech.structure.helper.loader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.raaftech.structure.base.Configuration;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.bus.GenericActor;
import com.raaftech.structure.exceptions.FileInjectionException;
import com.raaftech.structure.exceptions.JsonValidationException;
import com.raaftech.structure.graph.Node;
import com.raaftech.structure.graph.repositories.NodeRepository;
import com.raaftech.structure.helper.JsonHelper;
import org.apache.commons.codec.digest.DigestUtils;
import org.parboiled.common.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * NaiveSorter handles loading all json from bundle locations do dependency
 * resolving on all the nodes
 *
 * @author Freek Alleman <freek.alleman@raaftech.com>
 * @since 2016-10-11
 */
public class NaiveSorter {

    private static final Logger log = LoggerFactory.getLogger(NaiveSorter.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final Set<Path> bundlePaths;
    public static final Map<String, Path> checksumToFiles = new HashMap<>();
    public static final Map<String, String> checksumToUuid = new HashMap<>();
    private final NodeRepository nodeRepository = Context.getNodeRepository();

    private static final URI ENTITYSCHEMA = URI.create("resource:/schema/entity.schema.json");
    private static final String META = "meta";
    private static final String FILE = "file";
    private static final String CONTENT = "content";
    private static final String PATH = "path";
    private static final String UUID = "uuid";
    private static final String NAME = "name";

    private String globPattern = "**/*.{type,inst}.json";

    public NaiveSorter() {
        bundlePaths = Configuration.getBundlePaths();
    }

    public NaiveSorter(Set<Path> bundlePaths) {
        this.bundlePaths = bundlePaths;
    }

    /**
     * Write a checksum to the passed node
     *
     * @param node     the node to put the checksum in
     * @param checksum the checksum string
     * @return
     */
    private ObjectNode putChecksum(ObjectNode node, String checksum) {
        if (node.has(META)) {
            ((ObjectNode) node.get(META)).put("checksum", checksum);
        } else {
            node.putObject(META).put("checksum", checksum);
        }

        return node;
    }

    private ObjectNode putBundleName(ObjectNode node, String bundleName) {
        if (node.has(META)) {
            ((ObjectNode) node.get(META)).put("bundle", bundleName);
        } else {
            node.putObject(META).put("bundle", bundleName);
        }

        return node;
    }

    public enum ChecksumAlgorithm { MD5, SHA1 }
    private static Map<String, List<String>> checksumPropertyFilter = ImmutableMap.of(
            META, ImmutableList.of("checksum", "bundle", "path"),
            FILE, ImmutableList.of("checksum", "path")
    );

    private static String prepareForHashing(ObjectNode input) throws JsonProcessingException {
        if (BundleLoader.isInstance(input) || !input.get("name").asText().equals("Type")) {
            if (!JsonHelper.validateToSchema(ENTITYSCHEMA, input)) {
                throw new JsonValidationException("Cannot calculate a checksum for type ["+ input.get("name").asText() +"]: JSON does not validate against " +
                        "entity schema");
            }
        }

        final ArrayNode result = new ObjectMapper().createArrayNode();

        ObjectNode inputCopy = input.deepCopy();

        // remove unwanted keys from JSON before calculating checksum
        checksumPropertyFilter.keySet().stream()
                .filter(inputCopy::has)
                .forEach(key -> {
                        checksumPropertyFilter.get(key)
                                .forEach(val -> ((ObjectNode) inputCopy.get(key)).remove(val));
                });

        /*
        A type entity might miss its "abstract" property in which case it defaults to false.
        Because type entities that come from the graph will always have a value for "abstract"
        we'll have to inject it in case it's missing.
         */
        if (inputCopy.has(META) && !inputCopy.get(META).has("abstract")) {
            ((ObjectNode) inputCopy.get(META)).put("abstract", false);
        }


        /*
        A object mapper that has a special configuration to alphabetically sort keys so
        we calculate equal checksums for JSON objects even though properties are unsorted.
         */
        final ObjectMapper sortedMapper =
                new ObjectMapper().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

        /*
        Use sortedMapper to deserialize to object, then serialize back to a JSON string again.
        This way the mapper will sort all the keys, see:
        https://stackoverflow.com/questions/34817541/json-order-map-entries-by-keys-not-working-consistently
         */
        return sortedMapper.writeValueAsString(sortedMapper.treeToValue(inputCopy, Object.class));
    }

    /**
     * Calculate the checksum of a string.
     * @param input The string to calculate the hash for.
     * @param algorithm Either SHA1, or MD5, defaulting to SHA1.
     * @return The calculated hash.
     */
    @SuppressWarnings("Duplicates")
    public static String calculateChecksum(ObjectNode input, ChecksumAlgorithm algorithm) {
        final String output;
        final String preparedJson;
        try {
            preparedJson = prepareForHashing(input);
        } catch (JsonProcessingException e) {
            if (Configuration.isTraceModeOn()) {
                e.printStackTrace();
            }
            log.error("Error while preparing JSON for hashing: " + e.getMessage());
            return null;
        }

        switch (algorithm) {
            case MD5:
                output = DigestUtils.md5Hex(preparedJson);
                break;
            default:
                log.warn("Invalid algorithm given to calculateChecksum, defaulting to SHA1");
            case SHA1:
                output = DigestUtils.sha1Hex(preparedJson);
                break;
        }
        return output;
    }

    /**
     * Calculate a checksum of a file
     *
     * @param inputStream This input will be marshalled to a JSON object, and then used to calculate the checksum.
     * @param algorithm the algorithm
     * @return the checksum for the file using the algorithm
     * @throws IOException
     */
    public static String calculateChecksum(InputStream inputStream, ChecksumAlgorithm algorithm) throws IOException {
        JsonNode input = new ObjectMapper().readTree(inputStream);
        inputStream.close();
        if (!(input instanceof ObjectNode)) {
            throw new JsonValidationException("Expected a JSON object, got something else.");
        }

        return calculateChecksum((ObjectNode) input, algorithm);
    }

    /**
     * Get a List of Paths that match the specified globbing pattern
     *
     * @param path
     * @param pattern globbing pattern
     * @return List of Paths matching the specified globbing pattern
     */
    private List<Path> globFiles(Path path, String pattern) {

        final List<Path> matchingFiles = new ArrayList<>();
        final String workingDirectory = path.toString().replace('\\', '/');
        final String syntaxAndPattern = "glob:" + workingDirectory + "/" + pattern.replace('\\', '/');
        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher(syntaxAndPattern);

        log.debug("globFiles: got pattern: " + pattern);
        log.debug("globFiles: got workingDirectory: " + workingDirectory);
        log.debug("globFiles: got syntaxAndPattern: " + syntaxAndPattern);

        try {
            Files.walkFileTree(Paths.get(workingDirectory), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (matcher.matches(file)) {
                        //log.debug("globFiles: matched file: " + file);
                        matchingFiles.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error(e.getMessage());

            if (Configuration.isTraceModeOn()) {
                e.printStackTrace();
            }
        }

        return matchingFiles;
    }

    /**
     * Get all dependencies for a JsonNode
     *
     * @param daNode the JsonNode to fetch all dependencies for
     * @return Map with 2 Sets:
     * "name" (for dependencies to be found by "name") and
     * "uuid" (for dependencies to be found by "uuid")
     */
    private Map<String, Set<String>> getDeps(JsonNode daNode) {
        final Set<String> uuidDependencies = new HashSet<>();
        final Set<String> namedDependencies = new HashSet<>();

        Iterator<Map.Entry<String, JsonNode>> relationships = daNode.get(META).get("relations").fields();
        while(relationships.hasNext()){
            Map.Entry<String, JsonNode> relation = relationships.next();
            ArrayNode identifiers = (ArrayNode) relation.getValue().get("to");
            Iterator<JsonNode> iter = identifiers.elements();
            while (iter.hasNext()) {
                final String identifier = iter.next().asText();
                if (GenericActor.isUuid(identifier)) {
                    uuidDependencies.add(identifier);
                } else {
                    namedDependencies.add(identifier);
                }
            }
        }

        return ImmutableMap.of("uuids", uuidDependencies,
                "names", namedDependencies);
    }

    /**
     * Return the JsonNode described by the key and value specified from the passed array
     *
     * @param lookupField the key to search for
     * @param lookupValue the value for the searchkey (lookupField)
     * @param haystack    the array in which so search
     * @return the JsonNode described by the key/value
     */
    private Optional<JsonNode> getNode(String lookupField, String lookupValue, List<JsonNode> haystack) {
        return haystack.stream()
                .filter(node -> node.get(lookupField).asText().equals(lookupValue))
                .findFirst();
    }

    /**
     * See whether a (key,value)-pair exists in an array
     *
     * @param lookupField
     * @param lookupValue
     * @param haystack
     * @return
     */
    private boolean arrayContains(String lookupField, String lookupValue, List<JsonNode> haystack) {
        return haystack.stream()
                        .filter(node -> node.get(lookupField).asText().equals(lookupValue))
                        .count() > 0;
    }

    /**
     * Check whether all dependencies for node are present in sortedArray
     *
     * @param node        the node to check dependencies for
     * @param sortedArray the array containing dependencies
     * @return are all dependencies present in sortedArray
     */
    private boolean allDependenciesMet(JsonNode node, List<JsonNode> sortedArray) {
        Set<String> sortedUuids = sortedArray.stream()
                .map(sortedNode -> sortedNode.get(META).get(UUID).asText())
                .collect(Collectors.toSet());
        Set<String> sortedNames = sortedArray.stream()
                .map(sortedNode -> sortedNode.get(NAME).asText())
                .collect(Collectors.toSet());

        final Map<String, Set<String>> dependencies = getDeps(node);
        return
                dependencies.get("uuids").stream().allMatch(sortedUuids::contains) &&
                dependencies.get("names").stream().allMatch(sortedNames::contains);
    }

    /**
     * Split bundlePath into Paths and for each path perform globfile
     *
     * @return an ArrayNode containing all json found under the bundle-paths
     */
    public List<JsonNode> readAllJson() {

        ArrayList<JsonNode> unsorted = new ArrayList<>();
        List<Path> jsonFilePaths = new ArrayList<>();

        if(bundlePaths == null) {
            return unsorted;
        }

        for (Path bundlePathEntry: bundlePaths) {
            jsonFilePaths.addAll(globFiles(bundlePathEntry, globPattern));
        }

        try {
            for (Path jsonFilePath : jsonFilePaths) {
                JsonNode jsonFile = mapper.readTree(jsonFilePath.toFile());
                String fromBundle = null;
                // the next piece of code finds the bundle name where the json originates from
                for (Path s: bundlePaths) {
                    if (jsonFilePath.startsWith(s)) {
                        int bundleIndex = s.endsWith(File.separator) ? 0 : 1;
                        fromBundle = jsonFilePath.toString().replace(s.toString(), "").split(Pattern.quote(File.separator))[bundleIndex];
                        break;
                    }
                }

                // array of tokens
                if (jsonFile.isArray()) {
                    for (JsonNode token : jsonFile) {
                        final String entityChecksum = calculateChecksum(injectFileReferences(token, jsonFilePath), ChecksumAlgorithm.SHA1);
                        checksumToFiles.put(entityChecksum, jsonFilePath);
                        checksumToUuid.put(entityChecksum, token.get(META).get(UUID).asText());
                        unsorted.add(putBundleName(putChecksum((ObjectNode) token, entityChecksum), fromBundle));
                    }
                } else { // single token
                    final String entityChecksum = calculateChecksum(injectFileReferences(jsonFile, jsonFilePath), ChecksumAlgorithm.SHA1);
                    checksumToFiles.put(entityChecksum, jsonFilePath);
                    unsorted.add(putBundleName(putChecksum((ObjectNode) jsonFile, entityChecksum), fromBundle));
                }
            }
        } catch (IOException e) {
            if (Configuration.isTraceModeOn()) {
                e.printStackTrace();
            }
        }

        return unsorted;
    }

    public NaiveSorter setGlobPattern(String globPattern) {
        this.globPattern = globPattern;
        return this;
    }

    /**
     * Sort the array worklist based on dependencies
     *
     * @param worklist the unsorted array
     * @return a sorted array
     */
    public List<JsonNode> sortIt(List<JsonNode> worklist) throws Exception {
        List<JsonNode> sorted = new ArrayList<>();
        boolean eject = false;

        log.info("Started sorting {} items..", worklist.size());

        int i = 0;
        while (!eject) {
            // If we looped the same number of times as the length of the worklist, worklist seems unresolvable
            if (i >= worklist.size()) {
                throw new Exception("Unresolvable worklist was passed");
            }

            for (JsonNode item : worklist) {
                if (allDependenciesMet(item, sorted) && !sorted.contains(item)) {
                    sorted.add(item);
                }

                // Set eject if sorted and worklist are equal.
                if (sorted.size() >= worklist.size()) {
                    eject = true;
                }
            }
            i++;
        }

        return sorted;
    }

    /**
     * Resolve external file references and inject them so we can use them for checksum calculation.
     * @param entity A JSON representation of an entity.
     * @param entityFilePath The path of the file containing the entity JSON.
     * @return The same JSON as went in, but now with injected file content.
     */
    private ObjectNode injectFileReferences(JsonNode entity, Path entityFilePath) {
        if (!(entity instanceof ObjectNode)) {
            throw new FileInjectionException("Can't inject files into non object JSON.");
        }
        final ObjectNode entityCopy = entity.deepCopy();

        // do we even have a file object?
        if (entityCopy.has(FILE)) {
            // iterate over every file declaration
            final Iterator<Map.Entry<String, JsonNode>> files = entityCopy.get(FILE).fields();
            while (files.hasNext()) {
                final Map.Entry<String, JsonNode> file = files.next();
                final String fileName = file.getKey();
                final ObjectNode fileObject = (ObjectNode) file.getValue();

                /* if the file declaration does not specify a path its contents are declared inline
                and are already included in the checksum calculation
                 */
                if (!fileObject.has(PATH)) {
                    continue;
                }

                final Path entityFileReferencePath = entityFilePath.getParent().resolve(fileObject.get(PATH).asText());
                // read the file's contents and inject it into the file JSON object
                if (entityFileReferencePath.toFile().exists()) {
                    fileObject.put("content", FileUtils.readAllText(entityFileReferencePath.toFile()));
                } else if (fileObject.has(CONTENT)) {
                    // we can't read the file, but we already have some content. just use that data.
                    continue;
                } else {
                    throw new FileInjectionException("Couldn't read " + entityFileReferencePath.toString() + ".");
                }
            }
        }

        return entityCopy;
    }

    public List<JsonNode> getUpdateList(List<JsonNode> sortedList) {
        List<JsonNode> updateEntityList = sortedList.stream()
                .filter(n -> nodeRepository.findByUuid(n.get(META).get(UUID).asText()) != null)
                .filter(n -> {
                    Node node = nodeRepository.findByUuid(n.get(META).get(UUID).asText());
                    return !node.getChecksum().equals(n.get(META).get("checksum").asText());
                })
                .collect(Collectors.toList());

        List<JsonNode> newEntityList = sortedList.stream()
                .filter(n -> nodeRepository.findByUuid(n.get(META).get(UUID).asText()) == null)
                .collect(Collectors.toList());

        log.debug("Update list: {} , new list: {}", updateEntityList.size(), newEntityList.size());
        // combine updateEntityList and newEntitylist, then return
        return Stream.concat(updateEntityList.stream(), newEntityList.stream())
                .collect(Collectors.toList());
    }
}
