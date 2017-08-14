/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package com.raaftech.structure.base;

import com.raaftech.structure.graph.Group;
import com.raaftech.structure.graph.repositories.*;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SpringContext singleton/wrapper.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-11-12
 */
@Component
public class Context implements ApplicationContextAware {

    private static ApplicationContext context;
    private static Vertx vertx = null;
    private static AtomicBoolean instrumentsLoaded = new AtomicBoolean();
    private static final Logger log = LoggerFactory.getLogger(Context.class);



    // The hostname of the machine structure runs on
    private static String hostname = null;
    // The identifier used to identify this structure instance in the database.
    private static String selfIdentifier = null;
    // The Grouping representing the server Structure is running on
    private static Group structure;

    public static void setHostname(String hostname) {
        if (Context.hostname == null) {
            log.info("Setting hostname to {}", hostname);
            Context.hostname = hostname;
        }
    }

    public static void setSelfIdentifier(String selfIdentifier) {
        log.info("Setting self identifier to {}", selfIdentifier);
        Context.selfIdentifier = selfIdentifier;
    }

    public static void setSelfGroup(Group structure) {
        Context.structure = structure;
    }

    public static String getHostname() {
        return hostname;
    }

    public static String getSelfIdentifier() {
        return selfIdentifier;
    }

    /**
     * This returns the group that represents this Structure instance in the database.
     * @return See description.
     */
    public static Group getSelfGroup() {
        return structure;
    }

    public static NodeRepository getNodeRepository() {
        return (NodeRepository)context.getBean("nodeRepository");
    }

    public static ComponentRepository getComponentRepository() {
        return (ComponentRepository)context.getBean("componentRepository");
    }

    public static TypeRepository getTypeRepository() {
        return (TypeRepository)context.getBean("typeRepository");
    }

    public static InstanceRepository getInstanceRepository() {
        return (InstanceRepository)context.getBean("instanceRepository");
    }

    public static InstrumentRepository getInstrumentRepository() {
        return (InstrumentRepository)context.getBean("instrumentRepository");
    }

    public static ResultRepository getResultRepository() {
        return (ResultRepository)context.getBean("resultRepository");
    }

    public static ProfileRepository getProfileRepository() {
        return (ProfileRepository)context.getBean("profileRepository");
    }

    public static GroupingRepository getGroupingRepository() {
        return (GroupingRepository) context.getBean("groupingRepository");
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
