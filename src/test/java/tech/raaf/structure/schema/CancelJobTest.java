package tech.raaf.structure.schema;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

public class CancelJobTest extends SchemaTest {

  @Override
  protected Set<String> good() {
    return ImmutableSet.of("schema/canceljobGood01.json");
  }

  @Override
  protected Set<String> bad() {
    return ImmutableSet.of(
      "schema/canceljobBad01.json" // testing with bad UUID
    );
  }

  @Override
  protected String schema() {
    return "resource:/schema/manager/taskmanager/canceljob.schema.json";
  }
}
