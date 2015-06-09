package com.raaftech.structure.graph.repositories;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.repository.query.Param;

import com.raaftech.structure.graph.InstanceNode;

/**
 * Give a description of InstanceNodeRepository here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2015-01-14
 */
public interface InstanceNodeRepository extends GraphRepository<InstanceNode> {

    InstanceNode findByUuid(String uuid);
    InstanceNode findByName(String name);
    @Query ("START start=node:uuid(uuid={uuid}) " +
            "MATCH start-[:HAS_COMPONENT*1..]->(n)-[:ISA|INSTANCE_OF*1..]->({name:{type}}) " +
            "RETURN n")
    InstanceNode findComponentByType(@Param("uuid") String uuid, @Param("type") String type);
    // Workaround, returning count(n) > 0 returns null when n is NULL.
    @Query ("START start=node:uuid(uuid={uuid}) " +
            "MATCH start-[:INSTANCE_OF|ISA*1..]->(n{name:{type}}) " +
            "RETURN count(n)")
    int isOfType(@Param("uuid") String uuid, @Param("type") String type);
}
