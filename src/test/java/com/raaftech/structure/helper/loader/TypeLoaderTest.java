package com.raaftech.structure.helper.loader;

import com.raaftech.structure.base.SpringTestConfig;
import com.raaftech.structure.graph.TypeNode;
import com.raaftech.structure.graph.repositories.TypeNodeRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.transaction.annotation.Transactional;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.StringReader;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = SpringTestConfig.class)
public class TypeLoaderTest {

    @Autowired
    TypeNodeRepository typeNodeRepository;

    @Test
    @Transactional
    @Rollback
    public void load() throws Exception {
        String json = "[{\n" +
                "\"name\": \"testType1\",\n" +
                "\"desc\": \"used for testing\",\n" +
                "\"uuid\": \"34f26d03-c97a-439c-ade4-5b42fbda60df\",\n" +
                "\"descriptors\": {\n" +
                "\"foo\": { \"type\": \"boolean\", \"desc\": \"foo\" }\n" +
                "}\n" +
                "},\n" +
                "{\"name\": \"testType2\",\n" +
                "\"desc\": \"used for testing\",\n" +
                "\"uuid\": \"bcf8f47a-583d-4834-9425-10aa22ad1bc2\",\n" +
                "\"isa\": \"34f26d03-c97a-439c-ade4-5b42fbda60df\",\n" +
                "\"descriptors\": {\n" +
                "\"bar\": { \"type\": \"boolean\", \"desc\": \"foo\", \"required\": true }\n" +
                "}}\n" +
                "]";

        String json2 = "[{\n" +
                "\"name\": \"testType3\",\n" +
                "\"desc\": \"used for testing\",\n" +
                "\"uuid\": \"7b9d4b8e-3236-436a-9eb8-f6b02e339373\",\n" +
                "\"isa\": \"34f26d03-c97a-439c-ade4-5b42fbda60df\"\n" +
                "}]";
        new TypeLoader().load(new StringReader(json));
        new TypeLoader().load(new StringReader(json2));

        TypeNode testType1 = typeNodeRepository.findOneByName("testType1");
        assertNotNull(testType1);
        assertEquals(2, testType1.getChildIsaSet().size());
        testType1.getChildIsaSet().stream()
                .forEach(type -> {
                    assertTrue("testType2".equals(type.getName()) || "testType3".equals(type.getName()));
                });

    }

}