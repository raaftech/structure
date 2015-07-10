package com.raaftech.structure.graph.repositories;

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
public interface TypeNodeRepository extends GraphRepository<TypeNode> {

    TypeNode findByUuid(String uuid);

    @Query ("START node=node:name(name={name}) MATCH (node:TypeNode) return node")
    TypeNode findByName(@Param("name") String name);
}
