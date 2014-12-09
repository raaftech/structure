/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.base;

import org.neo4j.graphdb.GraphDatabaseService;
import org.openpcf.universal.graph.repositories.NodeRepository;
import org.openpcf.universal.graph.repositories.TypeNodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.rest.SpringRestGraphDatabase;


/**
 * Give a description of Application here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-11-04
 */
@Configuration
@EnableNeo4jRepositories("org.openpcf.universal.graph.repositories")
public class SpringConfig extends Neo4jConfiguration {

    public SpringConfig() {
        setBasePackage("org.openpcf.universal.graph");
    }

    @Bean
    GraphDatabaseService graphDatabaseService() {
        return new SpringRestGraphDatabase("http://localhost:7474/db/data");
    }

    @Autowired
    NodeRepository nodeRepository;

    @Autowired
    TypeNodeRepository typeNodeRepository;

    @Autowired
    GraphDatabase graphDatabase;
}
