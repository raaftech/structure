package tech.raaf.structure.schema;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

public class ControllerSchemaTest extends SchemaTest {

  @Override
  protected Set<String> good() {
    return ImmutableSet.of("schema/controllerGood01.json");
  }

  @Override
  protected Set<String> bad() {
    return ImmutableSet.of(
      "schema/controllerBad01.json", // Invalid instrument type.
      "schema/controllerBad02.json", // Invalid argument source.
      "schema/controllerBad03.json", // Invalid selection strategy.
      "schema/controllerBad04.json" // Argument source with a suffix.
    );
  }

  @Override
  protected String schema() {
    return "resource:/bundles/core/schema/controller.schema.json";
  }
}
