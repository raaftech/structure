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
    public static JsonNode jsonFromResource(String resource) {
        JsonNode result = null;
        InputStream inputStream = JsonHelper.class.getClassLoader().getResourceAsStream(resource);

        try {
            result = mapper.readTree(inputStream);
        } catch (Exception e) {
			if (log.isTraceEnabled()) {
	            log.error("Couldn't open as resource " + resource + ": " + e.getMessage());
				e.printStackTrace();
			}
        }

        return result;
    }

    // A json file loader (looks in filesystems)
    public static JsonNode jsonFromFile(String file) {
        JsonNode result = null;
        try {
            result = mapper.readTree(new File(file));
        } catch (IOException e) {            
			if (log.isTraceEnabled()) {
	            log.error("Couldn't open as file  " + file + ": " + e.getMessage());
				e.printStackTrace();
			}
        }
        return result;
    }

    // Load a json file, look in classpath/resources first, then on FS
    public static JsonNode jsonFromResourceOrFile(String file) {
    	JsonNode result = jsonFromResource(file);
        if (result == null) {
            result = jsonFromFile(file);
        }
        if (result == null) {
            log.error("Couldn't open " + file + " in classpath, or on filesystem.");
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
            if (log.isTraceEnabled()) {
                e.printStackTrace();
            }
        }

        return valid;
    }
}
