package com.raaftech.structure.graph.repositories;

import com.raaftech.structure.graph.Template;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Give a description of TemplateRepository here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2015-03-09
 */
public interface TemplateRepository extends GenericRepository<Template> {

	@Override
    @Query ("START node=node:name(name={name}) MATCH (node:Template) return node")
    Set<Template> findByName(@Param("name") String name);

	@Override
    @Query ("START node=node:name(name={name}) MATCH (node:Template) return node LIMIT 1")
    Template findOneByName(@Param("name") String name);

    @Query("MATCH (n:Template {startOfTemplate:true}) return n")
    List<Template> findAllTemplates();

    // This query returns all the UUID's + connected Discovery Handler addresses of a Template group
    @Query ("START start=node:uuid(uuid={uuid}) " +
            "MATCH start-[:HAS_COMPONENT*0..]->(component)-[:COMPONENT_FOR]->()-[:HAS_CAPABILITY]->({name:'Discoverable'})-[:HAS_HANDLER]->(handler) " +
            "RETURN component.uuid AS uuid, handler.kv AS kv")
    Iterable<Map<String, String>> getDiscoveryData(@Param("uuid") String uuid);
}
