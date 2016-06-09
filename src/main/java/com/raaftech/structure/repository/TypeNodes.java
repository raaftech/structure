package com.raaftech.structure.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.graph.TypeNode;
import com.raaftech.structure.graph.repositories.TypeNodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TypeNodes extends Repository<TypeNode> {

    private TypeNodeRepository repository;

    private Logger log = LoggerFactory.getLogger(TypeNodes.class);

    public TypeNodes(TypeNodeRepository repository) {
        super(repository);
        this.repository = repository;
    }

    @SuppressWarnings("unused")
    public MessageHandler getNamesAndGlyphs() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();
                ObjectNode data = result.putObject("data");
                repository.getNamesAndGlyphs().forEach(res -> data.put(res.getName(), res.getGlyphId()));
                return result;
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

}
