package tech.raaf.structure;

import org.junit.Before;
import org.junit.BeforeClass;
import tech.raaf.structure.base.Application;
import tech.raaf.structure.graph.Graph;

/**
 * Base class of the tests that use the graph. Connects to the Neo4j
 * test server and deletes all data before each test. Thus the test
 * server must not be the server of a normal run.
 */
public abstract class GraphTest {

    private static boolean connected = false;

    @BeforeClass
    public static void connect() {
        if (connected) {
            return;
        }
        final String uri = System.getenv("STRUCTURE_TEST_NEO4J_URI");
        final String password = System.getenv("STRUCTURE_TEST_NEO4J_PASSWORD");
        if (uri == null || password == null) {
            throw new IllegalStateException("Set STRUCTURE_TEST_NEO4J_URI and STRUCTURE_TEST_NEO4J_PASSWORD. "
                    + "The tests delete all data on that server.");
        }
        final String username = System.getenv("STRUCTURE_TEST_NEO4J_USERNAME");
        Graph.connect(uri, username == null ? "neo4j" : username, password);
        Application.createSchema();
        connected = true;
    }

    @Before
    public void deleteAll() {
        Graph.query("MATCH (n) DETACH DELETE n");
    }
}
