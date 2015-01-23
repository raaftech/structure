/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.base;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.openpcf.universal.graph.repositories.InstanceNodeRepository;
import org.openpcf.universal.graph.repositories.NodeRepository;
import org.openpcf.universal.graph.repositories.SchemaNodeRepository;
import org.openpcf.universal.graph.repositories.TypeNodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.support.Neo4jTemplate;


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
        // TODO: make path configurable
        GraphDatabaseBuilder graphDatabaseBuilder = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder("universal.db");
        graphDatabaseBuilder.setConfig("remote_shell_enabled", "true");
        return graphDatabaseBuilder.newGraphDatabase();
    }

    @Autowired
    NodeRepository nodeRepository;

    @Autowired
    Neo4jTemplate neo4jTemplate;

    @Autowired
    TypeNodeRepository typeNodeRepository;

    @Autowired
    SchemaNodeRepository schemaNodeRepository;

    @Autowired
    InstanceNodeRepository instanceNodeRepository;

    @Autowired
    GraphDatabase graphDatabase;
}
