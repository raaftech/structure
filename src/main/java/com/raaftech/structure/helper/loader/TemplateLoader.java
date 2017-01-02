package com.raaftech.structure.helper.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.raaftech.structure.base.Configuration;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.exceptions.JsonValidationException;
import com.raaftech.structure.exceptions.PackageIOException;
import com.raaftech.structure.graph.Template;
import com.raaftech.structure.graph.repositories.TemplateRepository;
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
    private static final TemplateRepository TEMPLATE_REPOSITORY = Context.getTemplateRepository();
    private static final String SCHEMA = "schema/helper/loader/template.schema.json";

    @Override
    public void load(Path filePath) {
        try {
            log.info("Loading defined template from " + filePath + ".");
            load(new FileReader(filePath.toFile()));
        } catch (IOException e) {
        	log.error(e.getMessage());

			if (Configuration.isTraceModeOn()) {
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

        if (TEMPLATE_REPOSITORY.findByUuid(json.get(UUID).asText()) != null) {
            return;
        }

        Template start = deserialize(json)
                .setRequired(true)
                .setUnique(true)
                .setStartOfTemplate(true);

        addComponents(start, json);

        start.save();
    }

    public static Template deserialize(JsonNode json) {
        final String startName = json.get(NAME).asText();
        final String startDesc = json.get(DESC).asText();
        final String startTemplateOf = json.get(TEMPLATEOF).asText();
        final String startUuid = (json.has(UUID)) ? json.get(UUID).asText() : null;

        Template template;
        if (startUuid != null) {
            template = new Template(startName, startDesc, startUuid)
                    .setIsa(startTemplateOf);
        } else {
            template = new Template(startName, startDesc)
                    .setIsa(startTemplateOf);
        }

        if (json.has(UNIQUE)) {
            template.setUnique(json.get(UNIQUE).asBoolean());
        } else {
            template.setUnique(false);
        }
        if (json.has(REQUIRED)) {
            template.setRequired(json.get(REQUIRED).asBoolean());
        } else {
            template.setRequired(false);
        }

        return template;
    }

    private void addComponents(Template current, JsonNode json) {
        if (!json.has(COMPONENTS)) {
            return;
        }

        Stream<JsonNode> componentStream = StreamSupport.stream(json.get(COMPONENTS).spliterator(), false);
        componentStream.forEach(componentJson -> {
            Template componentTemplate = deserialize(componentJson);
            current.addComponent(componentTemplate);
            addComponents(componentTemplate, componentJson);
        });
    }

}
