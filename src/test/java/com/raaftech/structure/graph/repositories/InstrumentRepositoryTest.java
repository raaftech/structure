package com.raaftech.structure.graph.repositories;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.base.SpringTestConfig;
import com.raaftech.structure.graph.*;
import com.raaftech.structure.helper.loader.EvenNaiverSorter;
import com.raaftech.structure.next.Entity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static io.vertx.core.json.Json.mapper;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = SpringTestConfig.class)
public class InstrumentRepositoryTest {

    @Autowired
    NodeRepository nodeRepository;
    @Autowired
    TypeRepository typeRepository;
    @Autowired
    InstrumentRepository instrumentRepository;
    @Autowired
    ComponentRepository componentRepository;
    @Autowired
    InstanceRepository instanceRepository;
    @Autowired
    Neo4jTemplate neo4jTemplate;
    @Autowired
    GraphDatabase graphDatabase;

    private Grouping structure;
    private Grouping host1;
    private Component host1OS;
    private Grouping host2;
    private Component host2OS;

    private static boolean databaseCreated = false;

    @Before
    @SuppressWarnings("Duplicates")
    public void setUpDatabase() throws Exception {
        if (!databaseCreated) {
            final String testBundle = "src/test/resources/bundles/core";
            com.raaftech.structure.base.Configuration
                    .setSchemaNamespace(FileSystems.getDefault().getPath(testBundle).toUri().resolve("types/"));
            EvenNaiverSorter evenNaiverSorter = new EvenNaiverSorter(testBundle);

            Transaction tx = graphDatabase.beginTx();
            try {
                evenNaiverSorter.getUpdateList(evenNaiverSorter.sortIt(evenNaiverSorter.readAllJson())).stream()
                        .map(node -> {
                            try {
                                return mapper.readValue(node.toString(), Entity.class);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return null;
                        })
                        .filter(node -> node != null)
                        .forEach(Entity::toGraph);
                tx.success();
                databaseCreated = true;
            } catch (Exception e) {
                tx.failure();
                throw e;
            } finally {
                tx.close();
            }
        }
    }

    @Before
    @Transactional
    public void setupComponents() throws IOException {
        // Create the simplest structure server representation
        Type groupingType = typeRepository.findOneByName("Grouping");
        assertNotNull(groupingType);
        ObjectNode groupingData = (ObjectNode) mapper.readTree(
                "{\n" +
                        "    \"desc\": \"A groupingType representing a computer.\",\n" +
                        "    \"name\": \"Computer\"\n" +
                        "}"
        );
        structure = new Grouping(groupingData, groupingType);
        Type genericLinuxType = typeRepository.findOneByName("Linux");
        assertNotNull(genericLinuxType);
        ObjectNode genericLinuxData = (ObjectNode) mapper.readTree(
                "{\n" +
                        "    \"desc\": \"A generic Linux distro\",\n" +
                        "    \"longName\": \"Generic Linux\",\n" +
                        "    \"name\": \"Generic Linux\",\n" +
                        "    \"shortName\": \"genericlinux\",\n" +
                        "    \"ver\": \"unknown\"\n" +
                        "}"
        );
        Component structureOS = new Component(genericLinuxData, genericLinuxType);

        Type sshClientType = typeRepository.findOneByName("OpenSshClient");
        ObjectNode sshClientData = (ObjectNode) mapper.readTree(
                "{\n" +
                        "    \"desc\": \"The OpenSSH client.\",\n" +
                        "    \"longName\": \"OpenSshClient\",\n" +
                        "    \"name\": \"OpenSshClient\",\n" +
                        "    \"shortName\": \"ssh\",\n" +
                        "    \"ver\": \"unknown\"\n" +
                        "}");
        Component structureSshClient = new Component(sshClientData, sshClientType);

        Type bashType = typeRepository.findOneByName("Bash");
        ObjectNode bashData = (ObjectNode) mapper.readTree(
                "{\n" +
                        "    \"desc\": \"The Bourne Again Shell.\",\n" +
                        "    \"longName\": \"The Bourne Again Shell.\",\n" +
                        "    \"name\": \"Bash\",\n" +
                        "    \"shortName\": \"bash\",\n" +
                        "    \"ver\": \"4.1.2(1)-release\"\n" +
                        "}"
        );
        Component bash = new Component(bashData, bashType);

        structureOS.addComponent(structureSshClient);
        structureOS.addComponent(bash);
        structure.getComponents().add(structureOS);
        structure.save();

        // Create the simplest host1 representation
        host1 = new Grouping(groupingData, groupingType);
        host1OS = new Component(genericLinuxData, genericLinuxType);

        Type sshServerType = typeRepository.findOneByName("SshServer");
        ObjectNode sshServerData = (ObjectNode) mapper.readTree(
                "{\n" +
                        "    \"desc\": \"An SSH server.\",\n" +
                        "    \"longName\": \"SshServer\",\n" +
                        "    \"name\": \"SshServer\",\n" +
                        "    \"shortName\": \"sshserver\",\n" +
                        "    \"ver\": \"unknown\"\n" +
                        "}");
        Component hostSshServer = new Component(sshServerData, sshServerType);

        Type systemdType = typeRepository.findOneByName("Systemd");
        ObjectNode systemdData = (ObjectNode) mapper.readTree(
                "{\n" +
                "    \"name\": \"Systemd\",\n" +
                "    \"longName\": \"Systemd\",\n" +
                "    \"shortName\": \"systemd\",\n" +
                "    \"desc\": \"Systemd is a suite of basic building blocks for a Linux system.\",\n" +
                "    \"ver\": \"1.0\"\n" +
                "}"
        );
        Component systemd = new Component(systemdData, systemdType);

        host1OS.addComponent(hostSshServer).addComponent(systemd);
        host1.getComponents().add(host1OS);
        host1.save();

        // Create the simplest host2 representation
        Type upstartType = typeRepository.findOneByName("Upstart");
        ObjectNode upstartData = (ObjectNode) mapper.readTree(
                "{\n" +
                        "    \"name\": \"Upstart\",\n" +
                        "    \"longName\": \"Upstart\",\n" +
                        "    \"shortName\": \"upstart\",\n" +
                        "    \"desc\": \"Upstart is an ... system is running. \",\n" +
                        "    \"ver\": \"1.0\"\n" +
                        "}"
        );
        Component upstart = new Component(upstartData, upstartType);

        host2 = new Grouping(groupingData, groupingType);
        host2OS = new Component(genericLinuxData, genericLinuxType);
        bash = new Component(bashData, bashType);
        hostSshServer = new Component(sshServerData, sshServerType);

        host2OS.addComponent(bash).addComponent(hostSshServer).addComponent(upstart);
        host2.getComponents().add(host2OS);
        host2.save();
    }

    @After
    @Transactional
    public void tearDown() throws Exception {
        neo4jTemplate.query("MATCH (:Component)-[r]-() DELETE r", null);
        neo4jTemplate.query("MATCH (n:Component) DELETE n", null);
        neo4jTemplate.query("MATCH (:Grouping)-[r]-() DELETE r", null);
        neo4jTemplate.query("MATCH (n:Grouping) DELETE n", null);
    }

    @Test
    public void getInstrumentCapability() throws Exception {
        Type bsdRcInit = typeRepository.findOneByName("BsdrcInit");
        assertNotNull(bsdRcInit);
        Controller stopController = null;
        for (Instrument instrument: bsdRcInit.getInstruments()) {
            if ("stop".equals(instrument.getName())) {
                stopController = (Controller) instrument;
            }
        }
        assertNotNull(stopController);
        assertEquals("Stop", instrumentRepository.getInstrumentCapability(stopController.getUuid()).getName());
    }

    @Test
    public void getCompatibleSendString() throws Exception {
        Set<Controller> compatible = instrumentRepository.getCompatibleSendString(structure.getUuid(), host1.getUuid());
        assertEquals(1, compatible.size());
        assertEquals("OpenSshClientSendString", compatible.iterator().next().getName());
    }

    @Test
    public void getCompatibleSendFile() throws Exception {
        Set<Controller> compatible = instrumentRepository.getCompatibleSendFile(structure.getUuid(), host1.getUuid());
        assertEquals(1, compatible.size());
        assertEquals("OpenSshClientSendFile", compatible.iterator().next().getName());
    }

    @Test
    @Transactional
    @Rollback
    public void getMatchingReceiveString() throws Exception {
        Controller sshSendString = (Controller) instrumentRepository.findOneByName("OpenSshClientSendString");
        assertNotNull(sshSendString);
        Marker compatible = instrumentRepository.getMatchingReceiveString(sshSendString.getUuid());
        assertNotNull(compatible);
        assertEquals("SshServerReceiveString", compatible.getName());
    }

    @Test
    public void getMatchingReceiveFile() throws Exception {
        Controller sshSendFile = (Controller) instrumentRepository.findOneByName("OpenSshClientSendFile");
        assertNotNull(sshSendFile);
        Marker compatible = instrumentRepository.getMatchingReceiveFile(sshSendFile.getUuid());
        assertNotNull(compatible);
        assertEquals("SshServerReceiveFile", compatible.getName());
    }

    @Test
    public void getInvokableInterpreter() throws Exception {
        Instrument invokable = instrumentRepository.getInvokableInterpreter(host1.getUuid());
        assertNull(invokable);
        invokable = instrumentRepository.getInvokableInterpreter(structure.getUuid());
        assertNotNull(invokable);
        assertEquals("BashInvoke", invokable.getName());
    }

    @Test
    @Transactional
    @Rollback
    public void purgeControllers() throws Exception {
        // get some counts so we can validate
        long nodeCount = StreamSupport.stream(nodeRepository.findAll().spliterator(), false).count();
        long instrumentCount = StreamSupport.stream(instrumentRepository.findAll().spliterator(), false).count();
        long nonInstrumentCount = nodeCount - instrumentCount;

        // purge
        instrumentRepository.purgeInstruments();
        assertEquals(0, StreamSupport.stream(instrumentRepository.findAll().spliterator(), false).count());
        nodeCount = StreamSupport.stream(nodeRepository.findAll().spliterator(), false).count();

        // check if we didn't accidentally remove more than we wanted to (NB: THIS DOES NOT COUNT RELATIONS)
        assertEquals(nonInstrumentCount, nodeCount);
    }

    @Test
    public void validateNonLocalInstrument() throws Exception {
        Set<Type> appliesTo = new HashSet<>();
        appliesTo.add(typeRepository.findOneByName("SshServer"));
        Controller nonLocal = new Controller.Builder()
                .subjectIsLocal(false)
                .name("nonlocal")
                .desc("nonlocal test instrument")
                .capability("State")
                .filePath("/tmp/foo")
                .handles(appliesTo)
                .providedBy(typeRepository.findOneByName("OpenSshClient"))
                .template("")
                .build();
        nonLocal.save();
        assertNotNull(instanceRepository.findByUuid(nonLocal.getUuid()));

        Set<Component> hostSshServers = componentRepository.findChildrenByType(host1OS.getUuid(), "SshServer");
        assertEquals(1, hostSshServers.size());
        Component hostSshServer = hostSshServers.iterator().next();
        assertNotNull(instrumentRepository.validateNonLocalInstrument(nonLocal.getUuid(), structure.getUuid(), hostSshServer.getUuid()));

        instanceRepository.delete(nonLocal.getId());
    }

    @Test
    public void findInstrumentOwner() throws Exception {
        Instrument instrument = instrumentRepository.findOneByName("OpenSshClientSendString");
        assertNotNull(instrument);
        assertEquals("OpenSshClient", instrumentRepository.findInstrumentOwner(instrument.getUuid()));
    }

    @Test
    public void findMatchingLocalController() throws Exception {
        // find the stop controllers
        final String instrumentName = "stop";
        Set<Instrument> instruments = instrumentRepository.findByName(instrumentName);
        assertTrue(instruments.size() > 1);

        // find the sshd belonging to host2
        final Component sshd = host2OS.getComponents().stream()
                .filter(c -> "SshServer".equals(c.getName()))
                .collect(Collectors.toList())
                .get(0);
        assertNotNull(sshd);

        Controller upstartStop = instrumentRepository.findMatchingLocalController(sshd.getUuid(), host2.getUuid(), instrumentName);
        assertNotNull(upstartStop);
        assertEquals("Upstart", upstartStop.getProvidedBy().getName());
    }

    @Test
    public void findLocalControllers() throws Exception {
        // find the sshd belonging to host2
        final Component sshd = host2OS.getComponents().stream()
                .filter(c -> "SshServer".equals(c.getName()))
                .collect(Collectors.toList())
                .get(0);
        assertNotNull(sshd);

        Set<Controller> controllers = instrumentRepository.findLocalControllers(sshd.getUuid());
        // expect 4 instruments: SshReceiveString, SshReceiveFile, stop, start
        assertEquals(4, controllers.stream().count());
    }

    @Test
    @Transactional
    @Rollback
    public void findRemoteControllers() throws Exception {
        // find the sshd belonging to host2
        final Component sshd = host2OS.getComponents().stream()
                .filter(c -> "SshServer".equals(c.getName()))
                .collect(Collectors.toList())
                .get(0);
        assertNotNull(sshd);

        final Component bash = host1OS.getComponents().stream()
                .filter(c -> "Systemd".equals(c.getName()))
                .collect(Collectors.toList())
                .get(0);
        Set<Controller> controllers = instrumentRepository.findRemoteControllers(bash.getUuid(), host1.getUuid());
        // expect 0 instruments
        assertEquals(0, controllers.stream().count());

        // create a controller which has subjectIsLocal set to false, then check again
        Set<Type> appliesTo = new HashSet<>();
        appliesTo.add(typeRepository.findOneByName("SshServer"));
        Controller nonLocal = new Controller.Builder()
                .subjectIsLocal(false)
                .name("nonlocal")
                .desc("nonlocal test instrument")
                .capability("State")
                .filePath("/tmp/foo")
                .handles(appliesTo)
                .providedBy(typeRepository.findOneByName("OpenSshClient"))
                .template("")
                .build();
        nonLocal.save();

        controllers = instrumentRepository.findRemoteControllers(sshd.getUuid(), structure.getUuid());
        // expect 1 instruments, the one we just made
        assertEquals(1, controllers.stream().count());
    }

}