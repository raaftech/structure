package tech.raaf.structure.bus.repository;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import tech.raaf.structure.annotations.SchemaPath;
import tech.raaf.structure.bus.GenericActor;
import tech.raaf.structure.graph.Node;
import tech.raaf.structure.graph.repositories.GenericRepository;

import java.util.Set;

/**
 * An abstract class for the NodeRepository interfaces
 * on the eventbus implementing three default methods
 * for querying nodes.
 * @author Boy van Duuren
 *
 * @param <T> The type of node that will be used.
 */
public abstract class Repository<T extends Node> extends GenericActor {

    private GenericRepository<T> repository;

    protected Repository(GenericRepository<T> repository) {
        this.repository = repository;
    }

    /**
     * Find nodes of type T using the name property.
     * @return An array with JSON objects representing
     * objects of type T, or an empty array if we
     * didn't find a match.
     */
    @SchemaPath("resource:/schema/repository/findByName.schema.json")
    @SuppressWarnings("unused")
    public MessageHandler findByName() {
        return new MessageHandler() {

            @Override
            public JsonNode exec(JsonNode request) {
                JsonNode args = request.get("args");
                ObjectNode result = mapper.createObjectNode();
                String name = args.get("name").asText();

                ArrayNode nodes = result.putArray("data");
                Set<T> found = repository.findByName(name);
                found.stream()
                        .map(Node::toJson)
                        .forEach(nodes::add);

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                // TODO Auto-generated method stub
                return null;
            }
        };

    }

    /**
     * Find a single node of type T using the name property.
     * @return JSON representation of an object
     * of type T if we have a match, else null.
     */
    @SchemaPath("resource:/schema/repository/findOneByName.schema.json")
    public MessageHandler findOneByName() {
        return new MessageHandler() {

            @Override
            public JsonNode exec(JsonNode request) {
                JsonNode args = request.get("args");
                ObjectNode result = mapper.createObjectNode();
                String name = args.get("name").asText();

                T found = repository.findOneByName(name);
                if (found != null) {
                    result.set("data", found.toJson());
                } else {
                    result.put("error", "Node wasn't found.");
                }

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                // TODO Auto-generated method stub
                return null;
            }

        };
    }

    /**
     * Find a single node of type T either by UUID or by name.
     * This can be used to find an UUID which belongs to a certain name,
     * or to check if a typenode with a specific UUID exists.
     * @return If we find a typenode we'll set the data field to its UUID,
     * but if we didn't find a typenode, the error field will be set with a message.
     */
    @SchemaPath("resource:/schema/repository/findOne.schema.json")
    @SuppressWarnings("unused")
    public MessageHandler findOne() {
        return new MessageHandler() {

            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();
                JsonNode args = request.get("args");
                String node = args.get("node").asText();

                if (isUuid(node)) {
                    T found = repository.findByUuid(node);
                    if (found != null) {
                        result.set("data", found.toJson());
                    } else {
                        result.put("error", "Couldn't find node.");
                    }
                } else {
                    T found = repository.findOneByName(node);
                    if (found != null) {
                        result.set("data", found.toJson());
                    } else {
                        result.put("error", "Couldn't find node.");
                    }
                }
                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                // TODO Auto-generated method stub
                return null;
            }

        };
    }

}
