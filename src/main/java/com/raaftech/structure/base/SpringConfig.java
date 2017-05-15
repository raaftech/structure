/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package com.raaftech.structure.base;

import com.raaftech.structure.graph.repositories.*;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.server.Bootstrapper;
import org.neo4j.server.WrappingNeoServerBootstrapper;
import org.neo4j.server.configuration.ServerConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
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
@ComponentScan(basePackages = "com.raaftech.structure",
        excludeFilters = {@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = SpringTestConfig.class)})
@EnableNeo4jRepositories("com.raaftech.structure.graph.repositories")
public class SpringConfig extends Neo4jConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SpringConfig.class);

    public SpringConfig() {
        setBasePackage("com.raaftech.structure.graph");
    }

    @Bean
    GraphDatabaseService graphDatabaseService() {
        Bootstrapper bootStrapper;

        GraphDatabaseBuilder graphDatabaseBuilder = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder("target/structure.db");
        GraphDatabaseService graphDatabaseService;
        if (com.raaftech.structure.base.Configuration.isUnsafeModeOn()) {
            log.info("Unsafe mode: Enabling graph database remote shell");
            graphDatabaseBuilder.setConfig("remote_shell_enabled", "true");

            graphDatabaseService = graphDatabaseBuilder.newGraphDatabase();

            log.info("Unsafe mode: Enabling graph database web interface");
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
    TypeRepository typeRepository;

    @Autowired
    InstanceRepository instanceRepository;

    @Autowired
    InstrumentRepository instrumentRepository;

    @Autowired
    ComponentRepository componentRepository;

    @Autowired
    ResultRepository resultRepository;

    @Autowired
    ProfileRepository profileRepository;

    @Autowired
    GroupingRepository groupingRepository;

    @Autowired
    GraphDatabase graphDatabase;
}
