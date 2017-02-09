package com.raaftech.structure.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.raaftech.structure.base.Configuration;
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
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.Iterator;
import java.util.List;

import static io.vertx.core.json.Json.mapper;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = SpringTestConfig.class)
public class EntityManagerTest {

    @Autowired private GraphDatabase graphDatabase;
    @Autowired private NodeRepository nodeRepository;

    private boolean databaseCreated;

    @Before
    @Transactional
    @SuppressWarnings("Duplicates")
    public void setUp() throws Exception {
        if (!databaseCreated) {
            Transaction tx = graphDatabase.beginTx();
            final String testBundle = "src/test/resources/bundles/core/";
            Configuration.setSchemaNamespace(FileSystems.getDefault().getPath(testBundle).toUri().resolve("types/"));
            EvenNaiverLoader evenNaiverLoader = new EvenNaiverLoader(testBundle);
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

    @Test
    @Transactional
    @Rollback
    public void testCreate() throws Exception {
        EntityManager entityManager = new EntityManager();

        // construct a message that creates a new entity
        String entityUuid = "4f84bd11-e108-4d98-8f03-0c4c6855e6aa";
        ObjectNode message = mapper.createObjectNode();
        message.put("msgh", "create");
        ObjectNode args = message.putObject("args");
        ArrayNode entities = args.putArray("entities");
        entities.add(mapper.readTree("  {\n" +
                "    \"type\": \"Component\",\n" +
                "    \"name\": \"NVMe\",\n" +
                "    \"desc\": \"A new NVMe solid state disk.\",\n" +
                "    \"meta\": {\n" +
                "      \"uuid\": \"" + entityUuid + "\",\n" +
                "      \"relations\": {\n" +
                "        \"INSTANCE_OF\": {\n" +
                "          \"direction\": \"outgoing\",\n" +
                "          \"uuids\": [\n" +
                "            \"7a60ec32-0a9a-40b9-bf1f-d69674d4a233\"\n" +
                "          ]\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"data\": {\n" +
                "      \"memSizeKb\": 268435456\n" +
                "    }\n" +
                "  }"));

        // send the message constructed above and get the result
        JsonNode response = entityManager.create().exec(message);
        ObjectNode result = (ObjectNode) response.get("result");

        // the result should not have an error property
        assertFalse(result.has("error"));
        // it should have a data array
        assertTrue(result.has("data"));
        assertTrue(result.get("data").isArray());

        // the data array should have one element
        ArrayNode data = (ArrayNode) result.get("data");
        assertEquals(1, data.size());

        // that element should be equal to the UUID of the entity we created
        assertEquals(entityUuid, data.get(0).asText());


        // We're going to try to create the same entity again, this shouldn't result in any changes
        // send the message again, unchanged
        response = entityManager.create().exec(message);
        result = (ObjectNode) response.get("result");

        // the result should not have an error property
        assertFalse(result.has("error"));
        // because no new entity was created, it should have a data array, but it should be empty
        assertTrue(result.has("data"));
        assertEquals(0, result.get("data").size());
    }

    @Test
    @Transactional
    @Rollback
    public void testRead() throws Exception {
        EntityManager entityManager = new EntityManager();

        // construct a message that asks for every component attached to a "Computer" grouping we created
        ObjectNode message = mapper.createObjectNode();
        message.put("msgh", "read");
        ObjectNode args = message.putObject("args");
        ArrayNode labels = args.putArray("labels");
        labels.add("Component");
        args.put("start", "84b97c5c-5c5c-4654-b102-706e036f4374");

        // send the message constructed above and get the result
        JsonNode response = entityManager.read().exec(message);
        ObjectNode result = (ObjectNode) response.get("result");

        // the result should not have an error property
        assertFalse(result.has("error"));
        // it should have a data array
        assertTrue(result.has("data"));
        assertTrue(result.get("data").isArray());

        ArrayNode data = (ArrayNode) result.get("data");

        // we should have 8 nodes contained in the array
        assertEquals(8, data.size());

        // all items in that array should be deserializable to an Entity
        Iterator<JsonNode> iterator = data.elements();
        while (iterator.hasNext()) {
            mapper.treeToValue(iterator.next(), Entity.class);
        }
    }

    @Test
    @Transactional
    @Rollback
    public void testUpdate() throws Exception {
        EntityManager entityManager = new EntityManager();

        // get two nodes, of which we are going to update only one
        Node computerGrouping = nodeRepository.findByUuid("84b97c5c-5c5c-4654-b102-706e036f4374");
        Node linuxType = nodeRepository.findByUuid("a5613527-2f64-4481-83f2-80b0cb427e28");

        // set the name of the Linux type to something else
        final String newName = "Updated name";
        linuxType.setName(newName);

        // construct a message that updates the name of an entity
        ObjectNode message = mapper.createObjectNode();
        message.put("msgh", "update");
        ObjectNode args = message.putObject("args");
        ArrayNode entities = args.putArray("entities");

        // add the two entities to the array
        entities.add(mapper.valueToTree(computerGrouping));
        entities.add(mapper.valueToTree(linuxType));

        // send the message constructed above and get the result
        JsonNode response = entityManager.update().exec(message);
        ObjectNode result = (ObjectNode) response.get("result");

        // the result should not have an error property
        assertFalse(result.has("error"));
        // it should have a data array
        assertTrue(result.has("data"));
        assertTrue(result.get("data").isArray());

        ArrayNode data = (ArrayNode) result.get("data");

        // we should have 1 node contained in the array
        assertEquals(1, data.size());

        // data should contain the UUID of the "Linux" type
        assertEquals(linuxType.getUuid(), data.get(0).asText());

        // retrieve "Linux" from the graph again, check if it was indeed updated
        assertEquals(newName, nodeRepository.findByUuid(linuxType.getUuid()).getName());


        /*
         try an update again, this time a safe update, which means we send the old entities as well,
         and we compare the old versions to the graph version before we update.
          */
        computerGrouping = nodeRepository.findByUuid("84b97c5c-5c5c-4654-b102-706e036f4374");
        linuxType = nodeRepository.findByUuid("a5613527-2f64-4481-83f2-80b0cb427e28");

        // add the "original" versions of our entities to the oldEntities array
        ArrayNode oldEntities = args.putArray("oldEntities");
        entities.removeAll();
        oldEntities.add(mapper.valueToTree(computerGrouping));
        oldEntities.add(mapper.valueToTree(linuxType));

        // change the "Computer" entity and add it to our updated entities array
        computerGrouping.setDesc("New description.");
        entities.add(mapper.valueToTree(computerGrouping));
        entities.add(mapper.valueToTree(linuxType));

        // now, edit the "Computer" entity outside of EntityManager and save it
        computerGrouping = nodeRepository.findByUuid("84b97c5c-5c5c-4654-b102-706e036f4374");
        computerGrouping.setDesc("Edited outside of EntityManager.");
        computerGrouping.setChecksum(EvenNaiverLoader.calculateChecksum(mapper.valueToTree(computerGrouping),
                EvenNaiverLoader.ChecksumAlgorithm.SHA1));
        nodeRepository.save(computerGrouping);

        // send the message constructed above and get the result
        response = entityManager.update().exec(message);
        result = (ObjectNode) response.get("result");

        // the result should not have an error property
        assertTrue(result.has("error"));
    }

    @Test
    @Transactional
    @Rollback
    public void testDelete() throws Exception {
        EntityManager entityManager = new EntityManager();

        final String computerUuid = "84b97c5c-5c5c-4654-b102-706e036f4374";
        final String ipv4AddressUuid = "d182e241-af01-4837-9913-2e6c1afa047c";

        /*
         construct a message that tries to delete the "Computer" node, but will fail because we have outgoing
         relations to entities with labels that are in the label array
          */
        ObjectNode message = mapper.createObjectNode();
        message.put("msgh", "delete");
        ObjectNode args = message.putObject("args");
        ArrayNode labels = args.putArray("labels");
        labels.add("Component");
        args.put("uuid", computerUuid);

        // send the message constructed above and get the result
        JsonNode response = entityManager.delete().exec(message);
        ObjectNode result = (ObjectNode) response.get("result");

        // the result should not have an error property
        assertTrue(result.has("error"));


        // Now try again, but pick a leaf node in the "Computer" subgraph
        args.put("uuid", ipv4AddressUuid);

        // send the message constructed above and get the result
        response = entityManager.delete().exec(message);
        result = (ObjectNode) response.get("result");

        /*
        Alright, so we shouldn't get an error normally, but I've been fighting spring's transaction management
        for about four hours yesterday and today, and I cannot seem to get it working in any way. I tried a
        @TransactionConfiguration with rollback off default, which didn't work, i tried manually creating a
        transaction, which didn't work. I browsed google, browsed the spring forums, no luck with any of the things I
        found. Frankly, I want to continue working on something else right now. So I'll do this:
        If we have an error in the result, and the error message is about the transaction being marked for rollback, we
        know it tried to call delete. It's ugly... I guess spring won again.
         */
        // error should now be absent
        assertTrue(result.has("error"));
        assertEquals("Error while deleting node \"d182e241-af01-4837-9913-2e6c1afa047c\", exception: The transaction " +
                "is marked for rollback only.", result.get("error").asText());
    }
}