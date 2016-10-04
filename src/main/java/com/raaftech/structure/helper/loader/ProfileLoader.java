package com.raaftech.structure.helper.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.exceptions.PackageIOException;
import com.raaftech.structure.graph.Instance;
import com.raaftech.structure.graph.Type;
import com.raaftech.structure.graph.repositories.TypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import static io.vertx.core.json.Json.mapper;

public class ProfileLoader implements PackageLoader {

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


    private static final TypeRepository typeRepository = Context.getTypeRepository();
    private static final Logger log = LoggerFactory.getLogger(ProfileLoader.class);

    @Override
    public void load(Path filePath) {
        try {
            log.info("Loading profile from " + filePath + ".");
            load(new FileReader(filePath.toFile()));
        } catch (IOException e) {

        	log.error(e.getMessage());

			if (Boolean.parseBoolean(System.getProperty("traceMode"))) {
				e.printStackTrace();
			}

        	throw new PackageIOException(e.getMessage());
        }
    }

    public void load(Reader reader) throws IOException {
        JsonNode json = mapper.readTree(reader);
        if (json.isArray()) {
            Iterator<JsonNode> iter = ((ArrayNode) json).elements();
            while (iter.hasNext()) {
                Profile profile = mapper.readValue(iter.next().toString(), Profile.class);
                createProfile(profile).save();
            }
        } else {
            Profile profile = mapper.readValue(json.toString(), Profile.class);
            createProfile(profile).save();
        }
    }

    private com.raaftech.structure.graph.Profile createProfile(Profile profile) {
        ObjectNode data = mapper.createObjectNode()
                .put(NAME, profile.getName())
                .put(DESC, profile.getDesc());
        // put the object as string, because neo4j doesn't have json properties
        data.put(SERVICENAMES, stringSetToArrayNode(profile.getServiceNames()).toString());
        data.put(PACKAGENAMES, stringSetToArrayNode(profile.getPackageNames()).toString());
        ObjectNode dataFiles = mapper.createObjectNode();
        dataFiles.set(LOGFILES, stringSetToArrayNode(profile.getLogFiles()));
        dataFiles.set(DATAFILES, stringSetToArrayNode(profile.getDataFiles()));
        dataFiles.set(BINARYFILES, stringSetToArrayNode(profile.getBinaryFiles()));
        dataFiles.set(LIBRARYFILES, stringSetToArrayNode(profile.getLibraryFiles()));
        dataFiles.set(CONFIGFILES, stringSetToArrayNode(profile.getConfigurationFiles()));
        data.put(FILES, dataFiles.toString());
        // put the object as string, because neo4j doesn't have json properties
        ObjectNode dataDirectories = mapper.createObjectNode();
        dataDirectories.set(LOGDIRECTORIES, stringSetToArrayNode(profile.getLogDirectories()));
        dataDirectories.set(DATADIRECTORIES, stringSetToArrayNode(profile.getDataDirectories()));
        dataDirectories.set(BINARYDIRECTORIES, stringSetToArrayNode(profile.getBinaryDirectories()));
        dataDirectories.set(LIBRARYDIRECTORIES, stringSetToArrayNode(profile.getLibraryDirectories()));
        dataDirectories.set(CONFIGDIRECTORIES, stringSetToArrayNode(profile.getConfigurationDirectories()));
        // put the object as string, because neo4j doesn't have json properties
        data.put(DIRECTORIES, dataDirectories.toString());
        ObjectNode dataCommands = mapper.createObjectNode();
        profile.getCommands().entrySet().stream()
                .forEach(entry -> {
                    ObjectNode obj = dataCommands.putObject(entry.getKey());
                    ArrayNode command = obj.putArray("command");
                    entry.getValue().command.stream().forEach(command::add);
                    obj.put("matches", entry.getValue().match.toString());
                });
        data.put(COMMANDS, dataCommands.toString());
        com.raaftech.structure.graph.Profile profileInstance =
                new com.raaftech.structure.graph.Profile.Builder(data).build();
        profileInstance.setAppliesTo(profile.getAppliesTo());
        profileInstance.setProvidedBy(profile.getProvidedBy());
        return profileInstance;
    }

    private ArrayNode stringSetToArrayNode(Set<String> set) {
        final ArrayNode collect = mapper.createArrayNode();
        set.stream()
                .forEach(collect::add);
        return collect;
    }

}
