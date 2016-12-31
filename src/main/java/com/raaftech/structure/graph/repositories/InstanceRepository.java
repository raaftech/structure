package com.raaftech.structure.graph.repositories;

import com.raaftech.structure.graph.Instance;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

/**
 * Give a description of InstanceRepository here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2015-01-14
 */
public interface InstanceRepository extends GenericRepository<Instance> {

    @Override
    @Query ("MATCH (node:Instance {name:{name}}) return node")
    Set<Instance> findByName(@Param("name") String name);

    @Override
    @Query ("MATCH (node:Instance {name:{name}}) return node LIMIT 1")
    Instance findOneByName(@Param("name") String name);

    // Workaround, returning count(n) > 0 returns null when n is NULL.
    // This returns 0 if we're not of queried type, >0 if we are.
    @Query ("MATCH (start {uuid:{uuid}})-[:INSTANCE_OF|ISA*1..]->(n{name:{type}}) " +
            "RETURN count(n)")
    int isOfType(@Param("uuid") String uuid, @Param("type") String type);

    // Get the root node of a componental group
    @Query ("MATCH (instance {uuid:{instanceUuid}})<-[:HAS_COMPONENT*0..]-(start) " +
            "WHERE NOT (start)<-[:HAS_COMPONENT]-() " +
            "RETURN start")
    Instance getStartOfComponentGroup(@Param("instanceUuid") String instanceUuid);

    @Query ("MATCH (n:Instance) WHERE NOT (n)-[:INSTANCE_OF]->({name:\"Handler\"}) RETURN n ORDER BY id(n) DESC LIMIT {amount}")
    List<Instance> getNLastCreated(@Param("amount") int amount);

}
