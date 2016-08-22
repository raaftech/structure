package com.raaftech.structure.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.processors.syntax.SyntaxValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Boy van Duuren &lt;boy@raaftech.com&gt;.
 */
public class JsonHelper {
    private final static ObjectMapper mapper =  new ObjectMapper();
    private final static Logger log = LoggerFactory.getLogger(JsonHelper.class);

    // A json file loader (looks in your classpath/resources.)
    public static JsonNode jsonFromResource(String resource, boolean showError) {
        JsonNode result = null;
        InputStream inputStream = JsonHelper.class.getClassLoader().getResourceAsStream(resource);

        try {
            result = mapper.readTree(inputStream);
        } catch (IOException x) {
            if (showError) {
                log.error("Error while opening " + resource + " as JSON: \n" + x.getMessage());
            }
        }

        return result;
    }

    public static JsonNode jsonFromResource(String resource) {
        return jsonFromResource(resource, true);
    }

    // A json file loader (looks in filesystems)
    public static JsonNode jsonFromFile(String file, boolean showError) {
        JsonNode result = null;
        try {
            result = mapper.readTree(new File(file));
        } catch (IOException e) {
            if (showError) {
                log.error("Error while opening " + file + " as JSON: \n" + e.getMessage());
            }
        }
        return result;
    }

    public static JsonNode jsonFromFile(String resource) {
        return jsonFromFile(resource, true);
    }

    // Load a json file, look in classpath/resources first, then on FS
    public static JsonNode jsonFromResourceOrFile(String file) {
        JsonNode result = jsonFromResource(file, false);
        if (result == null) {
            result = jsonFromFile(file, false);
        }
        if (result == null) {
            log.error("Couldn't open file " + file + " in classpath, or on filesystem.");
        }
        return result;
    }

    public static boolean validateToSchema(JsonNode schema, JsonNode object) {
        final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        final SyntaxValidator syntaxValidator = factory.getSyntaxValidator();
        ProcessingReport report = syntaxValidator.validateSchema(schema);
        boolean valid = false;

        if (!report.isSuccess()) {
            throw new IllegalArgumentException("Provided schema is not a valid JSON schema.");
        }

        try {
            final JsonSchema jsonSchema = factory.getJsonSchema(schema);
            valid = jsonSchema.validate(object).isSuccess();
        } catch (ProcessingException e) {
            e.printStackTrace();
        }

        return valid;
    }
}
