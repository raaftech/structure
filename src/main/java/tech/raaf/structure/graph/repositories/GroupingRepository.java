package tech.raaf.structure.graph.repositories;

import com.google.common.collect.ImmutableMap;
import java.util.Set;
import tech.raaf.structure.graph.Group;
import tech.raaf.structure.graph.Instance;

public class GroupingRepository extends GenericRepository<Group> {

  @Override
  protected String label() {
    return "Group";
  }

  public Set<Instance> getChildrenOfType(String groupUuid, String typeName) {
    return set(
      "MATCH (:Group {uuid: $uuid})<-[:MEMBER_OF]-()-[*1..]->(n:Instance)" +
        "-[:INSTANCE_OF|ISA*1..]->(:Type {name: $name}) RETURN DISTINCT n",
      ImmutableMap.of("uuid", groupUuid, "name", typeName)
    );
  }
}
