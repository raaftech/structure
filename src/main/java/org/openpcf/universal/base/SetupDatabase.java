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

            // Create child nodes of our root object
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

            // Create component type tree
            // First, we'll create component's children
            isa.clear();
            isa.add(nodeRepository.findByUuid("a79ce509-a021-4177-ae61-73abd01fb479"));

            createNode(new Configuration("Configuration", "An abstract configuration component.",
                    "3f4a43f7-b558-4bf0-ba7e-66b99cffaf16", isa));
            createNode(new Device("Device", "An abstract device component.", "The revision of the device.",
                    "ea6a7487-867c-4a74-8849-fb8539b8169f", isa));
            createNode(new Software("Software", "An abstract software component.", "The version of the software.",
                    "A short lowercase name for the Software without spaces.",
                    "The full descriptive name of the Software.", isa));

            // Branch out Component->Configuration tree first
            isa.clear();
            isa.add(nodeRepository.findByUuid("3f4a43f7-b558-4bf0-ba7e-66b99cffaf16"));

            createNode(new Configuration("DnsName",
                    "Either a short name or a fully-qualified domain name.",
                    "99c0da37-3599-418d-8ecc-de36d37ae9f4", isa)
                    .addProperty("dnsname", "somename"));
            createNode(new Configuration("IpAddress",
                    "An IP address (address only, can be assigned to a network interface).",
                    "299001a6-1091-4c0d-87be-c512af03e179", isa)
                    .addProperty("addr", "IP addresses should at least have the addr field."));
            createNode(new Configuration("IpListener",
                    "An IP listener (address and port, can be assigned to a network service).",
                    "91c999a0-33ec-42ee-bae0-6f8adae42535", isa)
                    .addProperty("addr", "IP listeners should at least have the addr and port fields.")
                    .addProperty("port", "IP listeners should at least have the addr and port fields."));
            createNode(new Configuration("Map",
                    "A configuration object that maps keys to multi-valued attributes in the JSON format.",
                    "c970f6d3-4898-4596-bfac-9d6d1f6b2541", isa)
                    .addProperty("map", "JSON string containing the Map."));
            createNode(new Configuration("Url", "a uniform resource locator.",
                    "0e9f72bd-5bc5-43dc-ab86-30a8f7171bc6", isa)
                    .addProperty("url", "scheme://domain:port/path?query_string#fragment_id"));

            // Branch out Component->Configuration->IpAddress
            isa.clear();
            isa.add(nodeRepository.findByUuid("299001a6-1091-4c0d-87be-c512af03e179"));

            createNode(new Configuration("Ipv4Address",
                    "An IPv4 address (address only, can be assigned to a network interface).",
                    "f7fbe917-9d49-4df5-95f6-6e132067a424", isa)
                    .addProperty("addr", "Fill in an IPv4 address here."));
            createNode(new Configuration("Ipv6Address",
                    "An IPv6 address (address only, can be assigned to a network interface).",
                    "47b5804d-5f5d-4bf8-89f6-a189c29b05ae", isa)
                    .addProperty("addr", "Fill in an IPv6 address here."));

            // Branch out Component->Configuration->IpListener
            isa.clear();
            isa.add(nodeRepository.findByUuid("91c999a0-33ec-42ee-bae0-6f8adae42535"));

            createNode(new Configuration("Ipv4Listener",
                    "An IPv4 listener (address and port, can be assigned to a network service).",
                    "5ca95f7a-5175-4e7f-8a61-862e24485af3", isa)
                    .addProperty("addr", "Fill in an IPv4 address here.")
                    .addProperty("port", "Fill in a port number between 0 and 65535 here."));
            createNode(new Configuration("Ipv6Listener",
                    "An IPv6 listener (address and port, can be assigned to a network service).",
                    "6a79b4e9-13ce-4a09-bc5b-b429d23b34d7", isa)
                    .addProperty("addr", "Fill in an IPv6 address here.")
                    .addProperty("port", "Fill in a port number between 0 and 65535 here."));

            // Branch out Component->Configuration->Map
            isa.clear();
            isa.add(nodeRepository.findByUuid("c970f6d3-4898-4596-bfac-9d6d1f6b2541"));

            createNode(new Configuration("DirectoryMap",
                    "A generic map of possible directories this software has. " +
                            "Key: os-shortname, Values: list of directories, Format: JSON.",
                    "64d50e90-8be2-4d97-9dba-dd8518eb3f84", isa)
                    .addProperty("map", "JSON string containing the DirectoryMap."));
            createNode(new Configuration("FileMap",
                    "A generic map of possible/known files this software has. " +
                            "Key: os-shortname, Values: list of files, Format: JSON.",
                    "35407e44-ec50-4d66-83f9-dacf1f1b49dd", isa)
                    .addProperty("map", "JSON string containing the FileMap."));
            createNode(new Configuration("NameMap",
                    "A generic map of possible/known names this software has. " +
                            "Key: os-shortname, Values: list of names, Format: JSON.",
                    "14372d63-a428-46b9-8359-9982ef22cfce", isa)
                    .addProperty("map", "JSON string containing the NameMap."));
            createNode(new Configuration("StringMap",
                    "A generic map of possible/known strings this software has. " +
                            "Key: *, Values: string pattern, Format: JSON.",
                    "0d554a04-67d6-428f-8e6f-6cdcdaca39cb", isa)
                    .addProperty("map", "JSON string containing the StringMap."));

            // Branch out to Component->Configuration->Map->DirectoryMap
            isa.clear();
            isa.add(nodeRepository.findByUuid("64d50e90-8be2-4d97-9dba-dd8518eb3f84"));
            createNode(new Configuration("BinaryDirectoryMap",
                    "A map of possible/known binary executable directories for this software. " +
                            "Key: os-shortname, Values: list of binary directories, Format: JSON.",
                    "33e5e211-88db-4a78-95cd-2f2a3cd9e297", isa)
                    .addProperty("map", "JSON string containing the BinaryDirectoryMap."));
            createNode(new Configuration("ConfigDirectoryMap",
                    "A map of possible/known configuration directories for this software. " +
                            "Key: os-shortname, Values: list of config directories, Format: JSON.",
                    "fb83aefa-ec5e-4ad9-a807-4a37ffaa404b", isa)
                    .addProperty("map", "JSON string containing the ConfigDirectoryMap."));
            createNode(new Configuration("DataDirectoryMap",
                    "A map of possible/known data directories for this software. " +
                            "Key: os-shortname, Values: list of data directories, Format: JSON.",
                    "07aaa5dd-a747-4474-850d-01ad00a2f533", isa)
                    .addProperty("map", "JSON string containing the DataDirectoryMap."));
            createNode(new Configuration("LibraryDirectoryMap",
                    "A map of possible/known dynamic link library directories for this software. " +
                            "Key: os-shortname, Values: list of library directories, Format: JSON.",
                    "8ccf2b1c-3650-408f-8560-4ab9f3a57bbe", isa)
                    .addProperty("map", "JSON string containing the LibraryDirectoryMap."));
            createNode(new Configuration("LogDirectoryMap",
                    "A map of possible/known log directories for this software. " +
                            "Key: os-shortname, Values: list of log directories, Format: JSON.",
                    "f8a9ce96-2a50-4a80-8deb-ec6fb294638e", isa)
                    .addProperty("map", "JSON string containing the LogDirectoryMap."));
            createNode(new Configuration("PrefixDirectoryMap",
                    "A map of possible/known prefix directories for this software. " +
                            "Key: os-shortname, Values: list of directory prefixes, Format: JSON.",
                    "d47e9ecd-d735-49a0-8d94-c4cb948ea226", isa)
                    .addProperty("map", "JSON string containing the PrefixDirectoryMap."));

            // Branch out to Component->Configuration->Map->FileMap
            isa.clear();
            isa.add(nodeRepository.findByUuid("35407e44-ec50-4d66-83f9-dacf1f1b49dd"));

            createNode(new Configuration("BinaryFileMap",
                    "A map of possible/known binary executable files for this software, non-full path, " +
                            "checked within BinaryDirectoryMap entries.",
                    "5ffab714-fa0e-416c-a237-9b023fe27bd7", isa)
                    .addProperty("map", "JSON string containing the BinaryFileMap."));
            createNode(new Configuration("ConfigFileMap",
                    "A map of possible/known configuration files for this software, non-full path, " +
                            "checked within ConfigDirectoryMap entries.",
                    "a6125034-55e0-4604-bf21-407837d0b419", isa)
                    .addProperty("map", "JSON string containing the ConfigFileMap."));
            createNode(new Configuration("DataFileMap",
                    "A map of possible/known data files for this software, non-full path, " +
                            "checked within DataDirectoryMap entries.",
                    "b19412ae-dffe-4fe8-9f00-3207ab686fb4", isa)
                    .addProperty("map", "JSON string containing the DataFileMap."));
            createNode(new Configuration("LogFileMap",
                    "A map of possible/known log files for this software, non-full path, " +
                            "checked within LogDirectoryMap entries.",
                    "ac583c10-f77c-4d40-84eb-4ffed77c79e0", isa)
                    .addProperty("map", "JSON string containing the LogFileMap."));
            createNode(new Configuration("LibraryFileMap",
                    "A map of possible/known dynamic link library files for this software, non-full path, " +
                            "checked within LibraryDirectoryMap entries.",
                    "99a6e35b-912e-41f1-b404-d24299e51fb2", isa)
                    .addProperty("map", "JSON string containing the LibraryFileMap."));

            // Branch out to Component->Configuration->Map->NameMap
            isa.clear();
            isa.add(nodeRepository.findByUuid("14372d63-a428-46b9-8359-9982ef22cfce"));

            createNode(new Configuration("PackageNameMap",
                    "A generic map of possible/known package names this software has. " +
                            "Key: os-shortname, Values: list of names, Format: JSON.",
                    "1d691f61-3c9f-46ad-b98b-b968352d59b3", isa)
                    .addProperty("map", "JSON string containing the PackageNameMap."));
            createNode(new Configuration("ServiceNameMap",
                    "A generic map of possible/known service names this software has. " +
                            "Key: os-shortname, Values: list of names, Format: JSON.",
                    "ead61277-e797-406b-96e1-95c155655fac", isa)
                    .addProperty("map", "JSON string containing the ServiceNameMap."));

            // Branch out to Component->Configuration->Map->StringMap
            isa.clear();
            isa.add(nodeRepository.findByUuid("0d554a04-67d6-428f-8e6f-6cdcdaca39cb"));
            createNode(new Configuration("VersionStringMap",
                    "A map of possible/known version strings this software has. " +
                            "Key: *, Values: version string pattern, Format: JSON.",
                    "1c1af482-f0e0-45f1-9aa1-f7702040067d", isa)
                    .addProperty("map", "JSON string containing the VersionStringMap."));

            tx.success();
        } finally {
            tx.close();
        }
        return true;
    }
}
