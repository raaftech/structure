package com.raaftech.structure.helper.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.exceptions.JsonValidationException;
import com.raaftech.structure.exceptions.NodeConstraintViolated;
import com.raaftech.structure.exceptions.PackageIOException;
import com.raaftech.structure.exceptions.TypeUnknownException;
import com.raaftech.structure.graph.Type;
import com.raaftech.structure.graph.repositories.TypeRepository;
import com.raaftech.structure.helper.JsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static io.vertx.core.json.Json.mapper;

class TypeLoader implements PackageLoader {

    private static final String NAME = "name";
    private static final String UUID = "uuid";
    private static final String ISA = "isa";
    private static final String GLYPHID = "glyphId";
    private static final String IMPLEMENTS = "implements";
    private static final String DESCRIPTORS = "descriptors";
    private static final String TYPE = "type";
    private static final String DESC = "desc";
    private static final String REQ = "required";
    private static final String PATTERN = "pattern";
    private static final String DEFAULT = "defaultValue";

    private static final Logger log = LoggerFactory.getLogger(PackageLoader.class);
    private static final TypeRepository TYPE_REPOSITORY = Context.getTypeRepository();
    private static final Map<String, Type> registeredTypes = new HashMap<>();

    private final JsonNode typeSchema = JsonHelper.jsonFromResource("schema/helper/loader/type.schema.json");

    private final Map<Type, JsonNode> typeWithImplementation = new HashMap<>();

    @Override
    public void load(Path filePath) {
        try {
            log.info("Loading defined types from " + filePath + ".");
            load(new FileReader(filePath.toFile()));
        } catch (IOException e) {
            
        	log.error(e.getMessage());

			if (Boolean.parseBoolean(System.getProperty("traceMode"))) {
				e.printStackTrace();
			}
			
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

        // Assume that when the first node in this package exists, the whole package has been applied
        if (json.isArray()) {
            // Check if we have any deserialized types at all
            if (json.size() < 1) {
                log.info("Skipped type package because it didn't contain any definitions.");
                return;
            }
            JsonNode firstType = json.get(0);
            if (JsonHelper.validateToSchema(typeSchema, firstType)) {
                if (firstType.has(UUID) && TYPE_REPOSITORY.findByUuid(firstType.get(UUID).asText()) != null) {
                    log.info("Skipped type package because it has already been loaded.");
                    return;
                }
            } else {
                throw new JsonValidationException("First type definition didn't validate against schema.");
            }
        } else {
            throw new JsonValidationException("Types should be defined as an array.");
        }

        List<Type> deserializedTypes = typeStream.map(this::deserializeType).collect(Collectors.toList());
        for (Type type: deserializedTypes) {
            try {
                type.save();
            } catch (DataIntegrityViolationException e) {
            	log.error(e.getMessage());

    			if (Boolean.parseBoolean(System.getProperty("traceMode"))) {
    				e.printStackTrace();
    			}
            	
            	throw new NodeConstraintViolated(e.getMessage());
            }
        }

        // if this type implements certain protocols, load and add them
        // this has to be done after saving all the new types, so we can use queries in the database
        typeWithImplementation.entrySet().stream()
                .forEach(entry -> addImplementations(entry.getKey(), entry.getValue()));

        log.info("Created " + deserializedTypes.size() + " new types.");
    }

    private Type deserializeType(JsonNode data) {
        if (!JsonHelper.validateToSchema(typeSchema, data)) {
            throw new JsonValidationException("Cannot deserialize type, JSON didn't validate against schema.");
        }

        Type deserializedType;

        final String name = data.get(NAME).asText();
        final String desc = data.get(DESC).asText();
        final String uuid = data.has(UUID) ? data.get(UUID).asText() : null;
        final String isa = data.has(ISA) ? data.get(ISA).asText() : null;
        final int glyphId = data.has(GLYPHID) ? data.get(GLYPHID).asInt() : 0;

        if (uuid == null) {
            deserializedType = new Type(name, desc, glyphId);
        } else {
            deserializedType = new Type(name, desc, uuid, glyphId);
        }

        if (isa != null) {
            Type isaType = getRegisteredType(isa);
            if (isaType != null) {
                deserializedType.setIsa(isaType);
            } else {
                throw new TypeUnknownException("Error while deserializing type " + deserializedType.getName()
                        + " because it referenced an unknown parent type (" + isa + ").");
            }
        }

        if (data.has(IMPLEMENTS)) {
            typeWithImplementation.put(deserializedType, data);
        }

        setDescriptors(deserializedType, data);
        registeredTypes.put(uuid, deserializedType);
        registeredTypes.put(name, deserializedType);

        return deserializedType;
    }

    private void addImplementations(Type deserializedType, JsonNode data) {
        final List<Type> implementedProtocols;
        final String name = deserializedType.getName();
        if (data.has(IMPLEMENTS)) {
            if (data.get(IMPLEMENTS).isArray()) {
                implementedProtocols = StreamSupport.stream(data.get(IMPLEMENTS).spliterator(), false)
                        .map(JsonNode::asText)
                        .map(TypeLoader::getRegisteredType)
                        .peek(t -> {
                            if (t == null) {
                                throw new TypeUnknownException("Unknown implemented type specified for type \"" + name + "\".");
                            }
                        })
                        .collect(Collectors.toList());
            } else {
                implementedProtocols = Arrays.asList(TypeLoader.getRegisteredType(data.get(IMPLEMENTS).asText()));
                if (implementedProtocols.get(0) == null) {
                    throw new TypeUnknownException("Unknown implemented type specified for type \"" + name + "\".");
                }
            }

            implementedProtocols.stream()
                    .forEach(deserializedType::addImplementedProtocol);

            deserializedType.save();
        }
    }

    /**
     * Get a registered type from the registeredTypes map, or the database.
     * @param id The UUID or name of the type.
     * @return The Type.
     * @throws TypeUnknownException Thrown when we can't find a type with the given UUID.
     */
    public static Type getRegisteredType(String id) {
        Type foundType;

        if (registeredTypes.containsKey(id)) {
            foundType = registeredTypes.get(id);
        } else {
            foundType = TYPE_REPOSITORY.findByUuid(id);
            if (foundType == null) {
                foundType = TYPE_REPOSITORY.findOneByName(id);
            }
            if (foundType == null) {
                throw new TypeUnknownException("Type with UUID \"" + id + "\" unknown.");
            } else {
                registeredTypes.put(foundType.getUuid(), foundType);
                registeredTypes.put(foundType.getName(), foundType);
            }
        }
        return foundType;
    }

    private void setDescriptors(Type node, JsonNode data) {
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
