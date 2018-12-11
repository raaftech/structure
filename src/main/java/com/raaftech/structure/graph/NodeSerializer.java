package com.raaftech.structure.graph;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.repositories.NodeRepository;
import com.raaftech.structure.helper.LambdaHelper;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static io.vertx.core.json.Json.mapper;

/**
 * Serialize Node objects to JSON.
 */
public class NodeSerializer extends StdSerializer<Node> {

    private final NodeRepository nodeRepository;
    private final Logger log = LoggerFactory.getLogger(NodeSerializer.class);

    public NodeSerializer() {
        super(Node.class);
        nodeRepository = Context.getNodeRepository();
    }

    /**
     * This constructor is used in tests, where Context.getNodeRepository() returns null.
     * @param nodeRepository The NodeRepository to use (usually @Autowired from enclosing test).
     */
    public NodeSerializer(NodeRepository nodeRepository) {
        super(Node.class);
        this.nodeRepository = nodeRepository;
    }

    @Override
    public void serialize(Node value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        // we need the actual backing node as well so we can traverse relations in a class independent manner
        org.neo4j.graphdb.Node graphNode = nodeRepository.findGraphNodeByUuid(value.getUuid());

        // start json
        gen.writeStartObject();
        gen.writeStringField("name", value.getName());
        gen.writeStringField("desc", value.getDesc());

        // start meta object
        gen.writeFieldName("meta");
        gen.writeStartObject();
        gen.writeStringField("uuid", value.getUuid());
        if (value instanceof Type) {
            gen.writeBooleanField("abstract", ((Type)value).isAbstract());
        }
        gen.writeStringField("checksum", value.getChecksum());

        // start relations object, which should always exist
        gen.writeFieldName("relations");
        gen.writeStartObject();
        // find all our outgoing relationships that aren't contained in our incomingRelationships exception list
        Stream<Relationship> outgoingRelationshipsStream = StreamSupport.stream(
                graphNode.getRelationships(Direction.OUTGOING).spliterator(), false);
        Map<RelationshipType, List<Relationship>> relMap = outgoingRelationshipsStream
                .sorted((a, b) -> {
                    // sort this stream based on the graphId of the relationships (ascending)
                    // we do this to ensure the serialized relationship order is the same as in the source of the entity
                    return a.getId() < b.getId() ? -1 :
                            a.getId() > b.getId() ? 1 : 0;
                })
                .collect(Collectors.groupingBy(Relationship::getType));

        relMap.entrySet().stream()
                .forEach(entry -> {
                    try {
                        final String name = entry.getKey().name();

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
                        List<Relationship> uuids = entry.getValue();
                        uuids.stream()
                                .map(rel -> (String) rel.getEndNode().getProperty("uuid"))
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
        final Optional<List<Relationship>> maybeConstraints = relMap.entrySet().stream()
                .filter(entry -> Objects.equals("CONSTRAINTS", entry.getKey().name()))
                .map(Map.Entry::getValue)
                .findFirst();
        if (maybeConstraints.isPresent()) {
            gen.writeFieldName("constraints");
            gen.writeStartObject();

            final Map<String, List<Relationship>> constraints = maybeConstraints.get()
                    .stream().collect(Collectors.groupingBy(rel -> rel.getProperty("forRelationship").toString()));

            for (Map.Entry<String, List<Relationship>> pair: constraints.entrySet()) {
                gen.writeFieldName(pair.getKey());
                gen.writeStartArray();

                for (Relationship rel: pair.getValue()) {
                    gen.writeStartObject();
                    gen.writeStringField("cardinality", rel.getProperty("cardinality").toString());
                    gen.writeStringField("to", rel.getEndNode().getProperty("uuid").toString());
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

            value.getData().entrySet().stream()
                    .forEach(entry -> LambdaHelper.wrapWithExceptionPropagation(() -> {
                        gen.writeObjectField(entry.getKey(), entry.getValue()); return true;}).get());

            // end the data object
            gen.writeEndObject();
        }

        // if we have files, start a file object
        if (value.getFiles() != null && value.getFiles().size() > 0) {
            gen.writeFieldName("file");
            gen.writeStartObject();

            // for every file, create an object
            value.getFiles().entrySet().stream()
                    .forEach(entry -> {
                        try {
                            gen.writeFieldName(entry.getKey());
                            gen.writeStartObject();

                            // write the object
                            JsonNode fileJson = mapper.readTree((String)entry.getValue());
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
