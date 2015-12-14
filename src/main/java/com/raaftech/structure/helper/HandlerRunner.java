package com.raaftech.structure.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Jackson2Helper;
import com.github.jknack.handlebars.JsonNodeValueResolver;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.InstanceNode;
import com.raaftech.structure.graph.repositories.InstanceNodeRepository;
import com.raaftech.structure.graph.repositories.TypeNodeRepository;
import com.raaftech.structure.helper.handlebars.MultiplePathFileTemplateLoader;
import com.raaftech.structure.helper.handlebars.ReachableAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * HandlerRunner runs a handler on a subject.
 * This is done either locally (by logging in on the subject), or remotely (e.g. ping).
 * Whether a handler is run locally or remotely is determined by a boolean property on a handler (runsLocally).
 */
public class HandlerRunner {

    private final Logger log = LoggerFactory.getLogger(InstanceNode.class);
    private final InstanceNodeRepository instanceNodeRepository = Context.INSTANCE.getInstanceNodeRepository();
    private final TypeNodeRepository typeNodeRepository = Context.INSTANCE.getTypeNodeRepository();
    private final ObjectMapper mapper = new ObjectMapper();
    // TODO: Check safety of this type of FileTemplateLoader
    private Handlebars handlebars;

    private String workingDirectory;
    private InstanceNode commander;
    // Subject is the InstanceNode we called a handler on
    private InstanceNode subject;
    // Group is the whole componental group in which the subject resides
    private InstanceNode group;
    // The handler to run
    private InstanceNode handler;
    // Is the handler run locally on the subject or remotely?
    private boolean runsLocally;
    private ObjectNode json = mapper.createObjectNode();

    /**
     * The HandlerRunner constructor/initializor.
     * While constructing the HandlerRunner we check whether it is at all possible to run the handler on the
     * given parameters. This is determined by a lot of variables which include paths to the patient from the
     * agent via the instrument, etc.
     * @param subject An InstanceNode (grouped by (subject_0)-[:HAS_COMPONENT]->(subject_n)) which is the subject for our handler.
     * @param handler An InstanceNode representing a Handler.
     */
    public HandlerRunner(InstanceNode subject, InstanceNode handler, JsonNode interactive) {
        log.info("Running handler " + handler.getName() + " on " + subject.getName() + ".");
        long current = System.currentTimeMillis();
        this.group = instanceNodeRepository.getStartOfComponentGroup(subject.getUuid());
        this.commander = instanceNodeRepository.findOneByName(Context.selfIdentifier);
        this.runsLocally = Boolean.parseBoolean(handler.getProperty("subjectIsLocal"));

        if (group == null) {
            log.error("Couldn't finish running the handler, getStartOfComponentGroup() failed.");
            throw new IllegalArgumentException("HandlerRunner wasn't called on a component group.");
        }
        if (commander == null) {
            log.error("Couldn't finish running the handler because we didn't find an agent.");
            throw new IllegalArgumentException("HandlerRunner couldn't find an agent node.");
        }

        // We need to check if we have a path from the handler
        if (runsLocally) {
            handler = instanceNodeRepository.getValidHandler(handler.getUuid(), subject.getUuid(), group.getUuid());
        } else {
            handler = instanceNodeRepository.getValidHandler(handler.getUuid(), subject.getUuid(), commander.getUuid());
        }

        if (subject == null || handler == null || !handler.isOfType("Handler")) {
            log.error("Couldn't finish running the handler, getValidHandler() failed.");
            throw new IllegalArgumentException("Subject and/or handler are null or of wrong type.");
        } else {
            final String tempDir = getCurrentWorkingDirectory() + "/generatedscripts/";
            createTempDirectory(tempDir);
            workingDirectory = tempDir;

            this.subject = subject;
            this.handler = handler;

            // Retrieve our arguments
            json.set("subject", subject.toFlatJson());
            json.set("group", group.toArguments());
            json.set("commander", commander.toArguments());

            // Override argument in case we have interactive arguments
            try {
                // Get our optional and required interactive fields
                String handlerInteractive = handler.getProperty("interactive");
                ObjectNode allowedInteractiveJson = null;
                ArrayNode requiredInteractiveJson = null;
                ArrayNode optionalInteractiveJson = null;
                if (handlerInteractive != null) {
                    allowedInteractiveJson = (ObjectNode) mapper.readTree(handler.getProperty("interactive"));
                }
                if (allowedInteractiveJson != null) {
                    requiredInteractiveJson = (ArrayNode) allowedInteractiveJson.get("required");
                    optionalInteractiveJson = (ArrayNode) allowedInteractiveJson.get("optional");
                }
                if (requiredInteractiveJson != null && requiredInteractiveJson.size() > 0 && interactive == null) {
                    log.error("Couldn't finish running the handler because we didn't receive the required arguments.");
                    throw new IllegalArgumentException("Handler " + handler.getName() + " has required interactive arguments, none given.");
                }
                if (interactive != null) {
                    // Marshall ArrayNodes to ArrayList<String> so we can use .contains
                    ArrayList<String> allowedInteractiveArgs = new ArrayList<>();
                    ArrayList<String> requiredInteractiveArgs = new ArrayList<>();
                    if (requiredInteractiveJson != null) {
                        requiredInteractiveJson.forEach(el -> requiredInteractiveArgs.add(el.asText()));
                    }
                    if (optionalInteractiveJson != null) {
                        optionalInteractiveJson.forEach(el -> allowedInteractiveArgs.add(el.asText()));
                    }
                    // Iterator over our supplied interactive arguments
                    Iterator<Map.Entry<String, JsonNode>> fieldIterator = interactive.fields();
                    while (fieldIterator.hasNext()) {
                        Map.Entry<String, JsonNode> entry = fieldIterator.next();
                        String field = entry.getKey();
                        // Do we allow this interactive field?
                        if (allowedInteractiveArgs.contains(field) || requiredInteractiveArgs.contains(field)) {
                            // Then override it.
                            log.debug("Received interactive argument " + field + ", saving it to the JSON object.");
                            json.put(field, entry.getValue().asText());
                            // If this is a required field then remove it from the list, so we
                            // can verify all required arguments were given.
                            if (requiredInteractiveArgs.contains(field)) {
                                requiredInteractiveArgs.remove(field);
                            }
                        } else {
                            log.error("Couldn't finish running the handler because we received an unallowed argument.");
                            throw new RuntimeException("Handler received unallowed argument \"" + field + "\".");
                        }
                    }
                    // Check if we got data for all our required interactive arguments.
                    if (requiredInteractiveArgs.size() > 0) {
                        log.error("Couldn't finish running the handler because we didn't receive the required arguments.");
                        throw new RuntimeException("Missing data for arguments: " + requiredInteractiveArgs.toString());
                    }
                }
            } catch (IOException | RuntimeException e) {
                // Only happens when "interactive" is null, in which case we abort.
                log.error(e.getMessage());
                throw new RuntimeException("Aborting handler " + handler.getName() + " please see logging.");
            }

            // Setup handlebars

            String handlers = System.getProperty("handlers");
            // We're using MultiplePathFileTemplateLoader because users can give
            // multiple directories to load handlers from. MultiplePathFileTemplateLoader checks if a given
            // template path matches one of these handler directories.
            if (handlers.contains(",")) {
                handlebars = new Handlebars(new MultiplePathFileTemplateLoader(Arrays.asList(handlers.split(","))));
            } else {
                handlebars = new Handlebars(new MultiplePathFileTemplateLoader(handlers));
            }
            // Enable pretty print (because wth is this not default?!)
            handlebars.prettyPrint(true);
            // Register the JSON helper so we can pass JSON as context.
            handlebars.registerHelper("json", Jackson2Helper.INSTANCE);
            // Register the reachable handler so we can resolve reachable IP addresses
            handlebars.registerHelper("reachable", ReachableAddress.INSTANCE);
            log.debug("Initialized HandlerRunner in " + (System.currentTimeMillis() - current) + "ms.");
        }
    }

    /*******************/
    /* Private Methods */
    /*******************/

    /**
     * Create a string using a handlebars template and some context.
     * @param handler The handler to use.
     * @param context JSON representing our context.
     * @return The outcome of the applied template.
     * @throws IOException This is thrown when we can't open the template, or something goes wrong when calling
     * Template.apply.
     */
    private String applyTemplate(InstanceNode handler, JsonNode context) throws IOException, ProcessingException {
        AtomicBoolean valid = new AtomicBoolean(true);
        if (handler.getProperty("non-interactive") != null) {
            ObjectNode non_interactive = (ObjectNode)mapper.readTree(handler.getProperty("non-interactive"));
            Iterator<Map.Entry<String, JsonNode>> entries = non_interactive.fields();
            while (entries.hasNext()) {
                Map.Entry<String, JsonNode> entry = entries.next();
                String objName = entry.getKey();
                ArrayNode requiredTypes = (ArrayNode)entry.getValue();

                log.debug("Checking " + objName + " JSON object for type information...");
                if (!objName.equals("root") && !json.has(objName)) {
                    throw new IllegalArgumentException("Handler " + handler.getName() + " requires a JSON" +
                            " object " + objName + ", aborting.");
                }
                if (objName.equals("root")) {
                    // Template-data on the root of the json object doesn't have to conform
                    // to a type hierarchy since it doesn't come from the Type tree.
                    requiredTypes.forEach(t -> {
                        String type = t.asText();
                        if (!json.has(type)) {
                            valid.set(false);
                            log.error("missing key " + type + ".");
                        }
                    });
                } else {
                    ArrayNode jsonToCheck = (ArrayNode) json.get(objName).get("_hierarchyPath");
                    requiredTypes.forEach(t -> {
                        String type = t.asText();
                        AtomicBoolean found = new AtomicBoolean(false);
                        jsonToCheck.forEach(ct -> {
                            String currentType = ct.asText();
                            if (currentType.startsWith(type)) {
                                found.set(true);
                            }
                        });
                        if (!found.get()) {
                            log.error("missing key " + type + ".");
                            valid.set(found.get());
                        }
                    });
                }
            }
        }

        // If we couldn't validate everything return null.
        if (!valid.get()) {
            log.error("Couldn't satisfy " + handler.getName() + "'s arguments.");
            return null;
        }

        // Get the template's file location and compile.
        String templatePath = handler.getProperty("payload");
        try {
            Template template = handlebars.compile(templatePath);
            // Create context with our JSON.
            com.github.jknack.handlebars.Context ctx = com.github.jknack.handlebars.Context.newBuilder(context)
                    .resolver(JsonNodeValueResolver.INSTANCE).build();

            // Apply the context to our template
            return template.apply(ctx).trim();
        } catch (NullPointerException e) {
            throw new RuntimeException("Ran into an error while applying template " + templatePath + " from handler " +
                    handler.getName() + ".");
        }
    }

    /**
     * Create a filewriter using a file in our working directory
     * @param name The name of the file.
     * @return A filewriter.
     */
    private FileWriter createFileWriter(String name) {
        FileWriter fileWriter = null;
        try {
            File file = new File(workingDirectory + name);
            fileWriter = new FileWriter(file);
            if (!file.setExecutable(true)) {
                log.error("Couldn't set file " + file.toString() + " executable, this result in problems.");
            }
        } catch (IOException e) {
            log.error("Couldn't create FileWriter to write generated script to.");
            e.printStackTrace();
        }

        return fileWriter;
    }

    /**
     * Create a temporary directory.
     * @param path The path of the directory.
     * @return True if we created the directory, false if we didn't.
     */
    private boolean createTempDirectory(String path) {
        File dir = new File(path);
        if (!new File(path).exists()) {
            return dir.mkdirs();
        } else {
            return false;
        }
    }

    /**
     * Pass findHeaviest a set of Handlers and it'll find the heaviest by weight.
     * If we have multiple handlers with the same weight the one emitted from the stream first will
     * be used.
     * @param handlers A set of InstanceNodes that are of the handler type. Other types are filtered.
     * @return The heaviest handler.
     */
    private InstanceNode findHeaviest(Set<InstanceNode> handlers) {
        // Collect handlers by weight filtering out duplicate weights
        Map<Integer, InstanceNode> handlersByWeight = handlers.stream()
                .filter(handler -> handler.isOfType("Handler"))
                .filter(handler -> handler.getProperty("weight") != null)
                .collect(Collectors.toMap(
                        handler -> Integer.parseInt(handler.getProperty("weight")),
                        handler -> handler,
                        (handler1, handler2) -> handler1));

        // Find the maximum weight
        Optional<Integer> heaviestHandler = handlersByWeight.keySet().stream()
                .max(Integer::compare);

        if (heaviestHandler.isPresent()) {
            return handlersByWeight.get(heaviestHandler.get());
        } else {
            return null;
        }
    }

    private String getCurrentWorkingDirectory() {
        return Paths.get("").toAbsolutePath().toString();
    }

    /**
     * Transport a payload to a host and execute it.
     * @param commander The host where the payload is to be executed.
     * @param payload The absolute path of the payload.
     * @return A list of results from the used scriptable handler.
     */
    private ProcessRunner.ProcessResult transportAndExecutePayload(InstanceNode commander, String payload) {
        ProcessRunner.ProcessResult result = null;

        // If either the commander or the subject don't exist in the database set error and return.
        if (commander == null) {
            log.error("Couldn't find commander.");
            return null;
        }

        // find a Scriptable on the commander and an Accessable on the subject
        // that share the same protocols. If we find more than one choose one based on weight.
        log.debug("Looking for compatible transport between the agent and patient.");
        Set<InstanceNode> scriptableHandlers =
                instanceNodeRepository.getCompatibleScriptableHandlers(commander.getUuid(), group.getUuid());
        Set<InstanceNode> accessableHandlers =
                instanceNodeRepository.getCompatibleAccessibleHandlers(commander.getUuid(), group.getUuid());

        // Did we find anything? If not, set error on result and return.
        if (scriptableHandlers.size() < 1 || accessableHandlers.size() < 1) {
            log.error("Couldn't find transport methods that share a protocol between commander " + commander.getUuid()
                    + " and subject " + subject.getUuid() + ".");
            return null;
        }
        log.debug("Found " + (scriptableHandlers.size()+accessableHandlers.size()) + " compatible handlers.");

        log.debug("Creating JSON to pass as arguments to the handlers...");
        json.putObject("payload")
                .put("absolute", payload)
                .put("filename", Paths.get(payload).getFileName().toString());

        // Get the scriptable with the most weight.
        log.debug("Getting the \"heaviest\" scriptable handler.");
        InstanceNode heaviestScriptable = findHeaviest(scriptableHandlers);

        // execute complete command
        try {
            log.debug("Creating our command using " + heaviestScriptable.getName() + " and our arguments JSON.");
            InstanceNode commanderScriptable = instanceNodeRepository.getScriptableInterpreterHandler(commander.getUuid());
            String parsedCommand = applyTemplate(heaviestScriptable, json);
            if (parsedCommand == null) {
                throw new ProcessingException("Couldn't satisfy arguments, cancelling.");
            }

            String extension = commanderScriptable.getProperty("extension");
            String generatedScriptLocation = commanderScriptable.getName() + "_" + System.currentTimeMillis() + extension;
            FileWriter fileWriter = createFileWriter(generatedScriptLocation);
            String generatedScript = applyTemplate(commanderScriptable, mapper.createObjectNode().put("script", parsedCommand));
            if (generatedScript == null) {
                throw new ProcessingException("Couldn't satisfy arguments, cancelling.");
            }
            fileWriter.write(generatedScript);
            fileWriter.close();

            result = ProcessRunner.run(workingDirectory + generatedScriptLocation, 2000);
        } catch (IOException | ProcessingException | InterruptedException e) {
            e.printStackTrace();
        }

        return result;
    }

    /******************/
    /* Public Methods */
    /******************/

    /**
     * Start the running the handler.
     * @return The UUID of the result.
     */
    public String startHandler() {
        String resultUuid = null;
        try {
            log.debug("Getting closest interpreter scriptable handler");
            InstanceNode runOn;
            if (runsLocally) {
                runOn = group;
            } else {
                runOn = commander;
            }
            InstanceNode interpreterHandler = instanceNodeRepository.getScriptableInterpreterHandler(runOn.getUuid());
            if (interpreterHandler == null) {
                throw new RuntimeException("Couldn't find interpreter handler for " + runOn.getName() + ", aborting.");
            }

            log.debug("Going to run " + handler.getName());
            String parsedPayload = applyTemplate(handler, json);
            json.put("script", parsedPayload);
            log.debug("Passing previous output to our interpreter handler.");
            parsedPayload = applyTemplate(interpreterHandler, json);

            String extension = interpreterHandler.getProperty("extension");
            String generatedScript = handler.getName() + "_" + System.currentTimeMillis() + extension;
            FileWriter fileWriter = createFileWriter(generatedScript);
            fileWriter.write(parsedPayload);
            fileWriter.close();


            ProcessRunner.ProcessResult result = null;
            if (runsLocally) {
                // We'll need to transfer the generated payload to the subject and execute it.
                log.debug("Going to transport our generated script to the patient.");
                result = transportAndExecutePayload(commander, workingDirectory + generatedScript);
                log.debug(result.toString());
            } else {
                // We need to run the handler on a neighboring network node.
                // For now, we'll assume the machine running structure is this node.
                try {
                    log.debug("Going to run generated script " + workingDirectory + generatedScript);
                    result = ProcessRunner.run(workingDirectory + generatedScript, 5000);
                    log.debug(result.toString());
                } catch (Exception e) {
                    log.error("Ran into an issue while executing script.");
                }
            }

            if (result != null) {
                log.debug("Persisting result to the database.");
                // Creating the result object
                ObjectNode resultJson = mapper.createObjectNode()
                        .put("name", "Result")
                        .put("desc", "Result of running a command.")
                        .put("task", "none")
                        .put("process", result.process)
                        .put("dateCreated", System.currentTimeMillis())
                        .put("returnCode", result.returnCode)
                        .put("stdout", result.stdout)
                        .put("stderr", result.stderr);


                InstanceNode resultNode = new InstanceNode(resultJson, typeNodeRepository.findOneByName("Result").getUuid());
                subject.addResult(resultNode);

                resultUuid = resultNode.getUuid();
            } else {
                log.error("We didn't get any results back from executing the payload.");
            }

        } catch (IOException | ProcessingException | RuntimeException e) {
            e.printStackTrace();
            throw new RuntimeException("Error while running handler, please see the logging.");
        }

        return resultUuid;
    }
}
