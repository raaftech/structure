package tech.raaf.structure.graph.repositories;

import com.google.common.collect.ImmutableMap;
import tech.raaf.structure.graph.Component;
import tech.raaf.structure.graph.Graph;
import tech.raaf.structure.graph.Group;
import tech.raaf.structure.graph.Instance;
import tech.raaf.structure.graph.NodeMapper;

import java.util.List;

/**
 * Give a description of InstanceRepository here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2015-01-14
 */
public class InstanceRepository extends GenericRepository<Instance> {

    public InstanceRepository() {
        super("Instance");
    }

    // This returns 0 if we're not of queried type, >0 if we are.
    public int isOfType(String uuid, String type) {
        return (int) number("MATCH (start:Node {uuid: $uuid})-[:INSTANCE_OF|ISA*1..]->(n {name: $type}) " +
                "RETURN count(n) AS n", ImmutableMap.of("uuid", uuid, "type", type));
    }

    // Get the root node of a component group
    public Group getStartOfComponentGroup(String instanceUuid) {
        return one("MATCH (instance:Instance {uuid: $uuid})<-[*0..]-()-[:MEMBER_OF]->(n) RETURN n LIMIT 1",
                ImmutableMap.of("uuid", instanceUuid));
    }

    // In creation sequence; nodes from the same millisecond sort by uuid
    public List<Instance> getNLastCreated(int amount) {
        return NodeMapper.mapAll(Graph.query("MATCH (n:Instance) " +
                "WHERE NOT EXISTS { (n)-[:INSTANCE_OF]->({name: 'Handler'}) } " +
                "RETURN n ORDER BY n.created DESC, n.uuid DESC LIMIT $amount",
                ImmutableMap.of("amount", amount)), "n");
    }

    public Component findDnsName(String hostname) {
        return one("MATCH (n:`type:DnsName` {`data-dnsname`: $hostname}) RETURN n LIMIT 1",
                ImmutableMap.of("hostname", hostname));
    }
}
