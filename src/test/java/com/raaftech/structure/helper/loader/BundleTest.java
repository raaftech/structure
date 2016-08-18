package com.raaftech.structure.helper.loader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.raaftech.structure.exceptions.JsonValidationException;
import com.raaftech.structure.helper.JsonHelper;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.*;

public class BundleTest {

    final ObjectMapper mapper = new ObjectMapper();
    final JsonNode bundleSchema = JsonHelper.jsonFromResource("schema/helper/loader/bundle.schema.json");

    @Test
    public void serialize() throws Exception {
        Bundle bundle = new Bundle()
                .setOrganizationId("com.raaftech")
                .setName("test")
                .setVersion(1)
                .setMapPackage(Arrays.asList("fooMap.json", "barMap.json"));

        String serialized =
                "{\n" +
                "  \"organizationId\" : \"com.raaftech\",\n" +
                "  \"name\" : \"test\",\n" +
                "  \"version\" : 1,\n" +
                "  \"packages\" : {\n" +
                "    \"map\" : [ \"fooMap.json\", \"barMap.json\" ]\n" +
                "  }\n" +
                "}";

        testSerializedJson(serialized, bundle);

        bundle = new Bundle()
                .setOrganizationId("com.raaftech")
                .setName("test")
                .setVersion(2)
                .setMetadata(mapper.readTree("{\"creator\": \"Boy van Duuren\"}"))
                .setInstrumentPackage(Arrays.asList("instrument.json"))
                .setTypePackage(Arrays.asList("operatingsystems.json", "interpreters.json"));

        serialized =
                "{\n" +
                "  \"organizationId\" : \"com.raaftech\",\n" +
                "  \"name\" : \"test\",\n" +
                "  \"version\" : 2,\n" +
                "  \"metadata\" : {\n" +
                "    \"creator\" : \"Boy van Duuren\"\n" +
                "  },\n" +
                "  \"packages\" : {\n" +
                "    \"type\" : [ \"operatingsystems.json\", \"interpreters.json\" ],\n" +
                "    \"instrument\" : \"instrument.json\"\n" +
                "  }\n" +
                "}";

        testSerializedJson(serialized, bundle);
    }

    @Test
    public void deserialize() throws Exception {
        String json =
                "{\n" +
                "    \"organizationId\": \"com.raaftech\",\n" +
                "    \"name\": \"deserializeme\",\n" +
                "    \"version\": 1,\n" +
                "    \"packages\": {\n" +
                "        \"type\": [\n" +
                "            \"foo.json\",\n" +
                "            \"bar.json\"\n" +
                "        ]\n" +
                "    }\n" +
                "}";

        JsonHelper.validateToSchema(bundleSchema, mapper.readTree(json));

        Bundle bundle = mapper.readValue(json, Bundle.class);
        assertEquals("com.raaftech", bundle.getOrganizationId());
        assertEquals("deserializeme", bundle.getName());
        assertEquals(1, bundle.getVersion());
        assertEquals(2, bundle.getTypePackage().size());
        assertEquals("foo.json", bundle.getTypePackage().get(0));
        assertEquals("bar.json", bundle.getTypePackage().get(1));
        assertEquals(null, bundle.getInstrumentPackage());
        assertEquals(null, bundle.getTemplatePackage());
        assertEquals(null, bundle.getMetadataPackage());
        assertEquals(null, bundle.getMapPackage());
        assertEquals(null, bundle.getArtworkPackage());
        assertEquals(null, bundle.getDocumentationPackage());

    }

    @Test(expected = JsonValidationException.class)
    public void illegalDeserialize() throws IOException {
        String json =
                "{\n" +
                        "    \"organizationId\": \"com.raaftech\",\n" +
                        "    \"version\": 1,\n" +
                        "    \"packages\": {\n" +
                        "        \"type\": [\n" +
                        "            \"foo.json\",\n" +
                        "            \"bar.json\"\n" +
                        "        ]\n" +
                        "    }\n" +
                        "}";

        mapper.readValue(json, Bundle.class);
    }

    // Checks whether a bundle serializes to the expected JSON string, and validates that JSON against
    // the bundle schema.
    private void testSerializedJson(String expected, Bundle bundle) throws JsonProcessingException, ProcessingException {
        JsonNode serializedBundle = mapper.valueToTree(bundle);
        assertEquals(expected, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(serializedBundle));
        assertEquals(true, JsonHelper.validateToSchema(bundleSchema, serializedBundle));
    }

}