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
import com.raaftech.structure.helper.classification.ArgumentsToClassificationTreeParser;
import com.raaftech.structure.helper.classification.tree.RootNode;
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
import java.util.function.BiConsumer;
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
    private final InstanceNode subject;
    private final InstanceNode agent;
    private final ObjectNode arguments;

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
    public InstrumentRunner(InstanceNode origin, InstanceNode instrument, ObjectNode arguments, String taskId) {
        this.taskId = taskId;
        this.origin = origin;
        this.instrument = instrument;
        this.arguments = arguments == null ? mapper.createObjectNode() : arguments;

        // TODO: Need a different way of finding the structure node.
        this.agent = instanceNodeRepository.findOneByName(Context.selfIdentifier);
        this.subject = instanceNodeRepository.getStartOfComponentGroup(origin.getUuid());
        // Is the instrument argument of the expected type?
        if (!instrument.isOfType("Instrument")) {
            throw new IllegalArgumentException("Instrument should be of type instrument.");
            // todo: log as result
        }

        // Does the instrument even work on the given origin?
        if (instrument.getHandledTypes().stream()
                .filter(type -> origin.isOfType(type.getName()))
                .count() < 1) {
            throw new IllegalArgumentException("Instrument is incompatible with the given origin.");
            // todo: log as result
        }

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
            Set<InstanceNode> sendStringInstruments = instanceNodeRepository.getCompatibleSendString(agent.getUuid(), subject.getUuid());
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

            Set<InstanceNode> sendFileInstruments = instanceNodeRepository.getCompatibleSendFile(agent.getUuid(), subject.getUuid());
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
                // create script file, this will put the file metadata in our static arguments
                createScript(instrument, invokableInterpreter);
                // send file to subject (assumes sendFile + receiveFile)
                jobList.add(sendFile(heaviestSendFile));
                // send execution to subject (assumes sendString + receiveString)
                jobList.add(sendString(heaviestSendString));

            } catch (Exception e) {
                log.error(e.getMessage());
                throw new RuntimeException("Error while creating script to transfer for instrument " + this.instrument.getName()
                        + ". See logging for more details.");
            }
        } else {
            // Check if the instrument is connected to structure via HAS_INSTRUMENT
            if (instanceNodeRepository.validateNonLocalInstrument(instrument.getUuid(), agent.getUuid(), origin.getUuid()) == null) {
                log.error("We can't run instrument \"" + instrument.getName() + "\", because \"" + agent.getName() + "\" [" + agent.getUuid() + "] " +
                        "doesn't have \"" + instanceNodeRepository.findInstrumentOwner(instrument.getUuid()) + "\".");
                // todo: log as result
            } else {
                log.debug("Valid, non-local path");
                try {
                    // TODO: create method
                    ObjectNode args = validateAndSelectArguments(instrument);
                    jobList.add(applyTemplate(instrument.getProperty(templateProperty), args));
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
     * @return An execution string.
     * @throws IOException              HandleBars might throw an IOException while parsing the template.
     * @throws IllegalArgumentException Context doesn't pass argument validation.
     */
    private String sendFile(InstanceNode sendFileInstrument) throws IOException, IllegalArgumentException {
        ObjectNode arguments = validateAndSelectArguments(sendFileInstrument);
        if (arguments != null) {
            return applyTemplate(sendFileInstrument.getProperty(templateProperty), arguments);
        } else {
            throw new IllegalArgumentException("Error while checking arguments.");
        }
    }

    /**
     * Given an instrument and the context (which consists of the origin, subject, agent and arguments)
     * return a filtered context usable in handlebars templates.
     * @param instrument The instrument we want to use the context in.
     * @return A properly formatted context, or null in case the instrument doesn't have an arguments object.
     */
    private ObjectNode validateAndSelectArguments(InstanceNode instrument) {
        // Result will contain the filtered result set of our context
        ObjectNode result;

        //
        // Create the context we're working with.
        //
        Set<JsonNode> context = new HashSet<>();
        // Add a _from field, before we collect items in our result object,
        // so we know where data came from. This is used for filtering.
        BiConsumer<JsonNode, String> collect = (arg, from) -> {
            ((ObjectNode)arg).put("_from", from);
            context.add(arg);
        };
        // convert (copies of) the origin, subject and agent trees and add them to the context
        subject.toArguments().forEach(arg -> collect.accept(arg, "subject"));
        agent.toArguments().forEach(arg -> collect.accept(arg, "agent"));
        ObjectNode originCopy = ((ObjectNode)origin.toFlatJson().deepCopy()).put("_from", "origin");
        context.add(originCopy);
        ObjectNode argumentsCopy = arguments.deepCopy();
        // create a copy of the given statically provided arguments
        // and add them to our context in a way we can filter
        argumentsCopy.fields().forEachRemaining(entry -> {
            ObjectNode node = mapper.createObjectNode()
                    .put("name", entry.getKey())
                    .put("data", entry.getValue().asText());
            collect.accept(node, "static");
        });

        //
        // Transform the instrument's arguments object into a classification tree.
        // After that, we start filtering using the class. tree, and check for missing
        // data in our filtered context.
        //
        JsonNode arguments;
        String tmpArguments = instrument.getProperty("arguments");
        // If we don't even have arguments, return right away.
        if (tmpArguments == null) {
            return null;
        }

        try {
            // Prepare objects
            arguments = mapper.readTree(tmpArguments);
            ArgumentsToClassificationTreeParser.ClassificationTreeObjects classTreeObjects =
                    ArgumentsToClassificationTreeParser.asTree((ObjectNode)arguments);
            RootNode<JsonNode> startOfClassificationTree = classTreeObjects.root;
            // Start classifying
            startOfClassificationTree.consumeInput(context);
            result = classTreeObjects.result;
            // Were there any failures?
            if (classTreeObjects.failed.size() > 0) {
                throw new RuntimeException("Validation of arguments " + classTreeObjects.failed.toString()
                        + " failed while running instrument " + this.instrument.getName() + " for task [ " + taskId + "].");
            } else if (classTreeObjects.askUser == null || classTreeObjects.askUser.size() > 0) {
                // todo: implement this. when we finish calling instruments from UI we'll have a better grasp at what & how
            } else {
                // Check for missing arguments, taking default values if provided
                final String optionalKey = "isOptional";
                final String defaultValueKey = "data";
                Set<String> missingKeys = new HashSet<>();
                Iterator<Map.Entry<String, JsonNode>> argIter = arguments.fields();
                while (argIter.hasNext()) {
                    Map.Entry<String, JsonNode> arg = argIter.next();
                    String key = arg.getKey();
                    JsonNode value = arg.getValue();
                    boolean missing = result.get(key) == null;
                    if (missing) {
                        JsonNode data = value.get("value").get(defaultValueKey);
                        if (data != null) {
                            result.set(key, data);
                        } else {
                            if (value.get(optionalKey) == null || !value.get(optionalKey).asBoolean()) {
                                missingKeys.add(key);
                            }
                        }
                    }
                }
                if (missingKeys.size() > 0) {
                    throw new RuntimeException("Missing or invalid parameter(s) " + missingKeys.toString() + " while running instrument "
                            + this.instrument.getName() + "->" + instrument.getName()
                            + " for task [" + taskId + "].");
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException("Ran into an unknown exception while parsing instrument " + this.instrument.getName()
                    +"'s arguments object. See logging for more details.");
        }
        return result;
    }

    /**
     * Create a send string execution string using the given SendFile instrument and context.
     *
     * @param sendStringInstrument An instrument that implements the SendFile capability.
     * @return An execution string.
     * @throws IOException              HandleBars might throw an IOException while parsing the template.
     * @throws IllegalArgumentException Context doesn't pass argument validation.
     */
    private String sendString(InstanceNode sendStringInstrument) throws IOException {
        ObjectNode arguments = validateAndSelectArguments(sendStringInstrument);
        if (arguments != null) {
            return applyTemplate(sendStringInstrument.getProperty(templateProperty), arguments);
        } else {
            throw new IllegalArgumentException("Error while checking arguments.");
        }
    }

    /**
     * Create a script to be run on a remote host.
     *
     * @param instrument           The instrument to run.
     * @param invokableInterpreter The interpreter to wrap the command in.
     * @return The context with added values from both the instrument and invokableInterpreter.
     * @throws IOException Is thrown when we run into trouble parsing the handlebars template.
     */
    private void createScript(InstanceNode instrument, InstanceNode invokableInterpreter) throws IOException {
        ObjectNode context = validateAndSelectArguments(instrument);
        String script = applyTemplate(instrument.getProperty(templateProperty), context);
        arguments.put("script", script);
        context = validateAndSelectArguments(invokableInterpreter);
        script = applyTemplate(invokableInterpreter.getProperty(templateProperty), context);

        String scriptFilename = origin.getName() + "-" + instrument.getName() + "_" + System.currentTimeMillis()
                + invokableInterpreter.getProperty("extension");
        FileWriter scriptWriter = createFileWriter(scriptFilename);
        scriptWriter.write(script);
        scriptWriter.close();

        arguments.put("payloadDirectory", workingDirectory);
        arguments.put("payloadFileName", scriptFilename);
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
