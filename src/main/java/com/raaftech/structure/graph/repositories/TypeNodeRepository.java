package com.raaftech.structure.graph.repositories;

import java.util.Set;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.repository.query.Param;

import com.raaftech.structure.graph.TypeNode;
import org.springframework.stereotype.Repository;

/**
 * Give a description of TypeNodeRepository here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-09
 */
@Repository
public interface TypeNodeRepository extends GenericRepository<TypeNode> {

	@Override
    @Query ("START node=node:name(name={name}) MATCH (node:TypeNode) return node")
    Set<TypeNode> findByName(@Param("name") String name);

	@Override
    @Query ("START node=node:name(name={name}) MATCH (node:TypeNode) return node LIMIT 1")
    TypeNode findOneByName(@Param("name") String name);

    // Get the hierarchical path of a type
    @Query ("START deepest=node:uuid(uuid={deepestNode}) " +
            "MATCH (deepest) " +
            "OPTIONAL MATCH (deepest)-[:ISA*..]->(m) " +
            "WITH deepest, m ORDER BY id(m), deepest " +
            "WITH deepest, collect(m.name) AS names " +
            "WITH deepest, reduce(path='', n in names| path + n + '.') AS path " +
            "RETURN path + deepest.name AS path")
    String getHierarchyPath(@Param("deepestNode") String uuid);

    // Workaround, returning count(n) > 0 returns null when n is NULL.
    @Query ("START start=node:uuid(uuid={typeUuid}) " +
            "MATCH start<-[:ISA*1..]-(n{name:{subtypeUuid}}) " +
            "RETURN count(n)")
    int isSubtype(@Param("typeUuid") String typeUuid, @Param("subtypeUuid") String subtypeUuid);
}
