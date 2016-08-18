package com.raaftech.structure.helper.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;

@JsonDeserialize(using = BundleDeserializer.class)
@JsonSerialize(using = BundleSerializer.class)
class Bundle {

    private String organizationId;
    private String name;
    private int version;
    private JsonNode metadata;
    private List<String> typePackage;
    private List<String> instrumentPackage;
    private List<String> templatePackage;
    private List<String> metadataPackage;
    private List<String> mapPackage;
    private List<String> artworkPackage;
    private List<String> documentationPackage;

    public String getOrganizationId() {
        return organizationId;
    }

    public Bundle setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
        return this;
    }

    public String getName() {
        return name;
    }

    public Bundle setName(String name) {
        this.name = name;
        return this;
    }

    public int getVersion() {
        return version;
    }

    public Bundle setVersion(int version) {
        this.version = version;
        return this;
    }

    public JsonNode getMetadata() {
        return metadata;
    }

    public Bundle setMetadata(JsonNode metadata) {
        this.metadata = metadata;
        return this;
    }

    public List<String> getTypePackage() {
        return typePackage;
    }

    public Bundle setTypePackage(List<String> typePackage) {
        this.typePackage = typePackage;
        return this;
    }

    public List<String> getInstrumentPackage() {
        return instrumentPackage;
    }

    public Bundle setInstrumentPackage(List<String> instrumentPackage) {
        this.instrumentPackage = instrumentPackage;
        return this;
    }

    public List<String> getTemplatePackage() {
        return templatePackage;
    }

    public Bundle setTemplatePackage(List<String> templatePackage) {
        this.templatePackage = templatePackage;
        return this;
    }

    public List<String> getMetadataPackage() {
        return metadataPackage;
    }

    public Bundle setMetadataPackage(List<String> metadataPackage) {
        this.metadataPackage = metadataPackage;
        return this;
    }

    public List<String> getMapPackage() {
        return mapPackage;
    }

    public Bundle setMapPackage(List<String> mapPackage) {
        this.mapPackage = mapPackage;
        return this;
    }

    public List<String> getArtworkPackage() {
        return artworkPackage;
    }

    public Bundle setArtworkPackage(List<String> artworkPackage) {
        this.artworkPackage = artworkPackage;
        return this;
    }

    public List<String> getDocumentationPackage() {
        return documentationPackage;
    }

    public Bundle setDocumentationPackage(List<String> documentationPackage) {
        this.documentationPackage = documentationPackage;
        return this;
    }
}
