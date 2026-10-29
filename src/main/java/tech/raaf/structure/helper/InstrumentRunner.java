package tech.raaf.structure.helper;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.jackson.JsonNodeValueResolver;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.raaf.structure.base.Configuration;
import tech.raaf.structure.base.Configuration.Setting;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.graph.*;
import tech.raaf.structure.graph.repositories.GroupingRepository;
import tech.raaf.structure.graph.repositories.InstanceRepository;
import tech.raaf.structure.graph.repositories.InstrumentRepository;
import tech.raaf.structure.helper.classification.ArgumentsToClassificationTreeParser;
import tech.raaf.structure.helper.classification.tree.RootNode;
import tech.raaf.structure.helper.handlebars.Quote;
import tech.raaf.structure.helper.handlebars.ReachableAddress;
import tech.raaf.structure.helper.handlebars.SecretFile;
import tech.raaf.structure.helper.loader.BundleFiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * @author Boy van Duuren <boy@raaf.tech>
 */
public class InstrumentRunner {

  private InstanceRepository instanceRepository = Context.getInstanceRepository();
  private InstrumentRepository instrumentRepository = Context.getInstrumentRepository();
  private GroupingRepository groupingRepository = Context.getGroupingRepository();
  private final ObjectMapper mapper = new ObjectMapper();

  private final String taskId;
  private final Instance origin;
  private final Controller controller;
  private final Group subject;
  private final Group agent;
  // Arguments of the caller, which this class does not change
  private final ObjectNode suppliedArguments;
  // Static arguments that Structure gives, such as the script file
  private final ObjectNode generatedArguments = mapper.createObjectNode();

  // Commands run in state-dir, so they can use this path without quotes
  private static final String payloadDirectory = "generated-scripts" + File.separator;
  private final Path workingDirectory = Configuration.get(Setting.STATE_DIR).resolve(payloadDirectory);
  // Files of the run, which TaskManager deletes when the task is done
  private final List<Path> temporaryFiles = new ArrayList<>();
  private final SecretFile secretFile = new SecretFile(workingDirectory, temporaryFiles);
  // Set timeout of transport to 10000ms
  private static final Optional<Long> transportTimeout = Optional.of(10000L);

  // jobs will contain system commands, paired with timeouts, eventually sent to the agent with the TaskManager.
  private final ArrayList<Pair<String, Optional<Long>>> jobList = new ArrayList<>();
  private final String taskManagerAddress = "tech.raaf.structure.bus.manager.taskmanager";
  private final Logger log = LoggerFactory.getLogger(InstrumentRunner.class);

  /**
   * Construct the InstrumentRunner with default allocated time, do some checks, etc.
   *
   * @param controller The controller to use on the origin.
   * @param origin     The object undergoing the action.
   * @param arguments  Given arguments, can be null.
   * @param taskId     The ID used to track progress of this action.
   */
  public InstrumentRunner(Instance origin, Controller controller, ObjectNode arguments, String taskId) {
    this(origin, controller, arguments, taskId, -1);
  }

  /**
   * Construct the InstrumentRunner with overridden time allocation, do some checks, etc.
   *
   * @param controller The controller to use on the origin.
   * @param origin     The object undergoing the action.
   * @param arguments  Given arguments, can be null.
   * @param taskId     The ID used to track progress of this action.
   * @param timeAllocated Override for default time allocation. Value of -1 means default will be used.
   */
  public InstrumentRunner(
    Instance origin,
    Controller controller,
    ObjectNode arguments,
    String taskId,
    long timeAllocated
  ) {
    this.taskId = taskId;
    this.origin = origin;
    this.controller = controller;
    this.suppliedArguments = arguments == null ? mapper.createObjectNode() : arguments;

    this.agent = groupingRepository.findByUuid(Context.getSelfIdentifier());
    this.subject = instanceRepository.getStartOfComponentGroup(origin.getUuid());

    // Did we find the agent?
    if (agent == null) {
      throw new IllegalArgumentException(
        "Couldn't find node running Structure by uuid \"" + Context.getSelfIdentifier() + "\"."
      );
    }

    // Does the controller even work on the given origin?
    if (
      controller
        .getHandledTypes()
        .stream()
        .filter(type -> origin.isOfType(type.getName()))
        .count() < 1
    ) {
      throw new IllegalArgumentException("Instrument is incompatible with the given origin.");
      // todo: log as result
    }

    try {
      addJobs(timeAllocated);
    } catch (RuntimeException e) {
      // No task runs, so delete the files now
      for (Path file : temporaryFiles) {
        try {
          Files.deleteIfExists(file);
        } catch (IOException deleteError) {
          log.error(deleteError.getMessage());
        }
      }
      throw e;
    }
  }

  private void addJobs(long timeAllocated) {
    if (controller.isSubjectIsLocal()) {
      // run subject as local routine
      // we need to do all the checks needed for a local routine, which are:
      // do we have a protocol to send a file?
      // do we have a protocol to send a command?
      log.debug("local routine");

      if (subject == null) {
        throw new RuntimeException(
          "Could not find the group that " + origin.getName() + " [" + origin.getUuid() + "] belongs to"
        );
      }

      // Get a (set of) sendString instruments that are capable of sending strings from the agent to the subject
      // using a particular protocol (SSH, telnet, ...). This query only returns instruments that have a ReceiveString
      // counterpart in the subject speaking common protocol.
      Set<Controller> sendStringControllers = instrumentRepository.getCompatibleSendString(
        agent.getUuid(),
        subject.getUuid()
      );
      if (sendStringControllers.size() <= 0) {
        // todo: log
        throw new RuntimeException("No matching sendString protocol between structure and " + subject.getName());
      }

      // Because we may have multiple protocols giving us SendString capabilities, we want the "heaviest" one. For
      // instance, SSH is preferred over telnet on all occasions, so that has a heavier weight.
      final Controller heaviestSendString;
      if (sendStringControllers.size() > 1) {
        heaviestSendString = findHeaviest(sendStringControllers);
      } else {
        heaviestSendString = sendStringControllers.iterator().next();
      }
      // Get the matching ReceiveString controller on the subject.
      Marker matchingReceiveString = null;
      if (heaviestSendString != null) {
        matchingReceiveString = instrumentRepository.getMatchingReceiveString(heaviestSendString.getUuid());
      }

      // This shouldn't happen because if we don't have a heaviest SendString capability this routine has thrown
      // an exception at instanceRepository.getCompatibleSendString() already.
      // A SendString capability always has a matching ReceiveString capability on the subject in this routine, because
      // that is a constraint of the getCompatibleSendString() query as well.
      if (heaviestSendString == null || matchingReceiveString == null) {
        // todo: log
        throw new RuntimeException("No matching protocol between structure and " + subject.getName());
      } else {
        log.debug(
          "Using the following {Send,Receive}String pair for task [" +
            taskId +
            "]: " +
            heaviestSendString.getName() +
            ":" +
            matchingReceiveString.getName()
        );
      }

      Set<Controller> sendFileControllers = instrumentRepository.getCompatibleSendFile(
        agent.getUuid(),
        subject.getUuid()
      );
      if (sendFileControllers.size() <= 0) {
        throw new RuntimeException("No matching sendFile protocol between structure and " + subject.getName());
      }

      // Because we may have multiple protocols giving us SendFile capabilities, we want the "heaviest" one. For
      // instance, SSH is preferred over ftp on all occasions, so that has a heavier weight.
      final Controller heaviestSendFile;
      if (sendFileControllers.size() > 1) {
        heaviestSendFile = findHeaviest(sendFileControllers);
      } else {
        heaviestSendFile = sendFileControllers.iterator().next();
      }
      // Get the matching ReceiveFile controller on the subject.
      Marker matchingReceiveFile = null;
      if (heaviestSendFile != null) {
        matchingReceiveFile = instrumentRepository.getMatchingReceiveFile(heaviestSendFile.getUuid());
      }

      // This shouldn't happen because if we don't have a heaviest SendFile capability this routine has thrown
      // an exception at instanceRepository.getCompatibleSendFile() already.
      // A SendFile capability always has a matching ReceiveFile capability on the subject in this routine, because
      // that is a constraint of the getCompatibleSendFile() query as well.
      if (heaviestSendFile == null || matchingReceiveFile == null) {
        // todo: log
        throw new RuntimeException("No matching protocol between structure and " + subject.getName());
      } else {
        log.debug(
          "Using the following {Send,Receive}File pair for task [" +
            taskId +
            "]: " +
            heaviestSendFile.getName() +
            ":" +
            matchingReceiveFile.getName()
        );
      }

      /*
        The instrument we're running might be a detect instrument, in which case we need to transport
        Profile data as well.
      */
      final String capability = controller.getCapability().getName();
      if (capability.equals("Detect")) {
        try {
          collectAndTransportProfiles();
          jobList.add(Pair.of(sendFile(heaviestSendFile), transportTimeout));
        } catch (IOException e) {
          if (Configuration.isTraceModeOn()) {
            e.printStackTrace();
          }
          log.error(e.getMessage());
          throw new RuntimeException(
            "Error while saving flattened profiles to file while running " + controller.getName()
          );
        }
      }

      /*
        Now we have a way of transporting a script and executing that script, we look whether we have an
        appropriate interpreter for the script.
        There are two cases:
        1) The controller simply creates a command which needs to be execute by an interpreter, in which case
           we don't care which interpreter, as long as it can execute commands.
        2) The controller is a script, in which case it has a hard dependency on a particular interpreter.
      */

      Optional<String> scriptInterpreter = controller.getInterpreter();
      if (scriptInterpreter.isPresent()) {
        // The controller is a script. So we expect a "script" file, and an interpreter for the script on the subject.
        Optional<String> scriptContent = controller.getScriptContent();
        if (!scriptContent.isPresent()) {
          throw new RuntimeException(
            "Controller " + controller.getName() + " should have a script, but none was found."
          );
        }
        Set<Instance> interpreters = groupingRepository.getChildrenOfType(subject.getUuid(), scriptInterpreter.get());
        if (interpreters.size() < 1) {
          throw new RuntimeException(
            "Expected interpreter \"" +
              scriptInterpreter.get() +
              "\" to be available on subject, " +
              "but none was found"
          );
        }

        try {
          createScriptWithoutTemplating(scriptContent.get());
          ObjectNode filteredContext = validateAndSelectArguments(controller);
          List<String> argumentsToScript = createArgumentList(controller, filteredContext);
          log.debug("Found the following arguments: " + argumentsToScript);
          // ssh joins its arguments, so quote them for the remote shell too
          final String quotedArguments = argumentsToScript
            .stream()
            .map(Quote.SH::quote)
            .collect(Collectors.joining(" "));
          // send file to subject (assumes sendFile + receiveFile)
          jobList.add(Pair.of(sendFile(heaviestSendFile), transportTimeout));
          // send execution to subject (assumes sendString + receiveString)

          // If timeAllocated has no value assigned, use getTimeAllocated, else use timeAllocated
          if (timeAllocated == -1) {
            jobList.add(Pair.of(sendString(heaviestSendString) + " " + quotedArguments, controller.getTimeAllocated()));
          } else {
            jobList.add(Pair.of(sendString(heaviestSendString) + " " + quotedArguments, Optional.of(timeAllocated)));
          }
        } catch (IOException e) {
          log.error(e.getMessage());

          if (Configuration.isTraceModeOn()) {
            e.printStackTrace();
          }

          throw new RuntimeException(
            "Error while creating script to transfer for controller " +
              this.controller.getName() +
              ". See logging for more details."
          );
        }
      } else {
        Controller invokableInterpreter = instrumentRepository.getInvokableInterpreter(subject.getUuid());
        if (invokableInterpreter == null) {
          throw new RuntimeException(
            "Couldn't find an invokable interpreter on subject [" +
              subject.getUuid() +
              "]" +
              " for task [" +
              taskId +
              "]."
          );
        } else {
          log.debug(
            "Using the following Invokable controller for task [" +
              taskId +
              "]: " +
              invokableInterpreter.getName() +
              "."
          );
        }
        // We now have everything we need: an controller with an origin to run from, the subject, and ways
        // to send files (the script) and strings (the command to run the script)
        try {
          // create script file, this will put the file metadata in our static arguments
          createScript(controller, invokableInterpreter);
          // send file to subject (assumes sendFile + receiveFile)
          jobList.add(Pair.of(sendFile(heaviestSendFile), transportTimeout));
          // send execution to subject (assumes sendString + receiveString)

          // If timeAllocated has no value assigned, use getTimeAllocated, else use timeAllocated
          if (timeAllocated == -1) {
            jobList.add(Pair.of(sendString(heaviestSendString), controller.getTimeAllocated()));
          } else {
            jobList.add(Pair.of(sendString(heaviestSendString), Optional.of(timeAllocated)));
          }
        } catch (Exception e) {
          log.error(e.getMessage());

          if (Configuration.isTraceModeOn()) {
            e.printStackTrace();
          }

          throw new RuntimeException(
            "Error while creating script to transfer for controller " +
              this.controller.getName() +
              ". See logging for more details."
          );
        }
      }
    } else {
      // Check if the controller is connected to structure via PROVIDED_BY
      if (
        instrumentRepository.validateNonLocalInstrument(controller.getUuid(), agent.getUuid(), origin.getUuid()) == null
      ) {
        log.warn(
          "We can't run controller \"" +
            controller.getName() +
            "\", because \"" +
            agent.getName() +
            "\" [" +
            agent.getUuid() +
            "] " +
            "doesn't have \"" +
            instrumentRepository.findInstrumentOwner(controller.getUuid()) +
            "\"."
        );
        // todo: log as result
      } else if (!instrumentRepository.originHasServer(controller.getUuid(), origin.getUuid())) {
        log.warn(
          "We can't run controller \"" +
            controller.getName() +
            "\", because nothing that has \"" +
            origin.getName() +
            "\" [" +
            origin.getUuid() +
            "] has a server for the protocol of \"" +
            instrumentRepository.findInstrumentOwner(controller.getUuid()) +
            "\"."
        );
      } else {
        log.debug("Valid, non-local path");
        try {
          // TODO: create method
          ObjectNode args = validateAndSelectArguments(controller);

          // If timeAllocated has no value assigned, use getTimeAllocated, else use timeAllocated
          if (timeAllocated == -1) {
            jobList.add(
              Pair.of(applyTemplate(controller.getTemplateContent().get(), args), controller.getTimeAllocated())
            );
          } else {
            jobList.add(
              Pair.of(applyTemplate(controller.getTemplateContent().get(), args), Optional.of(timeAllocated))
            );
          }
        } catch (IOException e) {
          if (Configuration.isTraceModeOn()) {
            e.printStackTrace();
          }
        }
      }
    }
  }

  /**
   * This is only called in case we're doing a detect. We query all the profiles that might match the origin,
   * flatten them, then transport them as a single file to the subject.
   * @return The location of the file we saved the flattened profiles in.
   */
  private void collectAndTransportProfiles() throws IOException {
    final String instanceOf = origin.getInstanceOf().getUuid();
    final List<Profile> applicableProfiles = Context.getProfileRepository().findAllProfilesForType(instanceOf);
    final StringBuilder flattenedProfiles = new StringBuilder();

    final String profilesIndex = applicableProfiles
      .stream()
      .map(Profile::getAppliesTo)
      .map(Node::getName)
      .collect(Collectors.joining(","));

    flattenedProfiles.append("PROFILES_INDEX=\"").append(StringEscapeUtils.escapeJava(profilesIndex)).append("\"\n");

    flattenedProfiles.append(
      applicableProfiles.stream().map(Profile::asDetectionConsumable).collect(Collectors.joining("\n"))
    );

    final String outputFile = constructScriptFilename(origin.getName(), controller.getName(), "profile");
    copyStringToFile(flattenedProfiles.toString(), outputFile);

    // This is nasty because these properties will be reused later on L365 and L645
    generatedArguments.put("payloadDirectory", payloadDirectory);
    generatedArguments.put("payloadFileName", outputFile);

    generatedArguments.put("profiles", outputFile);
  }

  /**
   * Simply create a script file from a string without using a templating engine.
   * @param script The string to use as content for the script.
   * @throws IOException Thrown when there are any issues creating the script.
   */
  private void createScriptWithoutTemplating(String script) throws IOException {
    // create script file, this will put the file metadata in our static arguments
    final String scriptFilename = constructScriptFilename(
      origin.getName(),
      controller.getName(),
      controller.getExtension().orElse("out")
    );
    copyStringToFile(script, scriptFilename);

    generatedArguments.put("payloadDirectory", payloadDirectory);
    generatedArguments.put("payloadFileName", scriptFilename);
  }

  private String constructScriptFilename(String originName, String controllerName, String extension) {
    return (
      new StringBuilder()
        .append(originName)
        .append("-")
        .append(controllerName)
        .append("_")
        .append(System.currentTimeMillis())
        .append(".")
        .append(extension)
        .toString()
        // Use only characters that need no quotes in a command
        .replaceAll("[^\\w.-]", "_")
    );
  }

  /**
   * Create a send file execution string using the given SendFile controller and context.
   *
   * @param sendFileController An controller that implements the SendFile capability.
   * @return An execution string.
   * @throws IOException              HandleBars might throw an IOException while parsing the template.
   * @throws IllegalArgumentException Context doesn't pass argument validation.
   */
  private String sendFile(Controller sendFileController) throws IOException, IllegalArgumentException {
    ObjectNode arguments = validateAndSelectArguments(sendFileController);
    final Optional<String> templateContent = sendFileController.getTemplateContent();
    if (arguments != null && templateContent.isPresent()) {
      return applyTemplate(templateContent.get(), arguments, Quote.SH, sendFileController);
    } else {
      throw new IllegalArgumentException(
        "Couldn't create a command to transfer the constructed file" +
          " because either we couldn't find a filename in the context, or we don't have a transfer method."
      );
    }
  }

  /**
   * Given a controller and a filtered context, create an ordered argument list that can be passed
   * to a script.
   * @param controller A controller which will be used as a script, not a command.
   * @param context The context.
   * @return An ordered list containing arguments as they can be passed along to a script.
   */
  private List<String> createArgumentList(Controller controller, ObjectNode context) {
    final String key = "key";
    final String pass = "pass";
    final String index = "index";
    final String option = "option";

    Optional<ObjectNode> arguments = controller.getArguments();

    // Check if we have everything we need
    if (!arguments.isPresent() || context == null) {
      log.warn("Cannot create an argument list, either no selection defined for controller or context is null");
      return ImmutableList.of();
    }

    /*
      When we filter something from the context that is meant to be passed as an argument to the script, that
      filter definition needs to have a "pass" object. Filter what we need from the controller's arguments.
    */
    BiMap<String, JsonNode> toPass = HashBiMap.create();
    Iterator<Map.Entry<String, JsonNode>> iter = arguments.get().properties().iterator();
    while (iter.hasNext()) {
      Map.Entry<String, JsonNode> entry = iter.next();
      JsonNode selector = entry.getValue();
      if (selector.has(pass)) {
        toPass.put(entry.getKey(), selector);
      }
    }
    log.debug("Found the following keys used for argument passing: " + toPass);

    Function<JsonNode, String> propertySelector = selector -> {
      final String contextKey = toPass.inverse().get(selector);
      // We may have a particular property we're interested in, or we just want the whole object
      final Optional<String> contextProperty = selector.get(pass).has(key)
        ? Optional.of(selector.get(pass).get(key).asString())
        : Optional.empty();
      /*
        If contextProperty is present, get the value of the key we want from the context.
        If it's not, just cast the whole object to a string and pass that in stead.
        Also, if we cannot find the object or property at all, it's selection was marked as optional, and we'll
        just return an empty string.
      */
      final String contextValue;
      if (context.has(contextKey)) {
        if (context.get(contextKey).isArray()) {
          throw new RuntimeException(
            "While constructing arguments to pass to controller \"" +
              controller.getName() +
              "\" we got an array in stead of an object"
          );
        }
        if (contextProperty.isPresent()) {
          contextValue = context.get(contextKey).has(contextProperty.get())
            ? context.get(contextKey).get(contextProperty.get()).asString()
            : "";
        } else {
          contextValue = context.get(contextKey).toString();
        }
      } else {
        contextValue = "";
      }

      final String arg;
      if (selector.get(pass).has(option)) {
        arg = Quote.SH.quote(selector.get(pass).get(option).asString()) + " " + Quote.SH.quote(contextValue);
      } else {
        arg = Quote.SH.quote(contextValue);
      }

      return arg;
    };

    return toPass
      .values()
      .stream()
      // Options should be passed first, indices should be sorted on numerical value ascending
      .sorted((s1, s2) -> {
        JsonNode pass1 = s1.get(pass);
        JsonNode pass2 = s2.get(pass);
        if (pass1.has(index) && pass2.has(option)) {
          return 1;
        } else if (pass1.has(index) && pass2.has(index)) {
          return pass1.get(index).asInt() - pass2.get(index).asInt() > 0 ? 1 : -1;
        } else {
          return 0;
        }
      })
      .map(propertySelector)
      .collect(Collectors.toList());
  }

  /**
   * Given an controller and the context (which consists of the origin, subject, agent and arguments)
   * return a filtered context usable in handlebars templates.
   * @param controller The controller we want to use the context in.
   * @return A properly formatted context, or null in case the controller doesn't have an arguments object.
   */
  private ObjectNode validateAndSelectArguments(Controller controller) {
    // Result will contain the filtered result set of our context
    ObjectNode result;

    Optional<ObjectNode> tmpArguments = controller.getArguments();
    // If we don't even have arguments, return right away.
    if (!tmpArguments.isPresent()) {
      return null;
    }

    log.debug("Validating and selecting arguments for \"{}\"", controller.getName());
    refuseUnsuppliable(controller, tmpArguments.get(), suppliedArguments);

    //
    // Create the context we're working with.
    //
    Set<JsonNode> context = new HashSet<>();
    // Add a _from field, before we collect items in our result object,
    // so we know where data came from. This is used for filtering.
    BiConsumer<JsonNode, String> collect = (arg, from) -> {
      ((ObjectNode) arg).put("_from", from);
      context.add(arg);
    };
    // convert (copies of) the origin, subject and agent trees and add them to the context
    if (subject != null) {
      subject.toArguments().forEach(arg -> collect.accept(arg, "subject"));
    }
    agent.toArguments().forEach(arg -> collect.accept(arg, "agent"));
    ObjectNode originCopy = origin.toJson().deepCopy().put("_from", "origin");
    context.add(originCopy);
    // Structure gives the other static arguments, its values win
    final ObjectNode staticArguments = suppliedArguments.deepCopy();
    staticArguments.setAll(generatedArguments);
    // add the static arguments to our context in a way we can filter
    staticArguments.properties().forEach(entry -> {
      ObjectNode node = mapper.createObjectNode().put("name", entry.getKey()).put("data", entry.getValue().asString());
      collect.accept(node, "static");
    });

    //
    // Transform the controller's arguments object into a classification tree.
    // After that, we start filtering using the class. tree, and check for missing
    // data in our filtered context.
    //

    // Prepare objects
    JsonNode arguments = tmpArguments.get();
    ArgumentsToClassificationTreeParser.ClassificationTreeObjects classTreeObjects =
      ArgumentsToClassificationTreeParser.asTree((ObjectNode) arguments);
    RootNode<JsonNode> startOfClassificationTree = classTreeObjects.root;
    // Start classifying
    startOfClassificationTree.consumeInput(context);
    result = classTreeObjects.result;
    // Were there any failures?
    if (classTreeObjects.failed.size() > 0) {
      throw new RuntimeException(
        "Validation of arguments " +
          classTreeObjects.failed.toString() +
          " failed while running controller " +
          this.controller.getName() +
          " for task [ " +
          taskId +
          "]."
      );
    } else if (classTreeObjects.askUser == null || classTreeObjects.askUser.size() > 0) {
      // todo: implement this. when we finish calling instruments from UI we'll have a better grasp at what & how
    } else {
      // Check for missing arguments, taking default values if provided
      final String optionalKey = "isOptional";
      final String defaultValueKey = "defaultValue";
      Set<String> missingKeys = new HashSet<>();
      Iterator<Map.Entry<String, JsonNode>> argIter = arguments.properties().iterator();
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
        throw new RuntimeException(
          "Missing or invalid parameter(s) " +
            missingKeys.toString() +
            " while running controller " +
            this.controller.getName() +
            "->" +
            controller.getName() +
            " for task [" +
            taskId +
            "]."
        );
      }
    }

    // A value can be a password, so log values only when unsafe
    log.debug(
      "Selected the following arguments: {}",
      Configuration.get(Setting.UNSAFE) ? result : ImmutableList.copyOf(result.propertyNames())
    );

    return result;
  }

  /**
   * Refuse each argument of the caller that the controller declares
   * without canBeUserSupplied. Structure gives these values itself.
   *
   * @param controller The controller that declares the arguments.
   * @param declared   The arguments object of the controller.
   * @param supplied   The arguments of the caller.
   */
  static void refuseUnsuppliable(Controller controller, ObjectNode declared, ObjectNode supplied) {
    declared.properties().forEach(entry -> {
      if (supplied.has(entry.getKey()) && !entry.getValue().path("canBeUserSupplied").asBoolean()) {
        throw new IllegalArgumentException(
          "A caller cannot supply argument \"" + entry.getKey() + "\" of controller " + controller.getName() + "."
        );
      }
    });
  }

  /**
   * Create a send string execution string using the given SendFile controller and context.
   *
   * @param sendStringInstrument An controller that implements the SendFile capability.
   * @return An execution string.
   * @throws IOException              HandleBars might throw an IOException while parsing the template.
   * @throws IllegalArgumentException Context doesn't pass argument validation.
   */
  private String sendString(Controller sendStringInstrument) throws IOException {
    ObjectNode arguments = validateAndSelectArguments(sendStringInstrument);
    final Optional<String> templateContent = sendStringInstrument.getTemplateContent();
    if (arguments != null && templateContent.isPresent()) {
      return applyTemplate(templateContent.get(), arguments, Quote.SH, sendStringInstrument);
    } else {
      throw new IllegalArgumentException("Error while checking arguments.");
    }
  }

  // Write a file that only the owner can read and run
  private void copyStringToFile(String content, String name) throws IOException {
    Files.createDirectories(workingDirectory);
    final Path file = Files.createFile(
      workingDirectory.resolve(name),
      PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"))
    );
    temporaryFiles.add(file);
    Files.write(file, content.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Create a script to be run on a remote host.
   *
   * @param controller           The controller to run.
   * @param invokableInterpreter The interpreter to wrap the command in.
   * @return The context with added values from both the controller and invokableInterpreter.
   * @throws IOException Is thrown when we run into trouble parsing the handlebars template.
   */
  private void createScript(Controller controller, Controller invokableInterpreter) throws IOException {
    final Optional<String> controllerTemplateContent = controller.getTemplateContent();
    final Optional<String> interpreterTemplateContent = invokableInterpreter.getTemplateContent();
    if (!(controllerTemplateContent.isPresent() && interpreterTemplateContent.isPresent())) {
      throw new IllegalArgumentException(
        "Cannot create a script for \"" +
          controller.getName() +
          "\" and" +
          " \"" +
          invokableInterpreter.getName() +
          "\", either one or both are missing a template."
      );
    }
    ObjectNode context = validateAndSelectArguments(controller);
    // The shell of the interpreter runs the script, so quote for it
    final Quote quote = Quote.forShell(invokableInterpreter.getExtension().orElse(""));
    String script = applyTemplate(controllerTemplateContent.get(), context, quote);
    generatedArguments.put("script", script);
    context = validateAndSelectArguments(invokableInterpreter);
    script = applyTemplate(interpreterTemplateContent.get(), context, Quote.SH, invokableInterpreter);

    final String scriptFilename = constructScriptFilename(
      origin.getName(),
      controller.getName(),
      controller.getExtension().orElse("out")
    );
    copyStringToFile(script, scriptFilename);

    generatedArguments.put("payloadDirectory", payloadDirectory);
    generatedArguments.put("payloadFileName", scriptFilename);
  }

  /**
   * Pass findHeaviest a set of Instruments and it'll find the heaviest by weight.
   * If we have multiple controllers with the same weight the one emitted from the stream first will
   * be used.
   *
   * @param controllers A set of Instances that are of the controller type. Other types are filtered.
   * @return The heaviest controller.
   */
  private Controller findHeaviest(Set<Controller> controllers) {
    // Collect controllers by weight filtering out duplicate weights
    Map<Integer, Controller> controllersByWeight = controllers
      .stream()
      .collect(
        Collectors.toMap(
          controller -> controller.getWeight(),
          instrument -> instrument,
          (instrument1, instrument2) -> instrument1
        )
      );

    // Find the maximum weight
    Optional<Integer> heaviestController = controllersByWeight.keySet().stream().max(Double::compare);

    if (heaviestController.isPresent()) {
      return controllersByWeight.get(heaviestController.get());
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
    return applyTemplate(template, context, Quote.SH);
  }

  // As applyTemplate, but the sh helper quotes for the given shell
  private String applyTemplate(String template, JsonNode context, Quote sh) throws IOException {
    return applyTemplate(template, context, sh, controller);
  }

  private String applyTemplate(String template, JsonNode context, Quote sh, Controller source) throws IOException {
    // Give each value as it is, the quote helpers make it safe for a shell
    Handlebars handlebars = new Handlebars().with(value -> value.toString());
    handlebars.registerHelper("reachable", ReachableAddress.INSTANCE);
    handlebars.registerHelper("bundleFile", (value, options) ->
      BundleFiles.resolve(source.getUuid(), value.toString()).toString()
    );
    handlebars.registerHelper("sh", sh);
    handlebars.registerHelper("cmd", Quote.CMD);
    handlebars.registerHelper("secretFile", secretFile);
    Template compiledTemplate = handlebars.compileInline(template);

    // Create context with our JSON.
    com.github.jknack.handlebars.Context ctx = com.github.jknack.handlebars.Context.newBuilder(context)
      .resolver(JsonNodeValueResolver.INSTANCE)
      .build();

    // Apply the context to our template
    String appliedTemplate = compiledTemplate.apply(ctx).trim();
    log.debug("Mustache returned:\n{}", appliedTemplate);
    return appliedTemplate;
  }

  public List<Pair<String, Optional<Long>>> getJobs() {
    return jobList;
  }

  // Schedule the actual execution of the controller.
  // This will always be done sequentially, until we implement automatic hierarchical execution of instruments.
  public void startInstrument() {
    log.debug("Sending execution task of \"" + controller.getName() + "\" to TaskManager.");
    final EventBus eventBus = Context.getVertx().eventBus();
    ObjectNode message = mapper.createObjectNode();
    message.put("msgh", "queueTask");
    ObjectNode args = message.putObject("args");
    args.put("taskId", taskId);
    args.put("sequential", true);
    ArrayNode files = args.putArray("temporaryFiles");
    temporaryFiles.forEach(file -> files.add(file.toString()));
    ArrayNode jobs = args.putArray("jobs");
    jobList.stream().forEachOrdered(commandAndTimeout -> {
      ObjectNode job = mapper.createObjectNode();
      job
        .putObject("metadata")
        .put("originName", origin.getName())
        .put("originUuid", origin.getUuid())
        .put("instrumentName", controller.getName())
        .put("instrumentUuid", controller.getUuid())
        .put("capability", instrumentRepository.getInstrumentCapability(controller.getUuid()).getName());
      Optional<ObjectNode> maybeStateMap = controller.getStateMapAsJson();
      if (maybeStateMap.isPresent()) {
        ((ObjectNode) job.get("metadata")).set("stateMap", maybeStateMap.get());
      }
      Optional<Long> maybeTimeAllocated = commandAndTimeout.getRight();
      if (maybeTimeAllocated.isPresent()) {
        ((ObjectNode) job.get("metadata")).put("timeAllocated", maybeTimeAllocated.get());
      }
      job.put("command", commandAndTimeout.getLeft());
      jobs.add(job);
    });

    eventBus.send(taskManagerAddress, new JsonObject(message.toString()));
  }
}
