package org.openpcf.universal.base;

import org.neo4j.graphdb.Transaction;
import org.openpcf.universal.graph.Node;
import org.openpcf.universal.graph.TypeNode;
import org.openpcf.universal.graph.repositories.NodeRepository;
import org.openpcf.universal.graph.repositories.TypeNodeRepository;
import org.springframework.data.neo4j.core.GraphDatabase;

/**
 * Give a description of SetupDatabase here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-11-25
 */
public class SetupDatabase {

    GraphDatabase graphDatabase;
    NodeRepository nodeRepository;
    TypeNodeRepository typeNodeRepository;

    public SetupDatabase() {
        this.graphDatabase = SpringContext.INSTANCE.getGraphDatabase();
        this.nodeRepository = SpringContext.INSTANCE.getNodeRepository();
        this.typeNodeRepository = SpringContext.INSTANCE.getTypeNodeRepository();
    }

    /**
     * Create a new node.
     * The node's uuid has to be unique in the database. If this
     * is not the case, we'll return false.
     *
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
            long startTime = System.nanoTime();
            // org.openpcf.model.base
            createNode(new TypeNode("Object", "The root object.", "b1bc963a-19cc-4518-aecb-a35d028c49e3"));
            createNode(new TypeNode("Model", "A model object.", "4e2dccfa-b5ca-4992-a76e-c390b7d90084")
                    .setKvs("ns", "string", true)
                    .setKvs("ver", "integer", false)
                    .setIsa(typeNodeRepository.findByUuid("b1bc963a-19cc-4518-aecb-a35d028c49e3")));
            createNode(new TypeNode("BaseModel", "The first model object, all base types.", "48c8698b-3afd-44e5-baf7-73f55494cb31")
                    .setIsa(typeNodeRepository.findByUuid("4e2dccfa-b5ca-4992-a76e-c390b7d90084")));
            createNode(new TypeNode("Component", "An abstract base component.", "a79ce509-a021-4177-ae61-73abd01fb479")
                    .setIsa(typeNodeRepository.findByUuid("b1bc963a-19cc-4518-aecb-a35d028c49e3")));
            createNode(new TypeNode("Configuration", "An abstract configuration component.", "3f4a43f7-b558-4bf0-ba7e-66b99cffaf16")
                    .setIsa(typeNodeRepository.findByUuid("a79ce509-a021-4177-ae61-73abd01fb479")));
            createNode(new TypeNode("IpAddress", "An IP address (address only, can be assigned to a network interface).",
                    "299001a6-1091-4c0d-87be-c512af03e179")
                    .setIsa(typeNodeRepository.findByUuid("3f4a43f7-b558-4bf0-ba7e-66b99cffaf16")));
            createNode(new TypeNode("Ipv4Address", "An IPv4 address (address only, can be assigned to a network interface).",
                    "f7fbe917-9d49-4df5-95f6-6e132067a424")
                    .setIsa(typeNodeRepository.findByUuid("299001a6-1091-4c0d-87be-c512af03e179")));
            createNode(new TypeNode("Ipv6Address", "An IPv6 address (address only, can be assigned to a network interface).",
                    "47b5804d-5f5d-4bf8-89f6-a189c29b05ae")
                    .setIsa(typeNodeRepository.findByUuid("299001a6-1091-4c0d-87be-c512af03e179")));
            createNode(new TypeNode("IpListener", "An IP listener (address and port, can be assigned to a network service).",
                    "91c999a0-33ec-42ee-bae0-6f8adae42535")
                    .setIsa(typeNodeRepository.findByUuid("3f4a43f7-b558-4bf0-ba7e-66b99cffaf16")));
            createNode(new TypeNode("Ipv4Listener", "An IPv4 listener (address and port, can be assigned to a network service).",
                    "5ca95f7a-5175-4e7f-8a61-862e24485af3")
                    .setIsa(typeNodeRepository.findByUuid("91c999a0-33ec-42ee-bae0-6f8adae42535")));
            createNode(new TypeNode("Ipv6Listener", "An IPv6 listener (address and port, can be assigned to a network service).",
                    "6a79b4e9-13ce-4a09-bc5b-b429d23b34d7")
                    .setIsa(typeNodeRepository.findByUuid("91c999a0-33ec-42ee-bae0-6f8adae42535")));
            createNode(new TypeNode("Url", "a uniform resource locator.", "0e9f72bd-5bc5-43dc-ab86-30a8f7171bc6")
                    .setIsa(typeNodeRepository.findByUuid("3f4a43f7-b558-4bf0-ba7e-66b99cffaf16")));
            createNode(new TypeNode("DnsName", "Either a short name or a fully-qualified domain name.",
                    "99c0da37-3599-418d-8ecc-de36d37ae9f4")
                    .setIsa(typeNodeRepository.findByUuid("3f4a43f7-b558-4bf0-ba7e-66b99cffaf16")));
            createNode(new TypeNode("Map", "A configuration object that maps keys to multi-valued attributes in the JSON format" +
                    ".", "c970f6d3-4898-4596-bfac-9d6d1f6b2541")
                    .setIsa(typeNodeRepository.findByUuid("3f4a43f7-b558-4bf0-ba7e-66b99cffaf16")));
            createNode(new TypeNode("DirectoryMap", "A generic map of possible directories this software has. Key: os-shortname, " +
                    "Values: list of directories, Format: JSON.", "64d50e90-8be2-4d97-9dba-dd8518eb3f84")
                    .setIsa(typeNodeRepository.findByUuid("c970f6d3-4898-4596-bfac-9d6d1f6b2541")));
            createNode(new TypeNode("PrefixDirectoryMap", "A map of possible/known prefix directories for this software. Key: " +
                    "os-shortname, Values: list of directory prefixes, Format: JSON.", "d47e9ecd-d735-49a0-8d94-c4cb948ea226")
                    .setIsa(typeNodeRepository.findByUuid("64d50e90-8be2-4d97-9dba-dd8518eb3f84")));
            createNode(new TypeNode("ConfigDirectoryMap", "A map of possible/known configuration directories for this software. " +
                    "Key: os-shortname, Values: list of config directories, Format: JSON.", "fb83aefa-ec5e-4ad9-a807-4a37ffaa404b")
                    .setIsa(typeNodeRepository.findByUuid("64d50e90-8be2-4d97-9dba-dd8518eb3f84")));
            createNode(new TypeNode("BinaryDirectoryMap", "A map of possible/known binary executable directories for this " +
                    "software. Key: os-shortname, Values: list of binary directories, Format: JSON.",
                    "33e5e211-88db-4a78-95cd-2f2a3cd9e297")
                    .setIsa(typeNodeRepository.findByUuid("64d50e90-8be2-4d97-9dba-dd8518eb3f84")));
            createNode(new TypeNode("LibraryDirectoryMap", "A map of possible/known dynamic link library directories for this " +
                    "software. Key: os-shortname, Values: list of library directories, Format: JSON.",
                    "8ccf2b1c-3650-408f-8560-4ab9f3a57bbe")
                    .setIsa(typeNodeRepository.findByUuid("64d50e90-8be2-4d97-9dba-dd8518eb3f84")));
            createNode(new TypeNode("DataDirectoryMap", "A map of possible/known data directories for this software. Key: " +
                    "os-shortname, Values: list of data directories, Format: JSON.", "07aaa5dd-a747-4474-850d-01ad00a2f533")
                    .setIsa(typeNodeRepository.findByUuid("64d50e90-8be2-4d97-9dba-dd8518eb3f84")));
            createNode(new TypeNode("LogDirectoryMap", "A map of possible/known log directories for this software. Key: " +
                    "os-shortname, Values: list of log directories, Format: JSON.", "f8a9ce96-2a50-4a80-8deb-ec6fb294638e")
                    .setIsa(typeNodeRepository.findByUuid("64d50e90-8be2-4d97-9dba-dd8518eb3f84")));
            createNode(new TypeNode("FileMap", "A generic map of possible/known files this software has. Key: os-shortname, " +
                    "Values: list of files, Format: JSON.", "35407e44-ec50-4d66-83f9-dacf1f1b49dd")
                    .setIsa(typeNodeRepository.findByUuid("c970f6d3-4898-4596-bfac-9d6d1f6b2541")));
            createNode(new TypeNode("ConfigFileMap", "A map of possible/known configuration files for this software, non-full " +
                    "path, checked within ConfigDirectoryMap entries.", "a6125034-55e0-4604-bf21-407837d0b419")
                    .setIsa(typeNodeRepository.findByUuid("35407e44-ec50-4d66-83f9-dacf1f1b49dd")));
            createNode(new TypeNode("BinaryFileMap", "A map of possible/known binary executable files for this software, non-full" +
                    " path, checked within BinaryDirectoryMap entries.", "5ffab714-fa0e-416c-a237-9b023fe27bd7")
                    .setIsa(typeNodeRepository.findByUuid("35407e44-ec50-4d66-83f9-dacf1f1b49dd")));
            createNode(new TypeNode("LibraryFileMap", "A map of possible/known dynamic link library files for this software, " +
                    "non-full path, checked within LibraryDirectoryMap entries.", "99a6e35b-912e-41f1-b404-d24299e51fb2")
                    .setIsa(typeNodeRepository.findByUuid("35407e44-ec50-4d66-83f9-dacf1f1b49dd")));
            createNode(new TypeNode("DataFileMap", "A map of possible/known data files for this software, non-full path, checked " +
                    "within DataDirectoryMap entries.", "b19412ae-dffe-4fe8-9f00-3207ab686fb4")
                    .setIsa(typeNodeRepository.findByUuid("35407e44-ec50-4d66-83f9-dacf1f1b49dd")));
            createNode(new TypeNode("LogFileMap", "A map of possible/known log files for this software, non-full path, checked " +
                    "within LogDirectoryMap entries.", "ac583c10-f77c-4d40-84eb-4ffed77c79e0")
                    .setIsa(typeNodeRepository.findByUuid("35407e44-ec50-4d66-83f9-dacf1f1b49dd")));
            createNode(new TypeNode("NameMap", "A generic map of possible/known names this software has. Key: os-shortname, " +
                    "Values: list of names, Format: JSON.", "14372d63-a428-46b9-8359-9982ef22cfce")
                    .setIsa(typeNodeRepository.findByUuid("c970f6d3-4898-4596-bfac-9d6d1f6b2541")));
            createNode(new TypeNode("PackageNameMap", "A generic map of possible/known package names this software has. Key: " +
                    "os-shortname, Values: list of names, Format: JSON.", "1d691f61-3c9f-46ad-b98b-b968352d59b3")
                    .setIsa(typeNodeRepository.findByUuid("14372d63-a428-46b9-8359-9982ef22cfce")));
            createNode(new TypeNode("ServiceNameMap", "A generic map of possible/known service names this software has. Key: " +
                    "os-shortname, Values: list of names, Format: JSON.", "ead61277-e797-406b-96e1-95c155655fac")
                    .setIsa(typeNodeRepository.findByUuid("14372d63-a428-46b9-8359-9982ef22cfce")));
            createNode(new TypeNode("StringMap", "A generic map of possible/known strings this software has. Key: *, Values: " +
                    "string pattern, Format: JSON.", "0d554a04-67d6-428f-8e6f-6cdcdaca39cb")
                    .setIsa(typeNodeRepository.findByUuid("c970f6d3-4898-4596-bfac-9d6d1f6b2541")));
            createNode(new TypeNode("VersionStringMap", "A map of possible/known version strings this software has. Key: *, " +
                    "Values: version string pattern, Format: JSON.", "1c1af482-f0e0-45f1-9aa1-f7702040067d")
                    .setIsa(typeNodeRepository.findByUuid("0d554a04-67d6-428f-8e6f-6cdcdaca39cb")));
            createNode(new TypeNode("Device", "An abstract device component.", "ea6a7487-867c-4a74-8849-fb8539b8169f")
                    .setIsa(typeNodeRepository.findByUuid("a79ce509-a021-4177-ae61-73abd01fb479")));
            createNode(new TypeNode("AssembledDevice", "An assembled device, containing various components.",
                    "dbb2884f-7c31-4f7d-a92e-d6a76dd1b7fe")
                    .setIsa(typeNodeRepository.findByUuid("ea6a7487-867c-4a74-8849-fb8539b8169f")));
            createNode(new TypeNode("Computer", "An assembled computer device.", "ec11db91-6973-4285-b018-b461fbb432d7")
                    .setIsa(typeNodeRepository.findByUuid("dbb2884f-7c31-4f7d-a92e-d6a76dd1b7fe")));
            createNode(new TypeNode("ConsoleDevice", "A console device.", "3d0865a2-3ecf-4035-886b-0bb603c0e2c2")
                    .setIsa(typeNodeRepository.findByUuid("ea6a7487-867c-4a74-8849-fb8539b8169f")));
            createNode(new TypeNode("GenericSerial", "A serial communication device.", "f5b25736-ec12-4a7e-b198-548b97ebc36f")
                    .setIsa(typeNodeRepository.findByUuid("3d0865a2-3ecf-4035-886b-0bb603c0e2c2")));
            createNode(new TypeNode("GraphicsDevice", "A graphics device.", "a642b995-c073-4c27-a611-c5bf3854958f")
                    .setIsa(typeNodeRepository.findByUuid("ea6a7487-867c-4a74-8849-fb8539b8169f")));
            createNode(new TypeNode("GenericVga", "A VGA graphics device.", "b3a2ac7c-ad8a-4047-a460-2ac41a7ddfef")
                    .setIsa(typeNodeRepository.findByUuid("a642b995-c073-4c27-a611-c5bf3854958f")));
            createNode(new TypeNode("MemoryDevice", "A memory device", "1c87c672-1f69-4a02-9bc8-9eff28dda5e5")
                    .setIsa(typeNodeRepository.findByUuid("ea6a7487-867c-4a74-8849-fb8539b8169f")));
            createNode(new TypeNode("VolatileMemory", "A generic volatile memory device.", "44131d46-c1b0-4030-ae5a-9a3108627749")
                    .setIsa(typeNodeRepository.findByUuid("1c87c672-1f69-4a02-9bc8-9eff28dda5e5")));
            createNode(new TypeNode("RandomAccessMemory", "Random access memory.", "4c0a0638-1fa7-4f5b-9d69-1d199dcedccc")
                    .setIsa(typeNodeRepository.findByUuid("44131d46-c1b0-4030-ae5a-9a3108627749")));
            createNode(new TypeNode("NonVolatileMemory", "A generic non-volatile memory device.",
                    "7f129e53-ff07-4ed8-9062-daf5f733a3a6")
                    .setIsa(typeNodeRepository.findByUuid("1c87c672-1f69-4a02-9bc8-9eff28dda5e5")));
            createNode(new TypeNode("HardDiskDrive", "A non-volatile harddisk-based storage device.",
                    "24cc3545-0d1a-4d85-8d08-a83c3dbe5912")
                    .setIsa(typeNodeRepository.findByUuid("7f129e53-ff07-4ed8-9062-daf5f733a3a6")));
            createNode(new TypeNode("SolidStateDrive", "A non-volatile solid-state storage device.",
                    "7a60ec32-0a9a-40b9-bf1f-d69674d4a233")
                    .setIsa(typeNodeRepository.findByUuid("7f129e53-ff07-4ed8-9062-daf5f733a3a6")));
            createNode(new TypeNode("FloppyDrive", "A floppy disk drive.", "118ec961-a823-465b-be17-b706f1c8c2f1")
                    .setIsa(typeNodeRepository.findByUuid("7f129e53-ff07-4ed8-9062-daf5f733a3a6")));
            createNode(new TypeNode("TapeDrive", "A tape drive.", "a507461f-647b-4b79-936a-721ca2dba145")
                    .setIsa(typeNodeRepository.findByUuid("7f129e53-ff07-4ed8-9062-daf5f733a3a6")));
            createNode(new TypeNode("CdDrive", "A Compact Disc drive.", "30af2fe3-aa05-4912-a3d5-dddfdacf74e2")
                    .setIsa(typeNodeRepository.findByUuid("7f129e53-ff07-4ed8-9062-daf5f733a3a6")));
            createNode(new TypeNode("DvdDrive", "A Digital Video Disc drive.", "23ab95d2-2815-4121-a81c-63aca40cdcfa")
                    .setIsa(typeNodeRepository.findByUuid("7f129e53-ff07-4ed8-9062-daf5f733a3a6")));
            createNode(new TypeNode("BdDrive", "A Digital Video Disc drive.", "cbc27482-11d1-4890-a3e1-e9f80baea82c")
                    .setIsa(typeNodeRepository.findByUuid("7f129e53-ff07-4ed8-9062-daf5f733a3a6")));
            createNode(new TypeNode("NetworkDevice", "A network device.", "86786ed6-d62a-4011-9ae8-0082000e48fa")
                    .setIsa(typeNodeRepository.findByUuid("ea6a7487-867c-4a74-8849-fb8539b8169f")));
            createNode(new TypeNode("WiredNetworkDevice", "A wired network device.", "0d4c880d-d888-435f-b3a3-3d3eb9e70ddf")
                    .setIsa(typeNodeRepository.findByUuid("86786ed6-d62a-4011-9ae8-0082000e48fa")));
            createNode(new TypeNode("ModemDevice", "A modem networking device.", "f34b76e4-f5a6-4bae-968e-f22ef09b1e35")
                    .setIsa(typeNodeRepository.findByUuid("0d4c880d-d888-435f-b3a3-3d3eb9e70ddf")));
            createNode(new TypeNode("EthernetDevice", "A wired ethernet device.", "0dbd26f2-baff-446c-9241-46eaa3e03736")
                    .setIsa(typeNodeRepository.findByUuid("0d4c880d-d888-435f-b3a3-3d3eb9e70ddf")));
            createNode(new TypeNode("WirelessNetworkDevice", "A wireless network device.", "61328f83-db43-4efe-81be-df47e17814a5")
                    .setIsa(typeNodeRepository.findByUuid("86786ed6-d62a-4011-9ae8-0082000e48fa")));
            createNode(new TypeNode("BluetoothDevice", "A Bluetooth device.", "009cf823-39c0-4b17-ae81-ad98ed0d4585")
                    .setIsa(typeNodeRepository.findByUuid("61328f83-db43-4efe-81be-df47e17814a5")));
            createNode(new TypeNode("IrdaDevice", "An IrDA device.", "42c7997c-0c56-4f93-956f-5fd0778c47c6")
                    .setIsa(typeNodeRepository.findByUuid("61328f83-db43-4efe-81be-df47e17814a5")));
            createNode(new TypeNode("WifiDevice", "A WiFi device.", "78ff02be-2abe-4a8f-a082-a0d9c3cdae70")
                    .setIsa(typeNodeRepository.findByUuid("61328f83-db43-4efe-81be-df47e17814a5")));
            createNode(new TypeNode("WwanDevice", "A wireless WAN device.", "8d6e0956-033e-4efc-8846-e85bfddc8f98")
                    .setIsa(typeNodeRepository.findByUuid("61328f83-db43-4efe-81be-df47e17814a5")));
            createNode(new TypeNode("ProcessingUnit", "A processing unit.", "b7b2a2ff-3af7-469b-afbf-e52f1b80be91")
                    .setIsa(typeNodeRepository.findByUuid("ea6a7487-867c-4a74-8849-fb8539b8169f")));
            createNode(new TypeNode("AlphaProcessor", "An Alpha processor.", "95b9f7a2-1a17-4cb9-8088-3e2e478ce638")
                    .setIsa(typeNodeRepository.findByUuid("b7b2a2ff-3af7-469b-afbf-e52f1b80be91")));
            createNode(new TypeNode("ArmProcessor", "An ARM processor.", "f3b1b497-1e83-4a09-a280-04f46a83fe24")
                    .setIsa(typeNodeRepository.findByUuid("b7b2a2ff-3af7-469b-afbf-e52f1b80be91")));
            createNode(new TypeNode("ItaniumProcessor", "An Itanium processor.", "241015c9-72f5-491f-a870-a30d8f7902fd")
                    .setIsa(typeNodeRepository.findByUuid("b7b2a2ff-3af7-469b-afbf-e52f1b80be91")));
            createNode(new TypeNode("M68kProcessor", "A Motorola 68000 series processor.", "33920524-4989-4322-a0ca-1cea614d7fef")
                    .setIsa(typeNodeRepository.findByUuid("b7b2a2ff-3af7-469b-afbf-e52f1b80be91")));
            createNode(new TypeNode("MipsProcessor", "A Motorola 68000 series processor.", "cbe81157-97f6-401b-bbaa-cfbc4f479298")
                    .setIsa(typeNodeRepository.findByUuid("b7b2a2ff-3af7-469b-afbf-e52f1b80be91")));
            createNode(new TypeNode("MosProcessor", "A MOS Technology processor.", "b4f67698-7eca-4ca1-a4cf-b7ef9737785c")
                    .setIsa(typeNodeRepository.findByUuid("b7b2a2ff-3af7-469b-afbf-e52f1b80be91")));
            createNode(new TypeNode("PariscProcessor", "A PA Risc processor.", "689a4f33-e08d-4e7f-83a5-79fd310bd3f9")
                    .setIsa(typeNodeRepository.findByUuid("b7b2a2ff-3af7-469b-afbf-e52f1b80be91")));
            createNode(new TypeNode("PowerProcessor", "A PowerPC processor.", "808f2737-feca-4b70-aa1a-53410b970b10")
                    .setIsa(typeNodeRepository.findByUuid("b7b2a2ff-3af7-469b-afbf-e52f1b80be91")));
            createNode(new TypeNode("SparcProcessor", "A SPARC processor.", "798ba8d1-8da7-471a-9efa-30530569b6a1")
                    .setIsa(typeNodeRepository.findByUuid("b7b2a2ff-3af7-469b-afbf-e52f1b80be91")));
            createNode(new TypeNode("X86Processor", "An x86-based processor.", "4d4971b6-43b3-4c14-a82a-c51d72ba23a5")
                    .setIsa(typeNodeRepository.findByUuid("b7b2a2ff-3af7-469b-afbf-e52f1b80be91")));
            createNode(new TypeNode("ZilogProcessor", "A ZiLOG processor.", "a68f42c5-2a1d-43cb-bebd-55339f5e61ee")
                    .setIsa(typeNodeRepository.findByUuid("b7b2a2ff-3af7-469b-afbf-e52f1b80be91")));
            createNode(new TypeNode("Software", "An abstract software component.", "8db2b492-9a4a-41fe-97a5-bc625da042ea")
                    .setIsa(typeNodeRepository.findByUuid("a79ce509-a021-4177-ae61-73abd01fb479")));
            createNode(new TypeNode("CloudSystem", "A cloud (operating) system and/or environment.",
                    "86c0439a-4219-46e6-82a3-6fb2ee774aa4")
                    .setIsa(typeNodeRepository.findByUuid("8db2b492-9a4a-41fe-97a5-bc625da042ea")));
            createNode(new TypeNode("OperatingSystem", "An operating system.", "f05e325e-1aee-4892-96e4-8bcd6e97c528")
                    .setIsa(typeNodeRepository.findByUuid("8db2b492-9a4a-41fe-97a5-bc625da042ea")));
            createNode(new TypeNode("ServiceManager", "An operating system initialization system.",
                    "c3008eec-b454-4ef6-bf7e-0bd92c13d3c3")
                    .setIsa(typeNodeRepository.findByUuid("8db2b492-9a4a-41fe-97a5-bc625da042ea")));
            createNode(new TypeNode("PackageManager", "A package manager.", "e2b8b9ee-b66f-4252-8fba-4fb172f741bc")
                    .setIsa(typeNodeRepository.findByUuid("8db2b492-9a4a-41fe-97a5-bc625da042ea")));
            createNode(new TypeNode("Interpreter", "A command or programming language interpreter.",
                    "d047f415-734c-4bd3-92b9-d68d5521ce69")
                    .setIsa(typeNodeRepository.findByUuid("8db2b492-9a4a-41fe-97a5-bc625da042ea")));
            createNode(new TypeNode("Hypervisor", "A virtualization hypervisor.", "c5c7fcb5-8ac3-40cc-b195-b2b6af368e81")
                    .setIsa(typeNodeRepository.findByUuid("8db2b492-9a4a-41fe-97a5-bc625da042ea")));
            createNode(new TypeNode("Emulator", "A hardware platform emulator.", "59aeae81-9741-4986-b9c2-931504cfc9e3")
                    .setIsa(typeNodeRepository.findByUuid("8db2b492-9a4a-41fe-97a5-bc625da042ea")));
            createNode(new TypeNode("Service", "A generic local/networked/daemonized service.",
                    "3fb4603b-281e-47c0-b338-768ea05d4660")
                    .setIsa(typeNodeRepository.findByUuid("8db2b492-9a4a-41fe-97a5-bc625da042ea")));
            createNode(new TypeNode("Grouping", "A group of references to things.", "5f1eb7c5-ec5e-4563-be6a-fc50774fc374")
                    .setIsa(typeNodeRepository.findByUuid("b1bc963a-19cc-4518-aecb-a35d028c49e3")));
            createNode(new TypeNode("Locational", "A place/location.", "f31b8dc9-7b9a-47cc-bd3d-20b94b6daac1")
                    .setIsa(typeNodeRepository.findByUuid("5f1eb7c5-ec5e-4563-be6a-fc50774fc374")));
            createNode(new TypeNode("Building", "A building.", "44197148-678f-4421-a618-dc6807beaf30")
                    .setIsa(typeNodeRepository.findByUuid("f31b8dc9-7b9a-47cc-bd3d-20b94b6daac1")));
            createNode(new TypeNode("Floor", "A floor.", "9ca8061a-898c-4f2f-aeb3-c77d1747559f")
                    .setIsa(typeNodeRepository.findByUuid("f31b8dc9-7b9a-47cc-bd3d-20b94b6daac1")));
            createNode(new TypeNode("Room", "A room.", "94721751-ab2f-4285-9c33-cd91cb919430")
                    .setIsa(typeNodeRepository.findByUuid("f31b8dc9-7b9a-47cc-bd3d-20b94b6daac1")));
            createNode(new TypeNode("Organizational", "An organizational group.", "c94c2c32-8e10-48c3-a31b-6337c2ad4df4")
                    .setIsa(typeNodeRepository.findByUuid("5f1eb7c5-ec5e-4563-be6a-fc50774fc374")));
            createNode(new TypeNode("Company", "A company.", "d7582691-8ccc-415c-886b-7d363c8b6e84")
                    .setIsa(typeNodeRepository.findByUuid("c94c2c32-8e10-48c3-a31b-6337c2ad4df4")));
            createNode(new TypeNode("Department", "A department.", "aa6bc876-33fe-4710-b409-edb31e1081de")
                    .setIsa(typeNodeRepository.findByUuid("c94c2c32-8e10-48c3-a31b-6337c2ad4df4")));
            createNode(new TypeNode("Team", "A team.", "9d68148a-28ec-4f24-b1d7-0fc2d407ab78")
                    .setIsa(typeNodeRepository.findByUuid("c94c2c32-8e10-48c3-a31b-6337c2ad4df4")));
            createNode(new TypeNode("Componental", "A group for organizing references to components.",
                    "ee92c308-80f4-40b2-af3a-77d26cd99517")
                    .setIsa(typeNodeRepository.findByUuid("5f1eb7c5-ec5e-4563-be6a-fc50774fc374")));
            createNode(new TypeNode("Actor", "Something or someone that interacts/starts/acts within the system.",
                    "3175d762-dd97-434f-a288-21e4591480cf")
                    .setIsa(typeNodeRepository.findByUuid("b1bc963a-19cc-4518-aecb-a35d028c49e3")));
            createNode(new TypeNode("User", "A user.", "88d8630d-54ce-4aa5-8505-2068c31df5b1")
                    .setIsa(typeNodeRepository.findByUuid("3175d762-dd97-434f-a288-21e4591480cf")));
            createNode(new TypeNode("Batch", "A batch user.", "395d3e08-842b-4640-8b57-751e1b0c9e4b")
                    .setIsa(typeNodeRepository.findByUuid("3175d762-dd97-434f-a288-21e4591480cf")));
            createNode(new TypeNode("Capability", "A software component gives capabilities to its parent or to siblings.",
                    "916e04a9-c773-4eaf-9c08-450016426e5a")
                    .setIsa(typeNodeRepository.findByUuid("b1bc963a-19cc-4518-aecb-a35d028c49e3")));
            createNode(new TypeNode("Checkable", "A capability that makes it possible to state-check.",
                    "b3cfd296-1090-43c0-ac2e-00e5d7fe7779")
                    .setIsa(typeNodeRepository.findByUuid("916e04a9-c773-4eaf-9c08-450016426e5a")));
            createNode(new TypeNode("Stopable", "A capability that makes it possible to stop something.",
                    "632ca701-2648-4df5-87ee-a41d3d1ef3da")
                    .setIsa(typeNodeRepository.findByUuid("916e04a9-c773-4eaf-9c08-450016426e5a")));
            createNode(new TypeNode("Startable", "A capability that makes it possible to stop something.",
                    "347ca5da-db19-4d98-9fed-60e72d5d6bba")
                    .setIsa(typeNodeRepository.findByUuid("916e04a9-c773-4eaf-9c08-450016426e5a")));
            createNode(new TypeNode("Restartable", "A capability that makes it possible to restart something.",
                    "cc558715-63dc-4162-95a4-9c51792c7953")
                    .setIsa(typeNodeRepository.findByUuid("916e04a9-c773-4eaf-9c08-450016426e5a")));
            createNode(new TypeNode("Installable", "A capability that makes it possible to install something.",
                    "3c077211-0ffa-452f-8d84-5bd5be5598f5")
                    .setIsa(typeNodeRepository.findByUuid("916e04a9-c773-4eaf-9c08-450016426e5a")));
            createNode(new TypeNode("Configurable", "A capability that makes it possible to configure something.",
                    "3dc7b06a-69a0-4650-897a-34c5d069e3c5")
                    .setIsa(typeNodeRepository.findByUuid("916e04a9-c773-4eaf-9c08-450016426e5a")));
            createNode(new TypeNode("Uninstallable", "A capability that makes it possible to uninstall something.",
                    "ea1b32de-cf31-4c1b-9b2b-f1a6d9983418")
                    .setIsa(typeNodeRepository.findByUuid("916e04a9-c773-4eaf-9c08-450016426e5a")));
            createNode(new TypeNode("Virtualizable", "A capability that makes it possible to virtualize something.",
                    "b0995f9f-90af-4066-9d00-6b7b33f9a19b")
                    .setIsa(typeNodeRepository.findByUuid("916e04a9-c773-4eaf-9c08-450016426e5a")));
            createNode(new TypeNode("Commandable", "A capability that makes it possible to run commands.",
                    "76fb3116-d4a8-459a-b62b-a2641bcb7e7d")
                    .setIsa(typeNodeRepository.findByUuid("916e04a9-c773-4eaf-9c08-450016426e5a")));
            createNode(new TypeNode("Protocol", "A protocol.", "db0c3c65-52fd-4dec-907c-6c8a669e88f3")
                    .setIsa(typeNodeRepository.findByUuid("b1bc963a-19cc-4518-aecb-a35d028c49e3")));
            createNode(new TypeNode("HTTP", "The hypertext transfer protocol.", "9fd04885-179f-4c72-99a2-52c692e61b19")
                    .setIsa(typeNodeRepository.findByUuid("db0c3c65-52fd-4dec-907c-6c8a669e88f3")));
            createNode(new TypeNode("HTTP09", "The hypertext transfer protocol, version 0.9.",
                    "09102127-1803-48b6-a9f4-2fd1b891293d")
                    .setIsa(typeNodeRepository.findByUuid("9fd04885-179f-4c72-99a2-52c692e61b19")));
            createNode(new TypeNode("HTTP10", "The hypertext transfer protocol, version 1.0.",
                    "256d493d-3b7a-4e28-8e64-8f81de4d92de")
                    .setIsa(typeNodeRepository.findByUuid("9fd04885-179f-4c72-99a2-52c692e61b19")));
            createNode(new TypeNode("HTTP11", "The hypertext transfer protocol, version 1.1.",
                    "b11b10d7-6f46-49c0-bc61-3ecdd4342cd7")
                    .setIsa(typeNodeRepository.findByUuid("9fd04885-179f-4c72-99a2-52c692e61b19")));
            createNode(new TypeNode("SMB", "The server message block protocol.", "0771de77-c3d8-4db1-92fa-7ac96205f9bb")
                    .setIsa(typeNodeRepository.findByUuid("db0c3c65-52fd-4dec-907c-6c8a669e88f3")));
            createNode(new TypeNode("SMB10", "The server message block protocol, version 1.0.",
                    "70960db9-f085-499d-9d22-2569badc41c7")
                    .setIsa(typeNodeRepository.findByUuid("0771de77-c3d8-4db1-92fa-7ac96205f9bb")));
            createNode(new TypeNode("SMB20", "The server message block protocol, version 2.0.",
                    "aeefff52-0300-4611-be60-e745bfbd3d37")
                    .setIsa(typeNodeRepository.findByUuid("0771de77-c3d8-4db1-92fa-7ac96205f9bb")));
            createNode(new TypeNode("SMB21", "The server message block protocol, version 2.1.",
                    "5c7b0507-dfd9-4617-a45f-3d617cd61e68")
                    .setIsa(typeNodeRepository.findByUuid("0771de77-c3d8-4db1-92fa-7ac96205f9bb")));
            createNode(new TypeNode("SMB30", "The server message block protocol, version 3.0.",
                    "dbfb220e-b22c-460e-b27d-65de22501618")
                    .setIsa(typeNodeRepository.findByUuid("0771de77-c3d8-4db1-92fa-7ac96205f9bb")));
            createNode(new TypeNode("SSH", "The secure shell protocol.", "008b9566-6317-4ddd-977c-9ddca00b79b8")
                    .setIsa(typeNodeRepository.findByUuid("db0c3c65-52fd-4dec-907c-6c8a669e88f3")));
            createNode(new TypeNode("SSH10", "The secure shell protocol, version 1.0.", "000b0fd3-a121-4194-a441-0857abca490d")
                    .setIsa(typeNodeRepository.findByUuid("008b9566-6317-4ddd-977c-9ddca00b79b8")));
            createNode(new TypeNode("SSH20", "The secure shell protocol, version 2.0.", "07b23795-140f-4a72-9c86-e1d3b86cd149")
                    .setIsa(typeNodeRepository.findByUuid("008b9566-6317-4ddd-977c-9ddca00b79b8")));
            createNode(new TypeNode("TELNET", "The telnet protocol.", "27fc7ed2-beb3-41c8-aa69-900ad7bb989f")
                    .setIsa(typeNodeRepository.findByUuid("db0c3c65-52fd-4dec-907c-6c8a669e88f3")));
            createNode(new TypeNode("Handler", "A representation of a software handler that gives end user function.",
                    "14dbfbfa-b50a-466f-8539-55d5d3f15e4a")
                    .setIsa(typeNodeRepository.findByUuid("b1bc963a-19cc-4518-aecb-a35d028c49e3")));
            createNode(new TypeNode("CommandHandler", "Creates a CommandOperation, executes that CommandOperation, creates " +
                    "CommandResult.", "6ac7f3f8-4a1f-42aa-87b8-b2f5516b5d7c")
                    .setIsa(typeNodeRepository.findByUuid("14dbfbfa-b50a-466f-8539-55d5d3f15e4a")));
            createNode(new TypeNode("VersionHandler", "Looks up a VersionOperation, executes command, creates VersionResult.",
                    "a57f47c3-7e27-4347-806b-204239c61e65")
                    .setIsa(typeNodeRepository.findByUuid("14dbfbfa-b50a-466f-8539-55d5d3f15e4a")));
            createNode(new TypeNode("StatusHandler", "Executes generic code to get a status or executes CustomStatusOperation if " +
                    "it exists. Creates StatusResult.", "3b6555cd-abd4-4cc6-ab64-b7c031c2b008")
                    .setIsa(typeNodeRepository.findByUuid("14dbfbfa-b50a-466f-8539-55d5d3f15e4a")));
            createNode(new TypeNode("ExistenceHandler", "Executes generic code to get a existence information or executes " +
                    "CustomExistenceOperation if it exists. Creates ExistenceResult.", "6a8a8dd7-f8d3-4170-8c25-3866cfc2037c")
                    .setIsa(typeNodeRepository.findByUuid("14dbfbfa-b50a-466f-8539-55d5d3f15e4a")));
            createNode(new TypeNode("UsageHandler", "Executes generic code to derive usage of resources or executes " +
                    "CustomUsageOperation if it exists. Creates UsageResult.", "dd0878eb-b560-4dc0-89da-1d7ddfcbbced")
                    .setIsa(typeNodeRepository.findByUuid("14dbfbfa-b50a-466f-8539-55d5d3f15e4a")));
            createNode(new TypeNode("InstallHandler", "Executes generic code to derive install method and executes or executes " +
                    "CustomInstallOperation if it exists.", "3e33df40-8d9d-49d2-939d-6712d89ca70f")
                    .setIsa(typeNodeRepository.findByUuid("14dbfbfa-b50a-466f-8539-55d5d3f15e4a")));
            createNode(new TypeNode("UninstallHandler", "Executes generic code to derive uninstall method and executes or " +
                    "executes CustomUninstallOperation if it exists.", "6b80a87e-9d24-4a06-b792-2d13c0713b6c")
                    .setIsa(typeNodeRepository.findByUuid("14dbfbfa-b50a-466f-8539-55d5d3f15e4a")));
            createNode(new TypeNode("StartHandler", "Executes generic code to derive start method and executes or executes " +
                    "CustomStartOperation if it exists.", "db00edc2-5900-46d8-99fc-b379fd8538e2")
                    .setIsa(typeNodeRepository.findByUuid("14dbfbfa-b50a-466f-8539-55d5d3f15e4a")));
            createNode(new TypeNode("StopHandler", "Executes generic code to derive stop method and executes or executes " +
                    "CustomStopOperation if it exists.", "5d97322d-9113-4d7c-ac6c-2690a8c65d0d")
                    .setIsa(typeNodeRepository.findByUuid("14dbfbfa-b50a-466f-8539-55d5d3f15e4a")));
            createNode(new TypeNode("RestartHandler", "Executes generic code to derive restart method and executes or executes " +
                    "CustomRestartOperation if it exists.", "c61c2f8c-0ba2-4e0d-99f2-66a5580a59a6")
                    .setIsa(typeNodeRepository.findByUuid("14dbfbfa-b50a-466f-8539-55d5d3f15e4a")));
            createNode(new TypeNode("Operation", "A command or method that can be called, with or without schedule.",
                    "46294722-b14e-438e-9fbc-95ff137b6b18")
                    .setKvs("timesInvoked", "integer", true)
                    .setKvs("data", "string", true)
                    .setKvs("dateCreated", "datetime", true)
                    .setKvs("dateStarted", "datetime", true)
                    .setKvs("dateFinished", "datetime", true)
                    .setIsa(typeNodeRepository.findByUuid("b1bc963a-19cc-4518-aecb-a35d028c49e3")));
            createNode(new TypeNode("CommandOperation", "An arbitrary command.", "de64c2f4-7fa3-4e68-ba10-6af8140dac36")
                    .setIsa(typeNodeRepository.findByUuid("46294722-b14e-438e-9fbc-95ff137b6b18")));
            createNode(new TypeNode("VersionOperation", "A command that displays a version string.",
                    "9598a74d-0293-459a-af63-e3563215162f")
                    .setIsa(typeNodeRepository.findByUuid("46294722-b14e-438e-9fbc-95ff137b6b18")));
            createNode(new TypeNode("CustomStatusOperation", "Custom command for a status check operation.",
                    "bfc01b44-075d-4d6e-82eb-474830bba06e")
                    .setIsa(typeNodeRepository.findByUuid("46294722-b14e-438e-9fbc-95ff137b6b18")));
            createNode(new TypeNode("CustomExistenceOperation", "Custom command for an existence operation.",
                    "97bf21ef-d112-4607-9392-83243e44577f")
                    .setIsa(typeNodeRepository.findByUuid("46294722-b14e-438e-9fbc-95ff137b6b18")));
            createNode(new TypeNode("CustomUsageOperation", "Custom command that shows/derives/fetches the usage of a resource.",
                    "32b9f847-3579-4750-b29b-c0f3af877adb")
                    .setIsa(typeNodeRepository.findByUuid("46294722-b14e-438e-9fbc-95ff137b6b18")));
            createNode(new TypeNode("CustomInstallOperation", "Custom command to install a software.",
                    "7fb7c8c0-6967-4af2-a4f9-e5342fc3fb24")
                    .setIsa(typeNodeRepository.findByUuid("46294722-b14e-438e-9fbc-95ff137b6b18")));
            createNode(new TypeNode("CustomUninstallOperation", "Custom command to uninstall a software.",
                    "1308c542-ea7a-4525-8a1b-65cf96033a62")
                    .setIsa(typeNodeRepository.findByUuid("46294722-b14e-438e-9fbc-95ff137b6b18")));
            createNode(new TypeNode("CustomStartOperation", "Custom command to start a software.",
                    "8cb7ac74-7923-4114-abd2-17f4c5f440d7")
                    .setIsa(typeNodeRepository.findByUuid("46294722-b14e-438e-9fbc-95ff137b6b18")));
            createNode(new TypeNode("CustomStopOperation", "Custom command to stop a software.",
                    "ffefceab-afd8-42da-b793-d65beb4f71eb")
                    .setIsa(typeNodeRepository.findByUuid("46294722-b14e-438e-9fbc-95ff137b6b18")));
            createNode(new TypeNode("CustomRestartOperation", "Custom command to restart a software.",
                    "3d5300bf-7b4d-443c-b33a-159c00572eb9")
                    .setIsa(typeNodeRepository.findByUuid("46294722-b14e-438e-9fbc-95ff137b6b18")));
            createNode(new TypeNode("Result", "A result from an operation.", "f3498c02-58c6-486b-87c6-9d54607fdb12")
                    .setKvs("data", "string", true)
                    .setKvs("dateCreated", "datetime", true)
                    .setIsa(typeNodeRepository.findByUuid("b1bc963a-19cc-4518-aecb-a35d028c49e3")));
            createNode(new TypeNode("CommandResult", "A generic textual result from a command operation.",
                    "6991d878-52be-467d-893a-6b95074f04c2")
                    .setIsa(typeNodeRepository.findByUuid("f3498c02-58c6-486b-87c6-9d54607fdb12")));
            createNode(new TypeNode("VersionResult", "A textual result representing a version string.",
                    "2f9703cb-3d68-44fa-99bf-935ed658ca5a")
                    .setIsa(typeNodeRepository.findByUuid("f3498c02-58c6-486b-87c6-9d54607fdb12")));
            createNode(new TypeNode("StatusResult", "A result that represents a status.", "48839d5c-8ebc-4989-80ec-f6fb90299a56")
                    .setIsa(typeNodeRepository.findByUuid("f3498c02-58c6-486b-87c6-9d54607fdb12")));
            createNode(new TypeNode("DerivedStatusResult", "A derived status. 1/ok, 0/not-ok.",
                    "9565a2ca-29e4-4c36-8f17-97e9a0074ded")
                    .setIsa(typeNodeRepository.findByUuid("48839d5c-8ebc-4989-80ec-f6fb90299a56")));
            createNode(new TypeNode("LocalDerivedStatusResult", "A derived 1/0 status from all statuses known to the software " +
                    "itself.", "9d007dd4-82df-4791-81f9-d91a65e9f3a3")
                    .setIsa(typeNodeRepository.findByUuid("48839d5c-8ebc-4989-80ec-f6fb90299a56")));
            createNode(new TypeNode("AggregateDerivedStatusResult", "A derived 1/0 status from all localderivedstatuses from its " +
                    "children.", "3f9ca86e-6286-4e59-9eb1-fa352103fe5c")
                    .setIsa(typeNodeRepository.findByUuid("48839d5c-8ebc-4989-80ec-f6fb90299a56")));
            createNode(new TypeNode("SoftwareInstalledStatusResult", "1/installed, 0/not-installed.",
                    "38b435e1-046c-415b-9cce-6578efb4eef6")
                    .setIsa(typeNodeRepository.findByUuid("48839d5c-8ebc-4989-80ec-f6fb90299a56")));
            createNode(new TypeNode("SoftwareRunningStatusResult", "1/running, 0/not-running.",
                    "4fda91ea-63bb-4689-bcf4-86bb02314c21")
                    .setIsa(typeNodeRepository.findByUuid("48839d5c-8ebc-4989-80ec-f6fb90299a56")));
            createNode(new TypeNode("IpAddressReachableStatusResult", "1/reachable, 0/unreachable.",
                    "729e0e03-3a27-47e2-8075-7e4a754d68a5")
                    .setIsa(typeNodeRepository.findByUuid("48839d5c-8ebc-4989-80ec-f6fb90299a56")));
            createNode(new TypeNode("ListenerAvailableStatusResult", "1/open, 0/closed.", "630e8753-7566-4c2f-9cdf-99f5b64492a1")
                    .setIsa(typeNodeRepository.findByUuid("48839d5c-8ebc-4989-80ec-f6fb90299a56")));
            createNode(new TypeNode("DnsNameResolvableStatusResult", "1/resolved, 0/not-resolved.",
                    "fffdce77-417f-4044-a570-6e6153db1dd0")
                    .setIsa(typeNodeRepository.findByUuid("48839d5c-8ebc-4989-80ec-f6fb90299a56")));
            createNode(new TypeNode("ExistenceResult", "A generic result from an ExistenceHandler.",
                    "6d23a2bc-9539-4914-aaf4-45406f59d8fc")
                    .setIsa(typeNodeRepository.findByUuid("f3498c02-58c6-486b-87c6-9d54607fdb12")));
            createNode(new TypeNode("DirectoryExistenceResult", "A generic directory existence result from an ExistenceHandler.",
                    "31736b3b-82da-4864-bc59-b49c196e3327")
                    .setIsa(typeNodeRepository.findByUuid("6d23a2bc-9539-4914-aaf4-45406f59d8fc")));
            createNode(new TypeNode("PrefixDirectoryExistenceResult", "A possible result from an ExistenceHandler, parsing a " +
                    "PrefixDirectoryMap.", "93e34319-fe7b-4768-b83d-58986a4cdb73")
                    .setIsa(typeNodeRepository.findByUuid("31736b3b-82da-4864-bc59-b49c196e3327")));
            createNode(new TypeNode("ConfigDirectoryExistenceResult", "A possible result from an ExistenceHandler, parsing a " +
                    "ConfigDirectoryMap.", "0182518c-2380-4abc-be84-5fa02679fe52")
                    .setIsa(typeNodeRepository.findByUuid("31736b3b-82da-4864-bc59-b49c196e3327")));
            createNode(new TypeNode("BinaryDirectoryExistenceResult", "A possible result from an ExistenceHandler, parsing a " +
                    "BinaryDirectoryMap.", "1b607b4a-5206-4d55-8743-0487f7b6d944")
                    .setIsa(typeNodeRepository.findByUuid("31736b3b-82da-4864-bc59-b49c196e3327")));
            createNode(new TypeNode("LibraryDirectoryExistenceResult", "A possible result from an ExistenceHandler, parsing a " +
                    "LibraryDirectoryMap.", "25a2e063-b7e7-4727-9725-63859005e6ed")
                    .setIsa(typeNodeRepository.findByUuid("31736b3b-82da-4864-bc59-b49c196e3327")));
            createNode(new TypeNode("DataDirectoryExistenceResult", "A possible result from an ExistenceHandler, parsing a " +
                    "DataDirectoryMap.", "22105b60-ea6d-4c1e-b3ad-14b8ff9e37a1")
                    .setIsa(typeNodeRepository.findByUuid("31736b3b-82da-4864-bc59-b49c196e3327")));
            createNode(new TypeNode("LogDirectoryExistenceResult", "A possible result from an ExistenceHandler, parsing a " +
                    "LogDirectoryMap.", "3b64fa0c-474e-4109-9f43-c861ac5fce19")
                    .setIsa(typeNodeRepository.findByUuid("31736b3b-82da-4864-bc59-b49c196e3327")));
            createNode(new TypeNode("FileExistenceResult", "A generic file existence result from an ExistenceHandler.",
                    "b72fd981-3473-4874-9a40-bc86d6b5362f")
                    .setIsa(typeNodeRepository.findByUuid("6d23a2bc-9539-4914-aaf4-45406f59d8fc")));
            createNode(new TypeNode("ConfigFileExistenceResult", "A possible result from an ExistenceHandler, parsing a " +
                    "ConfigFileMap.", "6615da84-00f9-4858-a753-38f5905e8c4c")
                    .setIsa(typeNodeRepository.findByUuid("b72fd981-3473-4874-9a40-bc86d6b5362f")));
            createNode(new TypeNode("BinaryFileExistenceResult", "A possible result from an ExistenceHandler, parsing a " +
                    "BinaryFileMap.", "6c510a61-53b1-4f16-a5cb-8816846062be")
                    .setIsa(typeNodeRepository.findByUuid("b72fd981-3473-4874-9a40-bc86d6b5362f")));
            createNode(new TypeNode("LibraryFileExistenceResult", "A possible result from an ExistenceHandler, parsing a " +
                    "LibraryFileMap.", "e4d04fd4-0d95-49db-9637-891d5f9b6e21")
                    .setIsa(typeNodeRepository.findByUuid("b72fd981-3473-4874-9a40-bc86d6b5362f")));
            createNode(new TypeNode("DataFileExistenceResult", "A possible result from an ExistenceHandler, parsing a DataFileMap" +
                    ".", "6a1d9396-707e-4224-965c-2dd8c5c97cfb")
                    .setIsa(typeNodeRepository.findByUuid("b72fd981-3473-4874-9a40-bc86d6b5362f")));
            createNode(new TypeNode("LogFileExistenceResult", "A possible result from an ExistenceHandler, parsing a LogFileMap" +
                    ".", "fbd1b321-3c95-4eb3-ad4f-16c1f7215e39")
                    .setIsa(typeNodeRepository.findByUuid("b72fd981-3473-4874-9a40-bc86d6b5362f")));
            createNode(new TypeNode("NameExistenceResult", "A possible result from an ExistenceHandler, parsing a NameMap.",
                    "db4c51ec-7455-4bba-b965-f1c067f22e37")
                    .setIsa(typeNodeRepository.findByUuid("6d23a2bc-9539-4914-aaf4-45406f59d8fc")));
            createNode(new TypeNode("PackageNameExistenceResult", "A possible result from an ExistenceHandler, parsing a " +
                    "PackageNameMap.", "7dd30b10-04bf-4875-8f98-2add74299648")
                    .setIsa(typeNodeRepository.findByUuid("db4c51ec-7455-4bba-b965-f1c067f22e37")));
            createNode(new TypeNode("ServiceNameExistenceResult", "A possible result from an ExistenceHandler, parsing a " +
                    "ServiceNameMap.", "9edc562a-f026-4ea8-861f-0dd0cfdf3c39")
                    .setIsa(typeNodeRepository.findByUuid("db4c51ec-7455-4bba-b965-f1c067f22e37")));
            createNode(new TypeNode("StringExistenceResult", "A possible result from an ExistenceHandler, parsing a StringMap.",
                    "7bcaa3ec-9ffb-468d-a5dd-21d26661078a")
                    .setIsa(typeNodeRepository.findByUuid("6d23a2bc-9539-4914-aaf4-45406f59d8fc")));
            createNode(new TypeNode("VersionStringExistenceResult", "A possible result from an ExistenceHandler, parsing a " +
                    "VersionStringMap.", "4b86cf7f-6a98-46cc-b78a-0e568d2afe1f")
                    .setIsa(typeNodeRepository.findByUuid("7bcaa3ec-9ffb-468d-a5dd-21d26661078a")));
            createNode(new TypeNode("UsageResult", "A generic usage result.", "61bc2f71-158c-450c-9346-b9100505054e")
                    .setIsa(typeNodeRepository.findByUuid("f3498c02-58c6-486b-87c6-9d54607fdb12")));
            createNode(new TypeNode("ProcessorUsageResult", "The processing unit usage results from a UsageHandler.",
                    "885e629b-3f03-4fa9-a876-67e3991711a0")
                    .setIsa(typeNodeRepository.findByUuid("61bc2f71-158c-450c-9346-b9100505054e")));
            createNode(new TypeNode("MemoryUsageResult", "The memory usage results from a UsageHandler.",
                    "c893bc3b-21de-47b8-96dd-0ba11006d35b")
                    .setIsa(typeNodeRepository.findByUuid("61bc2f71-158c-450c-9346-b9100505054e")));
            createNode(new TypeNode("StorageUsageResult", "The storage usage results from a UsageHandler.",
                    "2615131b-50ce-4699-a5ef-261fbb633fe3")
                    .setIsa(typeNodeRepository.findByUuid("61bc2f71-158c-450c-9346-b9100505054e")));
            createNode(new TypeNode("NetworkUsageResult", "The network bandwidth usage results from a UsageHandler.",
                    "ebaf3fae-351a-4d90-9b98-9970399160aa")
                    .setIsa(typeNodeRepository.findByUuid("61bc2f71-158c-450c-9346-b9100505054e")));

            // org.openpcf.model.cloudsystems-0.0.1
            createNode(new TypeNode("CloudSystemModel", "Main cloud (operating) system types.",
                    "4a8e288a-696d-4616-bb77-970dcf3e150d")
                    .setIsa(typeNodeRepository.findByUuid("4e2dccfa-b5ca-4992-a76e-c390b7d90084")));
            createNode(new TypeNode("ApacheCloudStack", "The Apache CloudStack platform.", "c1d0eada-0d22-4026-aa2d-d865c1583482")
                    .setIsa(typeNodeRepository.findByUuid("86c0439a-4219-46e6-82a3-6fb2ee774aa4")));
            createNode(new TypeNode("VmwareCloudFoundry", "The VMware CloudFoundry platform.",
                    "6d080eef-d9c6-4be7-b81b-b9c57f0fe515")
                    .setIsa(typeNodeRepository.findByUuid("86c0439a-4219-46e6-82a3-6fb2ee774aa4")));
            createNode(new TypeNode("Eucalyptus", "The Eucalyptus Cloud platform.", "87c66de2-e24b-4586-8170-5fcf6cc28424")
                    .setIsa(typeNodeRepository.findByUuid("86c0439a-4219-46e6-82a3-6fb2ee774aa4")));
            createNode(new TypeNode("OpenNebula", "The OpenNebula Cloud platform.", "0e5b0e12-5975-43f5-9e92-5298a75a5f7a")
                    .setIsa(typeNodeRepository.findByUuid("86c0439a-4219-46e6-82a3-6fb2ee774aa4")));
            createNode(new TypeNode("OpenShift", "The OpenShift Cloud platform.", "e57b8fbb-05a0-4c64-830a-8da17801e757")
                    .setIsa(typeNodeRepository.findByUuid("86c0439a-4219-46e6-82a3-6fb2ee774aa4")));
            createNode(new TypeNode("OpenStack", "The OpenStack Cloud platform.", "55e9f170-9a98-42e3-ba58-f35c592eaeb0")
                    .setIsa(typeNodeRepository.findByUuid("86c0439a-4219-46e6-82a3-6fb2ee774aa4")));

            // org.openpcf.model.emulators-0.0.1
            createNode(new TypeNode("EmulatorModel", "Main emulator types.", "ce00fbd8-b9e6-4e93-ae5e-ce0d9da5b3b4")
                    .setIsa(typeNodeRepository.findByUuid("4e2dccfa-b5ca-4992-a76e-c390b7d90084")));
            createNode(new TypeNode("Bochs", "The Bochs Emulator.", "cd8f940e-219b-488e-8b75-d9fece8b3b35")
                    .setIsa(typeNodeRepository.findByUuid("59aeae81-9741-4986-b9c2-931504cfc9e3")));
            createNode(new TypeNode("DosBox", "The DOSBox Emulator.", "7e0135dc-b720-440a-9f4e-76747f7a7fe5")
                    .setIsa(typeNodeRepository.findByUuid("59aeae81-9741-4986-b9c2-931504cfc9e3")));
            createNode(new TypeNode("Qemu", "The QEMU Emulator.", "c80fddb7-bdbf-44b9-ac54-f7dd7ce4dbd0")
                    .setIsa(typeNodeRepository.findByUuid("59aeae81-9741-4986-b9c2-931504cfc9e3")));

            // org.openpcf.model.hypervisors-0.0.1
            createNode(new TypeNode("HypervisorModel", "Main hypervisor types.", "76fc46bb-8ff8-4e35-a8b5-80c44a82346a")
                    .setIsa(typeNodeRepository.findByUuid("4e2dccfa-b5ca-4992-a76e-c390b7d90084")));
            createNode(new TypeNode("Kvm", "The Linux kernel-based virtual machine.", "04b4bd73-fcec-42f6-bb03-7686ad019fbb")
                    .setIsa(typeNodeRepository.findByUuid("c5c7fcb5-8ac3-40cc-b195-b2b6af368e81")));
            createNode(new TypeNode("VirtualBox", "The Oracle VM VirtualBox virtual machine.",
                    "3c47f326-d613-4f42-b4ac-804a6c22f076")
                    .setIsa(typeNodeRepository.findByUuid("c5c7fcb5-8ac3-40cc-b195-b2b6af368e81")));
            createNode(new TypeNode("VmwareEsxi", "The VMware ESXi virtual machine.", "b9739ee8-34a6-4308-b7e2-ba775c0e59cf")
                    .setIsa(typeNodeRepository.findByUuid("c5c7fcb5-8ac3-40cc-b195-b2b6af368e81")));
            createNode(new TypeNode("VmwareWorkstation", "The VMware Workstation virtual machine.",
                    "897281c1-05be-43aa-a49c-50fd9ed4fc88")
                    .setIsa(typeNodeRepository.findByUuid("c5c7fcb5-8ac3-40cc-b195-b2b6af368e81")));
            createNode(new TypeNode("Xen", "The Xen Project virtual machine.", "54916142-640a-41df-82ad-974e92d3371c")
                    .setIsa(typeNodeRepository.findByUuid("c5c7fcb5-8ac3-40cc-b195-b2b6af368e81")));

            // org.openpcf.model.interpreters-0.0.1
            createNode(new TypeNode("InterpreterModel", "Main interpreter types.", "452269e5-922e-49af-8442-ef3b58348f61")
                    .setIsa(typeNodeRepository.findByUuid("4e2dccfa-b5ca-4992-a76e-c390b7d90084")));
            createNode(new TypeNode("UnixShell", "A generic UNIX shell.", "86168e4a-8c99-43c9-ad95-c8f88d96ac32")
                    .setIsa(typeNodeRepository.findByUuid("d047f415-734c-4bd3-92b9-d68d5521ce69")));
            createNode(new TypeNode("Bash", "The Bourne Again Shell.", "6d467b28-449f-498c-9ca5-8660f522897f")
                    .setIsa(typeNodeRepository.findByUuid("86168e4a-8c99-43c9-ad95-c8f88d96ac32")));
            createNode(new TypeNode("Ksh", "The Korn Shell.", "fd95e858-c788-4578-9223-122eceb2e6c6")
                    .setIsa(typeNodeRepository.findByUuid("86168e4a-8c99-43c9-ad95-c8f88d96ac32")));
            createNode(new TypeNode("Csh", "The C Shell.", "608c9a23-a194-49b0-a986-c34bd52bf691")
                    .setIsa(typeNodeRepository.findByUuid("86168e4a-8c99-43c9-ad95-c8f88d96ac32")));
            createNode(new TypeNode("Tcsh", "The TENEX C Shell.", "bb3e3458-391d-4c75-8857-4a08be7e5905")
                    .setIsa(typeNodeRepository.findByUuid("86168e4a-8c99-43c9-ad95-c8f88d96ac32")));
            createNode(new TypeNode("Sh", "The Bourne Shell.", "7c1c3278-6eb4-4be5-8b8d-94204bfd2223")
                    .setIsa(typeNodeRepository.findByUuid("86168e4a-8c99-43c9-ad95-c8f88d96ac32")));
            createNode(new TypeNode("Zsh", "The Zhong Shao Shell.", "7d9afc14-8e12-4c56-8ca6-4c43fd9808ab")
                    .setIsa(typeNodeRepository.findByUuid("86168e4a-8c99-43c9-ad95-c8f88d96ac32")));
            createNode(new TypeNode("DosShell", "A generic DOS shell.", "bc101145-dde2-4363-a7ed-7091d2ff2621")
                    .setIsa(typeNodeRepository.findByUuid("d047f415-734c-4bd3-92b9-d68d5521ce69")));

            // org.openpcf.model.operatingsystems-0.0.1
            createNode(new TypeNode("OperatingSystemModel", "Main operating system types.", "90d2c69e-7199-45f4-bafa-5e0f848cf17d")
                    .setIsa(typeNodeRepository.findByUuid("4e2dccfa-b5ca-4992-a76e-c390b7d90084")));
            createNode(new TypeNode("DosLike", "A DOS-like operating system.", "e181e9e8-7f30-42a3-b87f-b8e183ddfd6f")
                    .setIsa(typeNodeRepository.findByUuid("f05e325e-1aee-4892-96e4-8bcd6e97c528")));
            createNode(new TypeNode("WindowsLike", "A Windows-like operating system.", "4edbaf6a-da58-4644-abac-ae80328ddb49")
                    .setIsa(typeNodeRepository.findByUuid("f05e325e-1aee-4892-96e4-8bcd6e97c528")));
            createNode(new TypeNode("Windows", "The Microsoft Windows Operating System.", "060a519d-08d0-4d12-82ac-1e3f14277009")
                    .setIsa(typeNodeRepository.findByUuid("4edbaf6a-da58-4644-abac-ae80328ddb49")));
            createNode(new TypeNode("UnixLike", "A UNIX-like operating system.", "6a1eea8b-1462-4439-8c60-12edbee6c397")
                    .setIsa(typeNodeRepository.findByUuid("f05e325e-1aee-4892-96e4-8bcd6e97c528")));
            createNode(new TypeNode("Unix", "The UNIX Operating System.", "a3b228b6-f737-460c-a3bc-bc653167b36c")
                    .setIsa(typeNodeRepository.findByUuid("6a1eea8b-1462-4439-8c60-12edbee6c397")));
            createNode(new TypeNode("Illumos", "The Illumos Operating System.", "f0bcc651-3ca6-4d4c-ac9d-5a60b2b08520")
                    .setIsa(typeNodeRepository.findByUuid("a3b228b6-f737-460c-a3bc-bc653167b36c")));
            createNode(new TypeNode("MacOsX", "The Apple Mac OS X Operating System.", "0f81d6dc-d221-464a-ab85-1e3e5d98c3f0")
                    .setIsa(typeNodeRepository.findByUuid("a3b228b6-f737-460c-a3bc-bc653167b36c")));
            createNode(new TypeNode("Bsd", "The Berkeley Software Distribution Operating System.",
                    "6004436f-7c88-4c2e-a29d-e8077bad10e9")
                    .setIsa(typeNodeRepository.findByUuid("6a1eea8b-1462-4439-8c60-12edbee6c397")));
            createNode(new TypeNode("Linux", "The Linux Operating System.", "a5613527-2f64-4481-83f2-80b0cb427e28")
                    .setIsa(typeNodeRepository.findByUuid("6a1eea8b-1462-4439-8c60-12edbee6c397")));

            // org.openpcf.model.packagemanagers-0.0.1
            createNode(new TypeNode("PackageManagerModel", "Main package manager types.", "88846580-7ca5-4d7c-aba4-f350e61dd5ce")
                    .setIsa(typeNodeRepository.findByUuid("4e2dccfa-b5ca-4992-a76e-c390b7d90084")));
            createNode(new TypeNode("PacMan", "Arch Linux Pacman Package Manager.", "ad047d85-792a-4385-a14c-b85266bc5176")
                    .setIsa(typeNodeRepository.findByUuid("e2b8b9ee-b66f-4252-8fba-4fb172f741bc")));
            createNode(new TypeNode("BsdPkg", "BSD pkg Package Manager.", "95d6093b-448b-4a62-8642-dc8d32b43622")
                    .setIsa(typeNodeRepository.findByUuid("e2b8b9ee-b66f-4252-8fba-4fb172f741bc")));
            createNode(new TypeNode("BsdPkgNg", "BSD pkgng Package Manager.", "1a879371-f680-4514-8189-5d6e227ca6cc")
                    .setIsa(typeNodeRepository.findByUuid("e2b8b9ee-b66f-4252-8fba-4fb172f741bc")));
            createNode(new TypeNode("Apt", "Debian Advanced Package Tool.", "74528ec1-d223-40ad-8e2f-703b7f6d33b0")
                    .setIsa(typeNodeRepository.findByUuid("e2b8b9ee-b66f-4252-8fba-4fb172f741bc")));
            createNode(new TypeNode("Yum", "YellowDog Updater, Modified.", "27a03a73-d654-4999-8888-72043c84172d")
                    .setIsa(typeNodeRepository.findByUuid("e2b8b9ee-b66f-4252-8fba-4fb172f741bc")));
            createNode(new TypeNode("Zypp", "ZYpp Package Manager.", "8a016a93-73f3-4b3d-9129-b3708f5d1afe")
                    .setIsa(typeNodeRepository.findByUuid("e2b8b9ee-b66f-4252-8fba-4fb172f741bc")));
            createNode(new TypeNode("OptZip", "The very simple unzip-in /opt package manager.",
                    "2115cfc1-a2e8-4efe-aa58-60ef64a729ca")
                    .setIsa(typeNodeRepository.findByUuid("e2b8b9ee-b66f-4252-8fba-4fb172f741bc")));

            // org.openpcf.model.servicemanagers-0.0.1
            createNode(new TypeNode("ServiceManagerModel", "Main service manager types.", "4c305d8f-a9bb-4cfa-a512-4cc514c63349")
                    .setIsa(typeNodeRepository.findByUuid("4e2dccfa-b5ca-4992-a76e-c390b7d90084")));
            createNode(new TypeNode("SysvInit", "SysV Init Service Manager.", "608bfaf1-9edc-4609-b5cd-56d8aeff8174")
                    .setIsa(typeNodeRepository.findByUuid("c3008eec-b454-4ef6-bf7e-0bd92c13d3c3")));
            createNode(new TypeNode("BsdrcInit", "BSD RC Init Service Manager.", "b4c067de-e3cf-4b5b-90ca-588725d1b6c4")
                    .setIsa(typeNodeRepository.findByUuid("c3008eec-b454-4ef6-bf7e-0bd92c13d3c3")));
            createNode(new TypeNode("Upstart", "Upstart Service Manager.", "3aac3563-a493-4efa-a31b-e751ee7d0a6f")
                    .setIsa(typeNodeRepository.findByUuid("c3008eec-b454-4ef6-bf7e-0bd92c13d3c3")));
            createNode(new TypeNode("Systemd", "Systemd Service Manager.", "0c189a0e-6b78-4177-832d-5d9b35b8a6af")
                    .setIsa(typeNodeRepository.findByUuid("c3008eec-b454-4ef6-bf7e-0bd92c13d3c3")));
            createNode(new TypeNode("SystemStarter", "Mac OS X SystemStarter.", "fd13bd74-6682-4494-92af-a6caf5e64f12")
                    .setIsa(typeNodeRepository.findByUuid("c3008eec-b454-4ef6-bf7e-0bd92c13d3c3")));
            createNode(new TypeNode("Launchd", "Mac OS X Launchd.", "7b0bfefb-2212-4c58-a7c5-46ff66f0166a")
                    .setIsa(typeNodeRepository.findByUuid("c3008eec-b454-4ef6-bf7e-0bd92c13d3c3")));
            createNode(new TypeNode("Scm", "Service Control Manager.", "198435e1-cf1d-4d0a-9fb8-c760b5e8ac2b")
                    .setIsa(typeNodeRepository.findByUuid("c3008eec-b454-4ef6-bf7e-0bd92c13d3c3")));

            // org.openpcf.model.services-0.0.1
            createNode(new TypeNode("ServiceModel", "Main service types.", "f947e94a-f9b2-4170-90e1-107c2a946bd7")
                    .setIsa(typeNodeRepository.findByUuid("4e2dccfa-b5ca-4992-a76e-c390b7d90084")));
            createNode(new TypeNode("HttpServer", "A generic HTTP Server.", "b46cb520-caaf-4d26-b59d-58edca723b8d")
                    .setIsa(typeNodeRepository.findByUuid("3fb4603b-281e-47c0-b338-768ea05d4660")));
            createNode(new TypeNode("SmbServer", "A generic SMB/CIFS Server.", "65af133b-1a2c-4faa-8d2a-a288eaa4f412")
                    .setIsa(typeNodeRepository.findByUuid("3fb4603b-281e-47c0-b338-768ea05d4660")));
            createNode(new TypeNode("SshServer", "A generic SSH Server.", "7db4a3ef-2d3c-4071-929b-e81505fbd5e2")
                    .setIsa(typeNodeRepository.findByUuid("3fb4603b-281e-47c0-b338-768ea05d4660")));
            createNode(new TypeNode("TelnetServer", "A generic TELNET Server.", "4a09bcce-59c6-4e94-9fc4-ba020733a27c")
                    .setIsa(typeNodeRepository.findByUuid("3fb4603b-281e-47c0-b338-768ea05d4660")));

            long endTime = System.nanoTime();
            System.out.println("Created the TypeNode database in " + (endTime-startTime)/1000000 + "ms.");
            tx.success();
        } finally {
            tx.close();
        }
        return true;
    }
}
