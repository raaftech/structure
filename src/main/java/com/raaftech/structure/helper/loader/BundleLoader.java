package com.raaftech.structure.helper.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raaftech.structure.base.Configuration;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.exceptions.JsonLoadException;
import com.raaftech.structure.helper.JsonHelper;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * BundleLoader is responsible for taking a loader and making sure all the definitions in that bundle are
 * made available to Structure.
 */
public class BundleLoader {

    private static final Logger log = LoggerFactory.getLogger(BundleLoader.class);

    private final Path bundlePath;

    /**
     * Load a bundle, providing structure with data.
     * @param bundlePath The file path to the bundle definition.
     */
    public BundleLoader(Path bundlePath) {
        this.bundlePath = bundlePath;
    }

    /**
     * Load all the packages defined in a bundle definition.
     */
    public void load() throws IOException {
        // Start by validating the provided bundle against our schema
        ObjectMapper mapper = new ObjectMapper();
        JsonNode bundleJson = JsonHelper.jsonFromResourceOrFile(bundlePath.toString());
        if (bundleJson == null) {
            throw new JsonLoadException("Error while loading JSON in " + bundlePath.toString() + ", possibly because of syntax errors.");
        }
        Bundle bundle = mapper.readValue(bundleJson.toString(), Bundle.class);
        log.info("Successfully deserialized bundle \"" + bundle.getName() + "\" from JSON.");

        Transaction tx = Context.getGraphDatabase().beginTx();
        try {
            loadPackage(bundle.getTypePackage(), new TypeLoader());
            loadPackage(bundle.getInstrumentPackage(), new InstrumentLoader());
            loadPackage(bundle.getTemplatePackage(), new TemplateLoader());
            loadPackage(bundle.getMetadataPackage(), new MetadataLoader());
            loadPackage(bundle.getProfilePackage(), new ProfileLoader());
            loadPackage(bundle.getArtworkPackage(), new ArtworkLoader());
            loadPackage(bundle.getDocumentationPackage(), new DocumentationLoader());
            tx.success();
            log.info("Loaded bundle \"" + bundle.getName() + "\".");
        } catch (Exception e) {
            log.error("Loading of bundle \"" + bundle.getName() + "\" cancelled. Transaction rolled back.\n"
                    + "Exception message is \"" + e.getMessage() + "\".");
            if (Configuration.isTraceModeOn()) {
                e.printStackTrace();
            }
            tx.failure();
        } finally {
            tx.close();
        }
    }

    private void loadPackage(List<String> packageElements, PackageLoader packageLoader) {
        if (packageElements == null)
            return;

        // Find all the files defined in the package
        List<Path> foundFiles = packageElements.stream()
                .map(this::globFiles)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        // Load every one of them
        foundFiles.stream()
                .forEachOrdered(packageLoader::load);
        
        if (foundFiles.isEmpty()) {
        	log.debug("loadPackage: No files found to load");
        }
    }

    private List<Path> globFiles(String pattern) {
    	final List<Path> matchingFiles = new ArrayList<>();
        final String workingDirectory = bundlePath.getParent().toString().replace('\\', '/');
        final String syntaxAndPattern = "glob:" + workingDirectory + "/" + pattern.replace('\\', '/');
        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher(syntaxAndPattern);
        
        log.debug("globFiles: got pattern: " + pattern);
        log.debug("globFiles: got workingDirectory: " + workingDirectory);
        log.debug("globFiles: got syntaxAndPattern: " + syntaxAndPattern);
        
        try {
            Files.walkFileTree(Paths.get(workingDirectory), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (matcher.matches(file)) {
                    	log.debug("globFiles: matched file: " + file);
                    	matchingFiles.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
        	log.debug(e.getMessage());
        	
        	if (Configuration.isTraceModeOn()) {
        		e.printStackTrace();
        	}
        }

        return matchingFiles;
    }
}
