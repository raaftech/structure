package com.raaftech.structure.graph;

import org.neo4j.graphdb.Direction;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.repositories.SchemaNodeRepository;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.annotation.RelatedTo;

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

    private static final int epoch = 1;
    private int revision;
    private boolean applied;

    @Transient
    private String requiredPackage;
    @Transient
    private int requiredRevision;

    @RelatedTo(type="REQUIRES", direction = Direction.OUTGOING)
    private SchemaNode requiresSchemaNode;

    @RelatedTo(type="REVISES", direction = Direction.OUTGOING)
    private SchemaNode revisesSchemaNode;

    @RelatedTo(type="CREATED", direction = Direction.OUTGOING)
    private Set<TypeNode> createdTypeNodes;

    @RelatedTo(type="UPDATED", direction = Direction.OUTGOING)
    private Set<TypeNode> updatedTypeNodes;

    private static SchemaNodeRepository schemaNodeRepository = Context.INSTANCE.getSchemaNodeRepository();

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
    }

    /**
     * Set a functional requirement for this SchemaNode.
     * The requirement is checked when a SchemaUpdate tied to this SchemaNode is being applied.
     * @param requiredPackage Name of the required SchemaNode.
     * @param requiredRevision Revision of the required SchemaNode.
     * @return Returns itself.
     */
    public SchemaNode requires(String requiredPackage, int requiredRevision) {
        if (requiredPackage != null && requiredRevision > 0) {
            this.requiredPackage = requiredPackage;
            this.requiredRevision = requiredRevision;
            // In case the requirement hasn't ran yet, this will be set to null
            // By the time getRequiresSchemaNode is ran in SchemaUpdate, this might have changed.
            requiresSchemaNode = schemaNodeRepository.findByNameAndRevision(requiredPackage, requiredRevision);
        } else {
            throw new RuntimeException("Package can't be null and revision has to be higher than 0.");
        }
        return this;
    }

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

    /**
     * Does this SchemaNode have a functional requirement?
     * @return return true if we have a functional requirement, false if not.
     */
    public boolean hasRequirement() {
        return requiredPackage != null && requiredRevision > 0;
    }

    public SchemaNode created(TypeNode node) {
        if (createdTypeNodes == null) {
            createdTypeNodes = new HashSet<>();
        }
        createdTypeNodes.add(node);
        return this;
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

    /**
     * Gets the SchemaNode's functional requirement.
     * Because a functional requirement might not have existed in the database when this SchemaNode was originally instantiated,
     * we run requires() again in case we have a requirement. This created the actual requirement relation.
     * @return Returns itself.
     */
    public SchemaNode getRequiresSchemaNode() {
        // Our requirement might not have existed during initialization of this SchemaNode,
        // but might exist now. For this reason we need to set required again.
        if (hasRequirement()) {
            requires(requiredPackage, requiredRevision);
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
     * Was the SchemaUpdate that created this SchemaNode successfully applied?
     * @return True if the Schema was applied, false if not.
     */
    public boolean isApplied() {
        return applied;
    }

    /**
     * Sets the SchemaNode's state to reflect that the paired SchemaUpdate was succesfully applied.
     */
    public void setApplied() {
        applied = true;
    }

    public String getRequiredPackage() {
        return requiredPackage;
    }

    public int getRequiredRevision() {
        return requiredRevision;
    }
}
