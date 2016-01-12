/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package com.raaftech.structure.base;

import com.raaftech.structure.graph.repositories.InstanceNodeRepository;
import com.raaftech.structure.graph.repositories.NodeRepository;
import com.raaftech.structure.graph.repositories.SchemaNodeRepository;
import com.raaftech.structure.graph.repositories.TypeNodeRepository;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.server.Bootstrapper;
import org.neo4j.server.WrappingNeoServerBootstrapper;
import org.neo4j.server.configuration.ServerConfigurator;
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
@EnableNeo4jRepositories("com.raaftech.structure.graph.repositories")
public class SpringConfig extends Neo4jConfiguration {

    public SpringConfig() {
        setBasePackage("com.raaftech.structure.graph");
    }

    @Bean
    GraphDatabaseService graphDatabaseService() {
        // TODO: make path configurable
        Bootstrapper bootStrapper;

        GraphDatabaseBuilder graphDatabaseBuilder = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder("structure.db");
        GraphDatabaseService graphDatabaseService;
        if (Boolean.parseBoolean(System.getProperty("debugMode"))) {
            graphDatabaseBuilder.setConfig("remote_shell_enabled", "true");

            graphDatabaseService = graphDatabaseBuilder.newGraphDatabase();

            ServerConfigurator webConfig = new ServerConfigurator((GraphDatabaseAPI) graphDatabaseService);
            webConfig.configuration().setProperty("webserver_bind_address", "localhost");

            bootStrapper = new WrappingNeoServerBootstrapper((GraphDatabaseAPI) graphDatabaseService, webConfig);
            bootStrapper.start();
        } else {
            graphDatabaseService = graphDatabaseBuilder.newGraphDatabase();
        }

        return graphDatabaseService;
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
