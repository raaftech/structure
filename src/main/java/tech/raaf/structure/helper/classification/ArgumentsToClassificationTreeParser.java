package tech.raaf.structure.helper.classification;

import static tech.raaf.structure.helper.JsonHelper.mapper;

import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import tech.raaf.structure.helper.JsonHelper;
import tech.raaf.structure.helper.classification.tree.BranchNode;
import tech.raaf.structure.helper.classification.tree.LeafNode;
import tech.raaf.structure.helper.classification.tree.RootNode;
import tech.raaf.structure.helper.classification.tree.Terminal;
import tech.raaf.structure.helper.classification.tree.implementation.BranchNodeJson;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * @author Boy van Duuren <boy@raaf.tech>
 */
public class ArgumentsToClassificationTreeParser {

  private static final String RESOURCE_SCHEME = "resource:/";
  private static final String INSTRUMENT_SCHEMA_PATH = "bundles/core/schema/";
  private static final URI INSTRUMENT_SCHEMA_NS = URI.create(RESOURCE_SCHEME + INSTRUMENT_SCHEMA_PATH);
  private static final String INSTRUMENT_SCHEMA_FILE = "controller.schema.json";

  private static JsonNode argumentsSchema = JsonHelper.jsonFromResource(INSTRUMENT_SCHEMA_PATH + INSTRUMENT_SCHEMA_FILE)
    .get("properties")
    .get("arguments");
  private static String typeKey = "_hierarchyPath";

  private static Function<Set<JsonNode>, Set<JsonNode>> useFirst = input -> {
    JsonNode[] out = new JsonNode[input.size()];
    input.toArray(out);
    Set<JsonNode> output = new HashSet<>();
    output.add(out[0]);
    return output;
  };

  private static Function<Set<JsonNode>, Set<JsonNode>> useLast = input -> {
    JsonNode[] out = new JsonNode[input.size()];
    input.toArray(out);
    Set<JsonNode> output = new HashSet<>();
    output.add(out[input.size() - 1]);
    return output;
  };

  private static Function<Set<JsonNode>, Set<JsonNode>> mustBeOne = input -> {
    return input.size() == 1 ? input : null;
  };

  private static Function<Set<JsonNode>, Set<JsonNode>> mustBeMany = input -> {
    return input.size() > 1 ? input : null;
  };

  public static class ClassificationTreeObjects {

    public RootNode<JsonNode> root;
    public ObjectNode result;
    public Set<JsonNode> failed = new HashSet<>();
    public Set<JsonNode> askUser = new HashSet<>();
  }

  public static boolean matchesTypePath(String typePath, String hierarchyPath) {
    return hierarchyPath.matches(Pattern.quote(typePath) + "(\\..*)?");
  }

  @SuppressWarnings("unchecked")
  public static ClassificationTreeObjects asTree(ObjectNode arguments) {
    if (!JsonHelper.validateToSchema(argumentsSchema, arguments, INSTRUMENT_SCHEMA_NS)) {
      throw new IllegalArgumentException("Provided arguments did not conform to arguments schema.");
    }

    ClassificationTreeObjects returnObject = new ClassificationTreeObjects();

    // Get defaults from the instrument schema. This is not very pretty.
    JsonNode argumentsKeySchema = argumentsSchema.get("patternProperties").get("^[a-zA-Z0-9]+$").get("properties");
    JsonNode valueKeySchema = argumentsKeySchema.get("value").get("properties");

    ObjectNode result = mapper.createObjectNode();
    returnObject.result = result;

    // Terminals
    Terminal<JsonNode> success = new Terminal<>(input -> {
      input.stream().forEach(node -> {
        String key = node.get("_key").asString();
        String from = node.get("_from").asString();

        // Sanitize
        ((ObjectNode) node).remove("_from");
        ((ObjectNode) node).remove("_key");
        ((ObjectNode) node).remove("_hierarchyPath");

        // There is more than one entity with this key.
        // Convert to ArrayNode.
        if (result.get(key) != null && result.isObject()) {
          JsonNode tmp = result.get(key);
          result.remove(key);
          result.putArray(key).add(tmp).add(node);
        } else if (result.get(key) != null && result.get(key).isArray()) {
          ((ArrayNode) result.get(key)).add(node);
        } else {
          if (from.equals("static")) {
            result.put(key, node.get("data").asString());
          } else {
            result.set(key, node);
          }
        }
      });
    });
    Terminal<JsonNode> fail = new Terminal<>(input -> returnObject.failed.addAll(input));
    Terminal<JsonNode> askUser = new Terminal<>(input -> returnObject.askUser.addAll(input));
    Terminal<JsonNode> ignore = new Terminal<>(input -> {});

    final String strategyDefault = valueKeySchema.get("strategy").get("default").asString();
    final String onErrorDefault = valueKeySchema.get("onError").get("default").asString();

    RootNode<JsonNode> root = new RootNode<>();
    returnObject.root = root;
    Map<String, BranchNodeJson> fromMap = new HashMap<>();

    Map<String, Function<Set<JsonNode>, Set<JsonNode>>> selectionMap = new HashMap<>();
    selectionMap.put("useFirst", useFirst);
    selectionMap.put("useLast", useLast);
    selectionMap.put("mustBeOne", mustBeOne);
    selectionMap.put("mustBeMany", mustBeMany);

    Map<String, Terminal> onErrorTerminalMap = new HashMap<>();
    onErrorTerminalMap.put("fail", fail);
    onErrorTerminalMap.put("askUser", askUser);
    onErrorTerminalMap.put("ignore", ignore);

    Iterator<Map.Entry<String, JsonNode>> argIter = arguments.properties().iterator();
    while (argIter.hasNext()) {
      Map.Entry<String, JsonNode> arg = argIter.next();
      ObjectNode value = (ObjectNode) arg.getValue().get("value");
      String keyName = arg.getKey();
      BranchNode<JsonNode> head;

      // Get the "from" key
      String fromString = value.get("from").asString();
      head = fromMap.get(fromString);
      if (head == null) {
        fromMap.put(fromString, new BranchNodeJson(root, "_from", fromString));
        head = fromMap.get(fromString);
      }

      // Get the "type" key
      if (!fromString.equals("static")) {
        String typePath = value.get("type").asString();
        head = new BranchNode<>(
          head,
          node -> node.has(typeKey) && matchesTypePath(typePath, node.get(typeKey).asString())
        );
      } else {
        head = new BranchNodeJson(head, "name", keyName);
      }

      // Get the "matches" key, which may be an object of K,V's, or a string
      JsonNode matches = value.get("matches");
      if (matches != null) {
        if (matches.isString()) {
          head = new BranchNodeJson(head, "data", value.get("matches").asString());
        } else if (matches.isObject()) {
          Iterator<Map.Entry<String, JsonNode>> iter = ((ObjectNode) matches).properties().iterator();
          while (iter.hasNext()) {
            Map.Entry<String, JsonNode> entry = iter.next();
            head = new BranchNodeJson(head, entry.getKey(), entry.getValue().asString());
          }
        }
      }

      // Insert the keyName into each data entity
      head = new BranchNode<>(head, jsonNode -> {
        ((ObjectNode) jsonNode).put("_key", keyName);
        return true;
      });

      // Get the strategy, which is a LeafNode.
      String strategy = value.get("strategy") != null ? value.get("strategy").asString() : strategyDefault;
      Function<Set<JsonNode>, Set<JsonNode>> strategyFunc = selectionMap.get(strategy);

      Terminal<JsonNode> failure = onErrorTerminalMap.get(value.path("onError").asString());
      if (failure == null) {
        failure = onErrorTerminalMap.get(onErrorDefault);
      }
      new LeafNode<>(head, success, failure, strategyFunc);
    }

    return returnObject;
  }
}
