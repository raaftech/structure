package com.raaftech.structure.graph.repositories;

import com.raaftech.structure.base.Configuration;
import com.raaftech.structure.base.SpringTestConfig;
import com.raaftech.structure.graph.Component;
import com.raaftech.structure.graph.Grouping;
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

import static io.vertx.core.json.Json.mapper;
import static org.junit.Assert.*;

public class InstanceRepositoryTest extends TypeSchemaTest {

    @Autowired
    InstanceRepository instanceRepository;
    @Autowired
    TypeRepository typeRepository;
    @Autowired
    GraphDatabase graphDatabase;

    private static Grouping grouping;
    private static Component parent, child01, child02;

    @Before
    @Transactional
    public void setUp() throws Exception {
        Type groupingType = typeRepository.findOneByName("Grouping");
        assertNotNull(groupingType);
        Type parentType = typeRepository.findOneByName("Software");
        assertNotNull(parentType);
        Type childType = typeRepository.findOneByName("Linux");
        assertNotNull(childType);
        if (grouping == null) {
            grouping = new Grouping(
                    mapper.createObjectNode()
                            .put("name", "Grouping")
                            .put("desc", "A description."),
                    groupingType);
        }
        if (parent == null) {
            parent = new Component(mapper.createObjectNode()
                    .put("name", "parent")
                    .put("desc", "Parent.")
                    .put("ver", "1")
                    .put("shortName", "parent")
                    .put("longName", "parent"), parentType);
            grouping.getComponents().add(parent);
        }
        if (child01 == null) {
            child01 = new Component(mapper.createObjectNode()
                    .put("name", "child")
                    .put("desc", "Child 01.")
                    .put("ver", "1")
                    .put("shortName", "child")
                    .put("longName", "child"), childType);
            parent.addComponent(child01);
        }
        if (child02 == null) {
            child02 = new Component(mapper.createObjectNode()
                    .put("name", "child")
                    .put("desc", "Child 02.")
                    .put("ver", "1")
                    .put("shortName", "child")
                    .put("longName", "child"), childType);
            parent.addComponent(child02);
        }
        grouping.save();
    }

    @Test
    public void findByName() throws Exception {
        assertEquals(2, instanceRepository.findByName("child").size());
    }

    @Test
    public void findOneByName() throws Exception {
        assertNotNull(instanceRepository.findOneByName("parent"));
    }

    @Test
    public void isOfType() throws Exception {
        assertTrue(instanceRepository.isOfType(parent.getUuid(), "Software") > 0);
    }

    @Test
    public void getStartOfComponentGroup() throws Exception {
        assertEquals(grouping.getUuid(), instanceRepository.getStartOfComponentGroup(child01.getUuid()).getUuid());
    }

    // Disabled this test. We don't test against a static bundle anymore so this test is hopeless
    public void getNLastCreated() throws Exception {
        assertEquals(4, instanceRepository.getNLastCreated(10).size());
    }

}