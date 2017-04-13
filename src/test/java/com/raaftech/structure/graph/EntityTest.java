package com.raaftech.structure.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.raaftech.structure.base.SpringTestConfig;
import com.raaftech.structure.exceptions.EntityDeserializationException;
import com.raaftech.structure.exceptions.JsonValidationException;
import com.raaftech.structure.graph.repositories.NodeRepository;
import com.raaftech.structure.graph.repositories.TypeRepository;
import com.raaftech.structure.helper.Triple;
import com.raaftech.structure.next.Entity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static io.vertx.core.json.Json.mapper;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = SpringTestConfig.class)
public class EntityTest {

    // relationship names
    private static final String REL_ROOT = "root";
    private static final String REL_ISA = "ISA";
    // keys for file object
    private static final String ENTITY_FILE_DATASCHEMA = "dataSchema";
    private static final String ENTITY_FILE_ENCODING = "encoding";
    private static final String ENTITY_FILE_FORMAT = "format";
    private static final String ENTITY_FILE_DATASCHEMA_PATH = "path";
    private static final String ENTITY_FILE_CONTENT = "content";
    private static final String ENTITY_FILE_CHECKSUM = "checksum";


    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private TypeRepository typeRepository;
    @Autowired
    private GraphDatabase graphDatabase;
    @Autowired
    private Neo4jTemplate neo4jTemplate;

    private String createJson(String type, String name, String desc, String meta, String file, String data) {
        if(data == null) return String.format("{\"type\": \"%s\", \"name\": \"%s\", \"desc\": \"%s\",\"meta\": %s, \"file\": %s}", type, name, desc, meta, file);
        else return String.format("{\"type\": \"%s\", \"name\": \"%s\", \"desc\": \"%s\",\"meta\": %s, \"file\": %s, \"data\": %s}", type, name, desc, meta, file, data);
    }

    private String createMetaSection(String bundle, String uuid, String relations) {
        return String.format("{\"bundle\": \"%s\", \"uuid\": \"%s\", \"relations\": %s}", bundle, uuid, relations);
    }

    private String createFileSection(String encoding, String format, String checksum, String path, String content) {
        String output = String.format("{\"dataSchema\": {\"encoding\": \"%s\", \"format\": \"%s\"",encoding, format);
        if(checksum != null) output += String.format(",\"checksum\": \"%s\"", checksum);
        if(path != null) output += String.format(",\"path\": \"%s\"", path);
        if(content != null) output += String.format(",\"content\": \"%s\"", content);
        output += "}}";

        return output;
    }

    private String createRelationSection() {
        return "{\"INSTANCE_OF\":{\"direction\":\"outgoing\", \"uuids\": [ \"" + UUID.randomUUID().toString() + "\" ]}}";
    }

    @Test
    @Transactional
    @Rollback
    public void testSimpleNodePersistence() {
        // create a new entity with name "generation 1" and labels Node and _Node
        Entity entity = new Entity();
        entity.setName("generation_1");
        entity.setUuid(UUID.randomUUID().toString());
        entity.setType("Type");
        entity.setRelations(
                ImmutableSet.of(Triple.of("ISA", "outgoing", ImmutableSet.of(entity.getUuid()))));
        entity.toGraph();

        // make sure we can retrieve the entity from the database, and it's
        // equal to 'entity'
        Set<Node> set1 = nodeRepository.findByName("generation_1");
        assertEquals(1, set1.size());
        Node retrievedNode = set1.iterator().next();
//        assertEquals(entity, retrievedNode);

        // change the name of 'entity'
        retrievedNode.setName("generation 2");
        retrievedNode.setType("Component");
        nodeRepository.save(retrievedNode);

        // make sure we can't find 'entity' in the db by it's old name anymore
        set1 = nodeRepository.findByName("generation 1");
        assertEquals(0, set1.size());

        // make sure we can retrieve the entity from the database with its new name, and it's
        // equal to 'entity'
        Set<Node> set2 = nodeRepository.findByName("generation 2");
        assertEquals(1, set2.size());
        retrievedNode = set2.iterator().next();
//        assertEquals(entity, retrievedNode);

        // make sure entity has only two labels, Node and _Node
        org.neo4j.graphdb.Node node = entity.getPersistentState();
        assertTrue(allLabelsMatch(node.getLabels(), "Node", "Type", "_Type", "type:generation_1"));
    }

    /**
     * Test update procedure for an Entity. Creating two entities with the same UUID
     * should result in one node, with the properties of the one that was persisted last.
     *
     * This test logs error messages because no relationships are defined for the test entity.
     */
    @Test
    @Transactional
    @Rollback
    public void testEntityUpdate() {
        final String uuid = UUID.randomUUID().toString();
        Entity entity01 = new Entity(neo4jTemplate);
        entity01.setName("entity01");
        entity01.setUuid(uuid);
        entity01.setDesc("Test entity.");
        entity01.setType("Type");
        entity01.toGraph();

        // Database should contain only one entity
        assertEquals(1, StreamSupport.stream(nodeRepository.findAll().spliterator(), false).count());

        Node entity01Node = nodeRepository.findByUuid(uuid);
        assertNotNull(entity01Node);
        assertEquals(entity01.getName(), entity01Node.getName());
        assertEquals(entity01.getDesc(), entity01Node.getDesc());
        assertEquals(entity01.getType(), entity01Node.getType());

        Entity entity02 = new Entity(neo4jTemplate);
        entity02.setName("entity02");
        entity02.setUuid(uuid);
        entity02.setDesc("Test entity.");
        entity02.setType("Type");
        entity02.toGraph();

        // Database should still only contain one entity
        assertEquals(1, StreamSupport.stream(nodeRepository.findAll().spliterator(), false).count());

        Node entity02Node = nodeRepository.findByUuid(uuid);
        assertNotNull(entity02Node);
        assertEquals(entity02.getName(), entity02Node.getName());
        assertEquals(entity02.getDesc(), entity02Node.getDesc());
        assertEquals(entity02.getType(), entity02Node.getType());

        assertEquals(entity01Node.getId(), entity02Node.getId());

        Entity entity03 = new Entity(neo4jTemplate);
        entity03.setName("entity03");
        entity03.setUuid(UUID.randomUUID().toString());
        entity03.setDesc("Test entity.");
        entity03.setType("Type");
        entity03.toGraph();

        // Database should contain two entities
        assertEquals(2, StreamSupport.stream(nodeRepository.findAll().spliterator(), false).count());
    }

    @Test
    public void testRootNodeCreationFromJson() throws Exception {
        Transaction tx = graphDatabase.beginTx();
        final String bundlePath = "src/test/resources/bundles/simple";
        System.setProperty("bundlePath", bundlePath);
        final String rootFileLocation = bundlePath + "/root.type.json";
        final String componentFileLocation = bundlePath + "/component.type.json";

        // deserialize root entity
        Entity rootNode = new ObjectMapper().readValue(getDataFromFile(rootFileLocation), Entity.class);
        rootNode.setNeo4jTemplate(neo4jTemplate);

        // assert checks for simple fields
        final String rootUuid = "b1bc963a-19cc-4518-aecb-a35d028c49e3";
        final String rootType = "Type";
        final String rootBundle = "core";
        final String rootName = "Type";
        final String rootDesc = "The root type.";
        assertEquals(rootUuid, rootNode.getUuid());
        assertEquals(rootType, rootNode.getType());
        assertEquals(rootBundle, rootNode.getBundle());
        assertEquals(rootName, rootNode.getName());
        assertEquals(rootDesc, rootNode.getDesc());

        // make sure our entity isn't persisted to the database yet
        Node result = nodeRepository.findByUuid(rootUuid);
        assertNull(result);

        // persist
        rootNode.toGraph();

        // and now it should be persisted
        result = nodeRepository.findByUuid(rootUuid);
        assertNotNull(result);

        // make sure the retrieved entity is the same as our deserialized entity
//        assertEquals(rootNode, result);
        System.out.println(result.toJson());
        assertEquals(rootUuid, result.getUuid());
        assertEquals(rootType, result.getType());
        assertEquals(rootBundle, result.getBundle());
        assertEquals(rootName, result.getName());
        assertEquals(rootDesc, result.getDesc());

        // get the persisted state of the entity
        org.neo4j.graphdb.Node node = rootNode.getPersistentState();

        // make sure the labels are correct
        assertTrue(allLabelsMatch(node.getLabels(), "Node", "_Type", "Type", "type:Type"));

        System.out.println(typeRepository.findOneByName(rootName).toJson());

        // make sure data and files are empty
        assertEquals(1, rootNode.getFiles().size());
        assertEquals(0, rootNode.getData().size());

        // get relationships
        Set<Relationship> relationships = StreamSupport.stream(node.getRelationships().spliterator(), false)
                .collect(Collectors.toSet());
        assertEquals(0, relationships.size());

        // deserialize component entity
        Entity componentNode = new ObjectMapper().readValue(getDataFromFile(componentFileLocation), Entity.class);
        componentNode.setNeo4jTemplate(neo4jTemplate);

        // assert checks for simple fields
        final String componentUuid = "a79ce509-a021-4177-ae61-73abd01fb479";
        final String componentType = "Type";
        final String componentBundle = "core";
        final String componentName = "Component";
        final String componentDesc = "An abstract base component.";
        assertEquals(componentUuid, componentNode.getUuid());
        assertEquals(componentType, componentNode.getType());
        assertEquals(componentBundle, componentNode.getBundle());
        assertEquals(componentName, componentNode.getName());
        assertEquals(componentDesc, componentNode.getDesc());

        // make sure out entity isn't persisted to the database yet
        result = nodeRepository.findByUuid(componentUuid);
        assertNull(result);

        // persist
        componentNode.toGraph();

        // and now it should be persisted
        result = nodeRepository.findByUuid(componentUuid);
        assertNotNull(result);
//        assertEquals(componentNode, result);
        assertEquals(componentUuid, result.getUuid());
        assertEquals(componentType, result.getType());
        assertEquals(componentBundle, result.getBundle());
        assertEquals(componentName, result.getName());
        assertEquals(componentDesc, result.getDesc());

        // get the persisted state of the entity
        node = componentNode.getPersistentState();

        // check if an ISA exists between component and root
        relationships = StreamSupport.stream(node.getRelationships().spliterator(), false)
                .collect(Collectors.toSet());
        assertEquals(1, relationships.size());
        Relationship relationship = relationships.iterator().next();
        assertEquals(node, relationship.getStartNode());
        assertEquals(rootNode.getPersistentState(), relationship.getEndNode());
        assertEquals(REL_ISA, relationship.getType().name());

        // check if file contains 1 item and data is empty
        assertEquals(0, componentNode.getData().size());
        assertEquals(1, componentNode.getFiles().size());

        // check the file object
        assertTrue(componentNode.getFiles().containsKey(ENTITY_FILE_DATASCHEMA));
        assertTrue(componentNode.getFiles().get(ENTITY_FILE_DATASCHEMA) instanceof String);
        String dataSchemaVal = (String) componentNode.getFiles().get(ENTITY_FILE_DATASCHEMA);
        ObjectNode dataSchema = (ObjectNode) mapper.readTree(dataSchemaVal);
        assertTrue(dataSchema.has(ENTITY_FILE_CONTENT));
        assertTrue(dataSchema.has(ENTITY_FILE_ENCODING));
        assertTrue(dataSchema.has(ENTITY_FILE_FORMAT));
        assertTrue(dataSchema.has(ENTITY_FILE_DATASCHEMA_PATH));
        assertTrue(dataSchema.has(ENTITY_FILE_CHECKSUM));

        // make sure the labels are correct
        assertTrue(allLabelsMatch(node.getLabels(), "Node", "_Type", "Type", "type:Type", "type:Component"));

        // create a child of component called foo
        Entity componentChild = new Entity();
        componentChild.setNeo4jTemplate(neo4jTemplate);
        componentChild.setType("Type");
        Set<Triple<String, String, Set<String>>> rel = new HashSet<>();
        Set<String> uuids = new HashSet<>();
        uuids.add(componentUuid);
        rel.add(new Triple<>("ISA", "outgoing", uuids));
        componentChild.setUuid(UUID.randomUUID().toString());
        componentChild.setRelations(rel);
        componentChild.setName("Foo");
        componentChild.toGraph();

        assertTrue(allLabelsMatch(componentChild.getPersistentState().getLabels(), "Node", "_Type", "Type", "type:Type",
               "type:Component", "type:Foo"));
        tx.failure();
        tx.close();
    }

    /**
     * Test the deserialization of an Node JSON with a correctly defined embedded file
     */
    @Test
    public void testCorrectEmbeddedFileWithPathDeserialization() throws Exception {
        final String encoding = "text";
        final String format = "json";
        final String path = "component.schema.json";
        final String content = "{\\n  \\\"$schema\\\": \\\"http://json-schema.org/schema#\\\",\\n  \\\"additionalProperties\\\": false,\\n  \\\"type\\\": \\\"object\\\",\\n  \\\"properties\\\": {\\n    \\\"name\\\": {\\n      \\\"type\\\": \\\"string\\\",\\n      \\\"description\\\": \\\"The name of this node.\\\"\\n    },\\n    \\\"desc\\\": {\\n      \\\"type\\\": \\\"string\\\",\\n      \\\"description\\\": \\\"The description of this node.\\\"\\n    }\\n  },\\n  \\\"required\\\": [\\n    \\\"name\\\",\\n    \\\"desc\\\"\\n  ]\\n}";
        final String entityJson = createJson("Type", "Component", "An abstract base component.",
                createMetaSection("core", "a79ce509-a021-4177-ae61-73abd01fb479",createRelationSection()),
                createFileSection(encoding, format, null, path,content), null);

        Entity entity = mapper.readValue(entityJson, Entity.class);

        // check the file object
        assertTrue(entity.getFiles().containsKey(ENTITY_FILE_DATASCHEMA));
        assertTrue(entity.getFiles().get(ENTITY_FILE_DATASCHEMA) instanceof String);
        String dataSchemaVal = (String) entity.getFiles().get(ENTITY_FILE_DATASCHEMA);
        ObjectNode dataSchema = (ObjectNode) mapper.readTree(dataSchemaVal);
        assertTrue(dataSchema.has(ENTITY_FILE_CONTENT));
        assertTrue(dataSchema.has(ENTITY_FILE_ENCODING));
        assertEquals(encoding, dataSchema.get(ENTITY_FILE_ENCODING).asText());
        assertTrue(dataSchema.has(ENTITY_FILE_FORMAT));
        assertEquals(format, dataSchema.get(ENTITY_FILE_FORMAT).asText());
        assertTrue(dataSchema.has(ENTITY_FILE_DATASCHEMA_PATH));
        assertEquals(path, dataSchema.get(ENTITY_FILE_DATASCHEMA_PATH).asText());
    }

    /**
     * Test the deserialization of an Node JSON with a correctly defined embedded file
     * without a defined path
     */
    @Test
    public void testCorrectEmbeddedFileWithoutPathDeserialization() throws Exception {
        final String encoding = "text";
        final String format = "json";
        final String path = "component.type.dataSchema.json";
        final String content = "{\\n  \\\"$schema\\\": \\\"http://json-schema.org/schema#\\\",\\n  \\\"additionalProperties\\\": false,\\n  \\\"type\\\": \\\"object\\\",\\n  \\\"properties\\\": {\\n    \\\"name\\\": {\\n      \\\"type\\\": \\\"string\\\",\\n      \\\"description\\\": \\\"The name of this node.\\\"\\n    },\\n    \\\"desc\\\": {\\n      \\\"type\\\": \\\"string\\\",\\n      \\\"description\\\": \\\"The description of this node.\\\"\\n    }\\n  },\\n  \\\"required\\\": [\\n    \\\"name\\\",\\n    \\\"desc\\\"\\n  ]\\n}";
        final String entityJson = createJson("Type", "Component", "An abstract base component.",
                createMetaSection("core", "a79ce509-a021-4177-ae61-73abd01fb479", createRelationSection()),
                createFileSection(encoding, format, null, null, content), null);
        Entity entity = mapper.readValue(entityJson, Entity.class);

        // check the file object
        assertTrue(entity.getFiles().containsKey(ENTITY_FILE_DATASCHEMA));
        assertTrue(entity.getFiles().get(ENTITY_FILE_DATASCHEMA) instanceof String);
        String dataSchemaVal = (String) entity.getFiles().get(ENTITY_FILE_DATASCHEMA);
        ObjectNode dataSchema = (ObjectNode) mapper.readTree(dataSchemaVal);
        assertTrue(dataSchema.has(ENTITY_FILE_CONTENT));
        assertTrue(dataSchema.has(ENTITY_FILE_ENCODING));
        assertEquals(encoding, dataSchema.get(ENTITY_FILE_ENCODING).asText());
        assertTrue(dataSchema.has(ENTITY_FILE_FORMAT));
        assertEquals(format, dataSchema.get(ENTITY_FILE_FORMAT).asText());
        assertTrue(dataSchema.has(ENTITY_FILE_DATASCHEMA_PATH));
        assertEquals(path, dataSchema.get(ENTITY_FILE_DATASCHEMA_PATH).asText());
    }

    /**
     * Test the deserialization of an Node JSON without path or content
     */
    @Test
    public void testNoPathNoContentDeserialization() throws Exception {
        final String encoding = "text";
        final String format = "json";
        final String entityJson = createJson("Type", "Component", "An abstract base component.",
                createMetaSection("core", "a79ce509-a021-4177-ae61-73abd01fb479", createRelationSection()),
                createFileSection(encoding, format,null,null,null), null);

        exception.expect(JsonValidationException.class);
        mapper.readValue(entityJson, Entity.class);
    }

    /**
     * Test the deserialization of an Node JSON with an incorrectly defined embedded file
     */
    @Test
    public void testIncorrectEmbeddedFileDeserialization() throws Exception {
        final String entityJson = createJson("Type", "Component", "An abstract base component.",
                createMetaSection("core", "a79ce509-a021-4177-ae61-73abd01fb479", createRelationSection()),
                createFileSection("text", "json",null, "component.schema.json", "{\\\"$schema\\\": \\\"INVALIDJSON\\\""), null);

        exception.expect(EntityDeserializationException.class);
        mapper.readValue(entityJson, Entity.class);
    }

    /**
     * Test the deserialization of an Node JSON where content matches the checksum
     */
    @Test
    public void testCorrectChecksumFileDeserialization() throws Exception {
        final String encoding = "text";
        final String format = "json";
        final String path = "component.schema.json";
        final String content = "{\\\"foo\\\":\\\"bar\\\"}";
        final String checksum = "e90bf31cb76a0d7807312c4a3d2b5a96c4313101";
        final String entityJson = createJson("Type", "Component", "An abstract base component.",
                createMetaSection("core", "a79ce509-a021-4177-ae61-73abd01fb479", createRelationSection()),
                createFileSection(encoding, format,checksum, path, content), null);

        mapper.readValue(entityJson, Entity.class);
    }

    /**
     * Test the deserialization of an Node JSON where content doesn't match the checksum
     */
    @Test
    public void testIncorrectChecksumFileDeserialization() throws Exception {
        final String encoding = "text";
        final String format = "json";
        final String path = "component.schema.json";
        final String content = "{\\\"foo\\\":\\\"bar\\\"}";
        final String checksum = "cafebabedeadbeeff01dab1eca11ab1edeadbea7";
        final String entityJson = createJson("Type", "Component", "An abstract base component.",
                createMetaSection("core", "a79ce509-a021-4177-ae61-73abd01fb479", createRelationSection()),
                createFileSection(encoding, format,checksum, path, content), null);

        exception.expect(EntityDeserializationException.class);
        mapper.readValue(entityJson, Entity.class);
    }

    @Test
    public void testInvalidDataDeserialization() throws Exception {
        final String componentJson = createJson("Type", "Component", "An abstract base component.",
                createMetaSection("core", "a79ce509-a021-4177-ae61-73abd01fb479", createRelationSection()),
                createFileSection("text", "json",null, "component.schema.json", null),
                "{\"invalid\": \"foo\", \"object\": \"bar\"}");

        exception.expect(EntityDeserializationException.class);
        mapper.readValue(componentJson, Entity.class);
    }

    /**
     * Check if a set of labels completely matches some strings
     * @param labels The set of labels, usually provided by a neo4j backed node.
     * @param keywords The strings to be matched
     * @return Are the names of the labels exactly the same as _all_ the given strings? Then true.
     */
    private boolean allLabelsMatch(Iterable<Label> labels, String... keywords) {
        final Set<String> match = Collections.unmodifiableSet(Stream.of(keywords).distinct().collect(Collectors.toSet()));
        final Set<String> labelStrings = Collections.unmodifiableSet(
                StreamSupport.stream(labels.spliterator(), true)
                        .map(Label::name)
                        .collect(Collectors.toSet()));

        return match.size() == labelStrings.size() && labelStrings.stream().allMatch(match::contains);
    }

    byte[] getDataFromFile(String fileLocation) throws IOException {
        File file = new File(fileLocation);
        Path filePath = file.toPath();
        return Files.readAllBytes(filePath);
    }
}