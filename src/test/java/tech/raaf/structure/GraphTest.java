package tech.raaf.structure;

import org.junit.Before;
import org.junit.BeforeClass;
import tech.raaf.structure.base.Application;
import tech.raaf.structure.base.Configuration;
import tech.raaf.structure.base.Configuration.Setting;
import tech.raaf.structure.graph.Graph;

/**
 * Base class of the tests that use the graph. Connects to the Neo4j
 * test server and deletes all data before each test. Thus the test
 * server must not be the server of a normal run.
 */
public abstract class GraphTest {

  // The name prefix of the test settings
  private static final String PREFIX = "test.structure";
  private static boolean connected = false;

  @BeforeClass
  public static void connect() {
    if (connected) {
      return;
    }
    final String uri = Configuration.get(PREFIX, Setting.NEO4J_BOLT_URI);
    if (uri.equals(Configuration.get(Setting.NEO4J_BOLT_URI))) {
      throw new IllegalStateException(
        "Set TEST_STRUCTURE_NEO4J_BOLT_URI to a server other than " +
          uri +
          ". The tests delete all data on that server."
      );
    }
    Graph.connect(uri, Configuration.get(PREFIX, Setting.NEO4J_USER), Configuration.get(PREFIX, Setting.NEO4J_PASS));
    Application.createSchema();
    connected = true;
  }

  @Before
  public void deleteAll() {
    Graph.query("MATCH (n) DETACH DELETE n");
  }
}
