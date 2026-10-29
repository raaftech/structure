package tech.raaf.structure.graph.repositories;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.List;
import tech.raaf.structure.graph.Component;
import tech.raaf.structure.graph.Graph;
import tech.raaf.structure.graph.Group;
import tech.raaf.structure.graph.Instance;
import tech.raaf.structure.graph.NodeMapper;

/**
 * Give a description of InstanceRepository here.
 *
 * @author Boy van Duuren <boy@raaf.tech>
 * @since 2015-01-14
 */
public class InstanceRepository extends GenericRepository<Instance> {

  @Override
  protected String label() {
    return "Instance";
  }

  // This returns 0 if we're not of queried type, >0 if we are.
  public int isOfType(String uuid, String type) {
    return (int) number(
      "MATCH (start:Node {uuid: $uuid})-[:INSTANCE_OF|ISA*1..]->(n {name: $type}) " + "RETURN count(n) AS n",
      ImmutableMap.of("uuid", uuid, "type", type)
    );
  }

  // Get the root node of a component group
  public Group getStartOfComponentGroup(String instanceUuid) {
    return one(
      "MATCH (instance:Instance {uuid: $uuid})<-[*0..]-()-[:MEMBER_OF]->(n) RETURN n LIMIT 1",
      ImmutableMap.of("uuid", instanceUuid)
    );
  }

  // Match any supplied label, or all instances if no labels are supplied
  public List<Instance> nLastCreated(int amount, String... labels) {
    return NodeMapper.mapAll(
      Graph.query(
        "MATCH (n:Instance) " +
          "WHERE size($labels) = 0 OR any(label IN $labels WHERE label IN labels(n)) " +
          "RETURN n ORDER BY n.created DESC, n.uuid DESC LIMIT $amount",
        ImmutableMap.of("amount", amount, "labels", Arrays.asList(labels))
      ),
      "n"
    );
  }

  public Component findDnsName(String hostname) {
    return one(
      "MATCH (n:`type:DnsName` {`data-dnsname`: $hostname}) RETURN n LIMIT 1",
      ImmutableMap.of("hostname", hostname)
    );
  }

  public Component findOperatingSystem(String componentUuid) {
    final List<Component> found = NodeMapper.mapAll(
      Graph.query(
        "MATCH (os:Component)-[:HAS*0..]->(:Component {uuid: $uuid}) " +
          "WHERE (os)-[:INSTANCE_OF|ISA*1..]->(:Type {name: 'OperatingSystem'}) " +
          "RETURN DISTINCT os",
        ImmutableMap.of("uuid", componentUuid)
      ),
      "os"
    );
    if (found.size() > 1) {
      throw new IllegalStateException("Component belongs to more than one operating system: " + componentUuid);
    }
    return found.isEmpty() ? null : found.getFirst();
  }

  public Instance findOneByData(String property, String value) {
    return one(
      "MATCH (n:Instance) WHERE n[$property] = $value RETURN n LIMIT 1",
      ImmutableMap.of("property", "data-" + property, "value", value)
    );
  }
}
