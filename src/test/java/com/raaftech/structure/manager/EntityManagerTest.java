package com.raaftech.structure.manager;

import com.google.common.collect.ImmutableSet;
import com.raaftech.structure.base.SpringTestConfig;
import com.raaftech.structure.graph.Node;
import com.raaftech.structure.graph.repositories.NodeRepository;
import com.raaftech.structure.helper.loader.EvenNaiverLoader;
import com.raaftech.structure.next.Entity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

import static io.vertx.core.json.Json.mapper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = SpringTestConfig.class)
public class EntityManagerTest {

    @Autowired private GraphDatabase graphDatabase;
    @Autowired private Neo4jTemplate neo4jTemplate;
    @Autowired private NodeRepository nodeRepository;

    private boolean databaseCreated;

    @Before
    @Transactional
    @SuppressWarnings("Duplicates")
    public void setUp() throws Exception {
        if (!databaseCreated) {
            Transaction tx = graphDatabase.beginTx();
            final String templateTestBundle = "src/test/resources/bundles/core/";
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

            databaseCreated = true;
        }
    }

    @Test
    public void testTraversal() throws Exception {
        Node computerGrouping = nodeRepository.findByUuid("84b97c5c-5c5c-4654-b102-706e036f4374");
        EntityManager entityManager = new EntityManager();

        List<Node> foundNodes = entityManager.labelBasedTraversal(ImmutableSet.of("Component"), computerGrouping, true);
        assertEquals(8, foundNodes.size());
        assertTrue(foundNodes.contains(computerGrouping));

        foundNodes = entityManager.labelBasedTraversal(ImmutableSet.of("Component"), computerGrouping, false);
        assertEquals(7, foundNodes.size());
        assertTrue(!foundNodes.contains(computerGrouping));

        Node operatingSystem = nodeRepository.findByUuid("73eacb63-0c2c-4632-9b37-ee9e4d2bef75");
        foundNodes = entityManager.labelBasedTraversal(ImmutableSet.of("Configuration"), operatingSystem, false);
        assertEquals(2, foundNodes.size());

        Node linuxType = nodeRepository.findByUuid("a5613527-2f64-4481-83f2-80b0cb427e28");
        foundNodes = entityManager.labelBasedTraversal(ImmutableSet.of("Type"), linuxType, false);
        assertEquals(5, foundNodes.size());
    }
}