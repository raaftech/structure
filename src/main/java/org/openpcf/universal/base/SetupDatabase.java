package org.openpcf.universal.base;

import org.neo4j.graphdb.Transaction;
import org.openpcf.universal.graph.SchemaNode;
import org.openpcf.universal.graph.repositories.NodeRepository;
import org.openpcf.universal.graph.repositories.TypeNodeRepository;
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
 * Give a description of SetupDatabase here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-11-25
 */
public class SetupDatabase {

    GraphDatabase graphDatabase;
    NodeRepository nodeRepository;
    TypeNodeRepository typeNodeRepository;

    public SetupDatabase() {
        this.graphDatabase = SpringContext.INSTANCE.getGraphDatabase();
        this.nodeRepository = SpringContext.INSTANCE.getNodeRepository();
        this.typeNodeRepository = SpringContext.INSTANCE.getTypeNodeRepository();
    }

    private List<Class> findMyTypes(String basePackage) throws IOException, ClassNotFoundException
    {
        ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
        MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resourcePatternResolver);

        List<Class> candidates = new ArrayList<Class>();
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

    public boolean createDatabase() {
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
        return true;
    }
}
