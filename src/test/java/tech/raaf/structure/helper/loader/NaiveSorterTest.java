package tech.raaf.structure.helper.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import mockit.Mocked;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;
import tech.raaf.structure.base.Configuration;
import tech.raaf.structure.base.Configuration.Setting;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.exceptions.JsonValidationException;
import tech.raaf.structure.helper.JsonHelper;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@ExtendWith(JMockitExtension.class)
public class NaiveSorterTest {

  @TempDir
  Path folder;

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void calculateChecksum(@Mocked Context context) throws Exception {
    /*
      Objects a and b only differ in the order of their keys. This should never trigger an update, so our checksum
      calculation should return the same checksums for both objects.
    */

    String instanceOf = UUID.randomUUID().toString();

    ObjectNode a = mapper.createObjectNode().put("name", "Foo").put("desc", "Foo.");
    ObjectNode meta = a.putObject("meta").put("bundle", "foo").put("uuid", "abba2750-7e05-4e70-8b36-1a56665dccd6");
    ObjectNode rela = meta.putObject("relations").putObject("INSTANCE_OF");
    rela.put("direction", "outgoing");
    rela.putArray("to").add(instanceOf);

    ObjectNode b = mapper.createObjectNode().put("desc", "Foo.").put("name", "Foo");
    ObjectNode metb = b.putObject("meta").put("uuid", "abba2750-7e05-4e70-8b36-1a56665dccd6").put("bundle", "foo");
    ObjectNode relb = metb.putObject("relations").putObject("INSTANCE_OF");
    relb.put("direction", "outgoing");
    relb.putArray("to").add(instanceOf);

    // our old way:
    assertFalse(Objects.equals(DigestUtils.md5Hex(a.toString()), DigestUtils.md5Hex(b.toString())));
    assertFalse(Objects.equals(DigestUtils.sha1Hex(a.toString()), DigestUtils.sha1Hex(b.toString())));

    // the new way:
    assertTrue(
      Objects.equals(
        NaiveSorter.calculateChecksum(a, NaiveSorter.ChecksumAlgorithm.MD5),
        NaiveSorter.calculateChecksum(b, NaiveSorter.ChecksumAlgorithm.MD5)
      )
    );
    assertTrue(
      Objects.equals(
        NaiveSorter.calculateChecksum(a, NaiveSorter.ChecksumAlgorithm.SHA1),
        NaiveSorter.calculateChecksum(b, NaiveSorter.ChecksumAlgorithm.SHA1)
      )
    );

    // test the overloaded InputStream version as well
    assertTrue(
      Objects.equals(
        NaiveSorter.calculateChecksum(
          new ByteArrayInputStream(a.toString().getBytes()),
          NaiveSorter.ChecksumAlgorithm.MD5
        ),
        NaiveSorter.calculateChecksum(
          new ByteArrayInputStream(b.toString().getBytes()),
          NaiveSorter.ChecksumAlgorithm.MD5
        )
      )
    );

    // add some properties that should be filtered from the JSON, see if the checksums still match
    meta.put("path", "foo");
    metb.put("path", "bar");
    assertTrue(
      Objects.equals(
        NaiveSorter.calculateChecksum(a, NaiveSorter.ChecksumAlgorithm.SHA1),
        NaiveSorter.calculateChecksum(b, NaiveSorter.ChecksumAlgorithm.SHA1)
      )
    );
  }

  @Test
  public void calculatesRootTypeChecksum() {
    ObjectNode root = (ObjectNode) JsonHelper.jsonFromResource("bundles/core/types/type.type.json");
    assertNotNull(NaiveSorter.calculateChecksum(root, NaiveSorter.ChecksumAlgorithm.SHA1));
  }

  @Test
  public void validatesRootTypeBeforeHashing() {
    ObjectNode root = (ObjectNode) JsonHelper.jsonFromResource("bundles/core/types/type.type.json");
    root.remove("desc");

    assertThrowsWith(
      JsonValidationException.class,
      () -> NaiveSorter.calculateChecksum(root, NaiveSorter.ChecksumAlgorithm.SHA1),
      "Type"
    );
  }

  @Test
  public void storesRelativePaths(@Mocked Context context) throws Exception {
    final List<JsonNode> inHome = new NaiveSorter(Paths.get("src/test/resources/bundles")).readAllJson();
    assertFalse(inHome.isEmpty());
    for (JsonNode node : inHome) {
      assertTrue(node.get("meta").get("path").asString().startsWith("simple/"));
    }
    final Path outside = folder;
    final Path simple = Configuration.get(Setting.HOME).resolve("src/test/resources/bundles/simple");
    FileUtils.copyDirectory(simple.toFile(), outside.resolve("simple").toFile());
    final List<JsonNode> elsewhere = new NaiveSorter(outside).readAllJson();
    assertFalse(elsewhere.isEmpty());
    for (JsonNode node : elsewhere) {
      assertTrue(node.get("meta").get("path").asString().startsWith("simple/"));
    }
    assertEquals(Set.copyOf(inHome), Set.copyOf(elsewhere));
  }

  @Test
  public void invalidJsonReportsFileName() throws Exception {
    Path file = Files.createDirectory(folder.resolve("bundle")).resolve("broken.type.json");
    Files.write(file, "{".getBytes(StandardCharsets.UTF_8));

    assertThrowsWith(
      Exception.class,
      () -> new NaiveSorter(folder).readAllJson(),
      file.getFileName().toString()
    );
  }

  @Test
  public void emptyWorklistReturnsEmptyList() throws Exception {
    NaiveSorter sorter = new NaiveSorter(folder);
    assertTrue(sorter.sortIt(Collections.emptyList()).isEmpty());
  }

  @Test
  public void missingBundlePathReportsPath() {
    Path missing = folder.resolve("missing");

    assertThrowsWith(
      Exception.class,
      () -> new NaiveSorter(missing).readAllJson(),
      missing.toString()
    );
  }

  @Test
  public void bundlePathMustBeDirectory() throws Exception {
    Path file = Files.createFile(folder.resolve("bundle"));

    assertThrowsWith(
      IllegalArgumentException.class,
      () -> new NaiveSorter(file).readAllJson(),
      file.toString()
    );
  }

  @Test
  public void emptyBundleDirectoryReturnsEmptyList() throws Exception {
    NaiveSorter sorter = new NaiveSorter(folder);
    assertTrue(sorter.sortIt(sorter.readAllJson()).isEmpty());
  }

  @Test
  public void missingUuidReportsFileName() throws Exception {
    ObjectNode root = rootType();
    ((ObjectNode) root.get("meta")).remove("uuid");
    Path file = writeBundle("root.type.json", root);

    assertThrowsWith(IllegalArgumentException.class, () -> sorter().readAllJson(), file.toString(), "meta.uuid");
  }

  @Test
  public void missingUuidInArrayReportsFileName() throws Exception {
    ObjectNode root = rootType();
    ((ObjectNode) root.get("meta")).remove("uuid");
    Path file = writeBundle("roots.type.json", mapper.createArrayNode().add(rootType()).add(root));

    assertThrowsWith(IllegalArgumentException.class, () -> sorter().readAllJson(), file.toString(), "meta.uuid");
  }

  @Test
  public void duplicateUuidReportsBothFiles() throws Exception {
    ObjectNode root = rootType();
    Path first = writeBundle("first.type.json", root);
    Path second = writeBundle("second.type.json", root.put("desc", "A changed description."));

    assertThrowsWith(
      IllegalArgumentException.class,
      () -> sorter().readAllJson(),
      "Duplicate uuid",
      root.path("meta").path("uuid").asString(),
      first.toString(),
      second.toString()
    );
  }

  @Test
  public void duplicateUuidInArrayReportsFileName() throws Exception {
    ObjectNode root = rootType();
    Path file = writeBundle("roots.type.json", mapper.createArrayNode().add(root).add(root));

    assertThrowsWith(
      IllegalArgumentException.class,
      () -> sorter().readAllJson(),
      "Duplicate uuid",
      root.path("meta").path("uuid").asString(),
      file.toString()
    );
  }

  @Test
  public void invalidEntityReportsFileName() throws Exception {
    ObjectNode root = rootType();
    root.remove("desc");
    Path file = writeBundle("root.type.json", root);

    assertThrowsWith(IllegalArgumentException.class, () -> sorter().readAllJson(), file.toString());
  }

  @Test
  public void emptyJsonReportsFileName() throws Exception {
    Path file = Files.createDirectory(folder.resolve("bundle")).resolve("empty.type.json");
    Files.createFile(file);

    assertThrowsWith(IllegalArgumentException.class, () -> sorter().readAllJson(), file.toString());
  }

  @Test
  public void trailingJsonReportsFileName() throws Exception {
    Path file = Files.createDirectory(folder.resolve("bundle")).resolve("trailing.type.json");
    Files.write(file, (rootType().toString() + " {}").getBytes(StandardCharsets.UTF_8));

    assertThrowsWith(IllegalArgumentException.class, () -> sorter().readAllJson(), file.toString());
  }

  @Test
  public void sortsDependenciesWithoutChangingWorklist() throws Exception {
    ObjectNode root = rootType();
    ObjectNode child = (ObjectNode) JsonHelper.jsonFromResource("bundles/simple/component.type.json");
    List<JsonNode> worklist = Arrays.asList(child, root);

    assertEquals(Arrays.asList(root, child), sorter().sortIt(worklist));
    assertEquals(Arrays.asList(child, root), worklist);
  }

  @Test
  public void unresolvedDependenciesReportOnlyMissingTargets() throws Exception {
    ObjectNode root = rootType();
    ObjectNode child = (ObjectNode) JsonHelper.jsonFromResource("bundles/simple/component.type.json");
    String missingUuid = UUID.randomUUID().toString();
    ((ObjectNode) child.get("meta")).put("path", "bundle/component.type.json");
    ((ObjectNode) child.path("meta").path("relations").path("ISA"))
      .putArray("to")
      .add(root.path("meta").path("uuid").asString())
      .add("Type")
      .add("MissingType")
      .add(missingUuid);

    try {
      sorter().sortIt(Arrays.asList(child, root));
      fail("Expected unresolved dependencies");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("Component"), e.getMessage());
      assertTrue(e.getMessage().contains(child.path("meta").path("uuid").asString()), e.getMessage());
      assertTrue(e.getMessage().contains("bundle/component.type.json"), e.getMessage());
      assertTrue(e.getMessage().contains("MissingType"), e.getMessage());
      assertTrue(e.getMessage().contains(missingUuid), e.getMessage());
      assertFalse(e.getMessage().contains(root.path("meta").path("uuid").asString()), e.getMessage());
    }
  }

  @Test
  public void cyclicDependenciesReportAllUnsortedEntities() throws Exception {
    ObjectNode first = rootType();
    ObjectNode second = (ObjectNode) JsonHelper.jsonFromResource("bundles/simple/component.type.json");
    ((ObjectNode) first.path("meta").path("relations"))
      .putObject("ISA")
      .putArray("to")
      .add(second.path("meta").path("uuid").asString());

    assertThrowsWith(
      IllegalArgumentException.class,
      () -> sorter().sortIt(Arrays.asList(first, second)),
      "Type",
      "Component",
      first.path("meta").path("uuid").asString(),
      second.path("meta").path("uuid").asString()
    );
  }

  // Assert that the call throws the type, with all parts in the message
  private static void assertThrowsWith(Class<? extends Throwable> type, Executable call, String... parts) {
    final String message = assertThrows(type, call).getMessage();
    for (String part : parts) {
      assertTrue(message.contains(part), message);
    }
  }

  private NaiveSorter sorter() {
    return new NaiveSorter(folder);
  }

  @Test
  public void rescanUpdatesPayloadPathsOutsideHome() throws Exception {
    JsonNode root = rootType();
    String uuid = root.path("meta").path("uuid").asString();
    Path source = writeBundle("root.type.json", root);
    sorter().readAllJson();
    assertEquals(source.getParent().resolve("payload/main.py"), BundleFiles.resolve(uuid, "payload/main.py"));

    Path moved = folder.resolve("moved bundle");
    Files.move(source.getParent(), moved);
    sorter().readAllJson();
    assertEquals(moved.resolve("payload/main.py"), BundleFiles.resolve(uuid, "payload/main.py"));
  }

  private ObjectNode rootType() {
    return (ObjectNode) JsonHelper.jsonFromResource("bundles/simple/root.type.json");
  }

  private Path writeBundle(String name, JsonNode document) throws Exception {
    Path file = folder.resolve("bundle").resolve(name);
    Files.createDirectories(file.getParent());
    mapper.writeValue(file.toFile(), document);
    return file;
  }
}
