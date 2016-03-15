package com.raaftech.structure.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.JsonNodeValueResolver;
import com.github.jknack.handlebars.Template;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.InstanceNode;
import com.raaftech.structure.graph.repositories.InstanceNodeRepository;
import com.raaftech.structure.helper.handlebars.ReachableAddress;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Boy van Duuren &lt;boy@raaftech.com&gt;.
 */
public class InstrumentRunner {

    private final InstanceNodeRepository instanceNodeRepository = Context.INSTANCE.getInstanceNodeRepository();
    private final ObjectMapper mapper = new ObjectMapper();

    private final String taskId;
    private final InstanceNode origin;
    private final InstanceNode instrument;
    private final String templateProperty = "template";
    private final String workingDirectory = getCurrentWorkingDirectory() + "/generatedscripts/";
    // jobs will contain the system commands eventually sent to the agent with the TaskManager.
    private final ArrayList<String> jobList = new ArrayList<>();
    private final String taskManagerAddress = "com.raaftech.structure.manager.taskmanager";

    private final Logger log = LoggerFactory.getLogger(InstrumentRunner.class);

    /**
     * Construct the InstrumentRunner, do some checks, etc.
     *
     * @param instrument The instrument to use on the origin.
     * @param origin     The object undergoing the action.
     * @param arguments  Given arguments, can be null.
     * @param taskId     The ID used to track progress of this action.
     */
    public InstrumentRunner(InstanceNode origin, InstanceNode instrument, JsonNode arguments, String taskId) {
        this.taskId = taskId;
        this.origin = origin;
        this.instrument = instrument;

        // TODO: Need a different way of finding the structure node.
        InstanceNode structure = instanceNodeRepository.findOneByName("structure");
        InstanceNode subject = instanceNodeRepository.getStartOfComponentGroup(origin.getUuid());
        // Is the instrument argument of the expected type?
        if (!instrument.isOfType("Instrument")) {
            throw new IllegalArgumentException("Instrument should be of type instrument.");
            // todo: log as result
        }

        // Does the instrument even work on the given origin?
        if (!instrument.getHandledTypes().contains(origin.getInstanceOf())) {
            throw new IllegalArgumentException("Instrument is incompatible with the given origin.");
            // todo: log as result
        }

        ObjectNode context = mapper.createObjectNode();
        if (arguments != null) {
            context = arguments.deepCopy();
        }
        context.set("subject", subject.toArguments());
        context.set("origin", origin.toFlatJson());

        boolean subjectIsLocal = Boolean.parseBoolean(instrument.getProperty("subjectIsLocal"));
        if (subjectIsLocal) {
            // run subject as local routine
            // we need to do all the checks needed for a local routine, which are:
            // do we have a protocol to send a file?
            // do we have a protocol to send a command?
            log.debug("local routine");

            // Get a (set of) sendString instruments that are capable of sending strings from the agent to the subject
            // using a particular protocol (SSH, telnet, ...). This query only returns instruments that have a ReceiveString
            // counterpart in the subject speaking common protocol.
            Set<InstanceNode> sendStringInstruments = instanceNodeRepository.getCompatibleSendString(structure.getUuid(), subject.getUuid());
            if (sendStringInstruments.size() <= 0) {
                // todo: log
                throw new RuntimeException("No matching sendString protocol between structure and " + subject.getName());
            }

            // Because we may have multiple protocols giving us SendString capabilities, we want the "heaviest" one. For
            // instance, SSH is preferred over telnet on all occasions, so that has a heavier weight.
            InstanceNode heaviestSendString = findHeaviest(sendStringInstruments);
            // Get the matching ReceiveString instrument on the subject.
            InstanceNode matchingReceiveString = null;
            if (heaviestSendString != null) {
                matchingReceiveString = instanceNodeRepository.getMatchingReceiveString(heaviestSendString.getUuid());
            }

            // This shouldn't happen because if we don't have a heaviest SendString capability this routine has thrown
            // an exception at instanceRepository.getCompatibleSendString() already.
            // A SendString capability always has a matching ReceiveString capability on the subject in this routine, because
            // that is a constraint of the getCompatibleSendString() query as well.
            if (heaviestSendString == null || matchingReceiveString == null) {
                // todo: log
                throw new RuntimeException("No matching protocol between structure and " + subject.getName());
            } else {
                log.debug("Using the following {Send,Receive}String pair for task [" + taskId + "]: "
                        + heaviestSendString.getName() + ":" + matchingReceiveString.getName());
            }

            Set<InstanceNode> sendFileInstruments = instanceNodeRepository.getCompatibleSendFile(structure.getUuid(), subject.getUuid());
            if (sendFileInstruments.size() <= 0) {
                throw new RuntimeException("No matching sendFile protocol between structure and " + subject.getName());
            }

            // Because we may have multiple protocols giving us SendFile capabilities, we want the "heaviest" one. For
            // instance, SSH is preferred over ftp on all occasions, so that has a heavier weight.
            InstanceNode heaviestSendFile = findHeaviest(sendFileInstruments);
            // Get the matching ReceiveFile instrument on the subject.
            InstanceNode matchingReceiveFile = null;
            if (heaviestSendFile != null) {
                matchingReceiveFile = instanceNodeRepository.getMatchingReceiveFile(heaviestSendFile.getUuid());
            }

            // This shouldn't happen because if we don't have a heaviest SendFile capability this routine has thrown
            // an exception at instanceRepository.getCompatibleSendFile() already.
            // A SendFile capability always has a matching ReceiveFile capability on the subject in this routine, because
            // that is a constraint of the getCompatibleSendFile() query as well.
            if (heaviestSendFile == null || matchingReceiveFile == null) {
                // todo: log
                throw new RuntimeException("No matching protocol between structure and " + subject.getName());
            } else {
                log.debug("Using the following {Send,Receive}File pair for task [" + taskId + "]: "
                        + heaviestSendFile.getName() + ":" + matchingReceiveFile.getName());
            }

            InstanceNode invokableInterpreter = instanceNodeRepository.getInvokableInterpreter(subject.getUuid());
            if (invokableInterpreter == null) {
                throw new RuntimeException("Couldn't find an invokable interpreter on subject [" + subject.getUuid() + "]"
                        + " for task [" + taskId + "].");
            } else {
                log.debug("Using the following Invokable instrument for task [" + taskId + "]: "
                        + invokableInterpreter.getName() + ".");
            }


            // We now have everything we need: an instrument with an origin to run from, the subject, and ways
            // to send files (the script) and strings (the command to run the script)
            try {
                // create script file, return context with file path
                context = createScript(instrument, invokableInterpreter, context);
                // send file to subject (assumes sendFile + receiveFile)
                jobList.add(sendFile(heaviestSendFile, context));
                // send execution to subject (assumes sendString + receiveString)
                jobList.add(sendString(heaviestSendString, context));

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Check if the instrument is connected to structure via HAS_INSTRUMENT
            if (instanceNodeRepository.validateNonLocalInstrument(instrument.getUuid(), structure.getUuid(), origin.getUuid()) == null) {
                log.error("We can't run instrument \"" + instrument.getName() + "\", because \"" + structure.getName() + "\" [" + structure.getUuid() + "] " +
                        "doesn't have \"" + instanceNodeRepository.findInstrumentOwner(instrument.getUuid()) + "\".");
                // todo: log as result
            } else {
                log.debug("Valid, non-local path");
                try {
                    if (checkArguments(instrument, context)) {
                        jobList.add(applyTemplate(instrument.getProperty(templateProperty), context));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Create a send file execution string using the given SendFile instrument and context.
     *
     * @param sendFileInstrument An instrument that implements the SendFile capability.
     * @param context            The context for the handlebars template.
     * @return An execution string.
     * @throws IOException              HandleBars might throw an IOException while parsing the template.
     * @throws IllegalArgumentException Context doesn't pass argument validation.
     */
    private String sendFile(InstanceNode sendFileInstrument, ObjectNode context) throws IOException, IllegalArgumentException {
        if (checkArguments(sendFileInstrument, context)) {
            return applyTemplate(sendFileInstrument.getProperty(templateProperty), context);
        } else {
            throw new IllegalArgumentException("Error while checking arguments.");
        }
    }

    private boolean checkArguments(InstanceNode instrument, JsonNode context) {
        // Get defaults from the instrument schema. This is not very pretty.
        JsonNode argumentsKeySchema = JsonHelper.jsonFromResource("schema/instrument.schema.json")
                .get("properties")
                .get("arguments")
                .get("patternProperties")
                .get("^[a-zA-Z0-9]+$")
                .get("properties");
        JsonNode valueKeySchema = argumentsKeySchema.get("value").get("properties");

        final boolean optionalDefault = argumentsKeySchema.get("isOptional").get("default").asBoolean();
        final boolean userSuppliedDefault = argumentsKeySchema.get("canBeUserSupplied").get("default").asBoolean();
        final String strategyDefault = valueKeySchema.get("strategy").get("default").asText();
        final String onErrorDefault = valueKeySchema.get("onError").get("default").asText();

        // Begin validation
        boolean valid = true;
        JsonNode arguments;
        String tmpArguments = instrument.getProperty("arguments");
        if (tmpArguments == null) {
            return true;
        }

        try {
            arguments = mapper.readTree(tmpArguments);
            Iterator<Map.Entry<String, JsonNode>> entries = arguments.fields();
            while (entries.hasNext()) {
                Map.Entry<String, JsonNode> entry = entries.next();
                String key = entry.getKey();
                JsonNode arg = entry.getValue();
                JsonNode value = arg.get("value");

                boolean optional = optionalDefault;
                if (arg.has("isOptional")) {
                    optional = arg.get("isOptional").asBoolean();
                }

                String from = value.get("from").asText();
                String type = value.get("type").asText();

                // Check if we have this argument, and set a boolean (and object)
                // accordingly.
                JsonNode contextArgument = null;
                boolean found = false;
                // If the argument comes from the static environment, we can just get it from the
                // root of the context.
                if (from.equals("static")) {
                    found = context.has(key);
                    if (found) {
                        contextArgument = context.get(key);
                    }
                } else {
                    // Else, get use the "from" key to get the proper subcontext (in case of e.g. subject, origin, or agent)
                    JsonNode hierarchyPath = context.get(from).get("_hierarchyPath");
                    // hierarchyPath may be textual, in case of origin, or an object in the other cases.
                    if (hierarchyPath.isTextual()) {
                        found = hierarchyPath.asText().equals(type);
                    } else if (hierarchyPath.isObject()) {
                        found = hierarchyPath.has(type);
                    }
                    if (found) {
                        // In case we need to check the origin, we can just grab the whole object at context.origin.
                        // Else we need to grab the proper object from the context.$from object by looking up the name
                        // in the hierarchyPath.
                        if (from.equals("origin") && hierarchyPath.isTextual()) {
                            contextArgument = context.get(from);
                        } else if ((from.equals("subject") || from.equals("agent")) && hierarchyPath.isObject()) {
                            contextArgument = context.get(from).get(hierarchyPath.get(type).asText());
                        }
                    }
                }

                if (found) {
                    // validate the argument
                    if (value.has("matches")) {
                        // matches can either be an object or a string
                        if (value.get("matches").isTextual()) {
                            // we have a string, so treat it as regex
                            String pattern = value.get("matches").asText();
                            // ignore IDE warning, found being true means contextArgument != null
                            if (!contextArgument.asText().matches(pattern)) {
                                log.error("Argument " + key + " doesn't match regex \"" + pattern + "\" in instrument "
                                        + instrument.getName() + "for task " + taskId + ".");
                                valid = false;
                            }
                        } else if (value.get("matches").isObject()) {
                            // matches is an object, containing keys and strings, the strings are regex
                            ObjectNode matches = (ObjectNode) value.get("matches");
                            Iterator<Map.Entry<String, JsonNode>> matchIter = matches.fields();
                            while (matchIter.hasNext()) {
                                Map.Entry<String, JsonNode> match = matchIter.next();
                                String propertyKey = match.getKey();
                                String propertyRegex = match.getValue().asText();
                                // ignore IDE warning, found being true means contextArgument != null
                                if (!contextArgument.has(propertyKey) || !contextArgument.get(propertyKey).asText().matches(propertyRegex)) {
                                    log.error("Argument " + key + "." + propertyKey + " doesn't match regex \"" + propertyRegex + "\" "
                                            + "in instrument " + instrument.getName() + " for task " + taskId + ".");
                                    valid = false;
                                    break;
                                }
                            }
                        }
                    }
                } else if (optional) {
                    // the argument is optional, but in the case it's from field is "static" and it has been given a
                    // data field, we inject that value in our context.
                    if (from.equals("static") && value.has("data")) {
                        ((ObjectNode) context).put(key, value.get("data").asText());
                    }
                } else {
                    // we didn't find the argument, and it's not optional either, so we're in an invalid state
                    log.error("Missing argument " + key + ", needed in instrument " + instrument.getName() + " for task " + taskId + ".");
                    valid = false;
                }
                // break early if we found arguments invalid
                if (!valid) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            valid = false;
        }

        return valid;
    }

    /**
     * Create a send string execution string using the given SendFile instrument and context.
     *
     * @param sendStringInstrument An instrument that implements the SendFile capability.
     * @param context              The context for the handlebars template.
     * @return An execution string.
     * @throws IOException              HandleBars might throw an IOException while parsing the template.
     * @throws IllegalArgumentException Context doesn't pass argument validation.
     */
    private String sendString(InstanceNode sendStringInstrument, ObjectNode context) throws IOException {
        if (checkArguments(sendStringInstrument, context)) {
            return applyTemplate(sendStringInstrument.getProperty(templateProperty), context);
        } else {
            throw new IllegalArgumentException("Error while checking arguments.");
        }
    }

    /**
     * Create a script to be run on a remote host.
     *
     * @param instrument           The instrument to run.
     * @param invokableInterpreter The interpreter to wrap the command in.
     * @param context              Context for the parsing of the instrument and invokableInterpreter templates.
     * @return The context with added values from both the instrument and invokableInterpreter.
     * @throws IOException Is thrown when we run into trouble parsing the handlebars template.
     */
    private ObjectNode createScript(InstanceNode instrument, InstanceNode invokableInterpreter, ObjectNode context) throws IOException {
        if (checkArguments(instrument, context)) {
            String script = applyTemplate(instrument.getProperty(templateProperty), context);
            context.put("script", script);
            script = applyTemplate(invokableInterpreter.getProperty(templateProperty), context);

            String scriptFilename = origin.getName() + "-" + instrument.getName() + "_" + System.currentTimeMillis()
                    + invokableInterpreter.getProperty("extension");
            FileWriter scriptWriter = createFileWriter(scriptFilename);
            scriptWriter.write(script);
            scriptWriter.close();

            context.put("payloadDirectory", workingDirectory);
            context.put("payloadFileName", scriptFilename);

            return context;
        } else {
            throw new IllegalArgumentException("Error while checking arguments.");
        }
    }

    /**
     * Get the current working directory of the java process.
     *
     * @return The current working directory.
     */
    private String getCurrentWorkingDirectory() {
        return Paths.get("").toAbsolutePath().toString();
    }

    /**
     * Create a filewriter using a file in our working directory
     *
     * @param name The name of the file.
     * @return A filewriter.
     */
    private FileWriter createFileWriter(String name) {
        FileWriter fileWriter = null;
        createTempDirectory(workingDirectory);
        try {
            File file = new File(workingDirectory + name);
            fileWriter = new FileWriter(file);
            if (!file.setExecutable(true, false)) {
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
     *
     * @param path The path of the directory.
     * @return True if we created the directory, false if we didn't.
     */
    private boolean createTempDirectory(String path) {
        File dir = new File(path);
        return !new File(path).exists() && dir.mkdirs();
    }

    /**
     * Pass findHeaviest a set of Instruments and it'll find the heaviest by weight.
     * If we have multiple instruments with the same weight the one emitted from the stream first will
     * be used.
     *
     * @param instruments A set of InstanceNodes that are of the instrument type. Other types are filtered.
     * @return The heaviest instrument.
     */
    private InstanceNode findHeaviest(Set<InstanceNode> instruments) {
        // Collect instruments by weight filtering out duplicate weights
        Map<Integer, InstanceNode> instrumentsByWeight = instruments.stream()
                .filter(instrument -> instrument.isOfType("Instrument"))
                .filter(instrument -> instrument.getProperty("weight") != null)
                .collect(Collectors.toMap(
                        instrument -> Integer.parseInt(instrument.getProperty("weight")),
                        instrument -> instrument,
                        (instrument1, instrument2) -> instrument1));

        // Find the maximum weight
        Optional<Integer> heaviestInstrument = instrumentsByWeight.keySet().stream()
                .max(Integer::compare);

        if (heaviestInstrument.isPresent()) {
            return instrumentsByWeight.get(heaviestInstrument.get());
        } else {
            return null;
        }
    }

    /**
     * Create a string using a handlebars template and some context.
     *
     * @param template A handlebars template to parse.
     * @param context  The JSON context to parse the template in.
     * @return The outcome of the applied template.
     * @throws IOException This is thrown when we can't open the template, or something goes wrong when calling
     *                     Template.apply.
     */
    private String applyTemplate(String template, JsonNode context) throws IOException {
        Handlebars handlebars = new Handlebars();
        handlebars.registerHelper("reachable", ReachableAddress.INSTANCE);
        Template compiledTemplate = handlebars.compileInline(template);

        // Create context with our JSON.
        com.github.jknack.handlebars.Context ctx = com.github.jknack.handlebars.Context.newBuilder(context)
                .resolver(JsonNodeValueResolver.INSTANCE).build();

        // Apply the context to our template
        return compiledTemplate.apply(ctx).trim();
    }

    // Schedule the actual execution of the instrument.
    // This will always be done sequentially, until we implement automatic hierarchical execution of instruments.
    public void startInstrument() {
        final EventBus eventBus = Context.INSTANCE.getVertx().eventBus();
        ObjectNode message = mapper.createObjectNode();
        message.put("msgh", "queueTask");
        ObjectNode args = message.putObject("args");
        args.put("taskId", taskId);
        args.put("sequential", true);
        ArrayNode jobs = args.putArray("jobs");
        jobList.stream()
                .forEachOrdered(command -> {
                    ObjectNode job = mapper.createObjectNode();
                    job.putObject("metadata")
                            .put("origin", origin.getName())
                            .put("instrument", instrument.getName())
                            .put("capability", instanceNodeRepository.getInstrumentCapability(instrument.getUuid()).getName());
                    job.put("command", command);
                    jobs.add(job);
                });

        eventBus.send(taskManagerAddress, new JsonObject(message.toString()));
    }
}
