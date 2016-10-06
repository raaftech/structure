package com.raaftech.structure.graph.repositories;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.base.SpringTestConfig;
import com.raaftech.structure.graph.*;
import com.raaftech.structure.helper.loader.BundleLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
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

    private Component structure;
    private Component host1;
    private Component host2;

    private static boolean databaseCreated = false;

    @Before
    public void setUpDatabase() throws Exception {
        if (!databaseCreated) {
            final String raafBundle = new File("bundles/raaf/bundle.json").getAbsolutePath();
            new BundleLoader(Paths.get(raafBundle)).load();
            databaseCreated = true;
        }
    }

    @Before
    @Transactional
    public void setupComponents() throws IOException {
        // Create the simplest structure server representation
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
        structure = new Component(genericLinuxData, genericLinuxType);

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

        structure.addComponent(structureSshClient);
        structure.addComponent(bash);
        structure.save();

        // Create the simplest host1 representation
        host1 = new Component(genericLinuxData, genericLinuxType);

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

        host1.addComponent(hostSshServer).addComponent(systemd);
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

        host2 = new Component(genericLinuxData, genericLinuxType);
        bash = new Component(bashData, bashType);
        hostSshServer = new Component(sshServerData, sshServerType);

        host2.addComponent(bash).addComponent(hostSshServer).addComponent(upstart);
        host2.save();
    }

    @After
    @Transactional
    public void tearDown() throws Exception {
        componentRepository.delete(structure);
        componentRepository.delete(host1);
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
    public void getMatchingReceiveString() throws Exception {
        Controller sshSendString = (Controller) instrumentRepository.findOneByName("OpenSSHClientSendString");
        assertNotNull(sshSendString);
        Marker compatible = instrumentRepository.getMatchingReceiveString(sshSendString.getUuid());
        assertNotNull(compatible);
        assertEquals("SshServerReceiveString", compatible.getName());
    }

    @Test
    public void getMatchingReceiveFile() throws Exception {
        Controller sshSendFile = (Controller) instrumentRepository.findOneByName("OpenSSHClientSendFile");
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
    @Transactional
    @Rollback
    public void validateNonLocalInstrument() throws Exception {
        Set<Type> handles = new HashSet<>();
        handles.add(typeRepository.findOneByName("SshServer"));
        Controller nonLocal = new Controller.Builder()
                .subjectIsLocal(false)
                .name("nonlocal")
                .desc("nonlocal test instrument")
                .capability("State")
                .filePath("/tmp/foo")
                .handles(handles)
                .providedBy(typeRepository.findOneByName("OpenSshClient"))
                .template("")
                .build();
        nonLocal.save();

        Set<Component> hostSshServers = componentRepository.findChildrenByType(host1.getUuid(), "SshServer");
        assertEquals(1, hostSshServers.size());
        Component hostSshServer = hostSshServers.iterator().next();
        assertNotNull(instrumentRepository.validateNonLocalInstrument(nonLocal.getUuid(), structure.getUuid(), hostSshServer.getUuid()));
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
        final Component sshd = host2.getComponents().stream()
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
        final Component sshd = host2.getComponents().stream()
                .filter(c -> "SshServer".equals(c.getName()))
                .collect(Collectors.toList())
                .get(0);
        assertNotNull(sshd);

        Set<Controller> controllers = instrumentRepository.findLocalControllers(sshd.getUuid(), host2.getUuid());
        // expect 4 instruments: SshReceiveString, SshReceiveFile, stop, start
        assertEquals(4, controllers.stream().count());
    }

    @Test
    @Transactional
    @Rollback
    public void findRemoteControllers() throws Exception {
        // find the sshd belonging to host2
        final Component sshd = host2.getComponents().stream()
                .filter(c -> "SshServer".equals(c.getName()))
                .collect(Collectors.toList())
                .get(0);
        assertNotNull(sshd);

        final Component bash = host1.getComponents().stream()
                .filter(c -> "Systemd".equals(c.getName()))
                .collect(Collectors.toList())
                .get(0);
        Set<Controller> controllers = instrumentRepository.findLocalControllers(bash.getUuid(), host1.getUuid());
        // expect 0 instruments
        assertEquals(0, controllers.stream().count());

        // create a controller which has subjectIsLocal set to false, then check again
        Set<Type> handles = new HashSet<>();
        handles.add(typeRepository.findOneByName("SshServer"));
        Controller nonLocal = new Controller.Builder()
                .subjectIsLocal(false)
                .name("nonlocal")
                .desc("nonlocal test instrument")
                .capability("State")
                .filePath("/tmp/foo")
                .handles(handles)
                .providedBy(typeRepository.findOneByName("OpenSshClient"))
                .template("")
                .build();
        nonLocal.save();

        controllers = instrumentRepository.findLocalControllers(sshd.getUuid(), structure.getUuid());
        // expect 1 instruments, the one we just made
        assertEquals(1, controllers.stream().count());
    }

}