package com.raaftech.structure.helper.loader;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.exceptions.JsonValidationException;
import com.raaftech.structure.helper.JsonHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class BundleDeserializer extends StdDeserializer<Bundle> {

    private ObjectMapper mapper = new ObjectMapper();
    private final JsonNode bundleSchema = JsonHelper.jsonFromResource("schema/helper/loader/bundle.schema.json");

    public BundleDeserializer() {
        super(Bundle.class);
    }

    @Override
    public Bundle deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException, JsonProcessingException {
        JsonNode data = jsonParser.getCodec().readTree(jsonParser);
        if (!JsonHelper.validateToSchema(bundleSchema, data)) {
            throw new JsonValidationException("Cannot deserialize bundle, JSON didn't validate against schema.");
        }
        ObjectNode packages = (ObjectNode) data.get("packages");
        return new Bundle()
                .setOrganizationId(data.get("organizationId").asText())
                .setName(data.get("name").asText())
                .setVersion(data.get("version").asInt())
                .setMetadata(data.get("metadata"))
                .setTypePackage(deserializePackage(packages, "type"))
                .setInstrumentPackage(deserializePackage(packages, "instrument"))
                .setTemplatePackage(deserializePackage(packages, "template"))
                .setMetadataPackage(deserializePackage(packages, "metadata"))
                .setMapPackage(deserializePackage(packages, "map"))
                .setArtworkPackage(deserializePackage(packages, "artwork"))
                .setDocumentationPackage(deserializePackage(packages, "documentation"));
    }

    // A package can be a string, or an array, so we need to be smart about deserializing them
    private List<String> deserializePackage(ObjectNode packages, String key) throws IOException {
        List<String> packageData;

        if (packages.has(key)) {
            JsonNode packageNode = packages.get(key);
            if (packageNode.getNodeType() == JsonNodeType.ARRAY) {
                packageData = mapper.readValue(packageNode.toString(),
                        mapper.getTypeFactory().constructCollectionType(ArrayList.class, String.class));
            } else {
                packageData =  new ArrayList<>();
                packageData.add(packageNode.asText());
            }
        } else {
            packageData = null;
        }

        return packageData;
    }

}
