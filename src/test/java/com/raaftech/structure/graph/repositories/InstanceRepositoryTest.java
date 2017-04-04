package com.raaftech.structure.graph.repositories;

import com.raaftech.structure.base.Configuration;
import com.raaftech.structure.base.SpringTestConfig;
import com.raaftech.structure.graph.Component;
import com.raaftech.structure.graph.Grouping;
import com.raaftech.structure.graph.Type;
import com.raaftech.structure.helper.loader.EvenNaiverSorter;
import com.raaftech.structure.next.Entity;
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

import java.io.IOException;
import java.nio.file.FileSystems;

import static io.vertx.core.json.Json.mapper;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = SpringTestConfig.class)
public class InstanceRepositoryTest {

    @Autowired
    InstanceRepository instanceRepository;
    @Autowired
    TypeRepository typeRepository;
    @Autowired
    GraphDatabase graphDatabase;
    private static boolean databaseCreated;

    private static Grouping grouping;
    private static Component parent, child01, child02;

    @Before
    @Transactional
    @SuppressWarnings("Duplicates")
    public void setUp() throws Exception {
        if (!databaseCreated) {
            Transaction tx = graphDatabase.beginTx();
            final String testBundle = "src/test/resources/bundles/repositorytests/";
            Configuration.setSchemaNamespace(FileSystems.getDefault().getPath(testBundle).toUri().resolve("types/"));
            EvenNaiverSorter evenNaiverSorter = new EvenNaiverSorter(testBundle);
            evenNaiverSorter.getUpdateList(evenNaiverSorter.sortIt(evenNaiverSorter.readAllJson())).stream()
                    .map(node -> {
                        try {
                            return mapper.readValue(node.toString(), Entity.class);
                        } catch (IOException e) {
                            throw new RuntimeException("Error while creating types");
                        }
                    })
                    .filter(node -> node != null)
                    .forEach(Entity::toGraph);

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
                        .put("checksum", "f1d2d2f924e986ac86fdf7b36c94bcdf32beec15")
                        .put("ver", "1")
                        .put("shortName", "parent")
                        .put("longName", "parent"), parentType);
                grouping.getComponents().add(parent);
            }
            if (child01 == null) {
                child01 = new Component(mapper.createObjectNode()
                        .put("name", "child")
                        .put("desc", "Child 01.")
                        .put("checksum", "f1d2d2f924e986ac86fdf7b36c94bcdf32beec15")
                        .put("ver", "1")
                        .put("shortName", "child")
                        .put("longName", "child"), childType);
                parent.addComponent(child01);
            }
            if (child02 == null) {
                child02 = new Component(mapper.createObjectNode()
                        .put("name", "child")
                        .put("desc", "Child 02.")
                        .put("checksum", "f1d2d2f924e986ac86fdf7b36c94bcdf32beec15")
                        .put("ver", "1")
                        .put("shortName", "child")
                        .put("longName", "child"), childType);
                parent.addComponent(child02);
            }
            grouping.save();

            tx.success();
            tx.close();
            databaseCreated = true;
        }
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

    @Test
    public void getNLastCreated() throws Exception {
        assertEquals(4, instanceRepository.getNLastCreated(10).size());
    }

}