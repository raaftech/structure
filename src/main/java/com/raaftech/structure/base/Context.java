/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package com.raaftech.structure.base;

import com.raaftech.structure.graph.repositories.*;
import io.vertx.core.Vertx;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * SpringContext singleton/wrapper.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-11-12
 */
@Component
public class Context implements ApplicationContextAware {

    @Autowired
    private static ApplicationContext context;
    private static Vertx vertx = null;
    private static Set<String> registeredPlugins = new HashSet<>();
    private static AtomicBoolean instrumentsLoaded = new AtomicBoolean();



    // The identifier used to identify this structure instance in the database.
    public final static String selfIdentifier = "structure";

    public static NodeRepository getNodeRepository() {
        return (NodeRepository)context.getBean("nodeRepository");
    }

    public static TypeNodeRepository getTypeNodeRepository() {
        return (TypeNodeRepository)context.getBean("typeNodeRepository");
    }

    public static SchemaNodeRepository getSchemaNodeRepository() {
        return (SchemaNodeRepository)context.getBean("schemaNodeRepository");
    }

    public static InstanceNodeRepository getInstanceNodeRepository() {
        return (InstanceNodeRepository)context.getBean("instanceNodeRepository");
    }

    public static TemplateNodeRepository getTemplateNodeRepository() {
        return (TemplateNodeRepository)context.getBean("templateNodeRepository");
    }

    public static GraphDatabase getGraphDatabase() {
        return (GraphDatabase)context.getBean("graphDatabase");
    }

    public static Neo4jTemplate getNeo4jTemplate() {
        return (Neo4jTemplate)context.getBean("neo4jTemplate");
    }

    public static Vertx getVertx() {
        return vertx;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    public static void setVertx(Vertx vertxRef) {
        vertx = vertxRef;
    }

    public static void setInstrumentsLoaded(boolean state) {
        instrumentsLoaded.set(state);
    }

    public static boolean getInstrumentsLoaded() {
        return instrumentsLoaded.get();
    }
}