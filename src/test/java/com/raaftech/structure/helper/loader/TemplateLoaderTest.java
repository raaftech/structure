package com.raaftech.structure.helper.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.raaftech.structure.helper.JsonHelper;
import org.junit.Test;

import java.nio.file.Paths;

import static io.vertx.core.json.Json.mapper;
import static org.junit.Assert.*;

public class TemplateLoaderTest {

    @Test
    public void testSchema() throws Exception {
        // invalid JSON because of components[0]
        JsonNode json = mapper.readTree(
                "{\n" +
                "\"name\":\"abc\",\n" +
                "\"desc\":\"def\",\n" +
                "\"templateOf\":\"541e0632-984f-448f-994c-3ca0ccd1ed4c\",\n" +
                "\"uuid\":\"541e0632-984f-448f-994c-3ca0ccd1ed4c\",\n" +
                "\"components\":[{\"abc\":\"def\"}]\n" +
                "}");
        assertFalse(JsonHelper.validateToSchema(
                JsonHelper.jsonFromResource("schema/helper/loader/template.schema.json"), json));

        // valid json
       json = mapper.readTree(
                "{\n" +
                "\"name\":\"abc\",\n" +
                "\"desc\":\"def\",\n" +
                "\"templateOf\":\"541e0632-984f-448f-994c-3ca0ccd1ed4c\",\n" +
                "\"uuid\":\"541e0632-984f-448f-994c-3ca0ccd1ed4c\",\n" +
                "\"components\":[{\"name\":\"def\"," +
                "\"desc\":\"xyz\"," +
                "\"uuid\":\"1b72c6b2-4e7e-46ff-8e71-fa6ba3d5728c\"," +
                "\"templateOf\":\"5ca1a5b1-ff73-4184-a4e3-a6690953346a\"}]\n" +
                "}");
        assertTrue(JsonHelper.validateToSchema(
                JsonHelper.jsonFromResource("schema/helper/loader/template.schema.json"), json));

        // same as above, but invalid UUID in first templateOf
        json = mapper.readTree(
                "{\n" +
                        "\"name\":\"abc\",\n" +
                        "\"desc\":\"def\",\n" +
                        "\"templateOf\":\"-541e0632-984f-448f-994c-3ca0ccd1ed4c\",\n" +
                        "\"uuid\":\"541e0632-984f-448f-994c-3ca0ccd1ed4c\",\n" +
                        "\"components\":[{\"name\":\"def\"," +
                        "\"desc\":\"xyz\"," +
                        "\"uuid\":\"1b72c6b2-4e7e-46ff-8e71-fa6ba3d5728c\"," +
                        "\"templateOf\":\"5ca1a5b1-ff73-4184-a4e3-a6690953346a\"}]\n" +
                        "}");
        assertFalse(JsonHelper.validateToSchema(
                JsonHelper.jsonFromResource("schema/helper/loader/template.schema.json"), json));

        // actual template in use
        json = mapper.readTree(
                "{\n" +
                "  \"name\": \"OperatingSystem\",\n" +
                "  \"desc\": \"Generic operating system.\",\n" +
                "  \"uuid\": \"1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa\",\n" +
                "  \"templateOf\": \"f05e325e-1aee-4892-96e4-8bcd6e97c528\",\n" +
                "  \"components\": [\n" +
                "    {\n" +
                "      \"name\": \"Client\",\n" +
                "      \"desc\": \"Generic client software.\",\n" +
                "      \"uuid\": \"e8e3c643-a520-4d63-9088-dd27086a1879\",\n" +
                "      \"templateOf\": \"8f371b1b-52a3-4409-be70-9cd947bec8dd\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"Server\",\n" +
                "      \"desc\": \"Generic server software.\",\n" +
                "      \"uuid\": \"54126ca8-3a5e-43b9-a501-98c8d158049d\",\n" +
                "      \"templateOf\": \"3fb4603b-281e-47c0-b338-768ea05d4660\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"Interpreter\",\n" +
                "      \"desc\": \"Generic interpreter.\",\n" +
                "      \"uuid\": \"2741cc5a-eb45-4532-9a64-c715c07f93bc\",\n" +
                "      \"templateOf\": \"d047f415-734c-4bd3-92b9-d68d5521ce69\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"Ipv4Address\",\n" +
                "      \"desc\": \"An IPv4 address.\",\n" +
                "      \"uuid\": \"0e843d97-c975-42ca-813c-c91f42a7d3cc\",\n" +
                "      \"templateOf\": \"f7fbe917-9d49-4df5-95f6-6e132067a424\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"DnsName\",\n" +
                "      \"desc\": \"The short or fully qualified domain name.\",\n" +
                "      \"uuid\": \"94d10031-5246-4233-b25d-b5f8ef9d0b41\",\n" +
                "      \"templateOf\": \"99c0da37-3599-418d-8ecc-de36d37ae9f4\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"ServiceManager\",\n" +
                "      \"desc\": \"Generic service manager.\",\n" +
                "      \"uuid\": \"34bfe99f-1d62-48cb-a8a3-e913b69ef225\",\n" +
                "      \"templateOf\": \"c3008eec-b454-4ef6-bf7e-0bd92c13d3c3\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"Hypervisor\",\n" +
                "      \"desc\": \"A generic hypervisor.\",\n" +
                "      \"uuid\": \"39e12352-8305-4b5d-af60-f1725ec458b8\",\n" +
                "      \"templateOf\": \"c5c7fcb5-8ac3-40cc-b195-b2b6af368e81\"\n" +
                "    }\n" +
                "  ]\n" +
                "}");
        assertTrue(JsonHelper.validateToSchema(
                JsonHelper.jsonFromResource("schema/helper/loader/template.schema.json"), json));
    }

}