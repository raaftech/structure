package tech.raaf.structure.bus.repository;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.raaf.structure.annotations.SchemaPath;
import tech.raaf.structure.base.Configuration;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.bus.NodeActor;
import tech.raaf.structure.graph.Entity;
import tech.raaf.structure.graph.Graph;
import tech.raaf.structure.graph.Instance;
import tech.raaf.structure.graph.Type;
import tech.raaf.structure.graph.repositories.InstanceRepository;
import tech.raaf.structure.graph.repositories.TypeRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public class Instances extends Repository<Instance> {

  private InstanceRepository repository;
  private static final TypeRepository TYPE_REPOSITORY = Context.getTypeRepository();

  private Logger log = LoggerFactory.getLogger(Instances.class);

  public Instances(InstanceRepository repository) {
    super(repository);
    this.repository = repository;
  }

  @SchemaPath("resource:/schema/repository/instances/findOneByData.schema.json")
  public MessageHandler findOneByData() {
    return new MessageHandler() {
      @Override
      public JsonNode exec(JsonNode request) {
        JsonNode args = request.get("args");
        return Graph.transaction(() -> {
          Instance found = repository.findOneByData(args.get("property").asString(), args.get("value").asString());
          return response(
            request,
            mapper.createObjectNode().set("data", found == null ? mapper.createObjectNode() : mapper.valueToTree(found))
          );
        });
      }

      @Override
      public JsonNode help(JsonNode request) {
        return jsonFromResource("help/instances.findonebydata.json");
      }
    };
  }

  /**
   * Get the latest instances, with an optional class label filter.
   * @return A data array with at most the requested number of nodes.
   */
  @SchemaPath("resource:/schema/repository/instances/nLastCreated.schema.json")
  @SuppressWarnings("unused")
  public MessageHandler nLastCreated() {
    return new MessageHandler() {
      @Override
      public JsonNode exec(JsonNode request) {
        ObjectNode result = mapper.createObjectNode();
        ObjectNode args = (ObjectNode) request.get("args");
        int n = args.get("amount").asInt();
        String[] labels = args.has("labels") ? mapper.convertValue(args.get("labels"), String[].class) : new String[0];

        ArrayNode data = result.putArray("data");
        repository.nLastCreated(n, labels).stream().map(Instance::toJson).forEach(data::add);

        return response(request, result);
      }

      @Override
      public JsonNode help(JsonNode request) {
        return jsonFromResource("help/instances.nlastcreated.json");
      }
    };
  }

  /**
   * Persist an Instance to the database.
   * @return A JSON object containing the UUID of the new Instance
   * or an error field if something went wrong.
   */
  @SchemaPath("resource:/schema/repository/instances/putNode.schema.json")
  @SuppressWarnings("unused")
  public MessageHandler putNode() {
    return new MessageHandler() {
      @Override
      public JsonNode exec(JsonNode request) {
        ObjectNode result = mapper.createObjectNode();
        ObjectNode args = (ObjectNode) request.get("args");
        String isa = args.get("node").asString();
        Type type = TYPE_REPOSITORY.findByUuid(isa);
        // Remove the node field so it won't interfere with the Type schema.
        args.remove("node");

        try {
          if (type.isOfType("Component")) {
            // The entity write path checks the data and the constraints
            final String uuid = UUID.randomUUID().toString();
            final ObjectNode json = mapper
              .createObjectNode()
              .put("name", args.path("name").asString())
              .put("desc", args.path("desc").asString());
            json
              .putObject("meta")
              .put("uuid", uuid)
              .putObject("relations")
              .putObject("INSTANCE_OF")
              .put("direction", "outgoing")
              .putArray("to")
              .add(type.getUuid());
            json.set("data", args.deepCopy());
            final Entity entity = mapper.treeToValue(json, Entity.class);
            Graph.transaction(() -> {
              entity.toGraph();
              Graph.afterCommit(() -> NodeActor.deploy(entity));
            });
            result.put("data", uuid);
          } else {
            // TODO: add support for other Instances
          }
        } catch (RuntimeException e) {
          log.error(e.getMessage());

          if (Configuration.isTraceModeOn()) {
            e.printStackTrace();
          }

          result.put("error", e.getMessage());
        }

        return response(request, result);
      }

      @Override
      public JsonNode help(JsonNode request) {
        return jsonFromResource("help/instances.putnode.json");
      }
    };
  }
}
