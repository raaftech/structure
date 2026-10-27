package tech.raaf.structure.graph.repositories;

import static io.vertx.core.json.Json.mapper;
import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import mockit.Mock;
import mockit.MockUp;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.bus.NodeActor;
import tech.raaf.structure.graph.*;
import tech.raaf.structure.schema.TypeSchemaTest;

public class InstrumentRepositoryTest extends TypeSchemaTest {

  private static final String UPSTART_STOP = "2b138899-51ed-4c72-bef8-c08aa5df8b12";

  private final NodeRepository nodeRepository = Context.getNodeRepository();
  private final TypeRepository typeRepository = Context.getTypeRepository();
  private final InstrumentRepository instrumentRepository = Context.getInstrumentRepository();
  private final ComponentRepository componentRepository = Context.getComponentRepository();
  private final InstanceRepository instanceRepository = Context.getInstanceRepository();

  private Group structure;
  private Group host1;
  private Component host1OS;
  private Group host2;
  private Component host2OS;

  @Before
  public void setupComponents() throws IOException {
    // Create the simplest structure server representation
    Type groupingType = typeRepository.findOneByName("Group");
    assertNotNull(groupingType);
    ObjectNode groupingData = (ObjectNode) mapper.readTree(
      "{\n" + "    \"desc\": \"A groupingType representing a computer.\",\n" + "    \"name\": \"Computer\"\n" + "}"
    );
    structure = new Group(groupingData, groupingType);
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
        "}"
    );
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
    structure.addMember(structureOS);
    structure.save();

    // Create the simplest host1 representation
    host1 = new Group(groupingData, groupingType);
    host1OS = new Component(genericLinuxData, genericLinuxType);

    Type sshServerType = typeRepository.findOneByName("SshServer");
    ObjectNode sshServerData = (ObjectNode) mapper.readTree(
      "{\n" +
        "    \"desc\": \"An SSH server.\",\n" +
        "    \"longName\": \"SshServer\",\n" +
        "    \"name\": \"SshServer\",\n" +
        "    \"shortName\": \"sshserver\",\n" +
        "    \"ver\": \"unknown\"\n" +
        "}"
    );
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
    host1.addMember(host1OS);
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

    host2 = new Group(groupingData, groupingType);
    host2OS = new Component(genericLinuxData, genericLinuxType);
    bash = new Component(bashData, bashType);
    hostSshServer = new Component(sshServerData, sshServerType);

    host2OS.addComponent(bash).addComponent(hostSshServer).addComponent(upstart);
    host2.addMember(host2OS);
    host2.save();
  }

  @After
  public void reset() {
    Context.setSelfGroup(null);
    Context.setSelfIdentifier(null);
  }

  @Test
  public void getInstrumentCapability() throws Exception {
    Type bsdRcInit = typeRepository.findOneByName("BsdrcInit");
    assertNotNull(bsdRcInit);
    Controller stopController = null;
    for (Instrument instrument : bsdRcInit.getInstruments()) {
      if ("stop".equals(instrument.getName())) {
        stopController = (Controller) instrument;
      }
    }
    assertNotNull(stopController);
    Assert.assertEquals("Stop", instrumentRepository.getInstrumentCapability(stopController.getUuid()).getName());
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
  public void getMatchingReceiversUseInheritedSmbProtocol() {
    Instrument sendString = instrumentRepository.findOneByName("WinexeSendString");
    assertNotNull(sendString);
    Marker receiveString = instrumentRepository.getMatchingReceiveString(sendString.getUuid());
    assertNotNull(receiveString);
    assertEquals("SmbReceiveString", receiveString.getName());

    Instrument sendFile = instrumentRepository.findOneByName("SmbClientSendFile");
    assertNotNull(sendFile);
    Marker receiveFile = instrumentRepository.getMatchingReceiveFile(sendFile.getUuid());
    assertNotNull(receiveFile);
    assertEquals("SmbReceiveFile", receiveFile.getName());
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
      .handles(appliesTo)
      .providedBy(typeRepository.findOneByName("OpenSshClient"))
      .build();
    nonLocal.save();
    assertNotNull(instanceRepository.findByUuid(nonLocal.getUuid()));

    Set<Component> hostSshServers = componentRepository.findChildrenByType(host1OS.getUuid(), "SshServer");
    assertEquals(1, hostSshServers.size());
    Component hostSshServer = hostSshServers.iterator().next();
    assertNotNull(
      instrumentRepository.validateNonLocalInstrument(nonLocal.getUuid(), structure.getUuid(), hostSshServer.getUuid())
    );

    instanceRepository.delete(nonLocal);
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
    final Component sshd = host2OS
      .getComponents()
      .stream()
      .filter(c -> "SshServer".equals(c.getName()))
      .collect(Collectors.toList())
      .get(0);
    assertNotNull(sshd);

    Controller upstartStop = instrumentRepository.findMatchingLocalController(
      sshd.getUuid(),
      host2.getUuid(),
      instrumentName
    );
    assertNotNull(upstartStop);
    assertEquals("Upstart", upstartStop.getProvidedBy().getName());
  }

  @Test
  public void stopCapabilitySelectsUpstartOnHost2() throws Exception {
    Component sshd = host2OS
      .getComponents()
      .stream()
      .filter(component -> "SshServer".equals(component.getName()))
      .findFirst()
      .get();
    // Give an unavailable controller a higher weight.
    Graph.query(
      "MATCH (n:Controller)-[:PROVIDED_BY]->(:Type {name: 'Systemd'}) " +
        "MATCH (n)-[:GIVES_CAPABILITY]->(:Type {name: 'Stop'}) SET n.`data-weight` = 100"
    );
    Context.setSelfGroup(structure);
    Context.setSelfIdentifier(structure.getUuid());

    CompletableFuture<Controller> selected = new CompletableFuture<>();
    new MockUp<Instance>() {
      @Mock
      public Instance runController(Controller controller, ObjectNode arguments, String taskId) {
        selected.complete(controller);
        return sshd;
      }
    };
    String deployment = NodeActor.deploy(sshd).get(10, TimeUnit.SECONDS);
    try {
      ObjectNode request = mapper.createObjectNode().put("msgh", "runCapability");
      request.putObject("args").put("capability", "Stop").putObject("arguments");
      CompletableFuture<Object> reply = new CompletableFuture<>();
      Context.getVertx()
        .eventBus()
        .send("tech.raaf.structure.bus.node." + sshd.getUuid(), new JsonObject(request.toString()), ar -> {
          if (ar.succeeded()) {
            reply.complete(ar.result().body());
          } else {
            reply.completeExceptionally(ar.cause());
          }
        });
      JsonNode response = mapper.readTree(reply.get(10, TimeUnit.SECONDS).toString());
      assertFalse(response.toString(), response.path("result").has("error"));
      Controller controller = selected.get(10, TimeUnit.SECONDS);
      assertEquals("Stop must use the provider on host2", UPSTART_STOP, controller.getUuid());
      assertEquals("UpstartStop", controller.getName());
    } finally {
      CompletableFuture<Void> undeployed = new CompletableFuture<>();
      Context.getVertx().undeploy(deployment, ar -> {
        if (ar.succeeded()) {
          undeployed.complete(null);
        } else {
          undeployed.completeExceptionally(ar.cause());
        }
      });
      undeployed.get(10, TimeUnit.SECONDS);
    }
  }

  @Test
  public void findLocalControllers() throws Exception {
    // find the sshd belonging to host2
    final Component sshd = host2OS
      .getComponents()
      .stream()
      .filter(c -> "SshServer".equals(c.getName()))
      .collect(Collectors.toList())
      .get(0);
    assertNotNull(sshd);

    Set<Controller> controllers = instrumentRepository.findLocalControllers(sshd.getUuid());
    // expect 4 instruments: SshReceiveString, SshReceiveFile, stop, start
    assertEquals(4, controllers.stream().count());
  }

  @Test
  public void findRemoteControllers() throws Exception {
    // find the sshd belonging to host2
    final Component sshd = host2OS
      .getComponents()
      .stream()
      .filter(c -> "SshServer".equals(c.getName()))
      .collect(Collectors.toList())
      .get(0);
    assertNotNull(sshd);

    final Component bash = host1OS
      .getComponents()
      .stream()
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
      .handles(appliesTo)
      .providedBy(typeRepository.findOneByName("OpenSshClient"))
      .build();
    nonLocal.save();

    controllers = instrumentRepository.findRemoteControllers(sshd.getUuid(), structure.getUuid());
    // expect 1 instruments, the one we just made
    assertEquals(1, controllers.stream().count());
  }
}
