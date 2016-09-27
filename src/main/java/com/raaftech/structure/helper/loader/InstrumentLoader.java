package com.raaftech.structure.helper.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.exceptions.InstrumentNotFoundException;
import com.raaftech.structure.exceptions.JsonValidationException;
import com.raaftech.structure.graph.InstanceNode;
import com.raaftech.structure.graph.TypeNode;
import com.raaftech.structure.graph.repositories.TypeNodeRepository;
import com.raaftech.structure.helper.JsonHelper;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
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
    private final TypeNodeRepository typeNodeRepository = Context.getTypeNodeRepository();

    // Preamble stop and start match mustache multiline comments.
    // We need these to extract the JSON contained in this comment.
    private final String preambleStart = "{{!";
    private final String preambleStop = "}}";
    // Our schema to validate the provided JSON of a template.
    private final String instrumentSchemaPath = "schema/instrument.schema.json";

    /*************************/
    /* Public Methods */
    /*************************/

    /*
     * This is the only exposed method on InstrumentLoader.
     * It is used to load an instrument defined in a file.
     * @param fileLocation A path to the instrument.
     */
    public void load(Path filePath) {
        // Check for null
        if (filePath == null) {
            return;
        }
        if (!Files.isRegularFile(filePath)) {
            throw new InstrumentNotFoundException("File \"" + filePath + "\" is not a regular file.");
        }

        log.info("Loading defined instruments.");
        readFile(filePath);
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
        Transaction tx = Context.getGraphDatabase().beginTx();
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
            if (Boolean.parseBoolean(System.getProperty("traceMode"))) {
                e.printStackTrace();
            }
            tx.failure();
        } finally {
            tx.close();
        }
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
        try {
            JsonNode preambleJson = mapper.readTree(preamble);
            ObjectNode instrumentSchema = (ObjectNode) JsonHelper.jsonFromResource(instrumentSchemaPath);
            log.debug("Validating preamble using schema: " + instrumentSchemaPath);
            if (JsonHelper.validateToSchema(instrumentSchema, preambleJson)) {
                log.debug("Preamble valid. Continuing creation of instruments..");

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
                log.error("Preamble of \"" + filePath + "\" didn't validate against instrument schema.");
                throw new JsonValidationException("Preamble of \"" + filePath + "\" didn't validate against "
                        + "instrument schema");
            }
        } catch (IOException e) {
            if (Boolean.parseBoolean(System.getProperty("traceMode"))) {
                e.printStackTrace();
            }
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
            if (Boolean.parseBoolean(System.getProperty("traceMode"))) {
                e.printStackTrace();
            }
        }
    }
}
