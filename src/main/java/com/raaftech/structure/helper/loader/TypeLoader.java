package com.raaftech.structure.helper.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.exceptions.JsonValidationException;
import com.raaftech.structure.exceptions.NodeConstraintViolated;
import com.raaftech.structure.exceptions.PackageIOException;
import com.raaftech.structure.exceptions.TypeUnknownException;
import com.raaftech.structure.graph.TypeNode;
import com.raaftech.structure.graph.repositories.TypeNodeRepository;
import com.raaftech.structure.helper.JsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static io.vertx.core.json.Json.mapper;

class TypeLoader implements PackageLoader {

    private static final String NAME = "name";
    private static final String UUID = "uuid";
    private static final String ISA = "isa";
    private static final String GLYPHID = "glyphId";
    private static final String DESCRIPTORS = "descriptors";
    private static final String TYPE = "type";
    private static final String DESC = "desc";
    private static final String REQ = "required";
    private static final String PATTERN = "pattern";
    private static final String DEFAULT = "defaultValue";

    private static final Logger log = LoggerFactory.getLogger(PackageLoader.class);

    private final JsonNode typeSchema = JsonHelper.jsonFromResource("schema/helper/loader/type.schema.json");
    private final TypeNodeRepository typeNodeRepository = Context.INSTANCE.getTypeNodeRepository();

    private static final Map<String, TypeNode> registeredTypes = new HashMap<>();

    @Override
    public void load(Path filePath) {
        try {
            log.info("Loading defined types from " + filePath + ".");
            load(new FileReader(filePath.toFile()));
        } catch (IOException e) {
            throw new PackageIOException(e.getMessage());
        }
    }

    /**
     * Load the type package with a reader. This is called by load(Path filePath).
     * This is also exposed this way so we can easily test the loader by passing StringReaders.
     * @param reader A reader containing the type definitions as JSON.
     * @throws IOException This can be thrown when we run into errors reading the reader.
     */
    public void load(Reader reader) throws IOException {
        JsonNode json = mapper.readTree(reader);
        Stream<JsonNode> typeStream = StreamSupport.stream(json.spliterator(), false);

        List<TypeNode> deserializedTypes = typeStream.map(this::deserializeType).collect(Collectors.toList());
        // Check if we have any deserialized types at all
        if (deserializedTypes.size() < 1) {
            log.info("Skipped type package because it didn't contain any definitions.");
            return;
        }
        // Assume that when the first node in this package exists, the whole package has been applied
        if (typeNodeRepository.findByUuid(deserializedTypes.get(0).getUuid()) != null) {
            log.info("Skipped type package because it has already been loaded.");
            return;
        }
        for (TypeNode type: deserializedTypes) {
            try {
                type.save();
            } catch (DataIntegrityViolationException e) {
                throw new NodeConstraintViolated(e.getMessage());
            }
        }
        log.info("Created " + deserializedTypes.size() + " new types.");
    }

    private TypeNode deserializeType(JsonNode data) {
        if (!JsonHelper.validateToSchema(typeSchema, data)) {
            throw new JsonValidationException("Cannot deserialize type, JSON didn't validate against schema.");
        }

        TypeNode deserializedTypeNode;

        final String name = data.get(NAME).asText();
        final String desc = data.get(DESC).asText();
        final String uuid = data.has(UUID) ? data.get(UUID).asText() : null;
        final String isa = data.has(ISA) ? data.get(ISA).asText() : null;
        final int glyphId = data.has(GLYPHID) ? data.get(GLYPHID).asInt() : 0;

        if (uuid == null) {
            deserializedTypeNode = new TypeNode(name, desc, glyphId);
        } else {
            deserializedTypeNode = new TypeNode(name, desc, uuid, glyphId);
        }

        if (isa != null) {
            TypeNode isaTypeNode = getRegisteredType(isa);
            if (isaTypeNode != null) {
                deserializedTypeNode.setIsa(isaTypeNode);
            } else {
                throw new TypeUnknownException("Error while deserializing type " + deserializedTypeNode.getName()
                        + " because it referenced an unknown parent type (" + isa + ").");
            }
        }

        setDescriptors(deserializedTypeNode, data);
        registeredTypes.put(uuid, deserializedTypeNode);

        return deserializedTypeNode;
    }

    /**
     * Get a registered type from the registeredTypes map, or the database.
     * @param uuid The UUID of the type.
     * @return The TypeNode.
     * @throws TypeUnknownException Thrown when we can't find a type with the given UUID.
     */
    private TypeNode getRegisteredType(String uuid) {
        TypeNode foundType;

        if (registeredTypes.containsKey(uuid)) {
            foundType = registeredTypes.get(uuid);
        } else {
            foundType = typeNodeRepository.findByUuid(uuid);
            if (foundType == null) {
                throw new TypeUnknownException("Type with UUID \"" + uuid + "\" unknown.");
            } else {
                registeredTypes.put(uuid, foundType);
            }
        }
        return foundType;
    }

    private void setDescriptors(TypeNode node, JsonNode data) {
        if (!data.has(DESCRIPTORS)) {
            return;
        }

        if (data.has(DESCRIPTORS)) {
            ObjectNode descriptors = (ObjectNode) data.get(DESCRIPTORS);
            Iterator<Map.Entry<String, JsonNode>> iter = descriptors.fields();
            while (iter.hasNext()) {
                Map.Entry<String, JsonNode> entry = iter.next();
                final String descriptorName = entry.getKey();
                final JsonNode descriptorData = entry.getValue();

                final String descriptorType = descriptorData.get(TYPE).asText();
                final String descriptorDesc = descriptorData.get(DESC).asText();
                final boolean descriptorRequired = descriptorData.has(REQ) && descriptorData.get(REQ).asBoolean();
                final String descriptorPattern = descriptorData.has(PATTERN) ?
                        descriptorData.get(PATTERN).asText() :
                        null;
                final String descriptorDefaultValue = descriptorData.has(DEFAULT) ?
                        descriptorData.get(DEFAULT).asText() :
                        null;

                node.setDescriptor(descriptorName, descriptorType, descriptorRequired, descriptorDesc,
                        descriptorDefaultValue, descriptorPattern);
            }
        }

    }
}
