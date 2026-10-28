package tech.raaf.structure.graph.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static tech.raaf.structure.helper.JsonHelper.mapper;

import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.graph.Component;
import tech.raaf.structure.graph.Type;
import tech.raaf.structure.schema.TypeSchemaTest;

public class ComponentRepositoryTest extends TypeSchemaTest {

  private final ComponentRepository componentRepository = Context.getComponentRepository();
  private final TypeRepository typeRepository = Context.getTypeRepository();

  private Type parentType;
  private Type childType;

  @BeforeEach
  public void setUp() throws Exception {
    parentType = typeRepository.findOneByName("Software");
    childType = typeRepository.findOneByName("Linux");
    Component parent = new Component(
      mapper
        .createObjectNode()
        .put("name", "parent")
        .put("desc", "Parent.")
        .put("ver", "1")
        .put("shortName", "parent")
        .put("longName", "parent"),
      parentType
    );
    Component child = new Component(
      mapper
        .createObjectNode()
        .put("name", "child")
        .put("desc", "Child.")
        .put("ver", "1")
        .put("shortName", "parent")
        .put("longName", "parent"),
      childType
    );
    parent.addComponent(child);
    parent.save();
  }

  @Test
  public void findChildrenByType() throws Exception {
    Component parent = componentRepository.findOneByName("parent");
    assertNotNull(parent);
    Component child = componentRepository.findOneByName("child");
    assertNotNull(child);

    Set<Component> childrenByType = componentRepository.findChildrenByType(parent.getUuid(), childType.getName());
    assertEquals(1, childrenByType.size());
    Assertions.assertEquals(
      child.getUuid(),
      componentRepository.findChildrenByType(parent.getUuid(), "Linux").stream().findFirst().get().getUuid()
    );
  }
}
