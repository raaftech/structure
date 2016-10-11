package com.raaftech.structure.helper.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.base.Configuration;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.exceptions.InstrumentNotFoundException;
import com.raaftech.structure.exceptions.JsonValidationException;
import com.raaftech.structure.graph.Controller;
import com.raaftech.structure.graph.Instrument;
import com.raaftech.structure.graph.Marker;
import com.raaftech.structure.graph.Type;
import com.raaftech.structure.graph.repositories.TypeRepository;
import com.raaftech.structure.helper.JsonHelper;

import org.apache.commons.io.FileUtils;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * InstrumentLoader is responsible for traversing a directory containing instruments (mustache templates containing a
 * certain preamble).
 * The preamble is contained in  a mustache comment {{! }}.
 * This preamble tells us what capability the instruments provides to which Types. These instruments
 * are then created in the database.
 */
public class InstrumentLoader implements PackageLoader {

    private final static Logger log = LoggerFactory.getLogger(InstrumentLoader.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final TypeRepository typeRepository = Context.getTypeRepository();

    // Our schema to validate the provided JSON of a template.
    private final String instrumentSchemaPath = "schema/helper/loader/instrument.schema.json";

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
        parseFile(filePath);
    }

    /*******************/
    /* Private Methods */
    /*******************/

    private void createInstrumentNode(Instrument instrument) {
        log.debug("Starting transaction to save the instrument.");
        Transaction tx = Context.getGraphDatabase().beginTx();
        try {
            instrument.save();
            tx.success();
            log.info("Successfully created the " + instrument.getName() + " instrument.");
        } catch (Exception e) {
            log.error("Error while saving " + instrument.getName() + ", rolling back transaction.");
            if (Configuration.isTraceModeOn()) {
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
    private void parseFile(Path filePath) {
        log.debug("Parsing " + filePath.toString() + ".");

        try {
            JsonNode instrumentJson = mapper.readTree(filePath.toFile());
            ObjectNode instrumentSchema = (ObjectNode) JsonHelper.jsonFromResource(instrumentSchemaPath);
            log.debug("Validating instrument using schema: " + instrumentSchemaPath);
            if (JsonHelper.validateToSchema(instrumentSchema, instrumentJson)) {
                log.debug("Instrument valid. Continuing..");

                Instrument newInstrument;
                String type = instrumentJson.get("instrumentType").asText();
                String uuid = instrumentJson.has("uuid") ? instrumentJson.get("uuid").asText() : null;
                String name = instrumentJson.get("name").asText();
                String desc = instrumentJson.get("desc").asText();
                
                // What Capability does this instrument give?
                String capability = instrumentJson.get("capability").asText();   
                
                if ("controller".equals(type)) {
               	    // What Type is providing this instrument?
                    String providedByType = instrumentJson.get("providedBy").asText();

                    // What Type(s) does this instrument apply to?
                    ArrayNode appliesJson = (ArrayNode) instrumentJson.get("appliesTo");
                    Stream<JsonNode> appliesStream = StreamSupport.stream(appliesJson.spliterator(), false);
                    Set<Type> appliesTypes = appliesStream
                        .map(JsonNode::asText)
                        .peek(t -> log.trace("Looking for type \"" + t + "\"."))
                        .map(typeRepository::findOneByName)
                        .peek(associationNode -> {
                            if (associationNode == null) {
                                throw new RuntimeException("Couldn't find associated Type.");
                            }
                        })
                        .collect(Collectors.toSet());             	
                    
                    // Is where the code runs equal to what the output data is about?
                    Boolean subjectIsLocal = instrumentJson.get("subjectIsLocal").asBoolean();
                    
//                  //The handlebars template.
                    File hbsFile = Paths.get(filePath.normalize().getParent().toString() + File.separator + instrumentJson.get("templateFile").asText()).toFile();
                    String handlebarsTemplate = FileUtils.readFileToString(hbsFile, "UTF-8");
                  
                    log.debug("Got associated handlebars template data from " + hbsFile.getName());
                   
                    Controller.Builder builder = new Controller.Builder()
                    		.filePath(filePath.toString())
                    		.name(name)
                            .desc(desc)
                            .providedBy(providedByType)
                            .handles(appliesTypes) 
                            .capability(capability)
                            .subjectIsLocal(subjectIsLocal)
                            .template(handlebarsTemplate);
                    		
                    if (uuid != null) {
                        builder.uuid(uuid);
                    }

                    // Optional fields
                    if (instrumentJson.has("weight")) {
                        builder.weight(instrumentJson.get("weight").asInt());
                    }
                    if (instrumentJson.has("extension")) {
                        builder.extension(instrumentJson.get("extension").asText());
                    }
                    if (instrumentJson.has("resultParser")) {
                        //TODO: implement
                    }
                    if (instrumentJson.has("arguments")) {
                        builder.arguments(mapper.writerWithDefaultPrettyPrinter()
                                .writeValueAsString(instrumentJson.get("arguments")));
                    }

                    newInstrument = builder.build();
                
                } else if ("marker".equals(type)) {
                	 // What Type is providing this instrument?
                    String providedByType = instrumentJson.get("providedBy").asText();
            
                    // What Type(s) does this instrument apply to?
                	ArrayNode appliesJson = (ArrayNode) instrumentJson.get("appliesTo");
                    Stream<JsonNode> appliesStream = StreamSupport.stream(appliesJson.spliterator(), false);
                    Set<Type> appliesTypes = appliesStream
                            .map(JsonNode::asText)
                            .peek(t -> log.trace("Looking for type \"" + t + "\"."))
                            .map(typeRepository::findOneByName)
                            .peek(associationNode -> {
                                if (associationNode == null) {
                                    throw new RuntimeException("Couldn't find associated Type.");
                                }
                            })
                            .collect(Collectors.toSet());
                    
                   
                    newInstrument = new Marker.Builder()
                            .name(name)
                            .desc(desc)
                            .filePath(filePath.toString())
                            .capability(capability)
                            .appliesTo(appliesTypes)
                            .providedBy(providedByType)
                            .build();
                
                } else if ("parser".equals(type)) {
                	// TODO: implement parser instrument.
                	log.error("Parser type instruments not implemented yet");
                	newInstrument = null;

                } else { 
                    // Should not be able to reach this.
                	log.error("This should be impossible. type is: " + type);
                	newInstrument = null;
                }
                
                // Create the instrument
                createInstrumentNode(newInstrument);
            
            } else {
                log.error("JSON in \"" + filePath + "\" didn't validate against instrument schema.");
                throw new JsonValidationException("JSON in \"" + filePath + "\" didn't validate against "
                        + "instrument schema");
            }
        } catch (IOException e) {
            if (Configuration.isTraceModeOn()) {
                e.printStackTrace();
            }
        }
    }
}
