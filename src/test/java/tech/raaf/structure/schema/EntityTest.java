package tech.raaf.structure.schema;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import java.net.URI;
import java.util.Set;
import org.junit.Test;
import tech.raaf.structure.helper.JsonHelper;

public class EntityTest extends SchemaTest {

  @Override
  protected Set<String> good() {
    return ImmutableSet.of("schema/entityGood01.json", "schema/entityGood02.json", "bundles/core/types/type.type.json");
  }

  @Override
  protected Set<String> bad() {
    return ImmutableSet.of(
      "schema/entityBad01.json", // testing with bad UUID
      "schema/entityBad02.json", // testing with bad UUID array
      "schema/entityBad03.json", // Type without a data schema.
      "schema/entityBad04.json", // Invalid relation target.
      "schema/entityBad05.json" // INSTANCE_OF without a target.
    );
  }

  @Override
  protected String schema() {
    return "resource:/schema/entity.schema.json";
  }

  @Test
  public void acceptsUuidAndTypeNameTargets() {
    ObjectNode entity = instance();
    relation(entity).putArray("to").add("a79ce509-a021-4177-ae61-73abd01fb479").add("Component");
    assertTrue(valid(entity));
  }

  @Test
  public void rejectsInvalidTargets() {
    for (String target : new String[] {
      "invalid type name",
      "type.component",
      "component",
      "",
      "a79ce509-a021-1177-ae61-73abd01fb479",
    }) {
      ObjectNode entity = instance();
      relation(entity).putArray("to").add(target);
      assertFalse(target, valid(entity));
    }
  }

  @Test
  public void targetsMustBeNonemptyAndUnique() {
    ObjectNode entity = instance();
    relation(entity).putArray("to");
    assertFalse(valid(entity));
    relation(entity).putArray("to").add("Component").add("Component");
    assertFalse(valid(entity));
  }

  @Test
  public void checksRelationsWithUnderscores() {
    ObjectNode entity = instance();
    relation(entity).put("direction", "undirected");
    assertFalse(valid(entity));
    relation(entity).remove("direction");
    assertFalse(valid(entity));
    ((ObjectNode) entity.path("meta").path("relations")).put("INSTANCE_OF", "Component");
    assertFalse(valid(entity));
  }

  @Test
  public void rejectsInvalidRelationNames() {
    for (String name : new String[] { "MEMBER-OF", "MEMBER OF", "MEMBER1", "" }) {
      ObjectNode entity = instance();
      ((ObjectNode) entity.path("meta").path("relations")).set(name, relation(entity).deepCopy());
      assertFalse(name, valid(entity));
    }
  }

  @Test
  public void onlyRootTypeCanHaveNoRelations() {
    ObjectNode entity = rootType();
    entity.put("name", "OtherType");
    assertFalse(valid(entity));
  }

  @Test
  public void rootTypeRequiresDataSchema() {
    ObjectNode entity = rootType();
    ObjectNode file = (ObjectNode) entity.get("file");
    file.set("script", file.remove("dataSchema"));
    assertFalse(valid(entity));
    entity.remove("file");
    assertFalse(valid(entity));
  }

  private ObjectNode instance() {
    return (ObjectNode) JsonHelper.jsonFromResource("schema/entityGood02.json");
  }

  private ObjectNode rootType() {
    return (ObjectNode) JsonHelper.jsonFromResource("bundles/core/types/type.type.json");
  }

  private ObjectNode relation(ObjectNode entity) {
    return (ObjectNode) entity.path("meta").path("relations").path("INSTANCE_OF");
  }

  private boolean valid(ObjectNode entity) {
    return JsonHelper.validateToSchema(URI.create(schema()), entity);
  }
}
