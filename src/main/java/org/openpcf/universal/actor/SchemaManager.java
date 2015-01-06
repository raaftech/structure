// Copyright © 2008-2014 RAAF Technology bv

package org.openpcf.universal.actor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.neo4j.graphdb.Transaction;
import org.openpcf.universal.base.BaseActor;
import org.openpcf.universal.base.SpringContext;
import org.openpcf.universal.graph.SchemaNode;
import org.openpcf.universal.graph.schema.SchemaUpdate;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.util.ClassUtils;
import org.springframework.util.SystemPropertyUtils;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * The SchemaManager actor.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-09-11
 */
public class SchemaManager extends BaseActor {

    GraphDatabase graphDatabase = SpringContext.INSTANCE.getGraphDatabase();

    private List<Class> findMyTypes(String basePackage) throws IOException, ClassNotFoundException
    {
        ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
        MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resourcePatternResolver);

        List<Class> candidates = new ArrayList<>();
        String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                resolveBasePackage(basePackage) + "/" + "**/*.class";
        Resource[] resources = resourcePatternResolver.getResources(packageSearchPath);
        for (Resource resource : resources) {
            if (resource.isReadable()) {
                MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
                if (isCandidate(metadataReader)) {
                    candidates.add(Class.forName(metadataReader.getClassMetadata().getClassName()));
                }
            }
        }
        return candidates;
    }

    private String resolveBasePackage(String basePackage) {
        return ClassUtils.convertClassNameToResourcePath(SystemPropertyUtils.resolvePlaceholders(basePackage));
    }

    private boolean isCandidate(MetadataReader metadataReader) throws ClassNotFoundException
    {
        try {
            Class c = Class.forName(metadataReader.getClassMetadata().getClassName());
            if (!Modifier.isAbstract(c.getModifiers())) {
                return true;
            }
        }
        catch(Throwable e){
        }
        return false;
    }

    private void applySchemaUpdates() {
        ArrayList<SchemaUpdate> schemaUpdateInstances = new ArrayList<>();
        Transaction tx = graphDatabase.beginTx();

        try {
            findMyTypes("org.openpcf.universal.graph.schema").stream()
                    .forEach(schemaUpdateClass -> {
                        try {
                            schemaUpdateInstances.add((SchemaUpdate) schemaUpdateClass.newInstance());
                        } catch (InstantiationException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        try {
            long startTime = System.nanoTime();
            ArrayList<String> done = new ArrayList<>();
            TreeMap<Integer, Integer> packagesPerRevision = new TreeMap<>();

            // Get a count of the amount of packages per revision number.
            schemaUpdateInstances.stream()
                    .forEach(schemaUpdate -> {
                        int revision = schemaUpdate.getUpdateInformation().getRevision();
                        if (packagesPerRevision.containsKey(revision)) {
                            packagesPerRevision.put(revision, packagesPerRevision.get(revision) + 1);
                        } else {
                            packagesPerRevision.put(revision, 1);
                        }
                    });

            // For every schemaUpdate revision number we have, we're going to
            // first apply the schemaUpdate that doesn't have dependencies, the second
            // iteration, we're going to apply schemaUpdates that rely on the schemaUpdates
            // applied in the previous iteration, and so on until every schemaUpdate for that
            // revision number is applied. Then we move onto the next revision number and repeat.
            packagesPerRevision.keySet().stream()
                    .forEach(revision -> {
                        // While we still have unapplied schemaUpdates for the current revision
                        while (packagesPerRevision.get(revision) > 0) {
                            schemaUpdateInstances.stream()
                                    .forEach(schemaUpdate -> {
                                        SchemaNode updateInformation = schemaUpdate.getUpdateInformation();
                                        // Build a string representation of the schemaUpdate's dependency version information
                                        String dependency = updateInformation.getRequiredPackage() +
                                                updateInformation.getRequiredRevision();

                                        // Build a string representation of our own version information
                                        String self = updateInformation.getName() + updateInformation.getRevision();
                                        // If we didn't apply this schemaUpdate yet and its' revision number is the
                                        // revision number we're currently applying
                                        if (!done.contains(self) && updateInformation.getRevision() == revision) {
                                            // If the schemaUpdate doesn't have a functional dependency, or its' functional
                                            // dependency is already applied, apply this schemaUpdate
                                            if (updateInformation.hasRequirement() == false ||
                                                    done.contains(dependency)) {
                                                schemaUpdate.apply();
                                                // Decrease the number of packages we have to apply for this revision by 1
                                                packagesPerRevision.put(revision, packagesPerRevision.get(revision) - 1);
                                                // Add this schemaUpdate to the done list
                                                done.add(self);
                                            }
                                        }
                                    });
                        }
                    });
            long endTime = System.nanoTime();
            System.out.println("Created the TypeNode database in " + (endTime - startTime) / 1000000 + "ms.");
            tx.success();
        } finally {
            tx.close();
        }
    }

    // Check if the database is available.
    private Boolean databaseIsAvailable() {
        return false;
    }

    // Check if the database contains data.
    private Boolean databaseIsUsed() {
        return false;
    }

    // Check if database is ours.
    private Boolean databaseIsOurs() {
        return false;
    }

    // Check update subscriptions/location.
    private ArrayList<String> listUpdateSites() {
        return new ArrayList<>();
    }

    // Fetch new schema files from update server.
    private void fetchUpdates() {
    }

    // Check current structure schema version.
    private String getCurrentVersion() {
        return "getCurrentVersion stub";
    }

    // Check structure software instance version.
    private String getAvailableVersion() {
        return "getAvailableVersion stub";
    }

    // Check if currentVer and availableVer are different.
    private Boolean currentVersionIsLatest() {
        return false;
    }

    // Create backup of current database.
    private void databaseBackup() {
    }

    // Restore backup of current database.
    private void databaseRestore() {
    }

    // Apply updated schema.
    private void applyUpdates() {
    }

    // Undo applied update.
    private void undoUpdates() {
    }

    public MessageHandler reinit() {
        return new MessageHandler() {
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                result.put("says", "I am reinit!");
                return response(request, result);
            }

            public JsonNode help(JsonNode request) {
                return jsonFromResource("help/schemamanager.reinit.json");
            }
        };
    }

    public MessageHandler update() {
        return new MessageHandler() {
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                result.put("says", "I am update!");
                applySchemaUpdates();
                return response(request, result);
            }

            public JsonNode help(JsonNode request) {
                return jsonFromResource("help/schemamanager.update.json");
            }
        };
    }

    public MessageHandler status() {
        return new MessageHandler() {
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                result.put("says", "I am status!");
                return response(request, result);
            }

            public JsonNode help(JsonNode request) {
                return jsonFromResource("help/schemamanager.status.json");
            }
        };
    }
}
