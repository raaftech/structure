package com.raaftech.structure.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.graph.Type;
import com.raaftech.structure.graph.repositories.TypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Types extends Repository<Type> {

    private TypeRepository repository;

    private Logger log = LoggerFactory.getLogger(Types.class);

    public Types(TypeRepository repository) {
        super(repository);
        this.repository = repository;
    }

    public MessageHandler findAllTypeGlyphs() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();
                List<Integer> allTypeGlyphs = repository.findAllTypeGlyphs();
                ArrayNode glyphs = result.putArray ("data");

                for (Integer glyphId : allTypeGlyphs) {
                    glyphs.add(glyphId);

                }
                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) { return null; }
        };
    }

}
