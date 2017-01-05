package com.raaftech.structure.graph;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.common.collect.ImmutableSet;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.repositories.NodeRepository;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    /*
    Because some Entities can have relationships to each other we need to make exceptions for some relationships.
    Default is to only save the outgoing relationships, but in some cases we need the incoming rel so we dont
    get an unsortable bundle. See #40 for more details.
     */
    private final Set<String> incomingRelationships = ImmutableSet.of("HAS_INSTRUMENT");

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
        gen.writeStringField("type", value.getType());
        gen.writeStringField("name", value.getName());
        gen.writeStringField("desc", value.getDesc());

        // start meta object
        gen.writeFieldName("meta");
        gen.writeStartObject();
        gen.writeStringField("uuid", value.getUuid());

        // start relations object, which should always exist
        gen.writeFieldName("relations");
        gen.writeStartObject();
        // find all our outgoing relationships that aren't contained in our incomingRelationships exception list
        Stream<Relationship> outgoingRelationshipsStream = StreamSupport.stream(graphNode.getRelationships(Direction.OUTGOING).spliterator(), false)
                .filter(rel -> !incomingRelationships.contains(rel.getType().name()));
        // find all the incoming relationships we actually are interested in
        Stream<Relationship> incomingRelationshipsStream = StreamSupport.stream(graphNode.getRelationships(Direction.INCOMING).spliterator(), false)
                .filter(rel -> incomingRelationships.contains(rel.getType().name()));
        Map<RelationshipType, List<Relationship>> relMap = Stream.concat(outgoingRelationshipsStream, incomingRelationshipsStream)
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
                        // because our lists only contain relationships with the same direction, we can safely
                        // determine the direction of the first relationship and assume the others are directed the same
                        final String direction = incomingRelationships.contains(name) ?
                                "incoming" : "outgoing";
                        // start an object with the relationship name
                        gen.writeFieldName(name);
                        gen.writeStartObject();

                        // write the direction
                        gen.writeStringField("direction", direction);

                        // write the uuids array
                        gen.writeFieldName("uuids");
                        gen.writeStartArray();
                        List<Relationship> uuids = entry.getValue();
                        uuids.stream()
                                .map(rel -> {
                                    /*
                                     if we are an outgoing relationship, the "other" node is the end node,
                                     in case of an incoming relationships, the "other" node is the start node
                                      */
                                    if (!incomingRelationships.contains(name)) {
                                        return (String) rel.getEndNode().getProperty("uuid");
                                    } else {
                                        return (String) rel.getStartNode().getProperty("uuid");
                                    }
                                })
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

        // end meta object
        gen.writeEndObject();

        // if we have files, start a file object
        if (value.getFiles().size() > 0) {
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
