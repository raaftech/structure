package com.raaftech.structure.graph.repositories;

import java.util.Map;
import java.util.Set;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.repository.query.Param;

import com.raaftech.structure.graph.TemplateNode;

/**
 * Give a description of TemplateNodeRepository here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2015-03-09
 */
public interface TemplateNodeRepository extends GraphRepository<TemplateNode> {

    TemplateNode findByUuid(String uuid);
    TemplateNode findByName(String name);

    @Query("MATCH (n {startOfTemplate:true}) return n")
    Set<TemplateNode> findAllTemplates();

    // This query returns all the UUID's + connected Discovery Handler addresses of a TemplateNode group
    @Query ("START start=node:uuid(uuid={uuid}) " +
            "MATCH start-[:HAS_COMPONENT*0..]->(component)-[:ISA]->()-[:HAS_CAPABILITY]->({name:'Discoverable'})-[:HAS_HANDLER]->(handler) " +
            "RETURN component.uuid AS uuid, handler.kv AS kv")
    Iterable<Map<String, String>> getDiscoveryData(@Param("uuid") String uuid);
}
