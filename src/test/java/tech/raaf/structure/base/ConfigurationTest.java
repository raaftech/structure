package tech.raaf.structure.base;

import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.raaf.structure.base.Configuration.Setting;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class ConfigurationTest {

  private static final Map<String, String> NONE = Collections.emptyMap();

  @TempDir
  Path folder;

  private static Properties properties(String key, String value) {
    final Properties properties = new Properties();
    properties.setProperty(key, value);
    return properties;
  }

  // A configuration with one key in a settings file.
  private static Configuration file(String key, String value) {
    return new Configuration(NONE, NONE, new Properties(), properties(key, value));
  }

  @Test
  public void names() throws Exception {
    assertEquals("structure.neo4j.bolt.uri", Configuration.property(Setting.NEO4J_BOLT_URI));
    assertEquals("STRUCTURE_NEO4J_BOLT_URI", Configuration.variable(Setting.NEO4J_BOLT_URI));
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
    assertEquals("arguments", new Configuration(arguments, environment, system, file).resolve(Setting.HTTPS_HOST));
    assertEquals("environment", new Configuration(NONE, environment, system, file).resolve(Setting.HTTPS_HOST));
    assertEquals("system", new Configuration(NONE, NONE, system, file).resolve(Setting.HTTPS_HOST));
    assertEquals("file", new Configuration(NONE, NONE, none, file).resolve(Setting.HTTPS_HOST));
    assertEquals("localhost", new Configuration(NONE, NONE, none, none).resolve(Setting.HTTPS_HOST));
  }

  @Test
  public void types() throws Exception {
    final Configuration none = file("structure.other", "");
    final Map<String, String> flag = Configuration.parse(new String[] { "--unsafe" });
    assertTrue(new Configuration(flag, NONE, new Properties(), new Properties()).resolve(Setting.UNSAFE));
    assertTrue(file("structure.unsafe", "TRUE").resolve(Setting.UNSAFE));
    assertFalse(file("structure.unsafe", "false").resolve(Setting.UNSAFE));
    assertFalse(none.resolve(Setting.UNSAFE));
    assertEquals("DEBUG", file("structure.log.level", "DEBUG").resolve(Setting.LOG_LEVEL));
    assertEquals(Integer.valueOf(9443), file("structure.https.port", "9443").resolve(Setting.HTTPS_PORT));
    assertEquals(
      Long.valueOf(5000000000L),
      none.resolve(new Setting<>("size", Setting.Type.LONG, "5000000000", "A size"))
    );
    assertEquals(Double.valueOf(0.5), none.resolve(new Setting<>("ratio", Setting.Type.DECIMAL, "0.5", "A ratio")));
    assertEquals(Paths.get("/opt/structure"), file("structure.home", "/opt/structure").resolve(Setting.HOME));
    final Path home = Configuration.get(Setting.HOME);
    assertEquals(
      Arrays.asList(home.resolve("a").toString(), "/b"),
      file("structure.bundle.path", " a, /b ,,").resolve(Setting.BUNDLE_PATH)
    );
  }

  @Test
  public void home() {
    assertEquals(Paths.get("/repo"), Configuration.homeOf(Paths.get("/repo/target/structure.jar")));
    assertEquals(Paths.get("/repo"), Configuration.homeOf(Paths.get("/repo/target/classes")));
    assertEquals(Paths.get("/opt/structure"), Configuration.homeOf(Paths.get("/opt/structure/lib/structure.jar")));
    // The tests run in development mode, from target/classes.
    final Path home = Configuration.get(Setting.HOME);
    assertEquals(Paths.get("").toAbsolutePath(), home);
    assertEquals(home.resolve("src/main/resources"), Configuration.resources());
    assertEquals(Paths.get("/opt/structure"), file("structure.home", "/opt/x/../structure").resolve(Setting.HOME));
  }

  @Test
  public void isDevEnv() throws Exception {
    final Path repo = Files.createDirectory(folder.resolve("repo"));
    final Path jar = repo.resolve("target/structure.jar");
    // A pom.xml alone is not enough.
    Files.createFile(repo.resolve("pom.xml"));
    assertFalse(Configuration.isDevEnv(jar, repo));
    Files.createDirectories(repo.resolve("src/main/java"));
    assertTrue(Configuration.isDevEnv(jar, repo));
    assertTrue(Configuration.isDevEnv(repo.resolve("target/classes"), repo));
    // The code must be in target.
    assertFalse(Configuration.isDevEnv(repo.resolve("lib/structure.jar"), repo));
    assertFalse(Configuration.isDevEnv(Paths.get("/opt/structure/lib/structure.jar"), Paths.get("/opt/structure")));
  }

  @Test
  public void invalidFlag() {
    assertThrows(IllegalArgumentException.class, () -> file("structure.unsafe", "yes").resolve(Setting.UNSAFE));
  }

  @Test
  public void invalidInteger() {
    assertThrows(IllegalArgumentException.class, () ->
      file("structure.https.port", "https").resolve(Setting.HTTPS_PORT)
    );
  }

  @Test
  public void required() {
    assertThrows(IllegalArgumentException.class, () -> file("structure.other", "").resolve(Setting.NEO4J_PASS));
  }

  @Test
  public void modifiers() {
    // The modifier also applies to the default.
    assertEquals(Long.valueOf(300000000000L), file("structure.other", "").resolve(Setting.WORKER_TIMEOUT));
    assertEquals(Long.valueOf(1000000000L), file("structure.worker.timeout", "1").resolve(Setting.WORKER_TIMEOUT));
    // A relative state-dir is relative to home, and in development mode,
    // the default is target in home.
    final Path home = Configuration.get(Setting.HOME);
    assertEquals(home.resolve("target"), file("structure.other", "").resolve(Setting.STATE_DIR));
    assertEquals(home.resolve("state"), file("structure.state.dir", "state").resolve(Setting.STATE_DIR));
    assertEquals(
      Paths.get("/var/lib/structure"),
      file("structure.state.dir", "/var/lib/structure").resolve(Setting.STATE_DIR)
    );
    // A relative keystore path is relative to state-dir.
    assertEquals(
      Configuration.get(Setting.HOME).resolve("target/structure.jks"),
      file("structure.other", "").resolve(Setting.JKS_FILE)
    );
    assertEquals(
      Paths.get("/etc/structure.jks"),
      file("structure.jks.file", "/etc/structure.jks").resolve(Setting.JKS_FILE)
    );
  }

  @Test
  public void write() throws Exception {
    final Path file = folder.resolve("target/structure.json");
    final Map<String, String> environment = singletonMap("STRUCTURE_NEO4J_PASS", "secret");
    new Configuration(NONE, environment, new Properties(), new Properties()).write(file);
    assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(file)));
    final JsonNode json = new ObjectMapper().readTree(file.toFile());
    assertFalse(json.has("help"));
    assertEquals("secret", json.get("neo4j-pass").asString());
    assertEquals("http://localhost:7474", json.get("neo4j-http-uri").asString());
    assertEquals(8443, json.get("https-port").asInt());
    assertFalse(json.has("rest-host"));
    assertFalse(json.has("rest-port"));
    assertEquals(
      Configuration.get(Setting.HOME).resolve("target/structure.jks").toString(),
      json.get("jks-file").asString()
    );
    assertEquals(
      Configuration.get(Setting.HOME).resolve("src/main/resources/bundles").toString(),
      json.get("bundle-path").get(0).asString()
    );
  }

  @Test
  public void load() throws Exception {
    final Path system = Files.createFile(folder.resolve("system.properties"));
    final Path user = Files.createFile(folder.resolve("user.properties"));
    Files.write(system, Arrays.asList("structure.https.port=9443", "structure.https.host=example.org"));
    Files.write(user, Arrays.asList("structure.https.port=10443"));
    // A later file has priority, and a missing file is no error.
    final Path missing = folder.resolve("missing.properties");
    final List<Path> loadedFiles = new ArrayList<>();
    final Properties properties = Configuration.load(Arrays.asList(system, user, missing), loadedFiles);
    assertEquals("10443", properties.getProperty("structure.https.port"));
    assertEquals("example.org", properties.getProperty("structure.https.host"));
    assertEquals(Arrays.asList(system, user), loadedFiles);
  }

  @Test
  public void files() {
    final Path home = Paths.get("/opt/structure");
    assertEquals(
      Collections.singletonList(home.resolve("src/main/resources/config.properties")),
      Configuration.files(true, home)
    );
    final List<Path> release = Configuration.files(false, home);
    assertEquals(Paths.get("/etc/structure/config.properties"), release.get(0));
    assertEquals(home.resolve("etc/config.properties"), release.get(1));
    assertTrue(release.get(2).endsWith("structure/config.properties"));
  }

  @Test
  public void stateDirectories() {
    final Path home = Paths.get("/opt/structure");
    assertEquals(Collections.singletonList(home.resolve("target")), Configuration.stateDirectories(true, home));
    final List<Path> release = Configuration.stateDirectories(false, home);
    assertEquals(Paths.get("/var/lib/structure"), release.get(0));
    assertEquals(home.resolve("var"), release.get(1));
    assertTrue(release.get(2).endsWith("structure"));
  }

  @Test
  public void releaseStateDir() throws Exception {
    // A writable /var/lib/structure on this machine would come first.
    assumeFalse(Files.isWritable(Paths.get("/var/lib/structure")));
    // Without var in home, the default is structure in $XDG_STATE_HOME.
    final Path home = Files.createDirectory(folder.resolve("opt"));
    assertTrue(Configuration.releaseStateDir(home).endsWith("structure"));
    // With a var in home that we can write to, the default is that var.
    Files.createDirectory(home.resolve("var"));
    assertEquals(home.resolve("var"), Configuration.releaseStateDir(home));
  }

  @Test
  public void homeOverride() throws Exception {
    // With another home, Structure reads etc/config.properties there, but
    // a settings file can not set home.
    final Path home = Files.createDirectory(folder.resolve("other"));
    Files.createDirectory(home.resolve("etc"));
    Files.write(home.resolve("etc/config.properties"), Arrays.asList("structure.https.port=9555", "structure.home=/x"));
    final Configuration configuration = Configuration.create(singletonMap("home", home.toString()));
    assertEquals(home, configuration.resolve(Setting.HOME));
    assertEquals(Integer.valueOf(9555), configuration.resolve(Setting.HTTPS_PORT));
  }

  @Test
  public void unknownOption() {
    assertThrows(ParseException.class, () -> Configuration.parse(new String[] { "-h" }));
    assertThrows(ParseException.class, () -> Configuration.parse(new String[] { "--rest-host", "localhost" }));
    assertThrows(ParseException.class, () -> Configuration.parse(new String[] { "--rest-port", "3443" }));
  }

  @Test
  public void unexpectedArgument() {
    assertThrows(ParseException.class, () -> Configuration.parse(new String[] { "--unsafe", "true" }));
  }
}
