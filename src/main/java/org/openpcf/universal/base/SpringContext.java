/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.base;

import org.openpcf.universal.graph.repositories.NodeRepository;
import org.openpcf.universal.graph.repositories.SchemaNodeRepository;
import org.openpcf.universal.graph.repositories.TypeNodeRepository;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import java.util.concurrent.locks.ReentrantLock;

/**
 * SpringContext singleton/wrapper.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-11-12
 */
public enum SpringContext {
    INSTANCE;

    private final ApplicationContext context;
    private static final ReentrantLock initLock = new ReentrantLock();

    private SpringContext() {
        context = new AnnotationConfigApplicationContext(SpringConfig.class, ConverterConfig.class);
    }

    public ApplicationContext getApplicationContext() {
        return context;
    }

    public NodeRepository getNodeRepository() {
        return (NodeRepository)context.getBean("nodeRepository");
    }

    public TypeNodeRepository getTypeNodeRepository() {
        return (TypeNodeRepository)context.getBean("typeNodeRepository");
    }

    public SchemaNodeRepository getSchemaNodeRepository() {
        return (SchemaNodeRepository)context.getBean("schemaNodeRepository");
    }

    public GraphDatabase getGraphDatabase() {
        return (GraphDatabase)context.getBean("graphDatabase");
    }

    public Neo4jTemplate getNeo4jTemplate() {
        return (Neo4jTemplate)context.getBean("neo4jTemplate");
    }
}