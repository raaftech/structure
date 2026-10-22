package tech.raaf.structure.graph;

import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import tech.raaf.structure.GraphTest;
import tech.raaf.structure.base.Application;
import tech.raaf.structure.base.Configuration;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.bus.manager.SchemaManager;
import tech.raaf.structure.graph.repositories.NodeRepository;
import tech.raaf.structure.helper.JsonHelper;

public class ProfileTest extends GraphTest {

  @Before
  public void setup() throws Exception {
    final Vertx vertx = Vertx.vertx();
    Configuration.init();
    Context.setVertx(vertx);
    HttpServer httpServer = vertx.createHttpServer();
    Router router = Router.router(vertx);
    // One of the endpoints will be routed to SchemaManager, so instance it
    SchemaManager schemaManager = new SchemaManager();
    // Setup routes
    router.route("/schemas/:schema").blockingHandler(schemaManager::getSchema, false);

    // Port 0 lets the system choose a free port
    final CompletableFuture<HttpServer> listening = new CompletableFuture<>();
    httpServer.requestHandler(router::accept).listen(0, "localhost", ar -> {
      if (ar.succeeded()) {
        listening.complete(ar.result());
      } else {
        listening.completeExceptionally(ar.cause());
      }
    });
    final int port = listening.get().actualPort();
    Configuration.setWebHost("localhost");
    Configuration.setWebPort(port);
    Configuration.setSchemaNamespace(URI.create("http://localhost:" + port + "/schemas/"));
    Application.init(vertx);
  }

  private final NodeRepository nodeRepository = Context.getNodeRepository();
  final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void createProfile() throws Exception {
    final JsonNode profileJson = JsonHelper.jsonFromResource("profile.json");
    assertNotNull(profileJson);

    final Entity profileEntity = mapper.treeToValue(profileJson, Entity.class);
    Graph.transaction(profileEntity::toGraph);

    Profile profile = (Profile) nodeRepository.findByUuid(profileEntity.getUuid());
    assertNotNull(profile);

    Optional<List<String>> maybePackageNames = profile.getPackageNames();
    assertTrue(maybePackageNames.isPresent());
    List<String> packageNames = maybePackageNames.get();
    assertEquals("foo", packageNames.get(0));
    assertEquals("bar", packageNames.get(1));

    Optional<List<String>> maybeServiceNames = profile.getServiceNames();
    assertTrue(maybeServiceNames.isPresent());
    List<String> serviceNames = maybeServiceNames.get();
    assertEquals("service01", serviceNames.get(0));
    assertEquals("service02", serviceNames.get(1));

    Optional<List<Integer>> maybePorts = profile.getPorts();
    assertTrue(maybePorts.isPresent());
    List<Integer> ports = maybePorts.get();
    assertEquals(80, ports.get(0).intValue());
    assertEquals(21, ports.get(1).intValue());

    Optional<Map<String, Map<String, String>>> maybeCommands = profile.getCommands();
    assertTrue(maybeCommands.isPresent());
    Map<String, Map<String, String>> commands = maybeCommands.get();

    Map<String, String> fooCommand = commands.get("foo");
    assertNotNull(fooCommand);
    assertEquals("bar", fooCommand.get("command"));
    assertEquals("something", fooCommand.get("matches"));

    Map<String, String> bazCommand = commands.get("baz");
    assertNotNull(bazCommand);
    assertEquals("blaat", bazCommand.get("command"));
    assertEquals("koe", bazCommand.get("matches"));

    Map<String, List<String>> directories = profile.getPossibleDirectories();
    assertEquals(1, directories.size());
    assertTrue(directories.containsKey("logDirectories"));
    assertEquals(2, directories.get("logDirectories").size());
    assertEquals("/some/path", directories.get("logDirectories").iterator().next());

    Map<String, List<String>> files = profile.getPossibleFiles();
    assertEquals(1, files.size());
    assertTrue(files.containsKey("logFiles"));
    assertEquals(1, files.get("logFiles").size());
    assertEquals("some_file", files.get("logFiles").iterator().next());
  }
}
