package com.raaftech.structure.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.processors.syntax.SyntaxValidator;
import com.raaftech.structure.base.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Created by Boy van Duuren &lt;boy@raaftech.com&gt;.
 */
public class JsonHelper {
	private final static ObjectMapper mapper = new ObjectMapper();
	private final static Logger log = LoggerFactory.getLogger(JsonHelper.class);

	/**
	 * Load a JSON file from the classpath.
	 * 
	 * @param resource
	 *            The name of the JSON file.
	 * @param via
	 *            Is this method called directly, or via a wrapper? This
	 *            controls the log level of the statement that gets printed when
	 *            we can't find the resource.
	 * @return A JsonNode representing the resource, or null if we can't find
	 *         anything.
	 */
	public static JsonNode jsonFromResource(String resource, boolean via) {
		JsonNode result = null;
		InputStream inputStream = JsonHelper.class.getClassLoader().getResourceAsStream(resource);

		try {
			result = mapper.readTree(inputStream);
		} catch (Exception e) {
			if (Configuration.isTraceModeOn()) {
				final String logStatement = "Couldn't open as resource " + resource + ": " + e.getMessage();
				if (via) {
					log.warn(logStatement);
				} else {
					log.error(logStatement);
				}
				e.printStackTrace();
			}
		}

		return result;
	}

	public static JsonNode jsonFromResource(String resource) {
		return jsonFromResource(resource, false);
	}

	/**
	 * Load a JSON file from a filesystem.
	 * 
	 * @param file
	 *            The name of the JSON file.
	 * @param via
	 *            Is this method called directly, or via a wrapper? This
	 *            controls the log level of the statement that gets printed when
	 *            we can't find the resource.
	 * @return A JsonNode representing the resource, or null if we can't find
	 *         anything.
	 */
	public static JsonNode jsonFromFile(String file, boolean via) {
		JsonNode result = null;
		try {
			result = mapper.readTree(new File(file));
		} catch (IOException e) {
			if (Configuration.isTraceModeOn()) {
				final String logStatement = "Couldn't open as file  " + file + ": " + e.getMessage();
				if (via) {
					log.warn(logStatement);
				} else {
					log.error(logStatement);
				}
				e.printStackTrace();
			}
		}
		return result;
	}

	public static JsonNode jsonFromFile(String file) {
		return jsonFromFile(file, false);
	}

	// Load a json file, look in classpath/resources first, then on FS
	public static JsonNode jsonFromResourceOrFile(String file) {
		JsonNode result = jsonFromResource(file, true);
		if (result == null) {
			result = jsonFromFile(file, true);
		}
		if (result == null) {
			log.error("Couldn't open " + file + " in classpath, or on filesystem.");
		}
		return result;
	}

	public static boolean validateToSchema(JsonNode schema, JsonNode object) {

		Boolean validationState = false;

		try {
			JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
			JsonSchema jsonSchema = factory.getJsonSchema(schema);

			SyntaxValidator syntaxValidator = factory.getSyntaxValidator();

			ProcessingReport validSchema = syntaxValidator.validateSchema(schema);
			ProcessingReport validJson = jsonSchema.validate(object);

			Iterator<ProcessingMessage> validSchemaIterator = validSchema.iterator();
			Iterator<ProcessingMessage> validJsonIterator = validJson.iterator();

			ProcessingMessage validSchemaMessage;
			ProcessingMessage validJsonMessage;

			if (!validSchema.isSuccess()) {
				while (validSchemaIterator.hasNext()) {
					validSchemaMessage = validSchemaIterator.next();
					if (validSchemaMessage != null) {
						log.error("JSON schema validation message: " + validSchemaMessage.getMessage());
					}
				}
				return validationState;
			}

			if (!validJson.isSuccess()) {
				while (validJsonIterator.hasNext()) {
					validJsonMessage = validJsonIterator.next();
					if (validJsonMessage != null) {
						log.error("JSON data validation message: " + validJsonMessage.getMessage());
					}
				}
				return validationState;
			} else {
				validationState = true;
			}
		} catch (ProcessingException e) {
			log.error(e.getMessage());
			return validationState;
		}

		return validationState;
	}
}
