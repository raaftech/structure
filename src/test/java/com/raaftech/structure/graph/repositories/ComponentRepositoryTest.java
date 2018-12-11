package com.raaftech.structure.graph.repositories;

import com.raaftech.structure.base.Configuration;
import com.raaftech.structure.base.SpringTestConfig;
import com.raaftech.structure.graph.Component;
import com.raaftech.structure.graph.Type;
import com.raaftech.structure.helper.loader.NaiveSorter;
import com.raaftech.structure.graph.Entity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.transaction.annotation.Transactional;
import schema.TypeSchemaTest;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.Set;

import static io.vertx.core.json.Json.mapper;
import static org.junit.Assert.*;

public class ComponentRepositoryTest extends TypeSchemaTest {

    @Autowired
    ComponentRepository componentRepository;
    @Autowired
    TypeRepository typeRepository;
    @Autowired
    GraphDatabase graphDatabase;

    private Type parentType;
    private Type childType;

    @Before
    @Transactional
    public void setUp() throws Exception {
        parentType = typeRepository.findOneByName("Software");
        childType = typeRepository.findOneByName("Linux");
        Component parent = new Component(mapper.createObjectNode()
                .put("name", "parent")
                .put("desc", "Parent.")
                .put("ver", "1")
                .put("shortName", "parent")
                .put("longName", "parent"), parentType);
        Component child = new Component(mapper.createObjectNode()
                .put("name", "child")
                .put("desc", "Child.")
                .put("ver", "1")
                .put("shortName", "parent")
                .put("longName", "parent"), childType);
        parent.addComponent(child);
        parent.save();
    }

    @Test
    public void findChildrenByType() throws Exception {
        Component parent = componentRepository.findOneByName("parent");
        assertNotNull(parent);
        Component child = componentRepository.findOneByName("child");
        assertNotNull(child);

        Set<Component> childrenByType = componentRepository.findChildrenByType(parent.getUuid(), childType.getName());
        assertEquals(1, childrenByType.size());
        assertEquals(child.getUuid(), componentRepository.findChildrenByType(parent.getUuid(), "Linux").stream().findFirst().get().getUuid());
    }

}