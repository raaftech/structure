package tech.raaf.structure.helper;

import com.networknt.schema.AbsoluteIri;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaException;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.SpecificationVersion;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.raaf.structure.base.Configuration;
import tech.raaf.structure.base.Configuration.Setting;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.bus.manager.SchemaManager;
import tech.raaf.structure.exceptions.JsonValidationException;
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
  // The host of the standard meta-schemas, which a client has itself
  private static final String metaSchemaHost = "http://json-schema.org/";
  // Type schemas come from the graph and can change, thus no cache.
  // No preload: it overflows the stack on a circular $ref without cache.
  private static final SchemaRegistry registry = SchemaRegistry.withDefaultDialect(
    SpecificationVersion.DRAFT_4,
    builder ->
      builder
        .schemaCacheEnabled(false)
        .schemaRegistryConfig(SchemaRegistryConfig.builder().preloadSchema(false).build())
        .schemaLoader(loader -> loader.fetchRemoteResources().allow(JsonHelper::allowed))
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

  // The location of a URI without its fragment
  private static URI document(URI uri) {
    return URI.create(uri.toString().replaceFirst("#.*", ""));
  }

  // Load a schema document from the permitted locations, or null
  private static JsonNode schemaDocument(URI uri) {
    // The loader gives the text, and does not follow the references
    final AbsoluteIri iri = AbsoluteIri.of(uri.toString());
    try (InputStream in = registry.getSchemaLoader().getSchemaResource(iri).getInputStream()) {
      return mapper.readTree(in);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Collect schema documents with all the documents that they refer to.
   * A client can resolve each $ref in the result without other sources.
   * The result does not contain the draft-04 meta-schema.
   * @param roots The URIs of the schemas to start from.
   * @return The schema documents, by document URI.
   * @throws JsonValidationException If a document is missing, a reference
   *     is not valid or two schemas have the same id.
   */
  public static SortedMap<URI, JsonNode> schemaCollection(Collection<URI> roots) {
    final SortedMap<URI, JsonNode> documents = new TreeMap<>();
    // The schemas with an id, the references, and the documents to load
    final Map<URI, JsonNode> ids = new HashMap<>();
    final Map<URI, String> refs = new LinkedHashMap<>();
    final Deque<URI> queue = new ArrayDeque<>(roots);
    final List<String> errors = new ArrayList<>();
    final Set<URI> missing = new TreeSet<>();
    while (!queue.isEmpty()) {
      final URI uri = document(queue.remove());
      if (documents.containsKey(uri) || missing.contains(uri) || uri.toString().startsWith(metaSchemaHost)) {
        continue;
      }
      final JsonNode schema = schemaDocument(uri);
      if (schema == null) {
        missing.add(uri);
        continue;
      }
      documents.put(uri, schema);
      scan(schema, uri, uri, ids, refs, queue, errors);
    }
    // A reference can point to the id of a schema in a document
    missing.removeAll(ids.keySet());
    missing.forEach(uri -> errors.add("missing document " + uri));
    ids.forEach((id, schema) -> {
      if (documents.containsKey(id) && documents.get(id) != schema) {
        errors.add("conflicting id " + id);
      }
    });
    refs.forEach((ref, where) -> {
      final URI target = document(ref);
      final JsonNode schema = documents.containsKey(target) ? documents.get(target) : ids.get(target);
      final String fragment = ref.getFragment();
      final boolean pointer = fragment == null || fragment.isEmpty() || fragment.startsWith("/");
      // A fragment is a JSON pointer, or the name of a schema with an id
      final boolean found = pointer
        ? schema != null && (fragment == null || !schema.at(fragment).isMissingNode())
        : ids.containsKey(ref);
      if (!found && !missing.contains(target) && !target.toString().startsWith(metaSchemaHost)) {
        errors.add("invalid reference " + ref + " in " + where);
      }
    });
    if (!errors.isEmpty()) {
      throw new JsonValidationException("Schema collection is not complete: " + errors);
    }
    return documents;
  }

  // Find each id and $ref in a schema. The base changes at each id.
  private static void scan(
    JsonNode schema,
    URI base,
    URI document,
    Map<URI, JsonNode> ids,
    Map<URI, String> refs,
    Deque<URI> queue,
    List<String> errors
  ) {
    if (schema.isObject()) {
      // Only a string is a keyword; an object is a property with this name
      final JsonNode id = schema.get("id");
      final JsonNode ref = schema.get("$ref");
      try {
        if (id != null && id.isString()) {
          base = base.resolve(new URI(id.asString().replaceFirst("#$", "")));
          final JsonNode other = ids.putIfAbsent(base, schema);
          if (other != null && other != schema) {
            errors.add("conflicting id " + base);
          }
        }
        if (ref != null && ref.isString()) {
          final URI target = base.resolve(new URI(ref.asString()));
          refs.putIfAbsent(target, document.toString());
          queue.add(target);
        }
      } catch (URISyntaxException e) {
        errors.add("invalid reference or id in " + document + ": " + e.getMessage());
      }
    }
    for (Map.Entry<String, JsonNode> child : schema.properties()) {
      // These keywords hold data and not schemas
      if (!child.getKey().equals("enum") && !child.getKey().equals("default")) {
        scan(child.getValue(), base, document, ids, refs, queue, errors);
      }
    }
    if (schema.isArray()) {
      for (JsonNode child : schema.values()) {
        scan(child, base, document, ids, refs, queue, errors);
      }
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
