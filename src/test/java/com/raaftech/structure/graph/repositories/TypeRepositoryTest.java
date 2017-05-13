package com.raaftech.structure.graph.repositories;

import com.raaftech.structure.base.SpringTestConfig;
import com.raaftech.structure.graph.Type;
import com.raaftech.structure.helper.loader.BundleLoader;
import com.raaftech.structure.helper.loader.NaiveSorter;
import com.raaftech.structure.next.Entity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.io.IOException;
import java.util.Set;

import static io.vertx.core.json.Json.mapper;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = SpringTestConfig.class)
public class TypeRepositoryTest {

    @Autowired
    TypeRepository typeRepository;
    @Autowired
    GraphDatabase graphDatabase;

    private static boolean databaseCreated = false;

    @Before
    @SuppressWarnings("Duplicates")
    public void setUpDatabase() throws Exception {
        if (!databaseCreated) {
            final String raafBundle = "src/main/resources/bundles/";
            Transaction tx = graphDatabase.beginTx();
            BundleLoader bundleLoader = new BundleLoader(raafBundle);
            bundleLoader.load();
            tx.success();
            databaseCreated = true;
        }
    }

    @Test
    public void findByNameAndUuid() throws Exception {
        Type linuxByName = typeRepository.findOneByName("Linux");
        assertNotNull(linuxByName);
        Type linuxByUuid = typeRepository.findByUuid(linuxByName.getUuid());
        assertNotNull(linuxByUuid);
        assertEquals(linuxByName.getUuid(), linuxByUuid.getUuid());
        assertEquals(linuxByName.getId(), linuxByUuid.getId());
    }

    @Test
    public void findByName() throws Exception {
        Set<Type> linux = typeRepository.findByName("Linux");
        assertEquals(1, linux.size());
    }

    @Test
    public void isOfType() throws Exception {
        Type linux = typeRepository.findOneByName("Linux");
        assertNotNull(linux);
        assertTrue(typeRepository.isOfType(linux.getUuid(), "OperatingSystem") > 0);

        // n.b.: as opposed to "isSubType", for every type a, isOfType(a, a) should be false
        assertTrue(typeRepository.isOfType(linux.getUuid(), linux.getName()) == 0);
    }

    @Test
    public void getCommonProtocols() throws Exception {
        // todo: create test
    }

    @Test
    public void getHierarchyPath() throws Exception {
        Type linux = typeRepository.findOneByName("Linux");
        assertEquals("type.component.software.operatingsystem.unixlike.linux",
                typeRepository.getHierarchyPath(linux.getUuid()).toLowerCase());
    }

    @Test
    public void isSubtype() throws Exception {
        Type linux = typeRepository.findOneByName("Linux");
        assertTrue(typeRepository.isSubtype(linux.getUuid(), linux.getUuid()) > 0);
    }

}
