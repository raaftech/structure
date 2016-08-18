package com.raaftech.structure.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.processors.syntax.SyntaxValidator;
import com.raaftech.structure.base.GenericActor;
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
    public static JsonNode jsonFromResource(String resource) {
        JsonNode result;
        InputStream inputStream = JsonHelper.class.getClassLoader().getResourceAsStream(resource);

        try {
            result = mapper.readTree(inputStream);
        } catch (IOException x) {
            log.error(x.getMessage());
            return null;
        }

        return result;
    }

    // A json file loader (looks in filesystems)
    public static JsonNode jsonFromFile(String file) {
        JsonNode result = null;
        try {
            result = mapper.readTree(new File(file));
        } catch (IOException e) {
            log.error(e.getMessage());
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
