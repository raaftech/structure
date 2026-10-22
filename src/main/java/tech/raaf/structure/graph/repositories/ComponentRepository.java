package tech.raaf.structure.graph.repositories;

import com.google.common.collect.ImmutableMap;
import java.util.Set;
import tech.raaf.structure.graph.Component;

public class ComponentRepository extends GenericRepository<Component> {

  public ComponentRepository() {
    super("Component");
  }

  public Set<Component> findChildrenByType(String uuid, String type) {
    return set(
      "MATCH (start:Node {uuid: $uuid})-[*1..]->(n:Instance)-[:ISA|INSTANCE_OF*1..]->({name: $type}) " +
        "RETURN DISTINCT n",
      ImmutableMap.of("uuid", uuid, "type", type)
    );
  }
}
