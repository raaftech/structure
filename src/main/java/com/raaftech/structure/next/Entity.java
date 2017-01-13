package com.raaftech.structure.next;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.repositories.NodeRepository;
import com.raaftech.structure.graph.repositories.TypeRepository;
import com.raaftech.structure.helper.Triple;
import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.fieldaccess.DynamicProperties;
import org.springframework.data.neo4j.fieldaccess.DynamicPropertiesContainer;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.index.IndexType;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * A new root entity node with heavily simplified and structured construction.
 *
 * @author Rubin Simons <rubin@raaftech.com>
 * @since 2016-10-19
 */
@JsonDeserialize(using = EntityDeserializer.class)
public class Entity {

    /* Logging object. */
	@Transient
    private final static Logger log = LoggerFactory.getLogger(Entity.class);

	/* Entity repository. Used for querying the graph. */
	private NodeRepository nodeRepository = Context.getNodeRepository();
    private TypeRepository typeRepository = Context.getTypeRepository();
    private Neo4jTemplate neo4jTemplate;

    public void setNeo4jTemplate(Neo4jTemplate neo4jTemplate) {
        this.neo4jTemplate = neo4jTemplate;
    }

    private Node persistedState;

    /* Internal graph database node id. */
    @GraphId
    private Long id;

    /* Main key value/pairs: bundle, type, name, desc, uuid. */
    @Indexed(indexType = IndexType.FULLTEXT, indexName = "bundle")
    private String bundle;

    @Indexed(indexType = IndexType.FULLTEXT, indexName = "type")
    private String type;

    @Indexed(indexType = IndexType.FULLTEXT, indexName = "name")
    private String name;

    @Indexed(indexType = IndexType.FULLTEXT, indexName = "desc")
    private String desc;

    @Indexed(indexType = IndexType.FULLTEXT, indexName = "uuid")
    private String uuid;

    /* Generated key/value pairs: checksum, path. */
    private String checksum;
    private String path;

    /* Dynamic property containers for inline data and files. */
    private DynamicProperties data = new DynamicPropertiesContainer();
    private DynamicProperties file = new DynamicPropertiesContainer();

    /* A holder for relations to be written by persistRelations().
     * From left to right, the triple should contain the relationship name,
     * the direction (incoming, outgoing, undirected) and the set of UUIDs
     * of the nodes that should be related by this relationship. */
    @Transient
    private Set<Triple<String, String, Set<String>>> relations;


    /* Empty constructor for Spring Data Neo4j's purposes. */
    public Entity() {
        this.neo4jTemplate = Context.getNeo4jTemplate();
    }

    // Allow construction of Entity with a specific neo4j template, for testing purposes
    public Entity(Neo4jTemplate neo4jTemplate) {
        this.neo4jTemplate = neo4jTemplate;
    }


	/* Getters. */
    public String getBundle() {
        return bundle;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public String getUuid() {
        return uuid;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getPath() {
        return path;
    }

    public Map<String, Object> getData() {
        return data.asMap();
    }

    public Map<String, Object> getFiles() {
        return file.asMap();
    }

    public Map<String, Object> getFile() {
        return file.asMap();
    }

    /**
     * Get the triples describing this entity's relations.
     * @return A triple containing left to right: relation name, direction, related UUIDs.
     */
	public Set<Triple<String, String, Set<String>>> getRelations() {
		return relations;
	}


    /* Setters. */
    public void setBundle(String bundle) {
        this.bundle = bundle;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setData(Map<String, Object> map) {
        this.data.setPropertiesFrom(map);
    }

    public void setFile(Map<String, Object> map) {
        this.file.setPropertiesFrom(map);
    }

	public void setRelations(Set <Triple<String, String, Set<String>>> relations) {
		this.relations = relations;
	}


    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;

        if (id == null)
            return false;

        if (!(other instanceof Entity))
            return false;

        return id.equals(((Entity) other).id);
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : id.hashCode();
    }


    public JsonNode toJson() {
        JsonNode entityJson = null;
        return entityJson;
    }

    public void toGraph() {
        persist();
        Node node = getPersistentState();
        persistRelations(node);
        persistTypeLabels(node);
    }

    public Node getPersistentState() {
        return persistedState;
    }

    public void persist() {
        Map<String, Object> properties = new HashMap<>();
        properties.putAll(data.asMap().entrySet().stream()
                .map(entry -> new AbstractMap.SimpleEntry<>("data-".concat(entry.getKey()), entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        properties.putAll(file.asMap().entrySet().stream()
                .map(entry -> new AbstractMap.SimpleEntry<>("file-".concat(entry.getKey()), entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        if (bundle != null) {
            properties.put("bundle", bundle);
        }
        if (type != null) {
            properties.put("type", type);
        }
        if (name != null) {
            properties.put("name", name);
        }
        if (desc != null) {
            properties.put("desc", desc);
        }
        if (uuid != null) {
            properties.put("uuid", uuid);
        }
        if (checksum != null) {
            properties.put("checksum", checksum);
        }
        if (path != null) {
            properties.put("path", path);
        }

        /* we use neo4jTemplate.merge(...), which is basically getOrCreate(...). Now, if the entity
        already existed, but has changed properties, we need to update them using Node.setProperty(...).
        I did not find a way in the API to do this atomically, like "SET" would do in Cypher */
        persistedState = neo4jTemplate.merge("Node", "uuid", uuid, properties, getClassLabels(type));
        properties.entrySet().stream().forEach(entry -> persistedState.setProperty(entry.getKey(), entry.getValue()));
        persistedState.getLabels().forEach(label -> persistedState.removeLabel(label));
        getClassLabels(type).stream()
                .map(DynamicLabel::label)
                .forEach(persistedState::addLabel);
    }

    private static List<String> getClassLabels(String type) {
        // A map containing the labels needed per type
        Map<String, List<String>> labelsPerType = ImmutableMap.<String, List<String>>builder()
                .put("Controller", ImmutableList.of("_Controller", "Controller", "Instrument", "Instance", "Node"))
                .put("Marker", ImmutableList.of("_Marker", "Marker", "Instrument", "Instance", "Node"))
                .put("Parser", ImmutableList.of("_Parser", "Parser", "Instrument", "Instance", "Node"))
                .put("Component", ImmutableList.of("_Component", "Component", "Instance", "Node"))
                .put("Configuration", ImmutableList.of("_Configuration", "Configuration", "Instance", "Node"))
                .put("Profile", ImmutableList.of("_Profile", "Profile", "Instance", "Node"))
                .put("Actor", ImmutableList.of("_Actor", "Actor", "Instance", "Node"))
                .put("Result", ImmutableList.of("_Result", "Result", "Instance", "Node"))
                .put("Grouping", ImmutableList.of("_Grouping", "Grouping", "Instance", "Node"))
                .put("Type", ImmutableList.of("_Type", "Type", "Node"))
                .build();

        return labelsPerType.get(type);
    }

    private void persistTypeLabels(Node node) {
        /* Add labels as Neo4j labels. Node will be null if entity is not persisted. */
        if (node != null && type !=null && uuid != null) {
            if ("Type".equals(type)) {
                nodeRepository.getTypeHierarchyForTypeUuid(uuid).stream()
                        .map(s -> "type:".concat(s))
                        .forEach(label -> node.addLabel(DynamicLabel.label(label)));
            }
        } else {
            log.error("Failed to persist labels for entity \"{}\". Are you sure the entity is in a persisted state?", name);
        }
    }

    private void persistRelations(Node node) {

        /* Add relations. Node will be null if entity is not persisted. */
        if (relations != null) {

			/*
			 * Create a set of all peers of all relations for the purpose of
			 * existence checking.
			 */
			Set<String> allPeers = new HashSet<String>();
			relations.forEach(relation -> allPeers.addAll(relation.getRight()));

			if (doPeersExist(allPeers)) {

				relations.forEach((relation) -> {
					String rname = relation.getLeft();
					String direction = relation.getMiddle();
					Set<String> peers = relation.getRight();

					peers.forEach((uuid) -> {
						Node peer = nodeRepository.findGraphNodeByUuid(uuid);

                        // check if this relationship already exists by getting al relationships with the same name and direction
                        // and then checking if we have one to "peer".
                        boolean relExists = StreamSupport.stream(node.getRelationships(Direction.valueOf(direction.toUpperCase()), (RelationshipType) () -> rname).spliterator(), false)
                                .filter(r -> r.getEndNode().getId() == peer.getId())
                                .count() > 0;
                        if (!relExists) {
                            switch (direction) {
                                case "incoming":
                                    log.debug("Creating incoming \"{}\" relation from \"{}\" to \"{}\"", rname, (String) peer.getProperty("name"), name);
                                    peer.createRelationshipTo(node, () -> rname);
                                    break;
                                case "outgoing":
                                    log.debug("Creating outgoing \"{}\" relation from \"{}\" to \"{}\"", rname, name, (String) peer.getProperty("name"));
                                    node.createRelationshipTo(peer, () -> rname);
                                    break;
                                case "undirected":
                                    log.debug("Creating undirected \"{}\" relation between \"{}\" and \"{}\"", rname, name, (String) peer.getProperty("name"));
                                    peer.createRelationshipTo(node, () -> rname);
                                    node.createRelationshipTo(peer, () -> rname);
                            }
                        }
					});

				});
			} else {
				log.error("One of the peers that entity \"{}\" would be related to, does not exist in the graph", name);
			}

		} else {
			log.error("Failed to persist relations for entity \"{}\". Are you sure the entity is in a persisted state?",
					name);
		}
    }

    private boolean doPeersExist(Set<String> peers) {
    	boolean result = true;

    	for (String peer : peers) {
    		if (nodeRepository.findByUuid(peer) == null) {
    			result = false;
    			break;
    		}
    	}

    	return result;
    }
}
