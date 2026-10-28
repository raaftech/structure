package tech.raaf.structure.graph;

import static tech.raaf.structure.helper.JsonHelper.mapper;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.raaf.structure.base.Configuration;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.node.ObjectNode;

/**
 * Give a description of Node here.
 *
 * @author Boy van Duuren <boy@raaf.tech>
 * @since 2014-11-26
 */
@JsonSerialize(using = NodeSerializer.class)
public abstract class Node {

  private static final Logger log = LoggerFactory.getLogger(Node.class);

  protected String name;
  protected String desc;
  protected String bundle;
  protected String path;
  protected String type;
  protected String checksum;
  String uuid;

  Map<String, Object> data = new HashMap<>();
  Map<String, Object> file = new HashMap<>();

  // NodeMapper sets this for a node that it reads from the graph
  boolean persisted;

  // Constructors

  public Node() {
    this.uuid = UUID.randomUUID().toString();
  }

  public Node(String name, String desc) {
    this(name, desc, UUID.randomUUID().toString());
  }

  @SuppressWarnings("unused")
  public Node(String name, String desc, String uuid) {
    UUID check = UUID.fromString(uuid); // throws exception on invalid uuid
    if (name.length() == 0 || desc.length() == 0) {
      throw new RuntimeException("Cannot create node with an empty name or description.");
    }
    this.name = name;
    this.desc = desc;
    this.uuid = uuid;
  }

  @SuppressWarnings("unused")
  public Node(String name, String desc, String uuid, String checksum) {
    UUID check = UUID.fromString(uuid); // throws exception on invalid uuid
    if (name.length() == 0 || desc.length() == 0) {
      throw new RuntimeException("Cannot create node with an empty name or description.");
    }
    this.name = name;
    this.desc = desc;
    this.uuid = uuid;
    this.checksum = checksum;
  }

  // Public Methods

  public String getDesc() {
    return desc;
  }

  public boolean isPersisted() {
    return persisted;
  }

  public String getName() {
    return name;
  }

  public String getNodeType() {
    return getClass().getSimpleName();
  }

  public String getUuid() {
    return uuid;
  }

  public String getChecksum() {
    return checksum;
  }

  public String getType() {
    if (this instanceof Type) {
      return "Type";
    } else {
      return "Instance";
    }
  }

  public String getBundle() {
    return bundle;
  }

  public String getPath() {
    return path;
  }

  public Map<String, Object> getFiles() {
    return file;
  }

  public Map<String, Object> getData() {
    return data;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setDesc(String desc) {
    this.desc = desc;
  }

  public void setBundle(String bundle) {
    this.bundle = bundle;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setChecksum(String checksum) {
    this.checksum = checksum;
  }

  public ObjectNode toJson() {
    ObjectNode json = mapper.createObjectNode();
    json.put("name", name).put("desc", desc).put("uuid", uuid).put("checksum", checksum);

    return json;
  }

  public Optional<ObjectNode> getFile(String name) {
    Optional<ObjectNode> maybeFile = Optional.empty();
    if (file.containsKey(name)) {
      try {
        maybeFile = Optional.of((ObjectNode) mapper.readTree((String) file.get(name)));
      } catch (JacksonException e) {
        if (Configuration.isTraceModeOn()) {
          log.error("Error while trying to get file object on {}", getUuid());
          e.printStackTrace();
        }
      }
    }
    return maybeFile;
  }

  public Optional<String> getFileContent(String name) {
    final Optional<String> content;
    final Optional<ObjectNode> file = getFile(name);
    if (file.isPresent()) {
      content = Optional.of(file.get().get("content").asString());
    } else {
      content = Optional.empty();
    }
    return content;
  }

  // The nodes at the end of a pattern from this node, as (b)
  @SuppressWarnings("unchecked")
  protected <T extends Node> Set<T> related(String pattern) {
    final List<Map<String, Object>> rows = Graph.query(
      "MATCH (a:Node {uuid: $uuid})" + pattern + " RETURN DISTINCT b",
      ImmutableMap.of("uuid", uuid)
    );
    final Set<T> related = new LinkedHashSet<>();
    rows.forEach(row -> related.add((T) NodeMapper.map((GraphNode) row.get("b"))));
    return related;
  }

  // The first node at the end of a pattern from this node, or null
  protected <T extends Node> T relatedOne(String pattern) {
    final Set<T> related = related(pattern);
    return related.isEmpty() ? null : related.iterator().next();
  }

  // A uuid identifies a node
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Node)) return false;
    return uuid != null && uuid.equals(((Node) o).uuid);
  }

  @Override
  public int hashCode() {
    return uuid != null ? uuid.hashCode() : System.identityHashCode(this);
  }
}
