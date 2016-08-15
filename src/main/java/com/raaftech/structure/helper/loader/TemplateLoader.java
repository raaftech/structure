package com.raaftech.structure.helper.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.exceptions.JsonValidationException;
import com.raaftech.structure.exceptions.PackageIOException;
import com.raaftech.structure.graph.TemplateNode;
import com.raaftech.structure.graph.repositories.TemplateNodeRepository;
import com.raaftech.structure.helper.JsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static io.vertx.core.json.Json.mapper;

class TemplateLoader implements PackageLoader {

    private static final String NAME = "name";
    private static final String DESC = "desc";
    private static final String TEMPLATEOF = "templateOf";
    private static final String UUID = "uuid";
    private static final String COMPONENTS = "components";
    private static final String UNIQUE = "unique";
    private static final String REQUIRED = "required";

    private static final Logger log = LoggerFactory.getLogger(TemplateLoader.class);
    private static final TemplateNodeRepository templateNodeRepository = Context.INSTANCE.getTemplateNodeRepository();
    private static final String SCHEMA = "schema/helper/loader/template.schema.json";

    @Override
    public void load(Path filePath) {
        try {
            log.info("Loading defined template from " + filePath + ".");
            load(new FileReader(filePath.toFile()));
        } catch (IOException e) {
        	log.error(e.getMessage());

			if (log.isTraceEnabled()) {
				e.printStackTrace();
			}
			
        	throw new PackageIOException(e.getMessage());
        }
    }

    /**
     * Load the template package with a reader. This is called by load(Path filePath).
     * This is also exposed this way so we can easily test the loader by passing StringReaders.
     * @param reader A reader containing the template definition as JSON.
     * @throws IOException This can be thrown when we run into errors reading the reader.
     */
    public void load(Reader reader) throws IOException {
        JsonNode json = mapper.readTree(reader);
        if (!JsonHelper.validateToSchema(JsonHelper.jsonFromResource(SCHEMA), json)) {
            throw new JsonValidationException("Template definition didn't match template schema.");
        }

        if (templateNodeRepository.findByUuid(json.get(UUID).asText()) != null) {
            return;
        }

        TemplateNode start = deserialize(json)
                .setRequired(true)
                .setUnique(true)
                .setStartOfTemplate(true);

        addComponents(start, json);

        start.save();
    }

    private TemplateNode deserialize(JsonNode json) {
        final String startName = json.get(NAME).asText();
        final String startDesc = json.get(DESC).asText();
        final String startTemplateOf = json.get(TEMPLATEOF).asText();
        final String startUuid = json.get(UUID).asText();

        TemplateNode templateNode = new TemplateNode(startName, startDesc, startUuid)
                .setIsa(startTemplateOf);

        if (json.has(UNIQUE)) {
            templateNode.setUnique(json.get(UNIQUE).asBoolean());
        } else {
            templateNode.setUnique(false);
        }
        if (json.has(REQUIRED)) {
            templateNode.setRequired(json.get(REQUIRED).asBoolean());
        } else {
            templateNode.setRequired(false);
        }

        return templateNode;
    }

    private void addComponents(TemplateNode current, JsonNode json) {
        if (!json.has(COMPONENTS)) {
            return;
        }

        Stream<JsonNode> componentStream = StreamSupport.stream(json.get(COMPONENTS).spliterator(), false);
        componentStream.forEach(componentJson -> {
            TemplateNode componentTemplate = deserialize(componentJson);
            current.addComponent(componentTemplate);
            addComponents(componentTemplate, componentJson);
        });
    }

}
