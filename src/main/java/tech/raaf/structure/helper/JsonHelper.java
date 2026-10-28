package tech.raaf.structure.helper;

import com.networknt.schema.AbsoluteIri;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaException;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.raaf.structure.base.Configuration;
import tech.raaf.structure.base.Configuration.Setting;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.bus.manager.SchemaManager;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Boy van Duuren <boy@raaf.tech>
 */
public class JsonHelper {

  // The mapper for all JSON in Structure
  public static final ObjectMapper mapper = new ObjectMapper();
  private static final Logger log = LoggerFactory.getLogger(JsonHelper.class);
  // Schemas can load from these locations and from the schema namespace
  private static final List<String> locations = List.of(
    "resource:",
    "classpath:",
    "http://json-schema.org/",
    "https://json-schema.org/"
  );
  // Type schemas come from the graph and can change, thus no cache
  private static final SchemaRegistry registry = SchemaRegistry.withDefaultDialect(
    SpecificationVersion.DRAFT_4,
    builder ->
      builder.schemaCacheEnabled(false).schemaLoader(loader -> loader.fetchRemoteResources().allow(JsonHelper::allowed))
  );
  // The draft-04 meta-schema, to check the syntax of a schema
  private static final Schema metaSchema = registry.getSchema(
    SchemaLocation.of(SpecificationVersion.DRAFT_4.getDialectId())
  );

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
      // Relative to home, never to the working directory.
      result = mapper.readTree(Configuration.get(Setting.HOME).resolve(file).toFile());
    } catch (JacksonException e) {
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

  // The location of the type schemas, or null if it is not set
  private static URI schemaNamespace() {
    final URI namespace = Context.getSchemaNamespace();
    return namespace == null ? null : namespace.resolve("/schemas/");
  }

  // Can a $ref load this location? This prevents requests to other hosts
  private static boolean allowed(AbsoluteIri iri) {
    final String location = iri.toString();
    final URI namespace = schemaNamespace();
    // A .. or %2e%2e segment can leave the namespace
    final boolean dots = location.toLowerCase().replace("%2e", ".").contains("..");
    return (
      locations.stream().anyMatch(location::startsWith) ||
      (namespace != null && location.startsWith(namespace.toString()) && !dots)
    );
  }

  // Load the schema, and give the reasons why the object is not valid
  private static List<String> validate(Supplier<Schema> schema, JsonNode object) {
    try {
      return schema.get().validate(object).stream().map(Error::toString).toList();
    } catch (SchemaException e) {
      return List.of(String.valueOf(e.getMessage()));
    }
  }

  /**
   * Validate a JSON object against a given schema.
   * We mainly use this method with static schema's located in our resources.
   * The caller reports the reasons; this method does not log them.
   * @param schema The URI of the schema to use for validation.
   * @param object The object to validate.
   * @return The reasons why object is not valid; empty if it is valid.
   */
  public static List<String> validationErrors(URI schema, JsonNode object) {
    return validate(() -> registry.getSchema(SchemaLocation.of(schema.toString())), object);
  }

  public static List<String> validationErrors(JsonNode schema, JsonNode object, URI customNamespace) {
    final List<Error> syntaxErrors = schemaErrors(schema);
    if (!syntaxErrors.isEmpty()) {
      return syntaxErrors
        .stream()
        .map(error -> "schema " + error)
        .toList();
    }
    if (Context.getSchemaNamespace() == null) {
      return validate(() -> registry.getSchema(schema), object);
    }
    final URI namespace = customNamespace == null ? schemaNamespace() : customNamespace;
    return validate(() -> registry.getSchema(SchemaLocation.of(namespace.toString()), schema), object);
  }

  // The reasons why a schema is not valid draft-04; empty if it is valid
  public static List<Error> schemaErrors(JsonNode schema) {
    return metaSchema.validate(schema);
  }

  /**
   * Validate a JSON object against a given schema.
   * This version of validationErrors(...) is mainly used for schema's that are retrieved from the database.
   * Because we can't natively expose the graph as a namespace for schema's, we need to provide a proxy to
   * these schema's. That is {@link SchemaManager SchemaManager}'s job,
   * and this method sets the namespace to {@link SchemaManager SchemaManager}'s
   * location on the webserver.
   * @param schema The URI of the schema to use for validation.
   * @param object The object to validate.
   * @return The reasons why object is not valid; empty if it is valid.
   */
  public static List<String> validationErrors(JsonNode schema, JsonNode object) {
    return validationErrors(schema, object, null);
  }
}
