package com.raaftech.structure.helper.loader;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.exceptions.JsonValidationException;
import com.raaftech.structure.exceptions.TypeUnknownException;
import com.raaftech.structure.graph.Type;
import com.raaftech.structure.graph.repositories.TypeRepository;
import com.raaftech.structure.helper.JsonHelper;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class ProfileDeserializer extends StdDeserializer<Profile> {

    private static final String NAME = "name";
    private static final String DESC = "desc";
    private static final String PROVIDEDBY = "providedBy";
    private static final String APPLIESTO = "appliesTo";
    private static final String SERVICENAMES = "serviceNames";
    private static final String PACKAGENAMES = "packageNames";
    private static final String FILES = "files";
    private static final String LOGFILES = "logFiles";
    private static final String DATAFILES = "dataFiles";
    private static final String BINARYFILES = "binaryFiles";
    private static final String LIBRARYFILES = "libraryFiles";
    private static final String CONFIGFILES = "configurationFiles";
    private static final String DIRECTORIES = "directories";
    private static final String LOGDIRECTORIES = "logDirectories";
    private static final String DATADIRECTORIES = "dataDirectories";
    private static final String BINARYDIRECTORIES = "binaryDirectories";
    private static final String LIBRARYDIRECTORIES = "libraryDirectories";
    private static final String CONFIGDIRECTORIES = "configurationDirectories";
    private static final String COMMANDS = "commands";
    private static final String COMMAND = "command";
    private static final String MATCHES = "matches";


    private final ObjectMapper mapper = new ObjectMapper();
    private final TypeRepository typeRepository = Context.getTypeRepository();
    private final JsonNode bundleSchema = JsonHelper.jsonFromResource("schema/helper/loader/profile.schema.json");

    public ProfileDeserializer() {
        super(Profile.class);
    }

    @Override
    public Profile deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        ObjectNode data = jsonParser.getCodec().readTree(jsonParser);
        Iterator<JsonNode> iter;
        if (!JsonHelper.validateToSchema(bundleSchema, data)) {
            throw new JsonValidationException("Cannot deserialize bundle, JSON didn't validate against schema.");
        }

        ArrayNode dataAppliesTo = (ArrayNode) data.get(APPLIESTO);
        Set<Type> appliesTo = new HashSet<>();
        iter = dataAppliesTo.elements();
        while (iter.hasNext()) {
            final String typeName = iter.next().asText();
            final Type type = typeRepository.findOneByName(typeName);
            if (type == null) {
                throw new TypeUnknownException("Can't find type with name " + typeName
                        + " while deserializing profile " + data.get(NAME).asText());
            }
            appliesTo.add(type);
        }
        final String providedByName = data.get(PROVIDEDBY).asText();
        final Type providedBy = typeRepository.findOneByName(providedByName);
        if (providedBy == null) {
            throw new TypeUnknownException("Can't find type with name " + providedByName
                    + " while deserializing profile " + data.get(NAME).asText());
        }

        final Profile profile = new Profile(data.get(NAME).asText(), data.get(DESC).asText(), providedBy, appliesTo);

        profile.setServiceNames(getStringSet(data, SERVICENAMES));
        profile.setPackageNames(getStringSet(data, PACKAGENAMES));

        if (data.has(FILES)) {
            ObjectNode dataFiles = (ObjectNode) data.get(FILES);
            profile.setLogFiles(getStringSet(dataFiles, LOGFILES));
            profile.setDataFiles(getStringSet(dataFiles, DATAFILES));
            profile.setBinaryFiles(getStringSet(dataFiles, BINARYFILES));
            profile.setLibraryFiles(getStringSet(dataFiles, LIBRARYFILES));
            profile.setConfigurationFiles(getStringSet(dataFiles, CONFIGFILES));
        }

        if (data.has(DIRECTORIES)) {
            ObjectNode dataDirectories = (ObjectNode) data.get(DIRECTORIES);
            profile.setLogDirectories(getStringSet(dataDirectories, LOGDIRECTORIES));
            profile.setDataDirectories(getStringSet(dataDirectories, DATADIRECTORIES));
            profile.setBinaryDirectories(getStringSet(dataDirectories, BINARYDIRECTORIES));
            profile.setLibraryDirectories(getStringSet(dataDirectories, LIBRARYDIRECTORIES));
            profile.setConfigurationDirectories(getStringSet(dataDirectories, CONFIGDIRECTORIES));
        }

        if (data.has(COMMANDS)) {
            ObjectNode dataCommands = (ObjectNode) data.get(COMMANDS);
            Map<String, Profile.Command> commands = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fieldIter = dataCommands.fields();
            while (fieldIter.hasNext()) {
                Map.Entry<String, JsonNode> entry = fieldIter.next();
                ObjectNode dataCommand = (ObjectNode) entry.getValue();
                Profile.Command command = new Profile.Command();
                iter = dataCommand.get(COMMAND).elements();
                while (iter.hasNext()) {
                    command.command.add(iter.next().asText());
                }
                command.match = Pattern.compile(dataCommand.get(MATCHES).asText());
                commands.put(entry.getKey(), command);
            }
            profile.setCommands(commands);
        }

        return profile;
    }

    private Set<String> getStringSet(ObjectNode data, String key) {
        Set<String> collect = new HashSet<>();
        if (data.has(key)) {
            ArrayNode arrayNode = (ArrayNode) data.get(key);
            Iterator<JsonNode> iter = arrayNode.elements();
            while (iter.hasNext()) {
                collect.add(iter.next().asText());
            }
        }
        return collect.size() > 0 ? collect : null;
    }
}
