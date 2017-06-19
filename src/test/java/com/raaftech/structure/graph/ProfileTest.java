package com.raaftech.structure.graph;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raaftech.structure.base.Application;
import com.raaftech.structure.base.Configuration;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.base.SpringTestConfig;
import com.raaftech.structure.bus.manager.SchemaManager;
import com.raaftech.structure.graph.repositories.NodeRepository;
import com.raaftech.structure.helper.JsonHelper;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = SpringTestConfig.class)
public class ProfileTest {

    private static final String webHost = "localhost";
    private static final int webPort = 8123;
    private static final URI schemaNS = URI.create(String.format("http://%s:%d/schemas/", webHost, webPort));

    @Before
    public void setup() {
        final Vertx vertx = Vertx.vertx();
        Configuration.init();
        Configuration.setWebHost("localhost");
        Configuration.setWebPort(8123);
        Configuration.setSchemaNamespace(schemaNS);
        Context.setVertx(vertx);
        HttpServer httpServer = vertx.createHttpServer();
        Router router = Router.router(vertx);
        // One of the endpoints will be routed to SchemaManager, so instance it
        SchemaManager schemaManager = new SchemaManager();
        // Setup routes
        router.route("/schemas/:schema").handler(schemaManager::getSchema);
        httpServer.requestHandler(router::accept).listen(Configuration.getWebPort(), Configuration.getWebHost());
        Application.init(vertx);
    }

    @Autowired
    NodeRepository nodeRepository;
    @Autowired
    GraphDatabase graphDatabase;
    final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void createProfile() throws Exception {
        Transaction tx = graphDatabase.beginTx();
        final JsonNode profileJson = JsonHelper.jsonFromResource("profile.json");
        assertNotNull(profileJson);

        final Entity profileEntity = mapper.treeToValue(profileJson, Entity.class);
        profileEntity.toGraph();
        tx.success();
        tx.close();

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
        assertEquals(1, directories.get("logDirectories").size());
        assertEquals("/some/path", directories.get("logDirectories").iterator().next());

        Map<String, List<String>> files = profile.getPossibleFiles();
        assertEquals(1, files.size());
        assertTrue(files.containsKey("logFiles"));
        assertEquals(1, files.get("logFiles").size());
        assertEquals("some_file", files.get("logFiles").iterator().next());
    }
}