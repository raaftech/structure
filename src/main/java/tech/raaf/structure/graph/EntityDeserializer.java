package tech.raaf.structure.graph;

import static tech.raaf.structure.helper.JsonHelper.mapper;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.raaf.structure.base.Configuration;
import tech.raaf.structure.base.Configuration.Setting;
import tech.raaf.structure.exceptions.EntityDeserializationException;
import tech.raaf.structure.exceptions.JsonValidationException;
import tech.raaf.structure.helper.JsonHelper;
import tech.raaf.structure.helper.Triple;
import tech.raaf.structure.helper.loader.BundleLoader;
import tech.raaf.structure.helper.loader.NaiveSorter;

public class EntityDeserializer extends StdDeserializer<Entity> {

  /* Serializable class version field. */
  private static final long serialVersionUID = 1L;

  /* Logging object. */
  private static final Logger log = LoggerFactory.getLogger(EntityDeserializer.class);

  /* URI of JSON Schema for Entity objects. */
  private final URI entitySchema = URI.create("resource:/schema/entity.schema.json");

  /* key names for entity JSON */
  private static final String FILE_DATASCHEMA = "dataSchema";

  private String bundle;
  private String name;
  private String desc;
  private String uuid;

  private boolean isInstance;

  private JsonNode metaJson;
  private JsonNode relationsJson;
  private JsonNode dataSchema;

  private String checksum;

  private Set<Triple<String, String, Set<String>>> relations;

  private JsonNode fileJson;
  private Map<String, Object> file;

  private JsonNode dataJson;
  private Map<String, Object> data;

  public EntityDeserializer() {
    super(Entity.class);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Entity deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
    log.info("Creating entity from JSON data");
    Entity entity = new Entity();
    ObjectNode entityJson = jsonParser.getCodec().readTree(jsonParser);

    /*
     * Validate schema and go. Notice we don't log anything,
     * validateToSchema does that for us. Also note that it is the schema's
     * responsibility to make sure the values are there and are correct!
     * Don't clutter the code with null checks and value mangling!
     */
    if (JsonHelper.validateToSchema(entitySchema, entityJson)) {
      /* Set name. */
      name = entityJson.get("name").asText();
      log.debug("Setting name to \"{}\"", name);
      entity.setName(name);

      /* Set description. */
      desc = entityJson.get("desc").asText();
      log.debug("Setting desc to \"{}\"", desc);
      entity.setDesc(desc);

      /* Load the meta object. */
      metaJson = entityJson.get("meta");

      /* Set instance flag. */
      isInstance = BundleLoader.isInstance(entityJson);

      /* Handle relations within meta. */
      relationsJson = metaJson.get("relations");
      relations = new HashSet<Triple<String, String, Set<String>>>();

      Iterator<Map.Entry<String, JsonNode>> relationsIterator = relationsJson.fields();
      while (relationsIterator.hasNext()) {
        Map.Entry<String, JsonNode> entry = relationsIterator.next();
        String key = entry.getKey();
        JsonNode value = entry.getValue();

        String rname = key;
        String direction = value.get("direction").asText();
        ArrayNode peersJson = (ArrayNode) value.get("to");
        Set<String> peers = new HashSet<String>();
        peersJson.forEach(peer -> peers.add(peer.asText()));

        log.debug("Adding {} relation triple for entity \"{}\" named \"{}\"", direction, name, rname);
        Triple<String, String, Set<String>> relation = new Triple<String, String, Set<String>>(rname, direction, peers);

        relations.add(relation);
      }

      entity.setRelations(relations);

      // Handle constraints
      if (metaJson.has("constraints")) {
        final JsonNode constraintsJson = metaJson.get("constraints");
        Iterator<Map.Entry<String, JsonNode>> iterConstraints = constraintsJson.fields();

        while (iterConstraints.hasNext()) {
          Map.Entry<String, JsonNode> entry = iterConstraints.next();
          final String relName = entry.getKey();
          final ArrayNode constraints = (ArrayNode) entry.getValue();

          Iterator<JsonNode> iterConstraintData = constraints.elements();
          while (iterConstraintData.hasNext()) {
            JsonNode constraintData = iterConstraintData.next();
            entity.addConstraint(
              relName,
              Constraint.Cardinality.valueOf(constraintData.get("cardinality").asText()),
              constraintData.get("to").asText()
            );
          }
        }
      }

      // Persist identity, the schema refuses it on an instance
      final String identityKey = "identity";
      if (metaJson.has(identityKey)) {
        final JsonNode identityProperty = metaJson.get(identityKey);
        final Set<String> identity = identityProperty.isArray()
          ? ImmutableSet.copyOf(mapper.convertValue(identityProperty, HashSet.class))
          : ImmutableSet.of(identityProperty.asText());
        entity.setIdentity(identity.toArray(new String[identity.size()]));
      }

      /* Handle uuid within meta if existent, else generate. */
      if (metaJson.has("uuid")) {
        uuid = metaJson.get("uuid").asText();
        log.debug("Setting uuid to \"{}\"", uuid);
        entity.setUuid(uuid);
      } else {
        uuid = UUID.randomUUID().toString();
        log.debug("Creating random uuid \"{}\"", uuid);
        entity.setUuid(uuid);
      }

      /* Handle bundle within meta if existent, else generate. */
      if (metaJson.has("bundle")) {
        bundle = metaJson.get("bundle").asText();
        log.debug("Setting bundle to \"{}\"", bundle);
        entity.setBundle(bundle);
      } else {
        // TODO: replace this guard code with actual retrieve bundle code
        log.debug("Setting default bundle name [core]");
        entity.setBundle("core");
      }

      /* Handle path within meta if existent. The bundle loader injects it. */
      if (metaJson.has("path")) {
        final String path = metaJson.get("path").asText();
        log.debug("Setting path from JSON data to \"{}\"", path);
        entity.setPath(path);
      }

      /* Handle checksum within meta if existent, else create. */
      if (metaJson.has("checksum")) {
        checksum = metaJson.get("checksum").asText();
      } else {
        checksum = NaiveSorter.calculateChecksum(entityJson, NaiveSorter.ChecksumAlgorithm.SHA1);
      }
      log.debug("Setting checksum to \"{}\"", checksum);
      entity.setChecksum(checksum);

      /* If an entity has the meta key `abstract` and its a type, add it */
      if (metaJson.has("abstract") && !isInstance) {
        entity.setAbstract(metaJson.get("abstract").asBoolean());
      }

      /* Handle the file object if existent. */
      if (entityJson.has("file")) {
        file = new HashMap<>();

        fileJson = entityJson.get("file");
        Iterator<Map.Entry<String, JsonNode>> fileIterator = fileJson.fields();
        while (fileIterator.hasNext()) {
          Map.Entry<String, JsonNode> entry = fileIterator.next();
          String key = entry.getKey();
          log.debug("Found \"{}\" in file object, starting work..", key);
          ObjectNode value = (ObjectNode) entry.getValue();

          String encoding = value.get("encoding").asText();
          String format = value.get("format").asText();

          if (value.has("path") && value.has("content")) {
            log.debug("Writing JSON object with name \"{}\" to file object", key);
            file.put(key, value.toString());
          } else if (value.has("path")) {
            File filePath = pathHelper(new File(value.get("path").asText()));

            if (filePath.exists() && !filePath.isDirectory()) {
              switch (encoding) {
                case "base64":
                  String fileString = FileUtils.readFileToString(filePath, "UTF-8");
                  log.debug("Base64 encoding file \"{}\" and injecting into content", filePath.toString());
                  value.put("content", Base64.encodeBase64String(fileString.getBytes("UTF-8")));
                  log.debug("Writing JSON object with name \"{}\" to file object", key);
                  file.put(key, value.toString());
                  break;
                case "text":
                  log.debug("Text encoding file \"{}\" and injecting into content", filePath.toString());
                  value.put("content", FileUtils.readFileToString(filePath, "UTF-8"));
                  log.debug("Writing JSON object with name \"{}\" to file object", key);
                  file.put(key, value.toString());
                  break;
              }
            } else {
              log.error("\"" + filePath + "\" does not exist or is a directory");
            }
          } else if (value.has("content")) {
            // we don't lowercase the key variable, because the schema is case sensitive
            String pathString = String.join(".", name.toLowerCase(), isInstance ? "instance" : "type", key, format);
            log.debug("Created path from name, type, file key and format as \"{}\"", pathString);
            value.put("path", pathString);
            log.debug("Writing JSON object with name \"{}\" to file object", key);
          }

          if (!value.has("content")) {
            log.error("No content field found, stopping load process.");
            throw new EntityDeserializationException("No content field found, stopping load process.");
          }
          // we now have content, so we can validate, or create a checksum
          final String checksum;
          final boolean checksumMatches;
          // we need to use toString so we don't lose escaping, but we don't need the encapsulating "
          final String content = value
            .get("content")
            .toString()
            .substring(1, value.get("content").toString().length() - 1);
          if (value.has("checksum")) {
            checksum = value.get("checksum").asText();
            checksumMatches = checksum.equals(DigestUtils.sha1Hex(content));
          } else {
            checksumMatches = true;
            checksum = DigestUtils.sha1Hex(content);
          }
          if (checksumMatches) {
            value.put("checksum", checksum);
          } else {
            throw new EntityDeserializationException(
              "Specified checksum for file \"" + key + "\" does not match content"
            );
          }
          value.put("checksum", DigestUtils.sha1Hex(value.get("content").asText()));

          // save file to properties
          file.put(key, value.toString());

          // do some validation checks:

          // if format is JSON, we need to make sure it is actually valid JSON, else a JsonProcessingException
          // gets thrown
          if ("json".equals(value.get("format").asText())) {
            try {
              mapper.readTree(value.get("content").asText());
            } catch (JsonParseException e) {
              throw new EntityDeserializationException(
                "Can't deserialize entity \"" +
                  entity +
                  "\" " +
                  "in bundle \"" +
                  bundle +
                  "\", because it contains invalid embedded JSON"
              );
            }
          }

          // all is well. does this key match the dataSchema key? then we keep track of it, because
          // we might use it later on for validating the data object
          if (FILE_DATASCHEMA.equals(key)) {
            dataSchema = mapper.readTree(value.get("content").asText());
          }
        }

        entity.setFile(file);
      }

      /* Handle the data object if existent. */
      if (entityJson.has("data")) {
        data = new HashMap<>();

        dataJson = entityJson.get("data");
        //FIXME the full schema validation should already do this, so i'm thinking this piece of code is obsolete
        // if this entity was provided with a dataSchema, make sure the data object validates against that
        // if (dataSchema != null) {
        //   if (!JsonHelper.validateToSchema(dataSchema, dataJson)) {
        //     throw new EntityDeserializationException("Provided data for entity \"" + name + "\" doesn't " +
        //         "validate against dataSchema.");
        //   }
        // }
        Iterator<Map.Entry<String, JsonNode>> dataIterator = dataJson.fields();

        while (dataIterator.hasNext()) {
          Map.Entry<String, JsonNode> next = dataIterator.next();
          final String key = next.getKey();
          log.debug("Found \"{}\" in data object, starting work..", key);

          final JsonNode value = next.getValue();
          switch (value.getNodeType()) {
            case ARRAY:
              log.debug("Writing key \"{}\" with ARRAY value \"{}\" in data object", key, value);
              data.put(key, value);
              break;
            case BINARY:
              log.warn("Binary value for key \"{}\" in data object not supported, not writing key", key);
              break;
            case BOOLEAN:
              log.debug("Writing key \"{}\" with BOOLEAN value \"{}\" in data object", key, value);
              data.put(key, value.asBoolean());
              break;
            case MISSING:
              log.warn("Missing value for key \"{}\" in data object, not writing key", key);
              break;
            case NULL:
              log.warn("Null value for key \"{}\" in data object, not writing key", key);
              break;
            case NUMBER:
              log.debug("Writing key \"{}\" with NUMBER value \"{}\" in data object", key, value);
              if (value.isInt()) {
                data.put(key, value.asInt());
              } else {
                data.put(key, value.asDouble());
              }
              break;
            case OBJECT:
              log.debug("Writing key \"{}\" with OBJECT value \"{}\" in data object", key, value);
              data.put(key, value);
              break;
            case POJO:
              log.warn("Pojo value for key \"{}\" in data object not supported, not writing key", key);
            case STRING:
              log.debug("Writing key \"{}\" with STRING value \"{}\" in data object", key, value);
              data.put(key, value.asText());
              break;
          }
        }

        entity.setData(data);
      }
    } else {
      throw new JsonValidationException("Entity JSON didn't validate against entity schema");
    }

    return entity;
  }

  // A relative path is relative to the bundle file, or else to home.
  private File pathHelper(File givenFilePath) {
    File filePath = givenFilePath;
    if (!filePath.isAbsolute()) {
      final Path bundleFile = NaiveSorter.checksumToFiles.get(checksum);
      final Path directory = bundleFile == null ? Configuration.get(Setting.HOME) : bundleFile.getParent();
      filePath = directory.resolve(givenFilePath.getPath()).toFile();
    }
    if (!filePath.isFile()) {
      log.warn("Path helper failed to find file: {}", filePath.getPath());
    }
    return filePath;
  }
}
