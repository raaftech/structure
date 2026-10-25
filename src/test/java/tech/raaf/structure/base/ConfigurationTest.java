package tech.raaf.structure.base;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.cli.ParseException;
import org.junit.Test;
import tech.raaf.structure.base.Configuration.Setting;

public class ConfigurationTest {

  private static final Map<String, String> NONE = Collections.emptyMap();

  private static Properties properties(String key, String value) {
    final Properties properties = new Properties();
    properties.setProperty(key, value);
    return properties;
  }

  // A configuration with one key in config.properties
  private static Configuration file(String key, String value) {
    return new Configuration(NONE, NONE, new Properties(), properties(key, value));
  }

  @Test
  public void names() throws Exception {
    assertEquals("structure.neo4j.uri", Configuration.property("structure", Setting.NEO4J_URI));
    assertEquals("STRUCTURE_NEO4J_URI", Configuration.variable("structure", Setting.NEO4J_URI));
    assertEquals("test.structure.neo4j.uri", Configuration.property("test.structure", Setting.NEO4J_URI));
    assertEquals("TEST_STRUCTURE_NEO4J_URI", Configuration.variable("test.structure", Setting.NEO4J_URI));
    assertEquals(
      singletonMap("neo4j-uri", "bolt://other"),
      Configuration.parse(new String[] { "--neo4j-uri", "bolt://other" })
    );
  }

  @Test
  public void lookupOrder() {
    final Map<String, String> arguments = singletonMap("https-host", "arguments");
    final Map<String, String> environment = singletonMap("STRUCTURE_HTTPS_HOST", "environment");
    final Properties system = properties("structure.https.host", "system");
    final Properties file = properties("structure.https.host", "file");
    final Properties none = new Properties();
    assertEquals(
      "arguments",
      new Configuration(arguments, environment, system, file).resolve("structure", Setting.HTTPS_HOST)
    );
    assertEquals(
      "environment",
      new Configuration(NONE, environment, system, file).resolve("structure", Setting.HTTPS_HOST)
    );
    assertEquals("system", new Configuration(NONE, NONE, system, file).resolve("structure", Setting.HTTPS_HOST));
    assertEquals("file", new Configuration(NONE, NONE, none, file).resolve("structure", Setting.HTTPS_HOST));
    assertEquals("localhost", new Configuration(NONE, NONE, none, none).resolve("structure", Setting.HTTPS_HOST));
  }

  @Test
  public void prefix() {
    final Configuration configuration = new Configuration(
      NONE,
      singletonMap("TEST_STRUCTURE_NEO4J_URI", "bolt://test"),
      new Properties(),
      properties("structure.neo4j.uri", "bolt://normal")
    );
    assertEquals("bolt://test", configuration.resolve("test.structure", Setting.NEO4J_URI));
    assertEquals("bolt://normal", configuration.resolve("structure", Setting.NEO4J_URI));
  }

  @Test
  public void types() throws Exception {
    final Configuration none = file("structure.other", "");
    final Map<String, String> flag = Configuration.parse(new String[] { "--unsafe" });
    assertTrue(new Configuration(flag, NONE, new Properties(), new Properties()).resolve("structure", Setting.UNSAFE));
    assertTrue(file("structure.unsafe", "TRUE").resolve("structure", Setting.UNSAFE));
    assertFalse(file("structure.unsafe", "false").resolve("structure", Setting.UNSAFE));
    assertFalse(none.resolve("structure", Setting.UNSAFE));
    assertEquals("DEBUG", file("structure.log.level", "DEBUG").resolve("structure", Setting.LOG_LEVEL));
    assertEquals(Integer.valueOf(9443), file("structure.https.port", "9443").resolve("structure", Setting.HTTPS_PORT));
    assertEquals(
      Long.valueOf(5000000000L),
      none.resolve("structure", new Setting<>("size", Setting.Type.LONG, "5000000000", "A size"))
    );
    assertEquals(
      Double.valueOf(0.5),
      none.resolve("structure", new Setting<>("ratio", Setting.Type.DECIMAL, "0.5", "A ratio"))
    );
    assertEquals(
      Paths.get("/opt/structure"),
      file("structure.home", "/opt/structure").resolve("structure", Setting.HOME)
    );
    assertEquals(
      Arrays.asList("a", "b"),
      file("structure.bundle.path", " a, b ,,").resolve("structure", Setting.BUNDLE_PATH)
    );
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidFlag() {
    file("structure.unsafe", "yes").resolve("structure", Setting.UNSAFE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidInteger() {
    file("structure.https.port", "https").resolve("structure", Setting.HTTPS_PORT);
  }

  @Test(expected = IllegalArgumentException.class)
  public void required() {
    file("structure.other", "").resolve("structure", Setting.NEO4J_PASS);
  }

  @Test
  public void modifiers() {
    // The modifier also applies to the default
    assertEquals(Long.valueOf(300000000000L), file("structure.other", "").resolve("structure", Setting.WORKER_TIMEOUT));
    assertEquals(
      Long.valueOf(1000000000L),
      file("structure.worker.timeout", "1").resolve("structure", Setting.WORKER_TIMEOUT)
    );
    // A relative keystore path is relative to home
    assertEquals(
      Configuration.get(Setting.HOME).resolve("target/structure.jks"),
      file("structure.other", "").resolve("structure", Setting.JKS_FILE)
    );
    assertEquals(
      Paths.get("/etc/structure.jks"),
      file("structure.jks.file", "/etc/structure.jks").resolve("structure", Setting.JKS_FILE)
    );
  }

  @Test(expected = ParseException.class)
  public void unknownOption() throws Exception {
    Configuration.parse(new String[] { "-h" });
  }

  @Test(expected = ParseException.class)
  public void unexpectedArgument() throws Exception {
    Configuration.parse(new String[] { "--unsafe", "true" });
  }
}
