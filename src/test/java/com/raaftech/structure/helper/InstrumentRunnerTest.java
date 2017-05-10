package com.raaftech.structure.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.*;
import com.raaftech.structure.graph.repositories.GroupingRepository;
import com.raaftech.structure.graph.repositories.InstanceRepository;
import com.raaftech.structure.graph.repositories.InstrumentRepository;
import mockit.*;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
    Grouping grouping;
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

    @Test
    public void testScriptInstrument() throws Exception {
        final String originUuid = UUID.randomUUID().toString();
        final String agentUuid = UUID.randomUUID().toString();
        final String subjectUuid = UUID.randomUUID().toString();
        final String sendStringUuid = UUID.randomUUID().toString();
        final String sendFileUuid = UUID.randomUUID().toString();
        final String handledType = "HandledType";
        final String calledControllerName = "MockedScriptController";
        final Optional<JsonNode> calledControllerArguments = Optional.of(mapper.readTree(
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
        ));

        new StrictExpectations() {{
            // Return a mocked grouping when querying the "structure" grouping
            groupingRepository.findByUuid(Context.getSelfIdentifier());
            result = grouping;

            // Set the UUID of the origin to originUuid
            origin.getUuid();
            result = originUuid;

            // We'll query the Grouping the origin is part of and return it
            instanceRepository.getStartOfComponentGroup(originUuid);
            result = grouping;

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

            // Next two statements are called in getCompatibleSendString

            // Return the agent's UUID
            grouping.getUuid();
            result = agentUuid;

            // Return the subject's UUID
            grouping.getUuid();
            result = subjectUuid;

            /*
            Return a compatible send string controller receive string controller pair,
            these are needed to establish a path between structure and the subject over
            which we can execute a command.
             */
            instrumentRepository.getCompatibleSendString(agentUuid, subjectUuid);
            result = ImmutableSet.of(controller);

            controller.getUuid();
            result = sendStringUuid;

            instrumentRepository.getMatchingReceiveString(sendStringUuid);
            result = marker;

            controller.getName();
            result = "MockedSendString";
            marker.getName();
            result = "MockedReceiveString";

            // Next two statements are called in getCompatibleSendString

            // Return the agent's UUID
            grouping.getUuid();
            result = agentUuid;

            // Return the subject's UUID
            grouping.getUuid();
            result = subjectUuid;

            // Same as above, but with {send,receive} file
            instrumentRepository.getCompatibleSendFile(anyString, anyString);
            result = ImmutableSet.of(controller);

            controller.getUuid();
            result = sendFileUuid;

            instrumentRepository.getMatchingReceiveFile(sendFileUuid);
            result = marker;

            controller.getName();
            result = "MockedSendFile";
            marker.getName();
            result = "MockedReceiveFile";

            // Return an interpreter. THIS TRIGGERS THE SCRIPT ROUTINE.
            controller.getInterpreter();
            result = Optional.of("Bash");

            // Return some script content
            controller.getScriptContent();
            result = Optional.of("#!/bin/bash\\necho foo");

            /*
             The subject is now queried to see if it has the needed interpreter, because
             we need it to execute the script.
              */
            grouping.getUuid();
            result = subjectUuid;
            groupingRepository.getChildrenOfType(subjectUuid, "Bash");
            result = ImmutableSet.of(instance);

            /*
            A script file containing the script will now be created on the filesystem, and its
            location will be pushed to the context.
             */
            origin.getName();
            result = "bash";
            controller.getName();
            result = calledControllerName;
            controller.getExtension();
            result = Optional.of("sh");

            // Inside validateAndSelectArguments
            controller.getArguments();
            result = calledControllerArguments;
            controller.getName();
            result = calledControllerName;

            // subject.toArguments
            grouping.toArguments();
            result = ImmutableSet.of(
                    mapper.createObjectNode()
                            .put("_hierarchyPath", "type.component.software.server.httpserver")
                            .put("shortName", "testShortName")
            );

            // agent.toArguments
            grouping.toArguments();
            result = Collections.EMPTY_SET;

            // origin.toJson
            origin.toJson();
            result = mapper.createObjectNode()
                    .put("name", "Origin");

            // Inside createArgumentList
            controller.getArguments();
            result = calledControllerArguments;

            // SendFile's validateAndSelectArguments, in this case we do need to return something
            controller.getArguments();
            result = Optional.of(mapper.readTree(
                    "{\"payloadDirectory\": {" +
                            "        \"value\": {" +
                            "          \"from\": \"static\"," +
                            "          \"type\": \"string\"," +
                            "          \"strategy\": \"mustBeOne\"" +
                            "        }," +
                            "        \"canBeUserSupplied\": false," +
                            "        \"isOptional\": false" +
                            "      }}"
            ));
            controller.getName();
            result = calledControllerName;

            // subject.toArguments
            grouping.toArguments();
            result = ImmutableSet.of(
                    mapper.createObjectNode()
                            .put("_hierarchyPath", "type.component.software.server.httpserver")
                            .put("shortName", "testShortName")
            );

            // agent.toArguments
            grouping.toArguments();
            result = Collections.EMPTY_SET;

            // origin.toJson
            origin.toJson();
            result = mapper.createObjectNode()
                    .put("name", "Origin");

            controller.getTemplateContent();
            result = Optional.of("scp somedirectory");

            // SendString's validateAndSelectArguments, in this case we do need to return something
            controller.getArguments();
            result = Optional.of(mapper.readTree(
                    "{\"payloadFileName\": {" +
                            "        \"value\": {" +
                            "          \"from\": \"static\"," +
                            "          \"type\": \"string\"," +
                            "          \"strategy\": \"mustBeOne\"" +
                            "        }," +
                            "        \"canBeUserSupplied\": false," +
                            "        \"isOptional\": false" +
                            "      }}"
            ));

            controller.getName();
            result = calledControllerName;

            // subject.toArguments
            grouping.toArguments();
            result = ImmutableSet.of(
                    mapper.createObjectNode()
                            .put("_hierarchyPath", "type.component.software.server.httpserver")
                            .put("shortName", "testShortName")
            );

            // agent.toArguments
            grouping.toArguments();
            result = Collections.EMPTY_SET;

            // origin.toJson
            origin.toJson();
            result = mapper.createObjectNode()
                    .put("name", "Origin");

            controller.getTemplateContent();
            result = Optional.of("ssh -ltoor someserver \"{{ payloadFileName }}\"");

            controller.getTimeAllocated();
            result = Optional.of(5000);
        }};

        InstrumentRunner instrumentRunner = new InstrumentRunner(
                origin,
                controller,
                mapper.createObjectNode(),
                "TASKID"
        );

        List<String> jobs = instrumentRunner.getJobs().stream().map(Pair::getLeft).collect(Collectors.toList());
        assertNotNull(jobs);
        assertEquals(2, jobs.size());
        assertEquals("scp somedirectory", jobs.get(0));
        Pattern pattern = Pattern.compile("ssh -ltoor someserver " +
                    "\"bash-MockedScriptController_[0-9]{13}.sh\"" +
                    " '--option' 'testShortName'");
        assertTrue(pattern.asPredicate().test(jobs.get(1)));
    }

}