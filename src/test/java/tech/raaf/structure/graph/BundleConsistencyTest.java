package tech.raaf.structure.graph;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.networknt.schema.Error;
import com.networknt.schema.SpecificationVersion;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tech.raaf.structure.base.Configuration;
import tech.raaf.structure.base.Configuration.Setting;
import tech.raaf.structure.helper.JsonHelper;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class BundleConsistencyTest {

  private static final Path RESOURCES = Configuration.get(Setting.HOME).resolve("src/main/resources");
  private static final Path BUNDLES = RESOURCES.resolve("bundles");
  private static final Set<String> RELATIONS = new HashSet<>(
    Arrays.asList(
      "ISA",
      "INSTANCE_OF",
      "APPLIES_TO",
      "PROVIDED_BY",
      "GIVES_CAPABILITY",
      "IMPLEMENTS",
      "HAS",
      "MEMBER_OF",
      "HAS_RESULT",
      "PARSED",
      "PRODUCED"
    )
  );
  private static final Pattern MUSTACHE = Pattern.compile("\\{\\{\\{?([^}]*)}?}}");
  // The content of a secret file goes to a file, not to a command
  private static final Pattern SECRET_FILE = Pattern.compile("(?s)\\{\\{#secretFile}}.*?\\{\\{/secretFile}}");
  // Code on purpose (#267), or names of files that Structure makes
  private static final Set<String> UNQUOTED = new HashSet<>(
    Arrays.asList("script", "command", "payloadDirectory", "payloadFileName")
  );
  private static final Map<Path, JsonNode> documents = new LinkedHashMap<>();
  private static final List<String> jsonErrors = new ArrayList<>();
  private static final List<BundleEntity> entities = new ArrayList<>();
  private static final Map<String, BundleEntity> byUuid = new LinkedHashMap<>();
  private static final Map<String, BundleEntity> types = new LinkedHashMap<>();
  private static List<Path> files;

  private final List<String> errors = new ArrayList<>();

  @BeforeAll
  public static void readResources() throws IOException {
    try (Stream<Path> paths = Files.walk(RESOURCES)) {
      files = paths
        .filter(Files::isRegularFile)
        .sorted()
        .collect(Collectors.toList());
    }
    ObjectMapper mapper = JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    for (Path path : files) {
      if (!path.toString().endsWith(".json")) {
        continue;
      }
      JsonNode document;
      try (JsonParser parser = mapper.createParser(path.toFile())) {
        document = mapper.readTree(parser);
        if (document == null || document.isMissingNode()) {
          jsonErrors.add(path + ": empty JSON file");
        }
        if (parser.nextToken() != null) {
          jsonErrors.add(path + ": content after the JSON document");
        }
      } catch (JacksonException e) {
        jsonErrors.add(path + ": " + e.getMessage());
        // Use the application parser so the other checks can run.
        try {
          document = new ObjectMapper().readTree(path.toFile());
        } catch (JacksonException invalid) {
          continue;
        }
      }
      if (document == null) {
        continue;
      }
      documents.put(path, document);
      String name = path.getFileName().toString();
      if (path.startsWith(BUNDLES) && (name.endsWith(".type.json") || name.endsWith(".inst.json"))) {
        for (JsonNode node : document.isArray() ? document : Arrays.asList(document)) {
          BundleEntity entity = new BundleEntity(path, node);
          entities.add(entity);
          byUuid.put(node.path("meta").path("uuid").asString(), entity);
          if (entity.isType()) {
            types.put(node.path("name").asString().toLowerCase(Locale.ROOT), entity);
          }
        }
      }
    }
    assertTrue(!entities.isEmpty(), "No bundle entities found in " + BUNDLES);
  }

  @Test
  public void jsonIsValidWithoutDuplicateKeys() {
    assertTrue(jsonErrors.isEmpty(), String.join("\n", jsonErrors));
  }

  @Test
  public void entityUuidsAreUniqueVersionFour() {
    Map<String, BundleEntity> seen = new LinkedHashMap<>();
    for (BundleEntity entity : entities) {
      String value = entity.node.path("meta").path("uuid").asString();
      try {
        UUID uuid = UUID.fromString(value);
        check(
          uuid.version() == 4 && uuid.variant() == 2 && uuid.toString().equals(value),
          entity + ": expected a v4 uuid, got " + value
        );
      } catch (IllegalArgumentException e) {
        errors.add(entity + ": missing or invalid uuid " + value);
      }
      BundleEntity previous = seen.put(value, entity);
      check(previous == null, entity + ": duplicate uuid " + value + ", also in " + previous);
    }
    assertNoErrors();
  }

  @Test
  public void instrumentNamesAreUnique() {
    Map<String, BundleEntity> seen = new LinkedHashMap<>();
    for (BundleEntity entity : entities) {
      if (!entity.isType()) {
        BundleEntity previous = seen.put(entity.node.path("name").asString(), entity);
        check(previous == null, entity + ": duplicate instrument name, also in " + previous);
      }
    }
    assertNoErrors();
  }

  @Test
  public void relationAndConstraintTargetsExist() {
    for (BundleEntity entity : entities) {
      entity.node
        .path("meta")
        .path("relations")
        .properties()
        .forEach(relation -> {
          JsonNode value = relation.getValue();
          boolean toType =
            "outgoing".equals(value.path("direction").asString()) && Entity.outgoingToType.contains(relation.getKey());
          for (JsonNode target : value.path("to")) {
            checkTarget(entity, relation.getKey(), target.asString(), toType);
          }
        });
      entity.node
        .path("meta")
        .path("constraints")
        .properties()
        .forEach(constraint -> {
          for (JsonNode value : constraint.getValue()) {
            checkTarget(entity, constraint.getKey(), value.path("to").asString(), true);
          }
        });
    }
    assertNoErrors();
  }

  @Test
  public void relationAndConstraintNamesAreCurrent() {
    for (BundleEntity entity : entities) {
      for (String field : Arrays.asList("relations", "constraints")) {
        entity.node
          .path("meta")
          .path(field)
          .propertyNames()
          .forEach(name -> check(RELATIONS.contains(name), entity + ": unknown " + field + " name " + name));
      }
    }
    assertNoErrors();
  }

  @Test
  public void bundleFilesExistAndAreUsed() {
    Set<Path> used = new HashSet<>();
    for (BundleEntity entity : entities) {
      used.add(entity.path);
      entity.node
        .path("file")
        .properties()
        .forEach(file -> {
          String value = file.getValue().path("path").asString();
          Path path = entity.path.getParent().resolve(value).normalize();
          check(
            !value.isEmpty() && Files.isRegularFile(path),
            entity + ": missing file." + file.getKey() + ".path " + value
          );
          used.add(path);
        });
    }
    for (Path path : files) {
      if (path.startsWith(BUNDLES)) {
        check(used.contains(path), path + ": file is not used by an entity");
      }
    }
    assertNoErrors();
  }

  @Test
  public void schemaFileNamesMatchTypeNames() {
    for (BundleEntity type : types.values()) {
      String expected = type.node.path("name").asString().toLowerCase(Locale.ROOT) + ".schema.json";
      check(
        type.schemaPath().getFileName().toString().equals(expected),
        type + ": expected schema file " + expected + ", got " + type.schemaPath().getFileName()
      );
    }
    assertNoErrors();
  }

  @Test
  public void schemasUseDraftFour() {
    String draft4 = SpecificationVersion.DRAFT_4.getDialectId();
    documents.forEach((path, schema) -> {
      if (path.toString().endsWith(".schema.json")) {
        String value = schema.path("$schema").asString();
        check(draft4.equals(value), path + ": expected $schema " + draft4 + ", got " + value);
      }
    });
    assertNoErrors();
  }

  @Test
  public void typeSchemasHaveValidSyntaxAndReferences() {
    for (BundleEntity type : types.values()) {
      JsonNode schema = type.schema();
      List<Error> errors = JsonHelper.schemaErrors(schema);
      check(errors.isEmpty(), type + ": invalid draft-04 schema: " + errors);
      walk(schema, "", (pointer, node) -> {
        if (node.has("$ref")) {
          checkReference(type, schema, pointer, node.path("$ref").asString());
        }
      });
    }
    assertNoErrors();
  }

  @Test
  public void requiredAndIdentityNamesHaveProperties() {
    for (BundleEntity type : types.values()) {
      JsonNode properties = type.schema().path("properties");
      for (JsonNode required : type.schema().path("required")) {
        check(properties.has(required.asString()), type + ": required property is absent: " + required.asString());
      }
      JsonNode identity = type.node.path("meta").path("identity");
      if (!identity.isMissingNode()) {
        for (JsonNode name : identity.isArray() ? identity : Arrays.asList(identity)) {
          check(properties.has(name.asString()), type + ": identity property is absent: " + name.asString());
        }
      }
    }
    assertNoErrors();
  }

  @Test
  public void patternAlternativesAreGrouped() {
    documents.forEach((path, schema) -> {
      if (path.toString().endsWith(".schema.json")) {
        walk(schema, "", (pointer, node) -> {
          if (node.has("pattern")) {
            String pattern = node.path("pattern").asString();
            check(
              !hasUngroupedAlternative(pattern),
              path + "#" + pointer + "/pattern: ungrouped alternative in " + pattern
            );
          }
        });
      }
    });
    assertNoErrors();
  }

  @Test
  public void templateValuesAreQuoted() throws IOException {
    for (Path path : files) {
      String name = path.getFileName().toString();
      if (!path.startsWith(BUNDLES) || !name.contains(".hbs.")) {
        continue;
      }
      String helper = name.endsWith(".cmd") || name.endsWith(".bat") ? "cmd" : "sh";
      String template = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
      Matcher tags = MUSTACHE.matcher(SECRET_FILE.matcher(template).replaceAll("{{sh secretFile}}"));
      while (tags.find()) {
        String tag = tags.group(1).trim();
        check(
          tag.startsWith(helper + " ") || tag.startsWith("#if ") || tag.equals("/if") || UNQUOTED.contains(tag),
          path + ": " + tags.group() + " does not use the " + helper + " helper"
        );
      }
    }
    assertNoErrors();
  }

  @Test
  public void childSchemasIncludeParentPropertiesAndRequiredNames() {
    for (BundleEntity type : types.values()) {
      for (JsonNode target : type.node.path("meta").path("relations").path("ISA").path("to")) {
        BundleEntity parent = findEntity(target.asString());
        if (parent == null || !parent.isType()) {
          errors.add(type + ": missing parent type " + target.asString());
          continue;
        }
        JsonNode childSchema = type.schema();
        JsonNode parentSchema = parent.schema();
        parentSchema
          .path("properties")
          .propertyNames()
          .forEach(name ->
            check(
              childSchema.path("properties").has(name),
              type + ": missing property " + name + " from " + parent.node.path("name").asString()
            )
          );
        Set<String> required = new HashSet<>();
        childSchema.path("required").forEach(name -> required.add(name.asString()));
        for (JsonNode name : parentSchema.path("required")) {
          check(
            required.contains(name.asString()),
            type + ": missing required name " + name.asString() + " from " + parent.node.path("name").asString()
          );
        }
      }
    }
    assertNoErrors();
  }

  private void checkTarget(BundleEntity source, String relation, String target, boolean toType) {
    BundleEntity entity = findEntity(target);
    check(entity != null, source + ": " + relation + " refers to missing entity " + target);
    if (toType) {
      check(
        entity != null && entity.isType() && target.equals(entity.node.path("name").asString()),
        source + ": " + relation + " must refer to a type by name, got " + target
      );
    }
  }

  private static BundleEntity findEntity(String target) {
    BundleEntity entity = byUuid.get(target);
    return entity == null ? types.get(target.toLowerCase(Locale.ROOT)) : entity;
  }

  private void checkReference(BundleEntity type, JsonNode schema, String pointer, String value) {
    String location = type.schemaPath() + "#" + pointer + "/$ref";
    try {
      URI reference = URI.create(value);
      if (reference.isAbsolute() || reference.getAuthority() != null || reference.getQuery() != null) {
        errors.add(location + ": reference must use a type name: " + value);
        return;
      }
      JsonNode target = schema;
      String path = reference.getPath();
      if (path != null && !path.isEmpty()) {
        String suffix = ".schema.json";
        BundleEntity referenced = path.endsWith(suffix)
          ? types.get(path.substring(0, path.length() - suffix.length()))
          : null;
        if (referenced == null) {
          errors.add(location + ": reference must use a type name: " + value);
          return;
        }
        target = referenced.schema();
      }
      String fragment = reference.getFragment();
      check(
        fragment == null || !target.at(fragment).isMissingNode(),
        location + ": JSON pointer does not exist: " + value
      );
    } catch (IllegalArgumentException e) {
      errors.add(location + ": invalid reference " + value);
    }
  }

  private static void walk(JsonNode node, String pointer, BiConsumer<String, JsonNode> action) {
    action.accept(pointer, node);
    if (node.isObject()) {
      node
        .properties()
        .forEach(field ->
          walk(field.getValue(), pointer + "/" + field.getKey().replace("~", "~0").replace("/", "~1"), action)
        );
    } else if (node.isArray()) {
      for (int i = 0; i < node.size(); i++) {
        walk(node.get(i), pointer + "/" + i, action);
      }
    }
  }

  private static boolean hasUngroupedAlternative(String pattern) {
    int depth = 0;
    boolean escaped = false;
    boolean inClass = false;
    for (char c : pattern.toCharArray()) {
      if (escaped) {
        escaped = false;
      } else if (c == '\\') {
        escaped = true;
      } else if (inClass) {
        if (c == ']') {
          inClass = false;
        }
      } else if (c == '[') {
        inClass = true;
      } else if (c == '(') {
        depth++;
      } else if (c == ')') {
        depth--;
      } else if (c == '|' && depth == 0) {
        return true;
      }
    }
    return false;
  }

  private void check(boolean condition, String message) {
    if (!condition) {
      errors.add(message);
    }
  }

  private void assertNoErrors() {
    assertTrue(errors.isEmpty(), String.join("\n", errors));
  }

  private static class BundleEntity {

    private final Path path;
    private final JsonNode node;

    private BundleEntity(Path path, JsonNode node) {
      this.path = path;
      this.node = node;
    }

    private boolean isType() {
      return path.toString().endsWith(".type.json");
    }

    private Path schemaPath() {
      return path.getParent().resolve(node.path("file").path("dataSchema").path("path").asString()).normalize();
    }

    private JsonNode schema() {
      JsonNode schema = documents.get(schemaPath());
      assertNotNull(schema, this + ": missing schema " + schemaPath());
      return schema;
    }

    @Override
    public String toString() {
      return RESOURCES.relativize(path) + " (" + node.path("name").asString() + ")";
    }
  }
}
