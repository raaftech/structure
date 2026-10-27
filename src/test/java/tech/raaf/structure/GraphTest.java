package tech.raaf.structure;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.testcontainers.neo4j.Neo4jContainer;
import tech.raaf.structure.base.Application;
import tech.raaf.structure.graph.Graph;

/**
 * Base class of the tests that use the graph. Starts one Neo4j container
 * for all the tests in the JVM, and deletes all data before each test.
 * Testcontainers removes the container when the JVM stops. The class is
 * also the category of these tests: failsafe runs them, surefire does not.
 */
@Category(GraphTest.class)
public abstract class GraphTest {

  private static final Neo4jContainer NEO4J = new Neo4jContainer("neo4j:latest");
  private static boolean connected = false;

  @BeforeClass
  public static void connect() {
    if (connected) {
      return;
    }
    NEO4J.start();
    Graph.connect(NEO4J.getBoltUrl(), "neo4j", NEO4J.getAdminPassword());
    Application.createSchema();
    connected = true;
  }

  @Before
  public void deleteAll() {
    Graph.query("MATCH (n) DETACH DELETE n");
  }
}
