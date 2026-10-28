package tech.raaf.structure.helper.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableSet;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.raaf.structure.GraphTest;
import tech.raaf.structure.graph.Graph;
import tech.raaf.structure.helper.JsonHelper;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public class BundleLoaderTest extends GraphTest {

  @TempDir
  Path temporaryFolder;

  @Test
  public void resolvesConstraintTargetsAfterAllTypesAreStored() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode root = (ObjectNode) JsonHelper.jsonFromResource("bundles/simple/root.type.json");
    ObjectNode component = (ObjectNode) JsonHelper.jsonFromResource("bundles/simple/component.type.json");
    constraint(root, "FORWARD", "Component");
    constraint(component, "SELF", "Component");
    constraint(component, "ROOT", root.path("meta").path("uuid").asString());
    Path bundle = Files.createDirectory(temporaryFolder.resolve("bundle"));
    for (ObjectNode entity : new ObjectNode[] { root, component }) {
      ((ObjectNode) entity.path("file").path("dataSchema")).remove("path");
      mapper.writeValue(bundle.resolve(entity.path("name").asString() + ".type.json").toFile(), entity);
    }

    new BundleLoader(ImmutableSet.of(temporaryFolder)).loadTypes();

    Set<String> actual = Graph.query(
      "MATCH (a:Type)-[c:CONSTRAINTS]->(b:Type) " +
        "RETURN a.name + '.' + c.forRelationship + '.' + c.cardinality + '.' + b.name AS target"
    )
      .stream()
      .map(row -> (String) row.get("target"))
      .collect(Collectors.toSet());
    assertEquals(
      ImmutableSet.of(
        "Type.FORWARD.OPTIONAL_TO_MANY.Component",
        "Component.SELF.OPTIONAL_TO_MANY.Component",
        "Component.ROOT.OPTIONAL_TO_MANY.Type"
      ),
      actual
    );
  }

  private void constraint(ObjectNode entity, String relation, String target) {
    ((ObjectNode) entity.path("meta"))
      .withObjectProperty("constraints")
      .putArray(relation)
      .addObject()
      .put("cardinality", "OPTIONAL_TO_MANY")
      .put("to", target);
  }
}
