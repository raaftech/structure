package com.raaftech.structure.graph.repositories;

import com.raaftech.structure.base.SpringTestConfig;
import com.raaftech.structure.graph.Component;
import com.raaftech.structure.graph.Type;
import com.raaftech.structure.helper.loader.EvenNaiverLoader;
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
import java.util.Set;

import static io.vertx.core.json.Json.mapper;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = SpringTestConfig.class)
public class ComponentRepositoryTest {

    @Autowired
    ComponentRepository componentRepository;
    @Autowired
    TypeRepository typeRepository;
    @Autowired
    GraphDatabase graphDatabase;
    private boolean databaseCreated;

    private Type parentType;
    private Type childType;

    @Before
    @Transactional
    @SuppressWarnings("Duplicates")
    public void setUp() throws Exception {
        if (!databaseCreated) {
            Transaction tx = graphDatabase.beginTx();
            final String templateTestBundle = "src/test/resources/bundles/repositorytests/";
            EvenNaiverLoader evenNaiverLoader = new EvenNaiverLoader(templateTestBundle);
            evenNaiverLoader.getUpdateList(evenNaiverLoader.sortIt(evenNaiverLoader.readAllJson())).stream()
                    .map(node -> {
                        try {
                            return mapper.readValue(node.toString(), Entity.class);
                        } catch (IOException e) {
                            throw new RuntimeException("Error while creating types");
                        }
                    })
                    .filter(node -> node != null)
                    .forEach(Entity::toGraph);

            parentType = typeRepository.findOneByName("Software");
            childType = typeRepository.findOneByName("Linux");
            Component parent = new Component(mapper.createObjectNode()
                    .put("name", "parent")
                    .put("desc", "Parent.")
                    .put("checksum", "f1d2d2f924e986ac86fdf7b36c94bcdf32beec15")
                    .put("ver", "1")
                    .put("shortName", "parent")
                    .put("longName", "parent"), parentType);
            Component child = new Component(mapper.createObjectNode()
                    .put("name", "child")
                    .put("desc", "Child.")
                    .put("checksum", "f1d2d2f924e986ac86fdf7b36c94bcdf32beec15")
                    .put("ver", "1")
                    .put("shortName", "parent")
                    .put("longName", "parent"), childType);
            parent.addComponent(child);
            parent.save();

            tx.success();
            tx.close();
            databaseCreated = true;
        }
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