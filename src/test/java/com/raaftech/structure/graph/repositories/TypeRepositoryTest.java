package com.raaftech.structure.graph.repositories;

import com.raaftech.structure.graph.Type;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.GraphDatabase;
import schema.TypeSchemaTest;

import java.util.Set;

import static org.junit.Assert.*;

public class TypeRepositoryTest extends TypeSchemaTest {

    @Autowired
    TypeRepository typeRepository;
    @Autowired
    GraphDatabase graphDatabase;

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
