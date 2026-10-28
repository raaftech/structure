package tech.raaf.structure;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.testcontainers.neo4j.Neo4jContainer;
import tech.raaf.structure.base.Application;
import tech.raaf.structure.graph.Graph;

/**
 * Base class of the tests that use the graph. Starts one Neo4j container
 * for all the tests in the JVM, and deletes all data before each test.
 * Testcontainers removes the container when the JVM stops. The tag graph
 * marks these tests: failsafe runs them, surefire does not.
 */
@Tag("graph")
public abstract class GraphTest {

  private static final Neo4jContainer NEO4J = new Neo4jContainer("neo4j:latest");
  private static boolean connected = false;

  @BeforeAll
  public static void connect() {
    if (connected) {
      return;
    }
    NEO4J.start();
    Graph.connect(NEO4J.getBoltUrl(), "neo4j", NEO4J.getAdminPassword());
    Application.createSchema();
    connected = true;
  }

  @BeforeEach
  public void deleteAll() {
    Graph.query("MATCH (n) DETACH DELETE n");
  }
}
