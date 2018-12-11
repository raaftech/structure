package tech.raaf.structure.graph.repositories;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.repository.query.Param;
import tech.raaf.structure.graph.Component;

import java.util.Set;

public interface ComponentRepository extends GenericRepository<Component> {

    @Query("MATCH (start {uuid:{uuid}})-[*1..]->(n:Instance)-[:ISA|INSTANCE_OF*1..]->({name:{type}}) RETURN n")
    Set<Component> findChildrenByType(@Param("uuid") String uuid, @Param("type") String type);

    @Query("MATCH (n:Component) RETURN count(n)")
    long count();

}
