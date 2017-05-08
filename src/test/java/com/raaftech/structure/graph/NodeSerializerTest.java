package com.raaftech.structure.graph;

import com.fasterxml.jackson.databind.JsonNode;
import com.raaftech.structure.base.SpringTestConfig;
import com.raaftech.structure.graph.repositories.NodeRepository;
import com.raaftech.structure.helper.JsonHelper;
import com.raaftech.structure.helper.Triple;
import com.raaftech.structure.helper.loader.BundleLoader;
import com.raaftech.structure.helper.loader.NaiveSorter;
import com.raaftech.structure.next.Entity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Set;

import static io.vertx.core.json.Json.mapper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = SpringTestConfig.class)
public class NodeSerializerTest {

    @Autowired
    NodeRepository nodeRepository;

    private boolean databaseCreated = false;

    @Before
    @Transactional
    @SuppressWarnings("Duplicates")
    public void setUpDatabase() throws Exception {
        if (!databaseCreated) {
            final String raafBundle = "src/test/resources/bundles/core/";
            BundleLoader bundleLoader = new BundleLoader(raafBundle);
            bundleLoader.load();
            databaseCreated = true;
        }
    }

    /*
     * Test serialization of a node, test the resulting json according to schema and property values
     */
    @Test
    @Transactional
    public void testDeserializationSerialization() throws Exception {
        Node componentNode = nodeRepository.findOneByName("Linux");
        assertNotNull(componentNode);

        String serializedJson = mapper.writeValueAsString(componentNode);
        assertTrue(JsonHelper.validateToSchema(JsonHelper.jsonFromResource("schema/next/entity.schema.json"),
                mapper.readTree(serializedJson)));
        Entity entity = mapper.readValue(serializedJson, Entity.class);
        assertNotNull(entity);
        assertEquals("Linux", entity.getName());
        assertEquals("The Linux Operating System.", entity.getDesc());
        assertEquals("a5613527-2f64-4481-83f2-80b0cb427e28", entity.getUuid());

        // test relationships
        assertEquals(1, entity.getRelations().size());
        Triple<String, String, Set<String>> rel = entity.getRelations().iterator().next();
        assertEquals("ISA", rel.getLeft());
        assertEquals("outgoing", rel.getMiddle());
        Set<String> peers = rel.getRight();
        assertEquals(1, peers.size());
        assertEquals("6a1eea8b-1462-4439-8c60-12edbee6c397", peers.iterator().next());

        // test files
        assertNotNull(entity.getFiles());
        JsonNode dataSchema = mapper.readTree((String)entity.getFiles().get("dataSchema"));
        assertNotNull(dataSchema);
        assertEquals("text", dataSchema.get("encoding").asText());
        assertEquals("json", dataSchema.get("format").asText());
        assertEquals("schemas/linux.schema.json", dataSchema.get("path").asText());
    }

    @Test
    @Transactional
    public void testIncomingRelationshipSerialization() throws Exception {
        /*
        Bash and bashInvoke have outgoing relationships towards each other. This will
        cause issues during loading with NaiveSorter. For that reason we need to make sure
        one of the relationships is serialized as an incoming relationship.
         */
        Node bash = nodeRepository.findOneByName("Bash");
        Node bashInvoke = nodeRepository.findOneByName("BashInvoke");

        assertNotNull(bash);
        assertNotNull(bashInvoke);

        JsonNode bashObj = mapper.valueToTree(bash);
        JsonNode bashInvokeObj = mapper.valueToTree(bashInvoke);

        /*
        Bash has an outgoing "HAS_INSTRUMENT" relationship to BashInvoke, but we persist that
        as an incoming "HAS_INSTRUMENT" on BashInvoke.
         */
        assertTrue(!bashObj.get("meta").get("relations").has("HAS_INSTRUMENT"));
        assertTrue(bashInvokeObj.get("meta").get("relations").has("HAS_INSTRUMENT"));
        assertEquals("incoming", bashInvokeObj.get("meta").get("relations").get("HAS_INSTRUMENT").get("direction").asText());
    }
}