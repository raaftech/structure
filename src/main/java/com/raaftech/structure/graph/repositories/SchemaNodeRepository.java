package com.raaftech.structure.graph.repositories;

import com.raaftech.structure.graph.SchemaNode;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

/**
 * Give a description of SchemaNodeRepository here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-12
 */
public interface SchemaNodeRepository extends GenericRepository<SchemaNode> {

	@Override
    @Query ("START node=node:name(name={name}) MATCH (node:SchemaNode) return node")
	Set<SchemaNode> findByName(@Param("name") String name);

	@Override
    @Query ("START node=node:name(name={name}) MATCH (node:SchemaNode) return node LIMIT 1")
	SchemaNode findOneByName(@Param("name") String name);

    SchemaNode findByNameAndRevision(String name, int revision);
}
