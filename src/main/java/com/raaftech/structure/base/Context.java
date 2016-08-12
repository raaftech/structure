/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package com.raaftech.structure.base;

import com.raaftech.structure.graph.repositories.*;
import io.vertx.core.Vertx;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.support.Neo4jTemplate;

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
public enum Context {
    INSTANCE;

    private final ApplicationContext context;
    private Vertx vertx = null;
    private Set<String> registeredPlugins = new HashSet<>();
    @SuppressWarnings("unused")
    private static final ReentrantLock initLock = new ReentrantLock();
    private AtomicBoolean instrumentsLoaded = new AtomicBoolean();

    // The identifier used to identify this structure instance in the database.
    public final static String selfIdentifier = "structure";

    Context() {
        context = new AnnotationConfigApplicationContext(SpringConfig.class, ConverterConfig.class);
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

    public InstanceNodeRepository getInstanceNodeRepository() {
        return (InstanceNodeRepository)context.getBean("instanceNodeRepository");
    }

    public TemplateNodeRepository getTemplateNodeRepository() {
        return (TemplateNodeRepository)context.getBean("templateNodeRepository");
    }

    public GraphDatabase getGraphDatabase() {
        return (GraphDatabase)context.getBean("graphDatabase");
    }

    public Neo4jTemplate getNeo4jTemplate() {
        return (Neo4jTemplate)context.getBean("neo4jTemplate");
    }

    public Vertx getVertx() {
        return vertx;
    }

    public void setVertx(Vertx vertx) {
        this.vertx = vertx;
    }

    public void setInstrumentsLoaded(boolean state) {
        instrumentsLoaded.set(state);
    }

    public boolean getInstrumentsLoaded() {
        return instrumentsLoaded.get();
    }
}