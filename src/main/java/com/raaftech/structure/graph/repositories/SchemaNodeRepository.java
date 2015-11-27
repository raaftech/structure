package com.raaftech.structure.graph.repositories;

import com.raaftech.structure.graph.SchemaNode;

import java.util.Set;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Give a description of SchemaNodeRepository here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-12
 */
@Repository
public interface SchemaNodeRepository extends GenericRepository<SchemaNode> {

	@Override
    @Query ("START node=node:name(name={name}) MATCH (node:SchemaNode) return node")
	Set<SchemaNode> findByName(@Param("name") String name);

	@Override
    @Query ("START node=node:name(name={name}) MATCH (node:SchemaNode) return node LIMIT 1")
	SchemaNode findOneByName(@Param("name") String name);

    SchemaNode findByNameAndRevision(String name, int revision);
}
