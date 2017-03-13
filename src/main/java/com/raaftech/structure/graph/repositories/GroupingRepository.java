package com.raaftech.structure.graph.repositories;

import com.raaftech.structure.graph.Grouping;
import com.raaftech.structure.graph.Instance;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface GroupingRepository extends GenericRepository<Grouping> {

    @Override
    @Query ("MATCH (grouping:Grouping {name:{0}}) RETURN grouping")
    Grouping findOneByName(String name);

    @Query("MATCH (:Grouping{uuid:{uuid}})-[:GROUPS|HAS_COMPONENT*1..]->(m)-[:INSTANCE_OF|ISA*1..]->(:Type{name: {name}})" +
            "RETURN m")
    Set<Instance> getChildrenOfType(@Param("uuid") String groupUuid, @Param("name") String typeName);
}
