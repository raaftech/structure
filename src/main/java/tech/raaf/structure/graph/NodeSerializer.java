package tech.raaf.structure.graph;

import static io.vertx.core.json.Json.mapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.raaf.structure.helper.LambdaHelper;

/**
 * Serialize Node objects to JSON.
 */
public class NodeSerializer extends StdSerializer<Node> {

  private final Logger log = LoggerFactory.getLogger(NodeSerializer.class);

  public NodeSerializer() {
    super(Node.class);
  }

  @Override
  public void serialize(Node value, JsonGenerator gen, SerializerProvider provider) throws IOException {
    // Sort by peer uuid, so that the output is the same each time
    final String outgoing =
      "MATCH (a:Node {uuid: $uuid})-[r]->(b) " +
      "RETURN type(r) AS rel, b.uuid AS peer, r.forRelationship AS forRelationship, " +
      "r.cardinality AS cardinality ORDER BY peer";
    final Map<String, List<Map<String, Object>>> relMap = new HashMap<>();
    for (Map<String, Object> row : Graph.query(outgoing, ImmutableMap.of("uuid", value.getUuid()))) {
      relMap.computeIfAbsent((String) row.get("rel"), k -> new ArrayList<>()).add(row);
    }

    // start json
    gen.writeStartObject();
    gen.writeStringField("name", value.getName());
    gen.writeStringField("desc", value.getDesc());

    // start meta object
    gen.writeFieldName("meta");
    gen.writeStartObject();
    gen.writeStringField("uuid", value.getUuid());
    if (value instanceof Type) {
      gen.writeBooleanField("abstract", ((Type) value).isAbstract());
    }
    gen.writeStringField("checksum", value.getChecksum());
    if (value.getBundle() != null) {
      gen.writeStringField("bundle", value.getBundle());
    }
    if (value.getPath() != null) {
      gen.writeStringField("path", value.getPath());
    }

    // start relations object, which should always exist
    gen.writeFieldName("relations");
    gen.writeStartObject();
    relMap
      .entrySet()
      .stream()
      .forEach(entry -> {
        try {
          final String name = entry.getKey();

          /*
            In case we come across a CONSTRAINTS relation return immediately.
            These need to be serialized differently.
          */
          if (Objects.equals(name, "CONSTRAINTS")) {
            return;
          }

          // because our lists only contain relationships with the same direction, we can safely
          // determine the direction of the first relationship and assume the others are directed the same
          final String direction = "outgoing";
          // start an object with the relationship name
          gen.writeFieldName(name);
          gen.writeStartObject();

          // write the direction
          gen.writeStringField("direction", direction);

          // write the uuids array
          gen.writeFieldName("to");
          gen.writeStartArray();
          entry
            .getValue()
            .stream()
            .map(row -> (String) row.get("peer"))
            .forEach(uuid -> {
              try {
                gen.writeString(uuid);
              } catch (IOException e) {
                log.error("Caught IOException: " + e.getMessage());
                throw new RuntimeException("Error while writing relationship entry during entity serialization.");
              }
            });
          // end the uuids array
          gen.writeEndArray();
          // end the relationship object
          gen.writeEndObject();
        } catch (IOException e) {
          log.error("Caught IOException: " + e.getMessage());
          throw new RuntimeException("Error while writing relationship object during entity serialization.");
        }
      });

    // end relations object
    gen.writeEndObject();

    // do we have constraints?
    final Optional<List<Map<String, Object>>> maybeConstraints = relMap
      .entrySet()
      .stream()
      .filter(entry -> Objects.equals("CONSTRAINTS", entry.getKey()))
      .map(Map.Entry::getValue)
      .findFirst();
    if (maybeConstraints.isPresent()) {
      gen.writeFieldName("constraints");
      gen.writeStartObject();

      final Map<String, List<Map<String, Object>>> constraints = maybeConstraints
        .get()
        .stream()
        .collect(Collectors.groupingBy(row -> row.get("forRelationship").toString()));

      for (Map.Entry<String, List<Map<String, Object>>> pair : constraints.entrySet()) {
        gen.writeFieldName(pair.getKey());
        gen.writeStartArray();

        for (Map<String, Object> row : pair.getValue()) {
          gen.writeStartObject();
          gen.writeStringField("cardinality", row.get("cardinality").toString());
          gen.writeStringField("to", row.get("peer").toString());
          gen.writeEndObject();
        }

        gen.writeEndArray();
      }

      gen.writeEndObject();
    }

    // end meta object
    gen.writeEndObject();

    // if we have data, start a data object
    if (value.getData() != null && value.getData().size() > 0) {
      gen.writeFieldName("data");
      gen.writeStartObject();

      value
        .getData()
        .entrySet()
        .stream()
        .forEach(entry ->
          LambdaHelper.wrapWithExceptionPropagation(() -> {
            gen.writeObjectField(entry.getKey(), entry.getValue());
            return true;
          }).get()
        );

      // end the data object
      gen.writeEndObject();
    }

    // if we have files, start a file object
    if (value.getFiles() != null && value.getFiles().size() > 0) {
      gen.writeFieldName("file");
      gen.writeStartObject();

      // for every file, create an object
      value
        .getFiles()
        .entrySet()
        .stream()
        .forEach(entry -> {
          try {
            gen.writeFieldName(entry.getKey());
            gen.writeStartObject();

            // write the object
            JsonNode fileJson = mapper.readTree((String) entry.getValue());
            gen.writeStringField("encoding", fileJson.get("encoding").asText());
            gen.writeStringField("format", fileJson.get("format").asText());
            if (fileJson.has("path")) gen.writeStringField("path", fileJson.get("path").asText());
            gen.writeStringField("content", fileJson.get("content").asText());

            // end the file specific object
            gen.writeEndObject();
          } catch (IOException e) {
            log.error("Caught IOException: " + e.getMessage());
            throw new RuntimeException("Error while writing file object during entity serialization");
          }
        });

      // end the file object
      gen.writeEndObject();
    }

    // end json
    gen.writeEndObject();
  }
}
