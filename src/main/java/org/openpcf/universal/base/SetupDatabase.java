package org.openpcf.universal.base;

import org.neo4j.graphdb.Transaction;
import org.openpcf.universal.model.*;
import org.openpcf.universal.repositories.NodeRepository;
import org.springframework.data.neo4j.core.GraphDatabase;

import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;

/**
 * Give a description of SetupDatabase here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-11-25
 */
public class SetupDatabase {

    GraphDatabase graphDatabase;
    NodeRepository nodeRepository;

    public SetupDatabase() {
        this.graphDatabase = SpringContext.INSTANCE.getGraphDatabase();
        this.nodeRepository = SpringContext.INSTANCE.getNodeRepository();
    }

    /**
     * Create a new node.
     * The node's uuid has to be unique in the database. If this
     * is not the case, we'll return false.
     * @param node the node to be saved to the database.
     * @return Whether the node was saved to the database.
     */
    public boolean createNode(Node node) {
        if (node == null) {
            return false;
        }
        Node existingNode = nodeRepository.findByUuid(node.getUuid());
        if (existingNode == null) {
            nodeRepository.save(node);
            return true;
        } else {
            return false;
        }
    }

    public boolean createDatabase() {
        Transaction tx = graphDatabase.beginTx();
        try {
            Set<Node> isa = new HashSet<Node>();

            Node object = new Node("Object", "The root object.", "b1bc963a-19cc-4518-aecb-a35d028c49e3", null);
            createNode(object);
            isa.add(object);

            createNode(new Capability("Capability",
                    "A software component gives capabilities to its parent or to siblings.",
                    "916e04a9-c773-4eaf-9c08-450016426e5a", isa));
            createNode(new Actor("Actor",
                    "Something or someone that interacts/starts/acts within the system.",
                    "3175d762-dd97-434f-a288-21e4591480cf", isa));
            createNode(new Grouping("Grouping", "A group of references to things.",
                    "5f1eb7c5-ec5e-4563-be6a-fc50774fc374", isa));
            createNode(new Component("Component", "An abstract base component.",
                    "a79ce509-a021-4177-ae61-73abd01fb479", isa));
            createNode(new Model("Model", "A model object.",
                    "4e2dccfa-b5ca-4992-a76e-c390b7d90084", "Specifies a namespace for the model.",
                    "Specifies a version number for the model.", isa));
            createNode(new Result("Result", "A result from an operation.",
                    "f3498c02-58c6-486b-87c6-9d54607fdb12", "The result data.",
                    new GregorianCalendar(2014, 11, 26).getTime(), isa));
            createNode(new Operation("Operation", "A command or method that can be called, with or without schedule.",
                    "46294722-b14e-438e-9fbc-95ff137b6b18", 0, "The stored command, method or eventbus message.",
                    new GregorianCalendar(2014, 11, 26).getTime(), null, null, isa));
            createNode(new Handler("Handler",
                    "A representation of a software handler that gives end user function.",
                    "14dbfbfa-b50a-466f-8539-55d5d3f15e4a", isa));
            createNode(new Protocol("Protocol", "A protocol.", "db0c3c65-52fd-4dec-907c-6c8a669e88f3", isa));

            tx.success();
        } finally {
            tx.close();
        }
        return true;
    }
}
