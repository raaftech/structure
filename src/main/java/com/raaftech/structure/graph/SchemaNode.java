package com.raaftech.structure.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.repositories.SchemaNodeRepository;
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * SchemaNode is used to track information of applied SchemaUpdates to the database.
 * It has to be used in implementations of SchemaUpdate. SchemaUpdate will do
 * the actual work of creating nodes, SchemaNode will track metadata.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-12
 */
public class SchemaNode extends Node {

    @SuppressWarnings("unused")
    private static final int epoch = 1;
    private int revision;
    private boolean applied;

    @Transient
    private HashMap<String, Integer> dependencies;

    @RelatedTo(type="REQUIRES", direction = Direction.OUTGOING)
    private Set<SchemaNode> requiresSchemaNode;

    @RelatedTo(type="REVISES", direction = Direction.OUTGOING)
    private SchemaNode revisesSchemaNode;

    @RelatedTo(type="CREATED", direction = Direction.OUTGOING)
    private Set<TypeNode> createdTypeNodes;

    @RelatedTo(type="UPDATED", direction = Direction.OUTGOING)
    private Set<TypeNode> updatedTypeNodes;

    @Transient
    private static SchemaNodeRepository schemaNodeRepository = Context.INSTANCE.getSchemaNodeRepository();

    /****************/
    /* Constructors */
    /****************/

    public SchemaNode() {}

    /**
     * Construct a new SchemaNode.
     * When revision number is higher than 1, an ancestral relationship will automatically be created to the parent.
     * The name+revision combination has to be unique.
     * @param name The name of the SchemaNode
     * @param desc The description of the SchemaNode.
     * @param uuid The UUID of the SchemaNode, this has to be unique.
     * @param revision The revision number, this should be > 0.
     */
    public SchemaNode(String name, String desc, String uuid, int revision) {
        super(name, desc, uuid);
        this.revision = revision;
        if (revision > 1) {
            this.revisesSchemaNode = schemaNodeRepository.findByNameAndRevision(name, revision - 1);
        }
        if (dependencies == null) {
            dependencies = new HashMap<>();
        }
        if (requiresSchemaNode == null) {
            requiresSchemaNode = new HashSet<>();
        }
    }

    /******************/
    /* Public Methods */
    /******************/

    /**
     * Try to query an existing SchemaNode, construct it when the node doesn't exist yet.
     * Name and revision are used to query the database.
     * @param name The name of the SchemaNode.
     * @param description A description of the SchemaNode.
     * @param optionalUuid An optional UUID. This is used when we have to construct a new SchemaNode.
     * @param revision The revision number of the SchemaNode.
     * @return Returns itself.
     */
    public static SchemaNode getOrCreateSchemaNode(String name, String description, String optionalUuid, int revision) {
        SchemaNode schemaNode = schemaNodeRepository.findByNameAndRevision(name, revision);
        if (schemaNode == null) {
            schemaNode = new SchemaNode(name, description, optionalUuid, revision);
        }
        return schemaNode;
    }

    public SchemaNode created(TypeNode node) {
        if (createdTypeNodes == null) {
            createdTypeNodes = new HashSet<>();
        }
        createdTypeNodes.add(node);
        return this;
    }

    public HashMap<String, Integer> getDependencies() {
        return dependencies;
    }

    /**
     * Gets the SchemaNode's functional requirement.
     * Because a functional requirement might not have existed in the database when this SchemaNode was originally instantiated,
     * we run requires() again in case we have a requirement. This created the actual requirement relation.
     * @return Returns itself.
     */
    public Set<SchemaNode> getRequiresSchemaNode() {
        // Our requirement might not have existed during initialization of this SchemaNode,
        // but might exist now. For this reason we need to set required again.
        if (hasRequirement()) {
            dependencies.entrySet().stream()
                .forEach(dependency -> requires(dependency.getKey(), dependency.getValue()));
        }
        return requiresSchemaNode;
    }

    /**
     * Gets parent dependency of the SchemaNode.
     * @return The parent dependency.
     */
    public SchemaNode getRevisesSchemaNode() {
        // revisesSchemaNode might be null if this node was instantiated before its' parent.
        // That means we have to query the database if revisesSchemaNode is null.
        if (revisesSchemaNode == null) {
            revisesSchemaNode = schemaNodeRepository.findByNameAndRevision(getName(), getRevision() - 1);
        }
        return revisesSchemaNode;
    }

    /**
     * Gets the revision number of the SchemaNode.
     * @return The revision number.
     */
    public int getRevision() {
        return revision;
    }

    /**
     * Does this SchemaNode have a functional requirement?
     * @return return true if we have a functional requirement, false if not.
     */
    public boolean hasRequirement() {
        return dependencies != null && dependencies.size() > 0;
    }

    /**
     * Was the SchemaUpdate that created this SchemaNode successfully applied?
     * @return True if the Schema was applied, false if not.
     */
    public boolean isApplied() {
        return applied;
    }

    /**
     * Set a functional requirement for this SchemaNode.
     * The requirement is checked when a SchemaUpdate tied to this SchemaNode is being applied.
     * @param requiredPackage Name of the required SchemaNode.
     * @param requiredRevision Revision of the required SchemaNode.
     * @return Returns itself.
     */
    public SchemaNode requires(String requiredPackage, Integer requiredRevision) {
        if (requiredPackage != null && requiredRevision > 0) {
            this.dependencies.put(requiredPackage, requiredRevision);
            // In case the requirement hasn't ran yet, this will be set to null
            // By the time getRequiresSchemaNode is ran in SchemaUpdate, this might have changed.
            SchemaNode requiredNode = schemaNodeRepository.findByNameAndRevision(requiredPackage, requiredRevision);
            if (requiredNode != null) {
                requiresSchemaNode.add(requiredNode);
            }
        } else {
            throw new RuntimeException("Package can't be null and revision has to be higher than 0.");
        }
        return this;
    }

    /**
     * Sets the SchemaNode's state to reflect that the paired SchemaUpdate was succesfully applied.
     */
    public void setApplied() {
        applied = true;
    }

    public ObjectNode toJson() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = mapper.createObjectNode();
        SchemaNode revises = getRevisesSchemaNode();
        Set<SchemaNode> requires = getRequiresSchemaNode();

        json.put("name", getName());
        json.put("uuid", getUuid());
        json.put("desc", getDesc());
        json.put("revision", getRevision());
        json.put("applied", isApplied());

        if (requires != null && requires.size() > 0) {
            ArrayNode requiredSchemas = json.putArray("requires");
            getRequiresSchemaNode().stream()
                    .forEach(schemaNode -> requiredSchemas.add(schemaNode.getUuid()));
        }

        if (getRevisesSchemaNode() != null) {
            json.put("revises", revises.getUuid());
        }

        json.put("_nodeType", getNodeType());

        return json;
    }

    /**
     * Used to keep track of which nodes are updated by this SchemaNode/SchemaUpdate pair.
     * @param node Instance of a node updated by this SchemaNode/SchemaUpdate pair.
     * @return Returns itself.
     */
    public SchemaNode updated(TypeNode node) {
        if (updatedTypeNodes == null) {
            updatedTypeNodes = new HashSet<>();
        }
        updatedTypeNodes.add(node);
        return this;
    }
}
