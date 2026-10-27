package tech.raaf.structure.helper.loader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import mockit.Mocked;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import tech.raaf.structure.base.Configuration;
import tech.raaf.structure.base.Configuration.Setting;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.exceptions.JsonValidationException;
import tech.raaf.structure.helper.JsonHelper;

public class NaiveSorterTest {

  @Rule
  public final TemporaryFolder folder = new TemporaryFolder();

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void calculateChecksum(@Mocked Context context) throws Exception {
    /*
      Objects a and b only differ in the order of their keys. This should never trigger an update, so our checksum
      calculation should return the same checksums for both objects.
    */

    String instanceOf = UUID.randomUUID().toString();

    ObjectNode a = mapper.createObjectNode().put("name", "Foo").put("desc", "Foo.");
    ObjectNode meta = a.putObject("meta").put("identity", "foo").put("uuid", "abba2750-7e05-4e70-8b36-1a56665dccd6");
    ObjectNode rela = meta.putObject("relations").putObject("INSTANCE_OF");
    rela.put("direction", "outgoing");
    rela.putArray("to").add(instanceOf);

    ObjectNode b = mapper.createObjectNode().put("desc", "Foo.").put("name", "Foo");
    ObjectNode metb = b.putObject("meta").put("uuid", "abba2750-7e05-4e70-8b36-1a56665dccd6").put("identity", "foo");
    ObjectNode relb = metb.putObject("relations").putObject("INSTANCE_OF");
    relb.put("direction", "outgoing");
    relb.putArray("to").add(instanceOf);
    System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(b));

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

    exception.expect(JsonValidationException.class);
    exception.expectMessage("Type");
    NaiveSorter.calculateChecksum(root, NaiveSorter.ChecksumAlgorithm.SHA1);
  }

  @Test
  public void storesRelativePaths(@Mocked Context context) throws Exception {
    // A relative bundle path is relative to home, and so is the stored path.
    final List<JsonNode> inHome = new NaiveSorter(
      Collections.singleton(Paths.get("src/main/resources/bundles"))
    ).readAllJson();
    assertFalse(inHome.isEmpty());
    for (JsonNode node : inHome) {
      assertTrue(node.get("meta").get("path").asText().startsWith("src/main/resources/bundles/"));
    }
    // Outside home, the stored path is relative to the bundle path.
    final Path outside = folder.getRoot().toPath();
    final Path simple = Configuration.get(Setting.HOME).resolve("src/test/resources/bundles/simple");
    FileUtils.copyDirectory(simple.toFile(), outside.resolve("simple").toFile());
    final List<JsonNode> elsewhere = new NaiveSorter(Collections.singleton(outside)).readAllJson();
    assertFalse(elsewhere.isEmpty());
    for (JsonNode node : elsewhere) {
      assertTrue(node.get("meta").get("path").asText().startsWith("simple/"));
    }
  }

  @Test
  public void invalidJsonReportsFileName() throws Exception {
    Path file = folder.newFolder("bundle").toPath().resolve("broken.type.json");
    Files.write(file, "{".getBytes(StandardCharsets.UTF_8));

    exception.expect(Exception.class);
    exception.expectMessage(file.getFileName().toString());
    new NaiveSorter(Collections.singleton(folder.getRoot().toPath())).readAllJson();
  }

  @Test
  public void emptyWorklistReturnsEmptyList() throws Exception {
    NaiveSorter sorter = new NaiveSorter(Collections.emptySet());
    assertTrue(sorter.sortIt(Collections.emptyList()).isEmpty());
  }

  @Test
  public void missingBundlePathReportsPath() {
    Path missing = folder.getRoot().toPath().resolve("missing");

    exception.expect(Exception.class);
    exception.expectMessage(missing.toString());
    new NaiveSorter(Collections.singleton(missing)).readAllJson();
  }

  @Test
  public void bundlePathMustBeDirectory() throws Exception {
    Path file = folder.newFile("bundle").toPath();

    exception.expect(IllegalArgumentException.class);
    exception.expectMessage(file.toString());
    new NaiveSorter(Collections.singleton(file)).readAllJson();
  }

  @Test
  public void emptyBundleDirectoryReturnsEmptyList() throws Exception {
    NaiveSorter sorter = new NaiveSorter(Collections.singleton(folder.getRoot().toPath()));
    assertTrue(sorter.sortIt(sorter.readAllJson()).isEmpty());
  }

  @Test
  public void missingUuidReportsFileName() throws Exception {
    ObjectNode root = rootType();
    ((ObjectNode) root.get("meta")).remove("uuid");
    Path file = writeBundle("root.type.json", root);

    exception.expect(IllegalArgumentException.class);
    exception.expectMessage(file.toString());
    exception.expectMessage("meta.uuid");
    sorter().readAllJson();
  }

  @Test
  public void missingUuidInArrayReportsFileName() throws Exception {
    ObjectNode root = rootType();
    ((ObjectNode) root.get("meta")).remove("uuid");
    Path file = writeBundle("roots.type.json", mapper.createArrayNode().add(rootType()).add(root));

    exception.expect(IllegalArgumentException.class);
    exception.expectMessage(file.toString());
    exception.expectMessage("meta.uuid");
    sorter().readAllJson();
  }

  @Test
  public void duplicateUuidReportsBothFiles() throws Exception {
    ObjectNode root = rootType();
    Path first = writeBundle("first.type.json", root);
    Path second = writeBundle("second.type.json", root.put("desc", "A changed description."));

    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Duplicate uuid");
    exception.expectMessage(root.path("meta").path("uuid").asText());
    exception.expectMessage(first.toString());
    exception.expectMessage(second.toString());
    sorter().readAllJson();
  }

  @Test
  public void duplicateUuidInArrayReportsFileName() throws Exception {
    ObjectNode root = rootType();
    Path file = writeBundle("roots.type.json", mapper.createArrayNode().add(root).add(root));

    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Duplicate uuid");
    exception.expectMessage(root.path("meta").path("uuid").asText());
    exception.expectMessage(file.toString());
    sorter().readAllJson();
  }

  @Test
  public void overlappingBundlePathsReadEachFileOnce() throws Exception {
    writeBundle("types/root.type.json", rootType());
    Path directory = folder.getRoot().toPath();
    NaiveSorter sorter = new NaiveSorter(new HashSet<>(Arrays.asList(directory, directory.resolve("bundle"))));

    assertEquals(1, sorter.readAllJson().size());
  }

  @Test
  public void invalidEntityReportsFileName() throws Exception {
    ObjectNode root = rootType();
    root.remove("desc");
    Path file = writeBundle("root.type.json", root);

    exception.expect(IllegalArgumentException.class);
    exception.expectMessage(file.toString());
    sorter().readAllJson();
  }

  @Test
  public void emptyJsonReportsFileName() throws Exception {
    Path file = folder.newFolder("bundle").toPath().resolve("empty.type.json");
    Files.createFile(file);

    exception.expect(IllegalArgumentException.class);
    exception.expectMessage(file.toString());
    sorter().readAllJson();
  }

  @Test
  public void trailingJsonReportsFileName() throws Exception {
    Path file = folder.newFolder("bundle").toPath().resolve("trailing.type.json");
    Files.write(file, (rootType().toString() + " {}").getBytes(StandardCharsets.UTF_8));

    exception.expect(IllegalArgumentException.class);
    exception.expectMessage(file.toString());
    sorter().readAllJson();
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
      .add(root.path("meta").path("uuid").asText())
      .add("Type")
      .add("MissingType")
      .add(missingUuid);

    try {
      sorter().sortIt(Arrays.asList(child, root));
      fail("Expected unresolved dependencies");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("Component"));
      assertTrue(e.getMessage(), e.getMessage().contains(child.path("meta").path("uuid").asText()));
      assertTrue(e.getMessage(), e.getMessage().contains("bundle/component.type.json"));
      assertTrue(e.getMessage(), e.getMessage().contains("MissingType"));
      assertTrue(e.getMessage(), e.getMessage().contains(missingUuid));
      assertFalse(e.getMessage(), e.getMessage().contains(root.path("meta").path("uuid").asText()));
    }
  }

  @Test
  public void cyclicDependenciesReportAllUnsortedEntities() throws Exception {
    ObjectNode first = rootType();
    ObjectNode second = (ObjectNode) JsonHelper.jsonFromResource("bundles/simple/component.type.json");
    ((ObjectNode) first.path("meta").path("relations"))
      .putObject("ISA")
      .putArray("to")
      .add(second.path("meta").path("uuid").asText());

    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Type");
    exception.expectMessage("Component");
    exception.expectMessage(first.path("meta").path("uuid").asText());
    exception.expectMessage(second.path("meta").path("uuid").asText());
    sorter().sortIt(Arrays.asList(first, second));
  }

  private NaiveSorter sorter() {
    return new NaiveSorter(Collections.singleton(folder.getRoot().toPath()));
  }

  private ObjectNode rootType() {
    return (ObjectNode) JsonHelper.jsonFromResource("bundles/simple/root.type.json");
  }

  private Path writeBundle(String name, JsonNode document) throws Exception {
    Path file = folder.getRoot().toPath().resolve("bundle").resolve(name);
    Files.createDirectories(file.getParent());
    mapper.writeValue(file.toFile(), document);
    return file;
  }
}
