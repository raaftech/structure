package tech.raaf.structure.graph.repositories;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.transaction.annotation.Transactional;
import tech.raaf.structure.schema.TypeSchemaTest;
import tech.raaf.structure.graph.Component;
import tech.raaf.structure.graph.Group;
import tech.raaf.structure.graph.Type;

import static io.vertx.core.json.Json.mapper;
import static org.junit.Assert.*;

public class InstanceRepositoryTest extends TypeSchemaTest {

    @Autowired
    InstanceRepository instanceRepository;
    @Autowired
    TypeRepository typeRepository;
    @Autowired
    GraphDatabase graphDatabase;

    private static Group group;
    private static Component parent, child01, child02;

    @Before
    @Transactional
    public void setUp() throws Exception {
        Type groupingType = typeRepository.findOneByName("Group");
        assertNotNull(groupingType);
        Type parentType = typeRepository.findOneByName("Software");
        assertNotNull(parentType);
        Type childType = typeRepository.findOneByName("Linux");
        assertNotNull(childType);
        if (group == null) {
            group = new Group(
                    mapper.createObjectNode()
                            .put("name", "Group")
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
            group.getGroupees().add(parent);
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
        group.save();
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
        // Linux reaches Software through UnixLike and OperatingSystem
        assertTrue(child01.isOfType("Linux"));
        assertTrue(child01.isOfType("Software"));
        assertTrue(child01.isOfType(typeRepository.findOneByName("Component")));
        assertFalse(child01.isOfType("Interpreter"));
        assertFalse(parent.isOfType("Linux"));
        assertFalse(child01.isOfType((Type) null));
    }

    @Test
    public void getStartOfComponentGroup() throws Exception {
        Assert.assertEquals(group.getUuid(), instanceRepository.getStartOfComponentGroup(child01.getUuid()).getUuid());
    }

    // Disabled this test. We don't test against a static bundle anymore so this test is hopeless
    public void getNLastCreated() throws Exception {
        assertEquals(4, instanceRepository.getNLastCreated(10).size());
    }

}