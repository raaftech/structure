package tech.raaf.structure.schema;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

public class EntityTest extends SchemaTest {

  @Override
  protected Set<String> good() {
    return ImmutableSet.of("schema/entityGood01.json", "schema/entityGood02.json");
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
}
