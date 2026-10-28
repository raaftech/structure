package tech.raaf.structure.bus;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.bus.manager.EntityManager;
import tech.raaf.structure.bus.manager.InstrumentManager;
import tech.raaf.structure.bus.manager.StructureManager;
import tech.raaf.structure.bus.manager.TaskManager;
import tech.raaf.structure.bus.repository.Instances;
import tech.raaf.structure.bus.repository.Nodes;
import tech.raaf.structure.bus.repository.Types;
import tech.raaf.structure.graph.Component;
import tech.raaf.structure.schema.TypeSchemaTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class HelpTest extends TypeSchemaTest {

  private static final ObjectMapper mapper = new ObjectMapper();

  // The help reply of each actor has each of its handlers, by name
  @Test
  public void eachHandlerHasHelp() {
    Component component = new Component(
      mapper.createObjectNode().put("name", "help").put("desc", "A component."),
      Context.getTypeRepository().findOneByName("Component")
    );
    component.save();
    List<GenericActor> actors = Arrays.asList(
      new EntityManager(),
      new InstrumentManager(),
      new StructureManager(),
      new TaskManager(1),
      new Instances(Context.getInstanceRepository()),
      new Types(Context.getTypeRepository()),
      new Nodes(Context.getNodeRepository()),
      new TypeActor(Context.getTypeRepository().findOneByName("Component")),
      new InstanceActor(component)
    );
    List<String> missing = new ArrayList<>();
    for (GenericActor actor : actors) {
      JsonNode help = actor.help().exec(mapper.createObjectNode().put("msgh", "help")).path("result");
      for (String handler : actor.handlers.keySet()) {
        if (!handler.equals(help.path(handler).path("name").asText())) {
          missing.add(actor.getClass().getSimpleName() + "." + handler);
        }
      }
    }
    assertTrue("Handlers without help: " + missing, missing.isEmpty());
  }
}
