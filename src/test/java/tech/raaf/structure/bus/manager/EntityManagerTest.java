package tech.raaf.structure.bus.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.test.annotation.Rollback;
import tech.raaf.structure.schema.TypeSchemaTest;
import tech.raaf.structure.graph.Entity;
import tech.raaf.structure.graph.Node;
import tech.raaf.structure.graph.repositories.NodeRepository;
import tech.raaf.structure.helper.loader.NaiveSorter;

import java.util.Iterator;
import java.util.List;

import static io.vertx.core.json.Json.mapper;
import static org.junit.Assert.*;

public class EntityManagerTest extends TypeSchemaTest {

    @Autowired private NodeRepository nodeRepository;
    @Autowired private GraphDatabase graphDatabase;

    @Ignore("Need to fix")
    @Test
    public void testTraversal() throws Exception {
        Transaction tx = graphDatabase.beginTx();

        Node computerGrouping = nodeRepository.findByUuid("f05e325e-1aee-4892-96e4-8bcd6e97c528");
        EntityManager entityManager = new EntityManager();

        List<Node> foundNodes = entityManager.labelBasedTraversal(ImmutableSet.of("type:Component"), computerGrouping, true);
        assertEquals(12, foundNodes.size());
        assertTrue(foundNodes.contains(computerGrouping));

        foundNodes = entityManager.labelBasedTraversal(ImmutableSet.of("type:Component"), computerGrouping, false);
        assertEquals(11, foundNodes.size());
        assertTrue(!foundNodes.contains(computerGrouping));

        Node ipAddress = nodeRepository.findByUuid("299001a6-1091-4c0d-87be-c512af03e179");
        foundNodes = entityManager.labelBasedTraversal(ImmutableSet.of("type:Configuration"), ipAddress, false);
        assertEquals(2, foundNodes.size());

        Node linuxType = nodeRepository.findByUuid("a5613527-2f64-4481-83f2-80b0cb427e28");
        foundNodes = entityManager.labelBasedTraversal(ImmutableSet.of("Type"), linuxType, false);
        assertEquals(15, foundNodes.size());

        tx.close();
    }

    @Ignore("Need to fix")
    @Test
    @Rollback
    public void testCreate() throws Exception {
        Transaction tx = graphDatabase.beginTx();

        EntityManager entityManager = new EntityManager();

        // construct a message that creates a new entity
        String entityUuid = "94fa591f-375b-4fff-84f7-2c6018cc7707";
        ObjectNode message = mapper.createObjectNode();
        message.put("msgh", "create");
        ObjectNode args = message.putObject("args");
        ArrayNode entities = args.putArray("entities");
        entities.add(mapper.readTree("  {\n" +
                "    \"type\": \"Component\",\n" +
                "    \"name\": \"GenericOS\",\n" +
                "    \"desc\": \"Instance of a generic OS.\",\n" +
                "    \"meta\": {\n" +
                "      \"uuid\": \"" + entityUuid + "\",\n" +
                "      \"relations\": {\n" +
                "        \"INSTANCE_OF\": {\n" +
                "          \"direction\": \"outgoing\",\n" +
                "          \"uuids\": [\n" +
                "            \"f05e325e-1aee-4892-96e4-8bcd6e97c528\"\n" +
                "          ]\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"data\": {\n" +
                "      \"ver\": \"ver\",\n" +
                "      \"shortName\": \"shortName\",\n" +
                "      \"longName\": \"longName\"\n" +
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

        tx.close();
    }

    @Ignore("Need to fix")
    @Test
    @Rollback
    public void testRead() throws Exception {
        Transaction tx = graphDatabase.beginTx();

        EntityManager entityManager = new EntityManager();

        /*
        Construct a message that asks for every type that can be found by walking from
        the "Software" type following outgoing relationships.
         */

        ObjectNode message = mapper.createObjectNode();
        message.put("msgh", "read");
        ObjectNode args = message.putObject("args");
        ArrayNode labels = args.putArray("labels");
        labels.add("Type");
        args.put("start", "8db2b492-9a4a-41fe-97a5-bc625da042ea");

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
        assertEquals(4, data.size());

        // all items in that array should be deserializable to an Entity
        Iterator<JsonNode> iterator = data.elements();
        while (iterator.hasNext()) {
            mapper.treeToValue(iterator.next(), Entity.class);
        }
        tx.close();
    }

    @Ignore("Need to fix")
    @Test
    @Rollback
    public void testUpdate() throws Exception {
        Transaction tx = graphDatabase.beginTx();

        EntityManager entityManager = new EntityManager();
        final String componentGroupTypeUuid = "ee92c308-80f4-40b2-af3a-77d26cd99517";
        final String linuxTypeUuid = "a5613527-2f64-4481-83f2-80b0cb427e28";

        // get two nodes, of which we are going to update only one
        Node computerGrouping = nodeRepository.findByUuid(componentGroupTypeUuid);
        Node linuxType = nodeRepository.findByUuid(linuxTypeUuid);

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
        Assert.assertEquals(newName, nodeRepository.findByUuid(linuxType.getUuid()).getName());


        /*
         try an update again, this time a safe update, which means we send the old entities as well,
         and we compare the old versions to the graph version before we update.
          */
        computerGrouping = nodeRepository.findByUuid(componentGroupTypeUuid);
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
        computerGrouping = nodeRepository.findByUuid(componentGroupTypeUuid);
        computerGrouping.setDesc("Edited outside of EntityManager.");
        computerGrouping.setChecksum(NaiveSorter.calculateChecksum(mapper.valueToTree(computerGrouping),
                NaiveSorter.ChecksumAlgorithm.SHA1));
        nodeRepository.save(computerGrouping);

        // send the message constructed above and get the result
        response = entityManager.update().exec(message);
        result = (ObjectNode) response.get("result");

        // the result should not have an error property
        assertTrue(result.has("error"));
        tx.close();

    }

    @Ignore("Need to fix")
    @Test
    @Rollback
    public void testDelete() throws Exception {
        Transaction tx = graphDatabase.beginTx();

        EntityManager entityManager = new EntityManager();

        final String computerUuid = "ec11db91-6973-4285-b018-b461fbb432d7";
        final String ipv4AddressUuid = "5ca95f7a-5175-4e7f-8a61-862e24485af3";

        /*
         construct a message that tries to delete the "Computer" node, but will fail because we have outgoing
         relations to entities with labels that are in the label array
          */
        ObjectNode message = mapper.createObjectNode();
        message.put("msgh", "delete");
        ObjectNode args = message.putObject("args");
        ArrayNode labels = args.putArray("labels");
        labels.add("type:Component");
        args.put("uuid", computerUuid);

        // send the message constructed above and get the result
        JsonNode response = entityManager.delete().exec(message);
        ObjectNode result = (ObjectNode) response.get("result");

        // the result should have an error property
        assertTrue(result.has("error"));


        // Now try again, but pick a leaf node in the type tree
        args.put("uuid", ipv4AddressUuid);
        labels.removeAll();
        // We won't have an outgoing rel to anything with label Foo, so this will succeed
        labels.add("Foo");

        // send the message constructed above and get the result
        response = entityManager.delete().exec(message);
        result = (ObjectNode) response.get("result");

        // error should now be absent
        assertTrue(!result.has("error"));

        tx.close();
    }

    @Test
    @Rollback
    public void testSpecialize() throws Exception {
        Transaction tx = graphDatabase.beginTx();

        //FIXME a true instance should be created. Not yet sure how.
        EntityManager entityManager = new EntityManager();

        ObjectNode message = mapper.createObjectNode();
        message.put("msgh", "specialize");

        ObjectNode args = message.putObject("args");
        args.put("instance", "a5613527-2f64-4481-83f2-80b0cb427e28");
        args.put("isa", "1e13acac-4efb-4e62-894d-812492f6c266");

        System.out.println(message.toString());
        JsonNode response = entityManager.specialize().exec(message);
        ObjectNode result = (ObjectNode) response.get("result");
        System.out.println(result.toString());

        //for now should be false since we are trying to specialize a type instead of an instance
        assertFalse(result.has("result"));

        tx.close();
    }
}