package com.raaftech.structure.graph.repositories;

import java.util.Set;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.repository.query.Param;

import com.raaftech.structure.graph.TypeNode;

/**
 * Give a description of TypeNodeRepository here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-09
 */
public interface TypeNodeRepository extends GenericRepository<TypeNode> {

	@Override
    @Query ("START node=node:name(name={name}) MATCH (node:TypeNode) return node")
    Set<TypeNode> findByName(@Param("name") String name);

	@Override
    @Query ("START node=node:name(name={name}) MATCH (node:TypeNode) return node LIMIT 1")
    TypeNode findOneByName(@Param("name") String name);
}
