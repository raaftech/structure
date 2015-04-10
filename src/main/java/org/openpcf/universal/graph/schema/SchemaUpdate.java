package org.openpcf.universal.graph.schema;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.openpcf.universal.base.Context;
import org.openpcf.universal.graph.SchemaNode;
import org.openpcf.universal.graph.Template;
import org.openpcf.universal.graph.TypeNode;
import org.openpcf.universal.graph.repositories.SchemaNodeRepository;
import org.openpcf.universal.graph.repositories.TypeNodeRepository;

/**
 * SchemaUpdate is an abstract class that has to be implemented for every update to the schema.
 * It has a SchemaNode attribute that keeps track of the version, dependencies, etc of the SchemaUpdate.
 * Dependency checks and other tasks is done in SchemaUpdate-implementations.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-12
 */
public abstract class SchemaUpdate {

    protected SchemaNode updateInformation;

    public SchemaUpdate() {
        setUpdateInformation();
    }

    /**
     * This method couples a SchemaNode to an implementation of SchemaUpdate.
     */
    protected abstract void setUpdateInformation();

    /**
     * This method should contain the actual code an implementation of SchemaUpdate needs to apply changes to the schema.
     * @return true if everything succeeded, false if not.
     */
    protected abstract void initNodes();

    protected SchemaNodeRepository schemaNodeRepository = Context.INSTANCE.getSchemaNodeRepository();
    protected TypeNodeRepository typeNodeRepository = Context.INSTANCE.getTypeNodeRepository();
    protected List<TypeNode> typeNodeList;

    /**
     * Create a new node.
     * The node's uuid has to be unique in the database. If this
     * is not the case, we'll return false.
     *
     * @param node the node to be saved to the database.
     * @return Whether the node was saved to the database.
     */
    private boolean createNode(TypeNode node) {
        if (node == null) {
            return false;
        }

        TypeNode isaNode = node.getIsa();

        // If we only know about our parent(isa) UUID, but not the actual
        // object, fetch the object from the database.
        if (isaNode == null && node.getIsaUuid() != null) {
            isaNode = typeNodeRepository.findByUuid(node.getIsaUuid());
            node.setIsa(isaNode);
        }

        TypeNode existingNode = typeNodeRepository.findByUuid(node.getUuid());
        if (existingNode == null) {
            System.out.print(".");
            updateInformation.created(node);
            typeNodeRepository.save(node);
            return true;
        } else {
            return false;
        }
    }

    private boolean createAllNodes() {
        if (typeNodeList != null) {
            boolean succeeded = true;
            Iterator<TypeNode> typeNodeIterator = typeNodeList.iterator();

            while (typeNodeIterator.hasNext() && succeeded) {
                succeeded &= createNode(typeNodeIterator.next());
            }

            return succeeded;
        } else {
            throw new RuntimeException("Could not apply SchemaUpdate: " +
                    updateInformation.getName() + "R" + updateInformation.getRevision() +
                    " (typeNodeList wasn't initialized)\n" +  "Transaction failed.");
        }
    }

    /**
     * Update an existing node, cascading changes down to child nodes for inherited data.
     * The existing node is queried using it's UUID.
     * @param node A TypeNode containing an existing UUID.
     * @return Returns true if we found and updated the TypeNode, false if not.
     */
    protected boolean updateNode(TypeNode node) {
        if (node == null) {
            return false;
        }
        TypeNode existingNode = typeNodeRepository.findByUuid(node.getUuid());
        if (existingNode == null) {
            return false;
        } else {
            System.out.print(".");
            // We'll only keep track of which node was directly updated by this SchemaUpdate, because
            // cascading is done automatically.
            updateInformation.updated(node);
            typeNodeRepository.save(node);
            cascadeDescription(existingNode, existingNode.getSchema(), node.getSchema());

            return true;
        }
    }

    /**
     * Recursive function that cascades TypeNode schema-updates to every ISA child of a given starting point in the graph.
     * @param start Starting point from where to cascade down.
     * @param oldSchema The old schema has to be passed so we know which keys to remove from the child nodes.
     * @param newSchema The new schema is put into every child node's schema HashMap.
     */
    private void cascadeDescription(TypeNode start, HashMap<String, Descriptor> oldSchema, HashMap<String, Descriptor> newSchema) {
        Set<Template> childIsaSet = start.getChildIsaSet();
        childIsaSet.stream()
                .filter(childNode -> childNode.getClass() == TypeNode.class)
                .map(TypeNode.class::cast)
                .forEach(childNode -> {
                    oldSchema.forEach((key, value) -> childNode.getSchema().remove(key));
                    childNode.getSchema().putAll(newSchema);
                    typeNodeRepository.save(childNode);
                    cascadeDescription(childNode, oldSchema, newSchema);
                });
    }

    /**
     * This method is called when a SchemaUpdate/SchemaNode pair's dependencies have been resolved.
     * It calls createNodes() which is implemented in SchemaUpdate implementations. When everything is successful, we call
     * setApplied() on the SchemaNode and persist it to the database.
     * @return Returns true if createNodes() was successful, false if not.
     */
    private boolean applyAndSet() {
        System.out.print(updateInformation.getName() + " R" + updateInformation.getRevision() + ": creating nodes...");
        initNodes();
        if (createAllNodes()) {
            System.out.println("done");
            updateInformation.setApplied();
            schemaNodeRepository.save(updateInformation);
            return true;
        } else {
            return false;
        }
    }

    /**
     * This method is the only public method, it checks if functional and ancestral dependencies are resolved and takes action
     * accordingly.
     */
    public boolean apply() {
        if (updateInformation == null) {
            return false;
        }

        SchemaNode requires = updateInformation.getRequiresSchemaNode();
        SchemaNode ancestor = updateInformation.getRevisesSchemaNode();

        // if Schema is already applied, return true
        if (updateInformation.isApplied()) {
            return true;
        }
        // no required and ancestoral dependency: createNodes()
        if (!updateInformation.hasRequirement()) {
            if (updateInformation.getRevision() == 1) {
                return applyAndSet();
            // no required and ancestoral dependency applied: createNodes()
            } else if (ancestor != null && ancestor.isApplied()) {
                return applyAndSet();
            }
        // no ancestoral dependency and required applied: createNode()
        } else if (updateInformation.getRevision() == 1) {
            if (requires != null && requires.isApplied()) {
                return applyAndSet();
            }
        // required and ancestoral dependency applied: createNode()
        } else if ((requires != null && requires.isApplied()) && (ancestor != null && ancestor.isApplied())) {
            return applyAndSet();
        }

        throw new RuntimeException("Schema " + updateInformation.getName() + " v" + updateInformation.getRevision() + " is " +
                "leaning on a functional or ancestral dependency that doesn't exist.");
    }

    public SchemaNode getUpdateInformation() {
        return updateInformation;
    }
}
