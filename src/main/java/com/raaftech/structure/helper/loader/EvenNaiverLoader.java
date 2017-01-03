package com.raaftech.structure.helper.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.base.Configuration;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.exceptions.FileInjectionException;
import com.raaftech.structure.graph.Node;
import com.raaftech.structure.graph.repositories.NodeRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.parboiled.common.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * EvenNaiverLoader handles loading all json from bundle locations do dependency
 * resolving on all the nodes
 *
 * @author Freek Alleman <freek.alleman@raaftech.com>
 * @since 2016-10-11
 */
public class EvenNaiverLoader {

    private static final Logger log = LoggerFactory.getLogger(EvenNaiverLoader.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final String bundlePath;
    public static final Map<String, Path> checksumToFiles = new HashMap<>();
    public static final Map<String, String> checksumToUuid = new HashMap<>();
    private static final NodeRepository nodeRepository = Context.getNodeRepository();

    private static final String META = "meta";
    private static final String FILE = "file";
    private static final String PATH = "path";
    private static final String UUID = "uuid";

    private String globPattern = "**/*.{type,inst}.json";

    public EvenNaiverLoader() {
        bundlePath = Configuration.getBundlePath();
    }

    public EvenNaiverLoader(String bundlePath) {
        this.bundlePath = bundlePath;
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

    /**
     * Calculate the checksum of a string.
     * @param input The string to calculate the hash for.
     * @param algorithm Either SHA1, or MD5, defaulting to SHA1.
     * @return The calculated hash.
     */
    @SuppressWarnings("Duplicates")
    private String calculateChecksum(String input, String algorithm) {
        final String output;

        switch (algorithm.toLowerCase()) {
            case "md5":
                output = DigestUtils.md5Hex(input);
                break;
            default:
                log.warn("Invalid algorithm given to calculateChecksum, defaulting to SHA1");
            case "sha1":
                output = DigestUtils.sha1Hex(input);
                break;
        }
        return output;
    }

    /**
     * Calculate a checksum of a file
     *
     * @param path the file to calculate the checksum of
     * @param algo the algorithm
     * @return the checksum for the file using the algorithm
     * @throws IOException
     */
    @SuppressWarnings("Duplicates")
    private String calculateChecksum(Path path, String algo) throws IOException {
        final String result;
        FileInputStream fis = new FileInputStream(path.toFile());

        switch (algo) {
            case "md5":
                result = DigestUtils.md5Hex(fis);
                break;
            default:
                log.warn("Invalid algorithm given to calculateChecksum, defaulting to SHA1");
            case "sha1":
                result = DigestUtils.sha1Hex(fis);
                break;
        }

        fis.close();

        return result;
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
                        log.debug("globFiles: matched file: " + file);
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
            log.debug(e.getMessage());

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
    private Set<String> getDeps(JsonNode daNode) {
        Set<String> dependencies = new HashSet<>();

        Iterator<Map.Entry<String, JsonNode>> relationships = daNode.get(META).get("relations").fields();
        while(relationships.hasNext()){
            Map.Entry<String, JsonNode> relation = relationships.next();
            ArrayNode uuids = (ArrayNode) relation.getValue().get("uuids");
            Iterator<JsonNode> iter = uuids.elements();
            while (iter.hasNext()) {
                dependencies.add(iter.next().asText());
            }
        }

        return dependencies;
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

        return getDeps(node).stream()
                .allMatch(sortedUuids::contains);
    }

    /**
     * Split bundlePath into Paths and for each path perform globfile
     *
     * @return an ArrayNode containing all json found under the bundle-paths
     */
    public List<JsonNode> readAllJson() {

        ArrayList<JsonNode> unsorted = new ArrayList<>();
        List<Path> jsonFilePaths = new ArrayList<>();

        if(bundlePath == null) {
            return unsorted;
        }

        for (String bundlePathEntryString : bundlePath.split(":")) {
            Path bundlePathEntry = Paths.get(bundlePathEntryString);

            jsonFilePaths.addAll(globFiles(bundlePathEntry, globPattern));
        }

        try {
            for (Path jsonFilePath : jsonFilePaths) {
                JsonNode jsonFile = mapper.readTree(jsonFilePath.toFile());
                String fileChecksum = calculateChecksum(jsonFilePath, "sha1");
                String[] bundlePaths = bundlePath.split(":");
                String fromBundle = null;
                // the next piece of code finds the bundle name where the json originates from
                for (String s: bundlePaths) {
                    if (jsonFilePath.startsWith(s)) {
                        int bundleIndex = s.endsWith(File.separator) ? 0 : 1;
                        fromBundle = jsonFilePath.toString().replace(s, "").split(File.separator)[bundleIndex];
                        break;
                    }
                }

                log.debug("{}.isArray()? {}", jsonFilePath.getFileName(), String.valueOf(jsonFile.isArray()));
                // array of tokens
                if (jsonFile.isArray()) {
                    for (JsonNode token : jsonFile) {
                        final String entityChecksum = calculateChecksum(injectFileReferences(token, jsonFilePath).toString(), "SHA1");
                        checksumToFiles.put(entityChecksum, jsonFilePath);
                        checksumToUuid.put(entityChecksum, token.get(META).get(UUID).asText());
                        unsorted.add(putBundleName(putChecksum((ObjectNode) token, entityChecksum), fromBundle));
                    }
                } else { // single token
                    final String entityChecksum = calculateChecksum(injectFileReferences(jsonFile, jsonFilePath).toString(), "SHA1");
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

    public EvenNaiverLoader setGlobPattern(String globPattern) {
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
                    log.debug("Sorted {} of {} items", sorted.size(), worklist.size());
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
    private JsonNode injectFileReferences(JsonNode entity, Path entityFilePath) {
        final JsonNode entityCopy = entity.deepCopy();

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

        System.out.println("Size of update list: " + updateEntityList.size());
        System.out.println("Size of new list: " + newEntityList.size());
        // combine updateEntityList and newEntitylist, then return
        return Stream.concat(updateEntityList.stream(), newEntityList.stream())
                .collect(Collectors.toList());
    }
}
