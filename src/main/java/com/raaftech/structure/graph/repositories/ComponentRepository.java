package com.raaftech.structure.graph.repositories;

import com.raaftech.structure.graph.Component;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface ComponentRepository extends GenericRepository<Component> {

    @Query("START start=node:uuid(uuid={uuid}) " +
            "MATCH start-[:HAS_COMPONENT*1..]->(n)-[:ISA|INSTANCE_OF*1..]->({name:{type}}) " +
            "RETURN n")
    Set<Component> findChildrenByType(@Param("uuid") String uuid, @Param("type") String type);

}
