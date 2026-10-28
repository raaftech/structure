package tech.raaf.structure.helper;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.ImmutableSet;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import tech.raaf.structure.base.Configuration;
import tech.raaf.structure.base.Configuration.Setting;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.graph.*;
import tech.raaf.structure.graph.repositories.GroupingRepository;
import tech.raaf.structure.graph.repositories.InstanceRepository;
import tech.raaf.structure.graph.repositories.InstrumentRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@ExtendWith(JMockitExtension.class)
public class InstrumentRunnerTest {

  final ObjectMapper mapper = new ObjectMapper();

  @Mocked
  Context context;

  @Mocked
  InstanceRepository instanceRepository;

  @Mocked
  InstrumentRepository instrumentRepository;

  @Mocked
  GroupingRepository groupingRepository;

  @Mocked
  Group group;

  @Mocked
  Component origin;

  @Mocked
  Controller controller;

  @Mocked
  Marker marker;

  @Mocked
  Type type;

  @Mocked
  Instance instance;

  @Mocked
  Type capability;

  @Test
  public void testScriptInstrument() throws Exception {
    final String originUuid = UUID.randomUUID().toString();
    final String agentUuid = UUID.randomUUID().toString();
    final String subjectUuid = UUID.randomUUID().toString();
    final String sendStringUuid = UUID.randomUUID().toString();
    final String sendFileUuid = UUID.randomUUID().toString();
    final String handledType = "HandledType";
    final String calledControllerName = "MockedScriptController";
    final Optional<JsonNode> calledControllerArguments = Optional.of(
      mapper.readTree(
        "{\"httpd\": {" +
          "        \"value\": {" +
          "          \"from\": \"subject\"," +
          "          \"type\": \"type.component.software.server.httpserver\"," +
          "          \"strategy\": \"mustBeOne\"," +
          "          \"onError\": \"fail\"" +
          "        }," +
          "        \"canBeUserSupplied\": false," +
          "        \"isOptional\": false," +
          "        \"pass\": {" +
          "           \"option\": \"--option\"," +
          "           \"key\": \"shortName\"" +
          "          }" +
          "      }}"
      )
    );

    final Set<ObjectNode> subjectArguments = ImmutableSet.of(
      mapper
        .createObjectNode()
        .put("_hierarchyPath", "type.component.software.server.httpserver")
        .put("shortName", "testShortName")
    );
    final Optional<JsonNode> sendFileArguments = Optional.of(
      mapper.readTree(
        "{\"payloadDirectory\": {" +
          "        \"value\": {" +
          "          \"from\": \"static\"," +
          "          \"type\": \"string\"," +
          "          \"strategy\": \"mustBeOne\"" +
          "        }," +
          "        \"canBeUserSupplied\": false," +
          "        \"isOptional\": false" +
          "      }}"
      )
    );
    final Optional<JsonNode> sendStringArguments = Optional.of(
      mapper.readTree(
        "{\"payloadFileName\": {" +
          "        \"value\": {" +
          "          \"from\": \"static\"," +
          "          \"type\": \"string\"," +
          "          \"strategy\": \"mustBeOne\"" +
          "        }," +
          "        \"canBeUserSupplied\": false," +
          "        \"isOptional\": false" +
          "      }}"
      )
    );

    // One mock is all the controllers, thus results come in call order
    new Expectations() {
      {
        // Return a mocked group when querying the "structure" group
        groupingRepository.findByUuid(anyString);
        result = group;

        // The origin, and the Group the origin is part of
        origin.getUuid();
        result = originUuid;
        instanceRepository.getStartOfComponentGroup(originUuid);
        result = group;
        origin.getName();
        result = "bash";
        origin.toJson();
        result = mapper.createObjectNode().put("name", "Origin");

        /*
          Return a set of one type, where the name matches origin.isOfType. This
          simulates the controller as a controller working on origin because origin's
          type is in controller.getHandledTypes.
        */
        controller.getHandledTypes();
        result = ImmutableSet.of(type);
        type.getName();
        result = handledType;
        origin.isOfType(handledType);
        result = true;

        // Simulate a local controller. THIS TRIGGERS A LOCAL ROUTINE.
        controller.isSubjectIsLocal();
        result = true;

        /*
          The agent and the subject for the send string, the same for the send file,
          then the subject that must have the interpreter of the script.
        */
        group.getUuid();
        returns(agentUuid, subjectUuid, agentUuid, subjectUuid, subjectUuid);

        /*
          Return a compatible send string controller receive string controller pair,
          these are needed to establish a path between structure and the subject over
          which we can execute a command. The same for send file and receive file.
        */
        instrumentRepository.getCompatibleSendString(agentUuid, subjectUuid);
        result = ImmutableSet.of(controller);
        instrumentRepository.getMatchingReceiveString(sendStringUuid);
        result = marker;
        instrumentRepository.getCompatibleSendFile(anyString, anyString);
        result = ImmutableSet.of(controller);
        instrumentRepository.getMatchingReceiveFile(sendFileUuid);
        result = marker;
        controller.getUuid();
        returns(sendStringUuid, sendFileUuid);
        marker.getName();
        returns("MockedReceiveString", "MockedReceiveFile");

        // The send string, the send file, then the script controller
        controller.getName();
        returns("MockedSendString", "MockedSendFile", calledControllerName);

        controller.getCapability();
        result = capability;
        capability.getName();
        result = "State";

        // Return an interpreter. THIS TRIGGERS THE SCRIPT ROUTINE.
        controller.getInterpreter();
        result = Optional.of("Bash");
        controller.getScriptContent();
        result = Optional.of("#!/bin/bash\\necho foo");
        controller.getExtension();
        result = Optional.of("sh");
        groupingRepository.getChildrenOfType(subjectUuid, "Bash");
        result = ImmutableSet.of(instance);

        /*
          Two times for the script (validateAndSelectArguments and
          createArgumentList), then the send file and the send string.
        */
        controller.getArguments();
        returns(calledControllerArguments, calledControllerArguments, sendFileArguments, sendStringArguments);

        // The subject, then the agent, for each of the three controllers
        group.toArguments();
        returns(
          subjectArguments,
          Collections.EMPTY_SET,
          subjectArguments,
          Collections.EMPTY_SET,
          subjectArguments,
          Collections.EMPTY_SET
        );

        controller.getTemplateContent();
        returns(Optional.of("scp somedirectory"), Optional.of("ssh -ltoor someserver \"{{ payloadFileName }}\""));

        controller.getTimeAllocated();
        result = Optional.of(5000);
      }
    };

    ObjectNode arguments = mapper.createObjectNode();
    InstrumentRunner instrumentRunner = new InstrumentRunner(origin, controller, arguments, "TASKID");

    List<String> jobs = instrumentRunner.getJobs().stream().map(Pair::getLeft).collect(Collectors.toList());
    assertNotNull(jobs);
    assertEquals(2, jobs.size());
    assertEquals("scp somedirectory", jobs.get(0));
    // The remote shell parses the arguments again, so they have two quotes
    Matcher matcher = Pattern.compile(
      "ssh -ltoor someserver \"(bash-MockedScriptController_[0-9]{13}.sh)\" " +
        Pattern.quote("''\"'\"'--option'\"'\"' '\"'\"'testShortName'\"'\"''")
    ).matcher(jobs.get(1));
    assertTrue(matcher.matches(), jobs.get(1));
    // Only the owner can read and run the script
    Path script = Configuration.get(Setting.STATE_DIR).resolve("generated-scripts").resolve(matcher.group(1));
    assertEquals("rwx------", PosixFilePermissions.toString(Files.getPosixFilePermissions(script)));
  }

  @Test
  public void callerSuppliesOnlyArgumentsThatAllowIt() throws Exception {
    final ObjectNode declared = (ObjectNode) mapper.readTree(
      "{\"username\": {\"value\": {\"from\": \"static\"}, \"canBeUserSupplied\": true}," +
        " \"payloadDirectory\": {\"value\": {\"from\": \"static\"}}}"
    );
    // Arguments that the controller does not declare are for other controllers
    InstrumentRunner.refuseUnsuppliable(
      controller,
      declared,
      mapper.createObjectNode().put("username", "root").put("password", "secret")
    );
    try {
      InstrumentRunner.refuseUnsuppliable(controller, declared, mapper.createObjectNode().put("payloadDirectory", "/"));
      fail("A caller must not supply payloadDirectory");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("payloadDirectory"), e.getMessage());
    }
  }
}
