package tech.raaf.structure.base;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.cli.ParseException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import tech.raaf.structure.base.Configuration.Setting;

public class ConfigurationTest {

  private static final Map<String, String> NONE = Collections.emptyMap();

  @Rule
  public final TemporaryFolder folder = new TemporaryFolder();

  private static Properties properties(String key, String value) {
    final Properties properties = new Properties();
    properties.setProperty(key, value);
    return properties;
  }

  // A configuration with one key in a settings file
  private static Configuration file(String key, String value) {
    return new Configuration(NONE, NONE, new Properties(), properties(key, value));
  }

  @Test
  public void names() throws Exception {
    assertEquals("structure.neo4j.bolt.uri", Configuration.property("structure", Setting.NEO4J_BOLT_URI));
    assertEquals("STRUCTURE_NEO4J_BOLT_URI", Configuration.variable("structure", Setting.NEO4J_BOLT_URI));
    assertEquals("test.structure.neo4j.bolt.uri", Configuration.property("test.structure", Setting.NEO4J_BOLT_URI));
    assertEquals("TEST_STRUCTURE_NEO4J_BOLT_URI", Configuration.variable("test.structure", Setting.NEO4J_BOLT_URI));
    assertEquals(
      singletonMap("neo4j-bolt-uri", "bolt://other"),
      Configuration.parse(new String[] { "--neo4j-bolt-uri", "bolt://other" })
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
      singletonMap("TEST_STRUCTURE_NEO4J_BOLT_URI", "bolt://test"),
      new Properties(),
      properties("structure.neo4j.bolt.uri", "bolt://normal")
    );
    assertEquals("bolt://test", configuration.resolve("test.structure", Setting.NEO4J_BOLT_URI));
    assertEquals("bolt://normal", configuration.resolve("structure", Setting.NEO4J_BOLT_URI));
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
    final Path home = Configuration.get(Setting.HOME);
    assertEquals(
      Arrays.asList(home.resolve("a").toString(), "/b"),
      file("structure.bundle.path", " a, /b ,,").resolve("structure", Setting.BUNDLE_PATH)
    );
  }

  @Test
  public void home() {
    assertEquals(Paths.get("/repo"), Configuration.homeOf(Paths.get("/repo/target/structure.jar")));
    assertEquals(Paths.get("/repo"), Configuration.homeOf(Paths.get("/repo/target/classes")));
    assertEquals(Paths.get("/opt/structure"), Configuration.homeOf(Paths.get("/opt/structure/lib/structure.jar")));
    // The tests run in development mode, from target/classes
    final Path home = Configuration.get(Setting.HOME);
    assertEquals(Paths.get("").toAbsolutePath(), home);
    assertEquals(home.resolve("src/main/resources"), Configuration.resources());
    assertEquals(
      Paths.get("/opt/structure"),
      file("structure.home", "/opt/x/../structure").resolve("structure", Setting.HOME)
    );
  }

  @Test
  public void isDevEnv() throws Exception {
    final Path repo = folder.newFolder("repo").toPath();
    final Path jar = repo.resolve("target/structure.jar");
    // A pom.xml alone is not enough
    Files.createFile(repo.resolve("pom.xml"));
    assertFalse(Configuration.isDevEnv(jar));
    Files.createDirectories(repo.resolve("src/main/java"));
    assertTrue(Configuration.isDevEnv(jar));
    assertTrue(Configuration.isDevEnv(repo.resolve("target/classes")));
    // The code must be in target
    assertFalse(Configuration.isDevEnv(repo.resolve("lib/structure.jar")));
    assertFalse(Configuration.isDevEnv(Paths.get("/opt/structure/lib/structure.jar")));
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

  @Test
  public void write() throws Exception {
    final Path file = folder.getRoot().toPath().resolve("target/structure.json");
    final Map<String, String> environment = singletonMap("STRUCTURE_NEO4J_PASS", "secret");
    new Configuration(NONE, environment, new Properties(), new Properties()).write(file);
    assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(file)));
    final JsonNode json = new ObjectMapper().readTree(file.toFile());
    assertFalse(json.has("help"));
    assertEquals("secret", json.get("neo4j-pass").asText());
    assertEquals("http://localhost:7474", json.get("neo4j-http-uri").asText());
    assertEquals(8443, json.get("https-port").asInt());
    assertEquals(3443, json.get("rest-port").asInt());
    assertEquals(
      Configuration.get(Setting.HOME).resolve("target/structure.jks").toString(),
      json.get("jks-file").asText()
    );
    assertEquals(
      Configuration.get(Setting.HOME).resolve("src/main/resources/bundles").toString(),
      json.get("bundle-path").get(0).asText()
    );
  }

  @Test
  public void load() throws Exception {
    final Path system = folder.newFile("system.properties").toPath();
    final Path user = folder.newFile("user.properties").toPath();
    Files.write(system, Arrays.asList("structure.https.port=9443", "structure.rest.port=9444"));
    Files.write(user, Arrays.asList("structure.https.port=10443"));
    // A later file has priority, and a missing file is no error
    final Path missing = folder.getRoot().toPath().resolve("missing.properties");
    final Properties properties = Configuration.load(Arrays.asList(system, user, missing));
    assertEquals("10443", properties.getProperty("structure.https.port"));
    assertEquals("9444", properties.getProperty("structure.rest.port"));
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
