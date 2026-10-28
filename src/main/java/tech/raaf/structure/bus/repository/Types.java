package tech.raaf.structure.bus.repository;

import java.util.List;
import tech.raaf.structure.annotations.SchemaPath;
import tech.raaf.structure.graph.Graph;
import tech.raaf.structure.graph.Type;
import tech.raaf.structure.graph.repositories.TypeRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public class Types extends Repository<Type> {

  private TypeRepository repository;

  public Types(TypeRepository repository) {
    super(repository);
    this.repository = repository;
  }

  @SchemaPath("resource:/schema/repository/findOneByName.schema.json")
  public MessageHandler findOneByCaseInsensitiveName() {
    return new MessageHandler() {
      @Override
      public JsonNode exec(JsonNode request) {
        final ObjectNode result = mapper.createObjectNode();
        final JsonNode args = request.get("args");
        final String name = args.get("name").asString().toLowerCase();

        Graph.transaction(() -> {
          final Type found = repository.findOneByCaseInsensitiveName(name);
          if (found != null) {
            result.set("data", mapper.valueToTree(found));
          } else {
            result.put("error", "Couldn't find type with name " + name);
          }
        });

        return response(request, result);
      }

      @Override
      public JsonNode help(JsonNode request) {
        return jsonFromResource("help/types.findonebycaseinsensitivename.json");
      }
    };
  }

  public MessageHandler getAll() {
    return new MessageHandler() {
      @Override
      public JsonNode exec(JsonNode request) {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode data = result.putArray("data");
        Graph.transaction(() ->
          repository
            .getAll()
            .stream()
            .map(mapper::valueToTree)
            .map(obj -> (ObjectNode) obj)
            .forEach(data::add)
        );

        return response(request, result);
      }

      @Override
      public JsonNode help(JsonNode request) {
        return jsonFromResource("help/types.getall.json");
      }
    };
  }

  public MessageHandler findAllTypeGlyphs() {
    return new MessageHandler() {
      @Override
      public JsonNode exec(JsonNode request) {
        ObjectNode result = mapper.createObjectNode();
        List<Integer> allTypeGlyphs = repository.findAllTypeGlyphs();
        ArrayNode glyphs = result.putArray("data");

        for (Integer glyphId : allTypeGlyphs) {
          glyphs.add(glyphId);
        }
        return response(request, result);
      }

      @Override
      public JsonNode help(JsonNode request) {
        return jsonFromResource("help/types.findalltypeglyphs.json");
      }
    };
  }
}
