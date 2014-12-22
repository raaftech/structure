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
            for (SchemaUpdate schemaUpdate: schemaUpdateInstances) {
                int revision = schemaUpdate.getUpdateInformation().getRevision();
                if (packagesPerRevision.containsKey(revision)) {
                    packagesPerRevision.put(revision, packagesPerRevision.get(revision)+1);
                } else {
                    packagesPerRevision.put(revision, 1);
                }
            }

            for (int revision: packagesPerRevision.keySet()) {
                while (packagesPerRevision.get(revision) > 0) {
                    for (SchemaUpdate schemaUpdate: schemaUpdateInstances) {
                        SchemaNode updateInformation = schemaUpdate.getUpdateInformation();
                        String dependency = updateInformation.getRequiredPackage() + updateInformation.getRequiredRevision();
                        String self = updateInformation.getName() + updateInformation.getRevision();
                        if (!done.contains(self) && updateInformation.getRevision() == revision) {
                            if (updateInformation.hasRequirement() == false ||
                                    done.contains(dependency)) {
                                schemaUpdate.apply();
                                packagesPerRevision.put(revision, packagesPerRevision.get(revision) - 1);
                                done.add(self);
                            }
                        }
                    }
                }
            }
            long endTime = System.nanoTime();
            System.out.println("Created the TypeNode database in " + (endTime - startTime) / 1000000 + "ms.");
            tx.success();
        } finally {
            tx.close();
        }
        return true;
    }
}
