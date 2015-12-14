package com.raaftech.structure.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.InstanceNode;
import com.raaftech.structure.graph.TypeNode;
import com.raaftech.structure.graph.repositories.TemplateNodeRepository;
import com.raaftech.structure.graph.repositories.TypeNodeRepository;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * HandlerLoader is responsible for traversing a directory containing handlers (mustache templates containing a
 * certain preamble).
 * The preamble is contained in  a mustache comment {{! }}.
 * This preamble tells us what capability the handlers provides to which TypeNodes. These handlers
 * are then created in the database.
 */
public class HandlerLoader {

    private final static Logger log = LoggerFactory.getLogger(HandlerLoader.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final TypeNodeRepository typeNodeRepository = Context.INSTANCE.getTypeNodeRepository();

    // Preamble stop and start match mustache multiline comments.
    // We need these to extract the JSON contained in this comment.
    private final String preambleStart = "{{!";
    private final String preambleStop = "}}";
    // Our schema to validate the provided JSON of a template.
    private final String handlerSchemaPath = "schema/handler.schema.json";
    // Extension of our handler/template files.
    private final String expectedExtension = ".hbs";

    // Private constructor so HandlerLoader can't be created externally.
    private HandlerLoader() {}

    /*************************/
    /* Public Static Methods */
    /*************************/

    /*
     * This is the only exposed method on HandlerLoader.
     * It is used to load all the handlers in a given directory.
     * @param directory A path to a directory containing handlers.
     */
    public static void loadHandlers(String directory) {
        HandlerLoader handlerLoader = new HandlerLoader();
        TemplateNodeRepository templateNodeRepository = Context.INSTANCE.getTemplateNodeRepository();

        String dht = new File(directory).getAbsolutePath();
        Path path = FileSystems.getDefault().getPath(dht);

        log.info("Started HandlerLoader for directory " + path.toString() + ".");
        try {
            Files.list(path)
                    .filter(Files::isDirectory)
                    .forEach(dir -> {
                        try {
                            handlerLoader.iterateFiles(dir);
                        } catch (IOException e) {
                            log.error("Error while iterating path " + dir + " for handlers...");
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            log.error("Error while loading handlers from path " + path + "...");
            e.printStackTrace();
        }
    }

    /*******************/
    /* Private Methods */
    /*******************/

    private void createHandlerNode(String handlerCapability, ObjectNode instanceData,
                                   String providedByType, ArrayNode appliesTo) {

        Stream<JsonNode> appliesToStream = StreamSupport.stream(appliesTo.spliterator(), false);

        String handlerName = instanceData.get("name").asText();

        TypeNode capability = typeNodeRepository.findOneByName(handlerCapability);
        TypeNode handlerType = typeNodeRepository.findOneByName("Handler");
        TypeNode providedBy = typeNodeRepository.findOneByName(providedByType);

        if (capability == null) {
            log.error("Couldn't find capability for handler " + handlerName + ".");
            return;
        }

        if (handlerType == null) {
            log.error("Couldn't find handler type, is the database initialized?");
            return;
        }

        if (providedBy == null) {
            log.error("Couldn't find the provider type.");
            return;
        }

        List<TypeNode> applies = appliesToStream
                .map(JsonNode::asText)
                .map(nodeUuid -> typeNodeRepository.findOneByName(nodeUuid))
                .peek(associationNode -> {
                    if (associationNode == null) {
                        throw new RuntimeException("Couldn't find associated TypeNode.");
                    }
                })
                .collect(Collectors.toList());

        log.debug("Found capability and handler type for " + handlerName + ", creating instance.");
        InstanceNode handler = new InstanceNode(instanceData, handlerType.getUuid());
        handler.setCapability(capability);

        log.debug("Starting transaction to save the handler.");
        Transaction tx = Context.INSTANCE.getGraphDatabase().beginTx();
        try {
            applies.stream()
                    .forEach(t -> {
                        handler.addHandledType(t);
                        t.save();
                    });
            providedBy.addHandler(handler);
            providedBy.save();
            handler.save();
            tx.success();
            log.info("Successfully created the " + handlerName + " handler.");
        } catch (Exception e) {
            log.error("Error while saving " + handlerName + ", rolling back transaction.");
            e.printStackTrace();
            tx.failure();
        } finally {
            tx.close();
        }
    }

    // Iterate all the files in a directory, calling readFile for .mt files.
    private void iterateFiles(Path directory) throws IOException {
        log.debug("Iterating on directory \"" + directory + "\", finding .hbs files.");
        Files.list(directory)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(expectedExtension))
                .forEach(this::readFile);
    }

    // A json file loader (looks in your classpath/resources.)
    private JsonNode jsonFromResource(String resource) {
        JsonNode result = JsonNodeFactory.instance.objectNode();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resource);

        try {
            result = mapper.readTree(inputStream);
        } catch (IOException x) {
            log.error(x.getMessage());
            return null;
        }

        return result;
    }

    // parsePreamble is passed the content of a handler/template and extracts the first
    // multi-line comment's contents. The comment is expected to start on the first line as preambleStart and
    // ends on the first found preambleStop.
    private void parsePreamble(String filePath, ArrayList<String> template) {
        log.debug("Parsing " + filePath + ".");
        // We expect at least one line.
        if (template.size() < 1) {
            log.error("File " + filePath + " couldn't be parsed.");
            return;
        }

        // Check if the first line contains our preamble's starting sequence.
        String line = template.get(0);
        if (!line.startsWith(preambleStart)) {
            log.error("Template missing preamble, can't generate Handler.");
            return;
        }

        // Find the line number of the first character sequence matching our preamble's stop sequence.
        String closingMustache = template.stream()
                .filter(s -> s.contains(preambleStop))
                .limit(1)
                .collect(Collectors.toList()).get(0);
        int indexClosingMustache = template.indexOf(closingMustache);

        if (indexClosingMustache == -1) {
            log.error("Couldn't find end of the preamble in " + filePath + ".");
            return;
        }

        // Build a new string of everything contained in the preamble.
        StringBuilder stringBuilder = new StringBuilder();
        IntStream.range(0, indexClosingMustache+1)
                .forEach(i -> {
                    String s = template.get(i);
                    s = s.replace(preambleStart, "");
                    s = s.replace(preambleStop, "");
                    stringBuilder.append(s);
                });

        // Is the preamble valid?
        log.debug("Validating found preamble against our schema.");
        try {
            JsonNode preambleJson = mapper.readTree(stringBuilder.toString());
            JsonSchemaBuilder jsonSchemaBuilder =
                    new JsonSchemaBuilder((ObjectNode)jsonFromResource(handlerSchemaPath));
            if (jsonSchemaBuilder.validateJson(preambleJson)) {
                log.debug("Preamble valid. Continuing creation of the handlers.");

                // Create the JSON needed to create the handler.
                ObjectNode instanceData = mapper.createObjectNode();
                // What kind of capability does this handler provide?
                String capability = preambleJson.get("capability").asText();

                // Required fields
                instanceData.put("payload", filePath);
                instanceData.put("name", preambleJson.get("name").asText());
                instanceData.put("desc", preambleJson.get("desc").asText());
                instanceData.put("subjectIsLocal", preambleJson.get("subjectIsLocal").asBoolean());

                // Optional fields
                if (preambleJson.has("weight")) {
                    instanceData.put("weight", preambleJson.get("weight").asInt());
                }
                if (preambleJson.has("extension")) {
                    instanceData.put("extension", preambleJson.get("extension").asText());
                }
                if (preambleJson.has("arguments")) {
                    JsonNode arguments = preambleJson.get("arguments");
                    if (arguments.has("non-interactive") && arguments.get("non-interactive").size() > 0) {
                        instanceData.put("non-interactive", arguments.get("non-interactive").toString());
                    }
                    if (arguments.has("interactive") && arguments.get("interactive").size() > 0) {
                        instanceData.put("interactive", arguments.get("interactive").toString());
                    }
                }

                // What TypeNode is providing this handler?
                String providedByType = preambleJson.get("providedBy").asText();
                // What does this handler apply to?
                ArrayNode appliesToTypes = (ArrayNode)preambleJson.get("appliesTo");
                // Create the handler
                createHandlerNode(capability, instanceData, providedByType, appliesToTypes);
            } else {
                log.error("Preamble of " + filePath + " didn't validate against handler.schema.json.");
            }
        } catch (ProcessingException | IOException e) {
            e.printStackTrace();
        }
    }

    // This method is called for a found template. It reads its contents to an ArrayList<String>
    // and passes that to our parse function.
    private void readFile(Path filePath) {
        log.debug("Reading contents of file " + filePath.toString() + ".");
        Charset charset = Charset.forName("UTF-8");
        try (BufferedReader reader = Files.newBufferedReader(filePath, charset)) {
            ArrayList<String> contents = new ArrayList<>();
            String line = null;
            while ((line = reader.readLine()) != null) {
                contents.add(line);
            }
            parsePreamble(filePath.toString(), contents);
        } catch (IOException e) {
            log.error("Error while reading handler template " + filePath.toString());
            e.printStackTrace();
        }
    }
}
