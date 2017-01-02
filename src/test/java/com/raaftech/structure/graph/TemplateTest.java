package com.raaftech.structure.graph;

import com.fasterxml.jackson.databind.JsonNode;
import com.raaftech.structure.base.SpringTestConfig;
import com.raaftech.structure.graph.repositories.InstanceRepository;
import com.raaftech.structure.graph.repositories.TemplateRepository;
import com.raaftech.structure.graph.repositories.TypeRepository;
import com.raaftech.structure.helper.loader.EvenNaiverLoader;
import com.raaftech.structure.next.Entity;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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

import static io.vertx.core.json.Json.mapper;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = SpringTestConfig.class)
public class TemplateTest {

    @Autowired
    private TemplateRepository templateRepository;
    @Autowired
    private TypeRepository typeRepository;
    @Autowired
    private InstanceRepository instanceRepository;
    @Autowired
    private GraphDatabase graphDatabase;

    @Before
    @SuppressWarnings("Duplicates")
    @Transactional
    public void setUpClass() throws Exception {
        Transaction tx = graphDatabase.beginTx();
        final String templateTestBundle = "src/test/resources/bundles/templatetest/";
        EvenNaiverLoader evenNaiverLoader = new EvenNaiverLoader(templateTestBundle);
        evenNaiverLoader.getUpdateList(evenNaiverLoader.sortIt(evenNaiverLoader.readAllJson())).stream()
                .map(node -> {
                    try {
                        return mapper.readValue(node.toString(), Entity.class);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                })
                .filter(node -> node != null)
                .forEach(Entity::toGraph);
        tx.success();
        tx.close();
    }

    @After
    @Transactional
    public void tearDown() throws Exception {
        Transaction tx = graphDatabase.beginTx();
        typeRepository.delete(typeRepository.findOneByName("Test1"));
        typeRepository.delete(typeRepository.findOneByName("Test2"));
        tx.success();
        tx.close();
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void addComponentWithException() {
        thrown.expect(RuntimeException.class);
        new Template("80148d1f-706a-4a78-aaa8-967ac31f2474")
                .addComponent(new Template("172f08da-e3f0-477f-88e7-d53de496739e"));
    }

    @Test
    @Transactional
    @Rollback
    public void addComponentSucceed() throws Exception {
        Template t1 = new Template("80148d1f-706a-4a78-aaa8-967ac31f2474")
                .setRequired(true)
                .setUnique(true)
                .setStartOfTemplate(true)
                .addComponent(new Template("172f08da-e3f0-477f-88e7-d53de496739e"));
        assertFalse(t1.getHasComponents().isEmpty());
        assertEquals("Test2", t1.getHasComponents().iterator().next().getName());
    }

    @Test
    @Transactional
    @Rollback
    public void getParentComponent() throws Exception {
        new Template("80148d1f-706a-4a78-aaa8-967ac31f2474")
                .setRequired(true)
                .setUnique(true)
                .setStartOfTemplate(true)
                .addComponent(new Template("172f08da-e3f0-477f-88e7-d53de496739e")).save();

        Template template = templateRepository.findOneByName("Test2");
        assertNotNull(template.getParentComponent());
        assertEquals("Test1", template.getParentComponent().getName());
    }

    @Test
    @Transactional
    @Rollback
    public void instantiateSimpleSucceed() throws Exception {
        Template template1 = new Template("80148d1f-706a-4a78-aaa8-967ac31f2474")
                .setRequired(true)
                .setUnique(true)
                .setStartOfTemplate(true)
                .addComponent(new Template("172f08da-e3f0-477f-88e7-d53de496739e")).save();
        Template template2 = templateRepository.findOneByName("Test2");

        String instanceData =
                "[{\n" +
                "    \"data\": {\n" +
                "        \"instances\": [{\n" +
                "            \"name\": \"Test1 instance\",\n" +
                "            \"desc\": \"wut\",\n" +
                "            \"foo\": \"lol\"\n" +
                "        }]\n" +
                "    },\n" +
                "    \"templateUuid\": \"" + template1.getUuid() + "\"\n" +
                "}, {\n" +
                "    \"data\": {\n" +
                "        \"instances\": [{\n" +
                "            \"name\": \"Test2 instance 1\",\n" +
                "            \"desc\": \"wut\",\n" +
                "            \"bar\": 6\n" +
                "        },{\n" +
                "            \"name\": \"Test2 instance 2\",\n" +
                "            \"desc\": \"wutwut\",\n" +
                "            \"bar\": 7\n" +
                "        }]\n" +
                "    },\n" +
                "    \"templateUuid\": \"" + template2.getUuid() + "\"\n" +
                "}]";

        // Instantiate the template
        JsonNode instanceDataObj = mapper.readTree(instanceData);
        Component startInstance = (Component) instanceRepository.findByUuid(template1.instantiate(instanceDataObj));

        assertTrue(startInstance.isOfType("Test1"));
        assertEquals(2, startInstance.getComponents().size());
    }

    @Test
    @Transactional
    @Rollback
    // Test instantiate a template with one optional node missing
    public void instantiateOptional() throws Exception {
        Template template1 = new Template("80148d1f-706a-4a78-aaa8-967ac31f2474")
                .setRequired(true)
                .setUnique(true)
                .setStartOfTemplate(true)
                .addComponent(new Template("172f08da-e3f0-477f-88e7-d53de496739e").setRequired(false)).save();

        String instanceData =
                "[{\n" +
                        "    \"data\": {\n" +
                        "        \"instances\": [{\n" +
                        "            \"name\": \"Test1 instance\",\n" +
                        "            \"desc\": \"wut\",\n" +
                        "            \"foo\": \"lol\"\n" +
                        "        }]\n" +
                        "    },\n" +
                        "    \"templateUuid\": \"" + template1.getUuid() + "\"\n" +
                        "}]";

        // Instantiate the template
        JsonNode instanceDataObj = mapper.readTree(instanceData);
        Component instance = (Component) instanceRepository.findByUuid(template1.instantiate(instanceDataObj));
        assertEquals(0, instance.getComponents().size());

        // Now do the same, only with Test2 required
        Template template2 = templateRepository.findOneByName("Test2");
        template2.setRequired(true).save();
        template1 = templateRepository.findOneByName("Test1");
        instanceDataObj = mapper.readTree(instanceData);
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Missing instancedata for non-optional Template Test2.");
        template1.instantiate(instanceDataObj);
    }

    @Test
    @Transactional
    @Rollback
    // This fails because the child component in the template is unique, and
    // we try to instantiate it twice
    public void instantiateSimpleFail() throws Exception {
        Template template1 = new Template("80148d1f-706a-4a78-aaa8-967ac31f2474")
                .setRequired(true)
                .setUnique(true)
                .setStartOfTemplate(true)
                .addComponent(new Template("172f08da-e3f0-477f-88e7-d53de496739e").setUnique(true)).save();
        Template template2 = templateRepository.findOneByName("Test2");

        String instanceData =
                "[{\n" +
                        "    \"data\": {\n" +
                        "        \"instances\": [{\n" +
                        "            \"name\": \"Test1 instance\",\n" +
                        "            \"desc\": \"wut\",\n" +
                        "            \"foo\": \"lol\"\n" +
                        "        }]\n" +
                        "    },\n" +
                        "    \"templateUuid\": \"" + template1.getUuid() + "\"\n" +
                        "}, {\n" +
                        "    \"data\": {\n" +
                        "        \"instances\": [{\n" +
                        "            \"name\": \"Test2 instance 1\",\n" +
                        "            \"desc\": \"wut\",\n" +
                        "            \"bar\": 6\n" +
                        "        },{\n" +
                        "            \"name\": \"Test2 instance 2\",\n" +
                        "            \"desc\": \"wutwut\",\n" +
                        "            \"bar\": 7\n" +
                        "        }]\n" +
                        "    },\n" +
                        "    \"templateUuid\": \"" + template2.getUuid() + "\"\n" +
                        "}]";

        // Instantiate the template
        JsonNode instanceDataObj = mapper.readTree(instanceData);
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Instantiating template failed on first validation.");
        template1.instantiate(instanceDataObj);
    }

    @Test
    public void isRequired() throws Exception {
        Template template = new Template("80148d1f-706a-4a78-aaa8-967ac31f2474");
        assertFalse(template.isRequired());
        template.setRequired(true);
        assertTrue(template.isRequired());
    }

    @Test
    public void setStartOfTemplate() throws Exception {
        Template template = new Template("80148d1f-706a-4a78-aaa8-967ac31f2474");
        assertFalse(template.isStartOfTemplate());
        template.setStartOfTemplate(true);
        assertTrue(template.isStartOfTemplate());
    }

    @Test
    public void setUnique() throws Exception {
        Template template = new Template("80148d1f-706a-4a78-aaa8-967ac31f2474");
        assertFalse(template.isStartOfTemplate());
        template.setStartOfTemplate(true);
        assertTrue(template.isStartOfTemplate());
    }

    @Test
    @Transactional
    @Rollback
    public void removeAllComponents() throws Exception {
        new Type("Test3", "Description", "172f08da-e3f0-477f-88e7-d53de496739f").save();
        new Type("Test4", "Description", "172f08da-e3f0-477f-88e7-d53de49673a0").save();
        Template template = new Template("80148d1f-706a-4a78-aaa8-967ac31f2474")
                .setRequired(true)
                .setUnique(true)
                .setStartOfTemplate(true)
                .addComponent(new Template("172f08da-e3f0-477f-88e7-d53de496739e")
                        .setUnique(true)
                        .setRequired(true)
                        .addComponent(new Template("172f08da-e3f0-477f-88e7-d53de49673a0")))
                .addComponent(new Template("172f08da-e3f0-477f-88e7-d53de496739f")).save();

        assertEquals(2, template.removeAllComponents());
        assertEquals(0, template.getHasComponents().size());

        // Make sure the Templates are actually deleted from the database
        assertNull(templateRepository.findOneByName("Test2"));
        assertNull(templateRepository.findOneByName("Test3"));
        assertNull(templateRepository.findOneByName("Test4"));
    }

    @Test
    @Transactional
    @Rollback
    public void removeComponent() throws Exception {
        new Template("80148d1f-706a-4a78-aaa8-967ac31f2474")
                .setRequired(true)
                .setUnique(true)
                .setStartOfTemplate(true)
                .addComponent(new Template("172f08da-e3f0-477f-88e7-d53de496739e")).save();

        Template template1 = templateRepository.findOneByName("Test1");
        assertNotNull(template1);
        assertEquals(1, template1.getHasComponents().size());
        Template template2 = templateRepository.findOneByName("Test2");
        template1.removeComponent(template2);
        assertEquals(0, template1.getHasComponents().size());
        // Make sure the Template is actually deleted from the database
        assertNull(templateRepository.findOneByName("Test2"));

        // template1 doesn't have any more children, next call should result in Exception
        thrown.expect(IndexOutOfBoundsException.class);
        template1.removeComponent(template2);
    }

    @Test
    @Transactional
    @Rollback
    public void setIsa() throws Exception {
        Template template = new Template("template", "foo", "23e0bacd-9365-43b7-87dd-4f0ade4f4b6f");
        template.setIsa("172f08da-e3f0-477f-88e7-d53de496739e");
        template.save();

        assertEquals("Test2", templateRepository.findOneByName("template").isa.getName());
    }

}