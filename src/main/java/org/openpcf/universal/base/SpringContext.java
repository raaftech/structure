/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.base;

import org.openpcf.universal.repositories.ComponentRepository;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.neo4j.core.GraphDatabase;

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
        context = new AnnotationConfigApplicationContext(SpringConfig.class);
    }

    public ApplicationContext getApplicationContext() {
        return context;
    }

    public ComponentRepository getComponentRepository() {
        return (ComponentRepository)context.getBean("componentRepository");
    }

    public GraphDatabase getGraphDatabase() {
        return (GraphDatabase)context.getBean("graphDatabase");
    }
}