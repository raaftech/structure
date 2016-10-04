package com.raaftech.structure.helper.loader;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.List;

class BundleSerializer extends StdSerializer<Bundle> {

    private JsonGenerator jsonGenerator;

    // Generic constructor that just calls super(T.class)
    public BundleSerializer() {
        super(Bundle.class);
    }

    @Override
    public void serialize(Bundle bundle, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        this.jsonGenerator = jsonGenerator;
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("organizationId", bundle.getOrganizationId());
        jsonGenerator.writeStringField("name", bundle.getName());
        jsonGenerator.writeNumberField("version", bundle.getVersion());
        if (bundle.getMetadata() != null) {
            jsonGenerator.writeObjectField("metadata", bundle.getMetadata());
        }
        jsonGenerator.writeObjectFieldStart("packages");
        writePackageData(bundle.getTypePackage(), "type");
        writePackageData(bundle.getInstrumentPackage(), "instrument");
        writePackageData(bundle.getTemplatePackage(), "template");
        writePackageData(bundle.getMetadataPackage(), "metadata");
        writePackageData(bundle.getProfilePackage(), "profile");
        writePackageData(bundle.getArtworkPackage(), "artwork");
        writePackageData(bundle.getDocumentationPackage(), "documentation");
        jsonGenerator.writeEndObject();
        jsonGenerator.writeEndObject();
    }

    private void writePackageData(List<String> list, String field) throws IOException {
        if (list != null && !list.isEmpty()) {
            if (list.size() > 1) {
                jsonGenerator.writeArrayFieldStart(field);
                for (String s : list) {
                    jsonGenerator.writeString(s);
                }
                jsonGenerator.writeEndArray();
            } else {
                jsonGenerator.writeStringField(field, list.get(0));
            }
        }
    }
}
