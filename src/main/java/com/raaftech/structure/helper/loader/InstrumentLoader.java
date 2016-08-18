package com.raaftech.structure.helper.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.InstanceNode;
import com.raaftech.structure.graph.TypeNode;
import com.raaftech.structure.graph.repositories.TypeNodeRepository;
import com.raaftech.structure.helper.JsonSchemaBuilder;
import org.apache.commons.io.FileUtils;
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
 * InstrumentLoader is responsible for traversing a directory containing instruments (mustache templates containing a
 * certain preamble).
 * The preamble is contained in  a mustache comment {{! }}.
 * This preamble tells us what capability the instruments provides to which TypeNodes. These instruments
 * are then created in the database.
 */
public class InstrumentLoader implements PackageLoader {

    private final static Logger log = LoggerFactory.getLogger(InstrumentLoader.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final TypeNodeRepository typeNodeRepository = Context.INSTANCE.getTypeNodeRepository();

    // Preamble stop and start match mustache multiline comments.
    // We need these to extract the JSON contained in this comment.
    private final String preambleStart = "{{!";
    private final String preambleStop = "}}";
    // Our schema to validate the provided JSON of a template.
    private final String instrumentSchemaPath = "schema/instrument.schema.json";
    // Extension of our instrument/template files.
    private final String expectedExtension = ".hbs";

    /*************************/
    /* Public Methods */
    /*************************/

    /*
     * This is the only exposed method on InstrumentLoader.
     * It is used to load all the instruments in a given directory.
     * @param directory A path to a directory containing instruments.
     */
    public void load(String directory) {
        InstrumentLoader instrumentLoader = new InstrumentLoader();

        String dht = new File(directory).getAbsolutePath();
        Path path = FileSystems.getDefault().getPath(dht);

        log.info("Started InstrumentLoader for directory " + path.toString() + ".");
        try {
            Files.list(path)
                    .filter(Files::isDirectory)
                    .forEach(dir -> {
                        try {
                            instrumentLoader.iterateFiles(dir);
                        } catch (IOException e) {
                            log.error("Error while iterating path " + dir + " for instruments...");
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            log.error("Error while loading instruments from path " + path + "...");
            e.printStackTrace();
        }
    }

    /*******************/
    /* Private Methods */
    /*******************/

    private void createHandlerNode(String instrumentCapability, ObjectNode instanceData,
                                   String providedByType, ArrayNode appliesTo) {

        Stream<JsonNode> appliesToStream = StreamSupport.stream(appliesTo.spliterator(), false);

        String instrumentName = instanceData.get("name").asText();

        TypeNode capability = typeNodeRepository.findOneByName(instrumentCapability);
        TypeNode instrumentType = typeNodeRepository.findOneByName("Instrument");
        TypeNode providedBy = typeNodeRepository.findOneByName(providedByType);

        if (capability == null) {
            log.error("Couldn't find capability for instrument " + instrumentName + ".");
            return;
        }

        if (instrumentType == null) {
            log.error("Couldn't find instrument type, is the database initialized?");
            return;
        }

        if (providedBy == null) {
            log.error("Couldn't find the provider type.");
            return;
        }

        List<TypeNode> applies = appliesToStream
                .map(JsonNode::asText)
                .map(typeNodeRepository::findOneByName)
                .peek(associationNode -> {
                    if (associationNode == null) {
                        throw new RuntimeException("Couldn't find associated TypeNode.");
                    }
                })
                .collect(Collectors.toList());

        log.debug("Found capability and instrument type for " + instrumentName + ", creating instance.");
        InstanceNode instrument = new InstanceNode(instanceData, instrumentType.getUuid());
        instrument.setCapability(capability);

        log.debug("Starting transaction to save the instrument.");
        Transaction tx = Context.INSTANCE.getGraphDatabase().beginTx();
        try {
            applies.stream()
                    .forEach(t -> {
                        instrument.addHandledType(t);
                        t.save();
                    });
            providedBy.addInstrument(instrument);
            providedBy.save();
            instrument.save();
            tx.success();
            log.info("Successfully created the " + instrumentName + " instrument.");
        } catch (Exception e) {
            log.error("Error while saving " + instrumentName + ", rolling back transaction.");
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
        JsonNode result;
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resource);

        try {
            result = mapper.readTree(inputStream);
        } catch (IOException x) {
            log.error(x.getMessage());
            return null;
        }

        return result;
    }

    // parsePreamble is passed the content of a instrument/template and extracts the first
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
        String preamble = stringBuilder.toString();

        stringBuilder.delete(0, stringBuilder.length());
        IntStream.range(indexClosingMustache+1, template.size())
                .mapToObj(template::get)
                .map(l-> l.concat(System.getProperty("line.separator")))
                .forEach(stringBuilder::append);

        String handlebarsTemplate = stringBuilder.toString().trim();

        // Is the preamble valid?
        log.debug("Validating found preamble against our schema.");
        try {
            JsonNode preambleJson = mapper.readTree(preamble);
            JsonSchemaBuilder jsonSchemaBuilder =
                    new JsonSchemaBuilder((ObjectNode)jsonFromResource(instrumentSchemaPath));
            if (jsonSchemaBuilder.validateJson(preambleJson)) {
                log.debug("Preamble valid. Continuing creation of the instruments.");

                // Create the JSON needed to create the instrument.
                ObjectNode instanceData = mapper.createObjectNode();
                // What kind of capability does this instrument provide?
                String capability = preambleJson.get("capability").asText();

                // Required fields
                instanceData.put("fileLocation", filePath);
                instanceData.put("name", preambleJson.get("name").asText());
                instanceData.put("desc", preambleJson.get("desc").asText());
                instanceData.put("type", preambleJson.get("instrumentType").asText());
                instanceData.put("subjectIsLocal", preambleJson.get("subjectIsLocal").asBoolean());
                instanceData.put("template", handlebarsTemplate);

                // Optional fields
                if (preambleJson.has("weight")) {
                    instanceData.put("weight", preambleJson.get("weight").asInt());
                }
                if (preambleJson.has("extension")) {
                    instanceData.put("extension", preambleJson.get("extension").asText());
                }
                if (preambleJson.has("resultParser")) {
                    instanceData.put("resultParser", preambleJson.get("resultParser").asText());
                }
                if (preambleJson.has("arguments")) {
                    instanceData.put("arguments", mapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(preambleJson.get("arguments")));
                }

                // What TypeNode is providing this instrument?
                String providedByType = preambleJson.get("providedBy").asText();
                // What does this instrument apply to?
                ArrayNode appliesToTypes = (ArrayNode)preambleJson.get("appliesTo");
                // Create the instrument
                createHandlerNode(capability, instanceData, providedByType, appliesToTypes);
            } else {
                log.error("Preamble of " + filePath + " didn't validate against instrument.schema.json.");
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
            String line;
            while ((line = reader.readLine()) != null) {
                contents.add(line);
            }
            parsePreamble(filePath.toString(), contents);
        } catch (IOException e) {
            log.error("Error while reading instrument template " + filePath.toString());
            e.printStackTrace();
        }
    }
}
