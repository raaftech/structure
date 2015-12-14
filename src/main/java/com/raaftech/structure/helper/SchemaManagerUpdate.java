package com.raaftech.structure.helper;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.util.ClassUtils;
import org.springframework.util.SystemPropertyUtils;

import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.SchemaNode;
import com.raaftech.structure.graph.schema.SchemaUpdate;

/**
 * Give a description of SchemaManagerUpdate here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2015-01-08
 */
public class SchemaManagerUpdate {

    GraphDatabase graphDatabase = Context.INSTANCE.getGraphDatabase();
    private Logger log = LoggerFactory.getLogger(SchemaManagerUpdate.class);

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

    private String resolveBasePackage(String basePackage) {
        return ClassUtils.convertClassNameToResourcePath(SystemPropertyUtils.resolvePlaceholders(basePackage));
    }

    public synchronized void applySchemaUpdates() throws RuntimeException {
        ArrayList<SchemaUpdate> schemaUpdateInstances = new ArrayList<>();
        Transaction tx = graphDatabase.beginTx();

        try {
            findMyTypes("com.raaftech.structure.graph.schema").stream()
            .forEach(schemaUpdateClass -> {
                try {
                    schemaUpdateInstances.add((SchemaUpdate) schemaUpdateClass.newInstance());
                } catch (InstantiationException e) {
                    log.error(e.getMessage());
                } catch (IllegalAccessException e) {
                    log.error(e.getMessage());
                }
            });
        } catch (IOException e) {
            log.error(e.getMessage());
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage());
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
                        // Check if our dependencies are satisfied.
                        HashMap<String, Integer> dependencies = updateInformation.getDependencies();
                        boolean dependenciesMet = true;
                        dependenciesMet = dependencies.entrySet().stream()
                                .allMatch(t -> done.contains(t.getKey() + t.getValue()));

                        // Build a string representation of our own version information
                        String self = updateInformation.getName() + updateInformation.getRevision();
                        // If we didn't apply this schemaUpdate yet and its' revision number is the
                        // revision number we're currently applying
                        if (!done.contains(self) && updateInformation.getRevision() == revision) {
                            // If the schemaUpdate doesn't have a functional dependency, or its' functional
                            // dependency is already applied, apply this schemaUpdate
                            if (updateInformation.hasRequirement() == false ||
                                    dependenciesMet) {
                                if (schemaUpdate.apply()) {
                                    // Decrease the number of packages we have to apply for this revision by 1
                                    packagesPerRevision.put(revision, packagesPerRevision.get(revision) - 1);
                                    // Add this schemaUpdate to the done list
                                    done.add(self);
                                } else {
                                    throw new RuntimeException("Could not apply SchemaUpdate: " +
                                            updateInformation.getName() + "R" + updateInformation.getRevision() +
                                            "\n" +  "Transaction failed.");
                                }
                            }
                        }
                    });
                }
            });
            long endTime = System.nanoTime();
            log.debug("Created the TypeNode database in " + (endTime - startTime) / 1000000 + "ms.");
            tx.success();
        } catch (RuntimeException e) {
            tx.failure();
            throw e;
        } finally {
            tx.close();
        }
    }
}
