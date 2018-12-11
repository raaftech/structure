/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package com.raaftech.structure.base;

import com.google.common.io.Files;
import com.raaftech.structure.graph.repositories.*;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
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
 * SpringTestConfig is the configuration used for unit and integration tests.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2016-09-23
 */
@Configuration
@ComponentScan(basePackages = "com.raaftech.structure",
        excludeFilters = {@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = SpringConfig.class)})
@EnableNeo4jRepositories("com.raaftech.structure.graph.repositories")
public class SpringTestConfig extends Neo4jConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SpringTestConfig.class);

    public SpringTestConfig() {
        setBasePackage("com.raaftech.structure.graph");
    }

    @Bean
    GraphDatabaseService graphDatabaseService() {
        String tempDir = Files.createTempDir().getAbsolutePath();
        GraphDatabaseBuilder graphDatabaseBuilder = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(tempDir);
        return graphDatabaseBuilder.newGraphDatabase();
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
    ResultRepository resultRepository;

    @Autowired
    ProfileRepository profileRepository;

    @Autowired
    GraphDatabase graphDatabase;
}
