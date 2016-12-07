package com.raaftech.structure.graph.repositories;

import com.raaftech.structure.graph.Component;
import com.raaftech.structure.graph.Instance;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface ComponentRepository extends GenericRepository<Component> {

    @Query("START start=node:uuid(uuid={uuid}) " +
            "MATCH start-[:HAS_COMPONENT*1..]->(n)-[:ISA|INSTANCE_OF*1..]->({name:{type}}) " +
            "RETURN n")
    Set<Component> findChildrenByType(@Param("uuid") String uuid, @Param("type") String type);

    @Query ("MATCH (n:Instance {`kv-dnsname`: {hostname}}) RETURN n")
    Component findHostname(@Param("hostname") String hostname);

    // Get the root node of a componental group
    @Query ("MATCH (component:Component {uuid:{instanceUuid}})<-[:HAS_COMPONENT*0..]-(start) " +
            "WHERE NOT (start)<-[:HAS_COMPONENT]-() " +
            "RETURN start")
    Component getStartOfComponentGroup(@Param("instanceUuid") String instanceUuid);

    @Query ("MATCH (n:Component)-[:HAS_COMPONENT]->(m)" +
            "WHERE NOT (n)<-[:HAS_COMPONENT]-() " +
            "RETURN DISTINCT n ORDER BY id(n) DESC LIMIT {amount}")
    Set<Component> getNLastCreated(@Param("amount") int amount);
}
