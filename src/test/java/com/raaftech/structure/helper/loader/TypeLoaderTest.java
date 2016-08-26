package com.raaftech.structure.helper.loader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.raaftech.structure.graph.TypeNode;
import org.junit.Test;

import java.io.StringReader;
import java.util.List;

import static io.vertx.core.json.Json.mapper;
import static org.junit.Assert.*;

/**
 * Created by boy on 8/23/16.
 */
public class TypeLoaderTest {
    @Test
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
    }

}