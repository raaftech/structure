package tech.raaf.structure.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.load.configuration.LoadingConfiguration;
import com.github.fge.jsonschema.core.load.uri.URITranslatorConfiguration;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.processors.syntax.SyntaxValidator;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.raaf.structure.base.Configuration;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.bus.manager.SchemaManager;

/**
 * @author Boy van Duuren <boy@raaf.tech>
 */
public class JsonHelper {

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final Logger log = LoggerFactory.getLogger(JsonHelper.class);

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

  public static boolean validateToSchema(JsonSchema schema, JsonNode object) {
    Boolean validationState = false;

    try {
      ProcessingReport validJson = schema.validate(object);
      Iterator<ProcessingMessage> validJsonIterator = validJson.iterator();
      ProcessingMessage validJsonMessage;

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

  /**
   * See if a JSON object validates against a given schema.
   * We mainly use this method with static schema's located in our resources.
   * @param schema The URI of the schema to use for validation.
   * @param object The object to validate.
   * @return Does object validate against schema?
   */
  public static boolean validateToSchema(URI schema, JsonNode object) {
    boolean validationState = false;
    JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.byDefault();
    try {
      JsonSchema jsonSchema = jsonSchemaFactory.getJsonSchema(schema.toString());
      validationState = validateToSchema(jsonSchema, object);
    } catch (ProcessingException e) {
      if (Configuration.isTraceModeOn()) {
        e.printStackTrace();
      }
    }

    return validationState;
  }

  public static boolean validateToSchema(JsonNode schema, JsonNode object, URI customNamespace) {
    Boolean validationState = false;

    try {
      JsonSchemaFactory jsonSchemaFactory;

      if (Context.getSchemaNamespace() != null) {
        final URI nameSpaceURI;
        if (customNamespace == null) {
          nameSpaceURI = Context.getSchemaNamespace().resolve("/schemas/");
        } else {
          nameSpaceURI = customNamespace;
        }
        final URITranslatorConfiguration uriTranslatorConfiguration = URITranslatorConfiguration.newBuilder()
          .setNamespace(nameSpaceURI)
          .freeze();
        final LoadingConfiguration loadingConfiguration = LoadingConfiguration.newBuilder()
          .setURITranslatorConfiguration(uriTranslatorConfiguration)
          .freeze();
        jsonSchemaFactory = JsonSchemaFactory.newBuilder().setLoadingConfiguration(loadingConfiguration).freeze();
      } else {
        jsonSchemaFactory = JsonSchemaFactory.byDefault();
      }
      SyntaxValidator syntaxValidator = jsonSchemaFactory.getSyntaxValidator();
      ProcessingReport validSchema = syntaxValidator.validateSchema(schema);
      ProcessingMessage validSchemaMessage;
      Iterator<ProcessingMessage> validSchemaIterator = validSchema.iterator();
      if (!validSchema.isSuccess()) {
        while (validSchemaIterator.hasNext()) {
          validSchemaMessage = validSchemaIterator.next();
          if (validSchemaMessage != null) {
            log.error("JSON schema validation message: " + validSchemaMessage.getMessage());
          }
        }
        return validationState;
      }

      JsonSchema jsonSchema = jsonSchemaFactory.getJsonSchema(schema);
      validationState = validateToSchema(jsonSchema, object);
    } catch (ProcessingException e) {
      e.printStackTrace();
    }

    return validationState;
  }

  /**
   * See if a JSON object validates against a given schema.
   * This version of validateSchema(...) is mainly used for schema's that are retrieved from the database.
   * Because we can't natively expose the graph as a namespace for schema's, we need to provide a proxy to
   * these schema's. That is {@link SchemaManager SchemaManager}'s job,
   * and this method sets the namespace to {@link SchemaManager SchemaManager}'s
   * location on the webserver.
   * @param schema The URI of the schema to use for validation.
   * @param object The object to validate.
   * @return Does object validate against schema?
   */
  public static boolean validateToSchema(JsonNode schema, JsonNode object) {
    return validateToSchema(schema, object, null);
  }
}
