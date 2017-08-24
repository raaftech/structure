package com.raaftech.structure.graph.repositories;

import com.raaftech.structure.graph.Group;
import com.raaftech.structure.graph.Instance;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface GroupingRepository extends GenericRepository<Group> {

    @Override
    @Query("MATCH (grouping:Group {uuid:{0}}) RETURN grouping")
    Group findByUuid(String uuid);

    @Override
    @Query ("MATCH (grouping:Group {name:{0}}) RETURN grouping")
    Group findOneByName(String name);

    @Query("MATCH (:Group{uuid:{uuid}})<-[:MEMBER_OF]-()-[*1..]->(m:Instance)-[:INSTANCE_OF|ISA*1..]->(:Type{name: {name}})" +
            "RETURN m")
    Set<Instance> getChildrenOfType(@Param("uuid") String groupUuid, @Param("name") String typeName);

    @Query("MATCH (n:Group) RETURN count(n)")
    long count();
}
