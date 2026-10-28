package tech.raaf.structure.graph.repositories;

import static org.junit.jupiter.api.Assertions.*;
import static tech.raaf.structure.helper.JsonHelper.mapper;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.graph.Component;
import tech.raaf.structure.graph.Entity;
import tech.raaf.structure.graph.Group;
import tech.raaf.structure.graph.Instance;
import tech.raaf.structure.graph.Instrument;
import tech.raaf.structure.graph.Profile;
import tech.raaf.structure.graph.Result;
import tech.raaf.structure.graph.Type;
import tech.raaf.structure.schema.TypeSchemaTest;

public class InstanceRepositoryTest extends TypeSchemaTest {

  private final InstanceRepository instanceRepository = Context.getInstanceRepository();
  private final TypeRepository typeRepository = Context.getTypeRepository();

  // GraphTest deletes the graph before each test, so make them again
  private Group group;
  private Component parent, child01, child02;

  @BeforeEach
  public void setUp() throws Exception {
    Type groupingType = typeRepository.findOneByName("Group");
    assertNotNull(groupingType);
    Type parentType = typeRepository.findOneByName("Software");
    assertNotNull(parentType);
    Type childType = typeRepository.findOneByName("Linux");
    assertNotNull(childType);
    group = new Group(mapper.createObjectNode().put("name", "Group").put("desc", "A description."), groupingType);
    parent = new Component(
      mapper
        .createObjectNode()
        .put("name", "parent")
        .put("desc", "Parent.")
        .put("ver", "1")
        .put("shortName", "parent")
        .put("longName", "parent"),
      parentType
    );
    group.getGroupees().add(parent);
    child01 = new Component(
      mapper
        .createObjectNode()
        .put("name", "child")
        .put("desc", "Child 01.")
        .put("ver", "1")
        .put("shortName", "child")
        .put("longName", "child"),
      childType
    );
    parent.addComponent(child01);
    child02 = new Component(
      mapper
        .createObjectNode()
        .put("name", "child")
        .put("desc", "Child 02.")
        .put("ver", "1")
        .put("shortName", "child")
        .put("longName", "child"),
      childType
    );
    parent.addComponent(child02);
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
    Assertions.assertEquals(group.getUuid(), instanceRepository.getStartOfComponentGroup(child01.getUuid()).getUuid());
  }

  @Test
  public void nLastCreated() throws Exception {
    Result result = new Entity.ResultBuilder().process("test").taskId("latest-instances-test").buildAndStore();
    List<Instance> instances = instanceRepository.nLastCreated(Integer.MAX_VALUE);
    assertEquals(instanceRepository.count(), instances.size());
    assertTrue(instances.containsAll(ImmutableSet.of(group, parent, child01, child02, result)));
    assertTrue(instances.stream().anyMatch(Instrument.class::isInstance));
    assertTrue(instances.stream().anyMatch(Profile.class::isInstance));
  }

  @Test
  public void nLastCreatedWithLabels() throws Exception {
    new Entity.ResultBuilder().process("test").taskId("latest-instances-test").buildAndStore();
    assertEquals(
      ImmutableSet.of(group.getUuid(), parent.getUuid(), child01.getUuid(), child02.getUuid()),
      instanceRepository
        .nLastCreated(10, "Component", "Group")
        .stream()
        .map(Instance::getUuid)
        .collect(Collectors.toSet())
    );
  }

  @Test
  public void nLastCreatedWithSingleLabel() {
    assertEquals(ImmutableSet.of(group), ImmutableSet.copyOf(instanceRepository.nLastCreated(10, "Group")));
  }

  @Test
  public void nLastCreatedWithUnknownLabel() {
    assertTrue(instanceRepository.nLastCreated(10, "NoSuchLabel").isEmpty());
  }
}
