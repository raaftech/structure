package com.raaftech.structure.graph.schema.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.graph.SchemaNode;
import com.raaftech.structure.graph.TypeNode;
import com.raaftech.structure.graph.schema.SchemaUpdate;

/**
 * Give a description of Base here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-12
 */
public class BaseR1 extends SchemaUpdate {

        ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void setUpdateInformation() {
        updateInformation = SchemaNode.getOrCreateSchemaNode("BaseModel", "The first model object, all base types.",
                "1d924647-6815-4772-a38d-ca86717185ee", 1);
    }

    @Override
    protected void initNodes() {
        // raaftech.com.model.base
        addToMap(new TypeNode("Type", "The root type.", "b1bc963a-19cc-4518-aecb-a35d028c49e3", 0x00E9));
                addToMap(new TypeNode("Component", "An abstract base component.", "a79ce509-a021-4177-ae61-73abd01fb479", 0x003E)
                        .setIsa(getFromMap("b1bc963a-19cc-4518-aecb-a35d028c49e3")));
                addToMap(new TypeNode("Configuration", "An abstract configuration component.", "3f4a43f7-b558-4bf0-ba7e" +
                        "-66b99cffaf16", 0x0042)
                        .setIsa(getFromMap("b1bc963a-19cc-4518-aecb-a35d028c49e3")));
                addToMap(new TypeNode("IpAddress", "An IP address (address only, can be assigned to a network interface).",
                        "299001a6-1091-4c0d-87be-c512af03e179", 0x006D)
                        .setDescriptor("addr", "string", true, "IP addresses should at least have the addr field.", null, null)
                        .setDescriptor("reachable", "boolean", true, "Is this IP address reachable from the outside?", null, null)
                        .setIsa(getFromMap("3f4a43f7-b558-4bf0-ba7e-66b99cffaf16")));
                addToMap(new TypeNode("Ipv4Address", "An IPv4 address (address only, can be assigned to a network " +
                        "interface).",
                        "f7fbe917-9d49-4df5-95f6-6e132067a424", 0x006F)
                        .setIsa(getFromMap("299001a6-1091-4c0d-87be-c512af03e179")));
                addToMap(new TypeNode("Ipv6Address", "An IPv6 address (address only, can be assigned to a network interface).",
                        "47b5804d-5f5d-4bf8-89f6-a189c29b05ae", 0x0071)
                        .setIsa(getFromMap("299001a6-1091-4c0d-87be-c512af03e179")));
                addToMap(new TypeNode("IpListener", "An IP listener (address and port, can be assigned to a network service).",
                        "91c999a0-33ec-42ee-bae0-6f8adae42535", 0x006E)
                        .setDescriptor("addr", "string", true, "IP listeners should at least have the addr and port fields.", null,
                                null)
                        .setDescriptor("port", "number", true, "IP listeners should at least have the addr and port fields.", null,
                                null)
                        .setIsa(getFromMap("3f4a43f7-b558-4bf0-ba7e-66b99cffaf16")));
                addToMap(new TypeNode("Ipv4Listener", "An IPv4 listener (address and port, can be assigned to a network service).",
                        "5ca95f7a-5175-4e7f-8a61-862e24485af3", 0x0070)
                        .setIsa(getFromMap("91c999a0-33ec-42ee-bae0-6f8adae42535")));
                addToMap(new TypeNode("Ipv6Listener", "An IPv6 listener (address and port, can be assigned to a network service).",
                        "6a79b4e9-13ce-4a09-bc5b-b429d23b34d7", 0x0072)
                        .setIsa(getFromMap("91c999a0-33ec-42ee-bae0-6f8adae42535")));
                addToMap(new TypeNode("Url", "a uniform resource locator.", "0e9f72bd-5bc5-43dc-ab86-30a8f7171bc6", 0x00F0)
                        .setDescriptor("url", "string", true, "scheme://domain:port/path?query_string#fragment_id", null, null)
                        .setIsa(getFromMap("3f4a43f7-b558-4bf0-ba7e-66b99cffaf16")));
                addToMap(new TypeNode("DnsName", "Either a short name or a fully-qualified domain name.",
                        "99c0da37-3599-418d-8ecc-de36d37ae9f4", 0x004A)
                        .setDescriptor("dnsname", "string", true, "somename", null, null)
                        .setIsa(getFromMap("3f4a43f7-b558-4bf0-ba7e-66b99cffaf16")));
                addToMap(new TypeNode("Map", "A configuration object that maps keys to multi-valued attributes in the JSON format" +
                        ".", "c970f6d3-4898-4596-bfac-9d6d1f6b2541", 0x007D)
                        .setDescriptor("map", "string", true, "JSON string containing the Map.", null, null)
                        .setIsa(getFromMap("3f4a43f7-b558-4bf0-ba7e-66b99cffaf16")));
                addToMap(new TypeNode("DirectoryMap", "A generic map of possible directories this software has. Key: os-shortname, " +
                        "Values: list of directories, Format: JSON.", "64d50e90-8be2-4d97-9dba-dd8518eb3f84")
                        .setIsa(getFromMap("c970f6d3-4898-4596-bfac-9d6d1f6b2541")));
                addToMap(new TypeNode("PrefixDirectoryMap", "A map of possible/known prefix directories for this software. Key: " +
                        "os-shortname, Values: list of directory prefixes, Format: JSON.", "d47e9ecd-d735-49a0-8d94" +
                        "-c4cb948ea226")
                        .setIsa(getFromMap("64d50e90-8be2-4d97-9dba-dd8518eb3f84")));
                addToMap(new TypeNode("ConfigDirectoryMap", "A map of possible/known configuration directories for this software. " +
                        "Key: os-shortname, Values: list of config directories, Format: JSON.", "fb83aefa-ec5e-4ad9-a807" +
                        "-4a37ffaa404b")
                        .setIsa(getFromMap("64d50e90-8be2-4d97-9dba-dd8518eb3f84")));
                addToMap(new TypeNode("BinaryDirectoryMap", "A map of possible/known binary executable directories for this " +
                        "software. Key: os-shortname, Values: list of binary directories, Format: JSON.",
                        "33e5e211-88db-4a78-95cd-2f2a3cd9e297")
                        .setIsa(getFromMap("64d50e90-8be2-4d97-9dba-dd8518eb3f84")));
                addToMap(new TypeNode("LibraryDirectoryMap", "A map of possible/known dynamic link library directories for this " +
                        "software. Key: os-shortname, Values: list of library directories, Format: JSON.",
                        "8ccf2b1c-3650-408f-8560-4ab9f3a57bbe")
                        .setIsa(getFromMap("64d50e90-8be2-4d97-9dba-dd8518eb3f84")));
                addToMap(new TypeNode("DataDirectoryMap", "A map of possible/known data directories for this software. Key: " +
                        "os-shortname, Values: list of data directories, Format: JSON.", "07aaa5dd-a747-4474-850d-01ad00a2f533")
                        .setIsa(getFromMap("64d50e90-8be2-4d97-9dba-dd8518eb3f84")));
                addToMap(new TypeNode("LogDirectoryMap", "A map of possible/known log directories for this software. Key: " +
                        "os-shortname, Values: list of log directories, Format: JSON.", "f8a9ce96-2a50-4a80-8deb-ec6fb294638e")
                        .setIsa(getFromMap("64d50e90-8be2-4d97-9dba-dd8518eb3f84")));
                addToMap(new TypeNode("FileMap", "A generic map of possible/known files this software has. Key: os-shortname, " +
                        "Values: list of files, Format: JSON.", "35407e44-ec50-4d66-83f9-dacf1f1b49dd")
                        .setIsa(getFromMap("c970f6d3-4898-4596-bfac-9d6d1f6b2541")));
                addToMap(new TypeNode("ConfigFileMap", "A map of possible/known configuration files for this software, non-full " +
                        "path, checked within ConfigDirectoryMap entries.", "a6125034-55e0-4604-bf21-407837d0b419")
                        .setIsa(getFromMap("35407e44-ec50-4d66-83f9-dacf1f1b49dd")));
                addToMap(new TypeNode("BinaryFileMap", "A map of possible/known binary executable files for this software, non-full" +
                        " path, checked within BinaryDirectoryMap entries.", "5ffab714-fa0e-416c-a237-9b023fe27bd7")
                        .setIsa(getFromMap("35407e44-ec50-4d66-83f9-dacf1f1b49dd")));
                addToMap(new TypeNode("LibraryFileMap", "A map of possible/known dynamic link library files for this software, " +
                        "non-full path, checked within LibraryDirectoryMap entries.", "99a6e35b-912e-41f1-b404-d24299e51fb2")
                        .setIsa(getFromMap("35407e44-ec50-4d66-83f9-dacf1f1b49dd")));
                addToMap(new TypeNode("DataFileMap", "A map of possible/known data files for this software, non-full path, checked " +
                        "within DataDirectoryMap entries.", "b19412ae-dffe-4fe8-9f00-3207ab686fb4")
                        .setIsa(getFromMap("35407e44-ec50-4d66-83f9-dacf1f1b49dd")));
                addToMap(new TypeNode("LogFileMap", "A map of possible/known log files for this software, non-full path, checked " +
                        "within LogDirectoryMap entries.", "ac583c10-f77c-4d40-84eb-4ffed77c79e0")
                        .setIsa(getFromMap("35407e44-ec50-4d66-83f9-dacf1f1b49dd")));
                addToMap(new TypeNode("NameMap", "A generic map of possible/known names this software has. Key: os-shortname, " +
                        "Values: list of names, Format: JSON.", "14372d63-a428-46b9-8359-9982ef22cfce")
                        .setIsa(getFromMap("c970f6d3-4898-4596-bfac-9d6d1f6b2541")));
                addToMap(new TypeNode("PackageNameMap", "A generic map of possible/known package names this software has. Key: " +
                        "os-shortname, Values: list of names, Format: JSON.", "1d691f61-3c9f-46ad-b98b-b968352d59b3")
                        .setIsa(getFromMap("14372d63-a428-46b9-8359-9982ef22cfce")));
                addToMap(new TypeNode("ServiceNameMap", "A generic map of possible/known service names this software has. Key: " +
                        "os-shortname, Values: list of names, Format: JSON.", "ead61277-e797-406b-96e1-95c155655fac")
                        .setIsa(getFromMap("14372d63-a428-46b9-8359-9982ef22cfce")));
                addToMap(new TypeNode("StringMap", "A generic map of possible/known strings this software has. Key: *, Values: " +
                        "string pattern, Format: JSON.", "0d554a04-67d6-428f-8e6f-6cdcdaca39cb")
                        .setIsa(getFromMap("c970f6d3-4898-4596-bfac-9d6d1f6b2541")));
                addToMap(new TypeNode("VersionStringMap", "A map of possible/known version strings this software has. Key: *, " +
                        "Values: version string pattern, Format: JSON.", "1c1af482-f0e0-45f1-9aa1-f7702040067d")
                        .setIsa(getFromMap("0d554a04-67d6-428f-8e6f-6cdcdaca39cb")));
                addToMap(new TypeNode("Hardware", "A hardware component.", "ea6a7487-867c-4a74-8849-fb8539b8169f", 0x0048)
                        .setDescriptor("rev", "string", false, "The revision of the device.", null, null)
                        .setIsa(getFromMap("a79ce509-a021-4177-ae61-73abd01fb479")));
                addToMap(new TypeNode("ConsoleHardware", "A console device.", "3d0865a2-3ecf-4035-886b-0bb603c0e2c2", 0x0043)
                        .setDescriptor("baudRate", "number", false, "110,300,600,1200,2400,4800,9600,14400,19200,28800,38400,56000," +
                                "57600 or 115200.", null, null)
                        .setDescriptor("stopBits", "number", false, "1, 1.5 or 2.", null, null)
                        .setDescriptor("parity", "string", false, "none (N)); odd (O), even (E), mark (M), or space (S).", null, null)
                        .setDescriptor("dataBits", "number", false, "5,6,7,8 or 9.", null, null)
                        .setIsa(getFromMap("ea6a7487-867c-4a74-8849-fb8539b8169f")));
                addToMap(new TypeNode("GenericSerial", "A serial communication device.",
                        "f5b25736-ec12-4a7e-b198-548b97ebc36f", 0x005A)
                        .setIsa(getFromMap("3d0865a2-3ecf-4035-886b-0bb603c0e2c2")));
                addToMap(new TypeNode("GraphicsHardware", "A graphics device.", "a642b995-c073-4c27-a611-c5bf3854958f", 0x005E)
                        .setDescriptor("renderOutputCores", "number", false, "Number of render output cores.", null, null)
                        .setDescriptor("memClockHz", "number", false, "Memory speed in Hz.", null, null)
                        .setDescriptor("gpuCoreCount", "number", false, "Number of GPU cores if applicable.", null, null)
                        .setDescriptor("memSizekb", "number", false, "Memory size in Kb.", null, null)
                        .setDescriptor("flops", "number", false, "Number of floating-point operations per second.", null, null)
                        .setDescriptor("pixelRatesPerSec", "number", false, "Pixel fillrate, in pixels per second.", null, null)
                        .setDescriptor("texelRatesPerSec", "number", false, "Texture fillrate, in texels per second.", null, null)
                        .setDescriptor("memBusBits", "number", false, "Memory bus width in bits.", null, null)
                        .setDescriptor("maxColorBitsPerChannel", "number", false, "Maximum number of bits to describe color in a " +
                                "color channel (R,G,B,A).", null, null)
                        .setDescriptor("memBandwidthKbs", "number", false, "Memory bandwidth in kilobytes per second.", null, null)
                        .setDescriptor("maxColorBitsPerScreen", "number", false, "Maximum number of bits per pixel on a screen.",
                                null, null)
                        .setDescriptor("textureMapUnits", "number", false, "Number of texture mapping units.", null, null)
                        .setDescriptor("gpuClockhz", "number", false, "Clockspeed of the GPU in Hz.", null, null)
                        .setDescriptor("shadingUnits", "number", false, "Number of shading units.", null, null)
                        .setDescriptor("maxVerticalRefreshHz", "number", false, "Max", null, null)
                        .setIsa(getFromMap("ea6a7487-867c-4a74-8849-fb8539b8169f")));
                addToMap(new TypeNode("GenericVga", "A VGA graphics device.", "b3a2ac7c-ad8a-4047-a460-2ac41a7ddfef", 0x005B)
                        .setIsa(getFromMap("a642b995-c073-4c27-a611-c5bf3854958f")));
                addToMap(new TypeNode("MemoryHardware", "A memory device", "1c87c672-1f69-4a02-9bc8-9eff28dda5e5", 0x007E)
                        .setDescriptor("memSizeKb", "number", true, "Memory size in Kb.", null, null)
                        .setIsa(getFromMap("ea6a7487-867c-4a74-8849-fb8539b8169f")));
                addToMap(new TypeNode("VolatileMemory", "A generic volatile memory device.", "44131d46-c1b0-4030-ae5a" +
                        "-9a3108627749", 0x00F9)
                        .setDescriptor("memClockHz", "number", true, "Memory speed in Hz.", null, null)
                        .setIsa(getFromMap("1c87c672-1f69-4a02-9bc8-9eff28dda5e5")));
                addToMap(new TypeNode("RandomAccessMemory", "Random access memory.", "4c0a0638-1fa7-4f5b-9d69-1d199dcedccc", 0x00C6)
                        .setIsa(getFromMap("44131d46-c1b0-4030-ae5a-9a3108627749")));
                addToMap(new TypeNode("NonVolatileMemory", "A generic non-volatile memory device.",
                        "7f129e53-ff07-4ed8-9062-daf5f733a3a6", 0x00AB)
                        .setIsa(getFromMap("1c87c672-1f69-4a02-9bc8-9eff28dda5e5")));
                addToMap(new TypeNode("HardDiskDrive", "A non-volatile harddisk-based storage device.",
                        "24cc3545-0d1a-4d85-8d08-a83c3dbe5912")
                        .setIsa(getFromMap("7f129e53-ff07-4ed8-9062-daf5f733a3a6")));
                addToMap(new TypeNode("SolidStateDrive", "A non-volatile solid-state storage device.",
                        "7a60ec32-0a9a-40b9-bf1f-d69674d4a233", 0x00DB)
                        .setIsa(getFromMap("7f129e53-ff07-4ed8-9062-daf5f733a3a6")));
                addToMap(new TypeNode("FloppyDrive", "A floppy disk drive.", "118ec961-a823-465b-be17-b706f1c8c2f1", 0x0057)
                        .setIsa(getFromMap("7f129e53-ff07-4ed8-9062-daf5f733a3a6")));
                addToMap(new TypeNode("TapeDrive", "A tape drive.", "a507461f-647b-4b79-936a-721ca2dba145", 0x00E4)
                        .setIsa(getFromMap("7f129e53-ff07-4ed8-9062-daf5f733a3a6")));
                addToMap(new TypeNode("CdDrive", "A Compact Disc drive.", "30af2fe3-aa05-4912-a3d5-dddfdacf74e2", 0x0037)
                        .setIsa(getFromMap("7f129e53-ff07-4ed8-9062-daf5f733a3a6")));
                addToMap(new TypeNode("DvdDrive", "A Digital Video Disc drive.", "23ab95d2-2815-4121-a81c-63aca40cdcfa", 0x004F)
                        .setIsa(getFromMap("7f129e53-ff07-4ed8-9062-daf5f733a3a6")));
                addToMap(new TypeNode("BdDrive", "A Digital Video Disc drive.", "cbc27482-11d1-4890-a3e1-e9f80baea82c", 0x002E)
                        .setIsa(getFromMap("7f129e53-ff07-4ed8-9062-daf5f733a3a6")));
                addToMap(new TypeNode("NetworkHardware", "A network device.", "86786ed6-d62a-4011-9ae8-0082000e48fa", 0x00A9)
                        .setIsa(getFromMap("ea6a7487-867c-4a74-8849-fb8539b8169f")));
                addToMap(new TypeNode("WiredNetworkHardware", "A wired network device.", "0d4c880d-d888-435f-b3a3-3d3eb9e70ddf", 0x00FF)
                        .setIsa(getFromMap("86786ed6-d62a-4011-9ae8-0082000e48fa")));
                addToMap(new TypeNode("ModemHardware", "A modem networking device.", "f34b76e4-f5a6-4bae-968e-f22ef09b1e35", 0x00A4)
                        .setDescriptor("baudRate", "number", true, "110,300,600,1200,2400,4800,9600,14400,19200,28800,38400,56000," +
                                "57600 or 115200.", null, null)
                        .setIsa(getFromMap("0d4c880d-d888-435f-b3a3-3d3eb9e70ddf")));
                addToMap(new TypeNode("EthernetHardware", "A wired ethernet device.", "0dbd26f2-baff-446c-9241-46eaa3e03736", 0x0052)
                        .setDescriptor("speedMbit", "number", true, "Speed in Mbits.", null, null)
                        .setDescriptor("macAddr", "string", true, "A media access control address.", null, null)
                        .setIsa(getFromMap("0d4c880d-d888-435f-b3a3-3d3eb9e70ddf")));
                addToMap(new TypeNode("WirelessNetworkHardware", "A wireless network device.", "61328f83-db43-4efe-81be-df47e17814a5", 0x0100)
                        .setIsa(getFromMap("86786ed6-d62a-4011-9ae8-0082000e48fa")));
                addToMap(new TypeNode("BluetoothHardware", "A Bluetooth device.", "009cf823-39c0-4b17-ae81-ad98ed0d4585", 0x002F)
                        .setDescriptor("macAddr", "string", true, "A media access control address.", null, null)
                        .setDescriptor("speedKbit", "number", true, "Speed in Kbits.", null, null)
                        .setIsa(getFromMap("61328f83-db43-4efe-81be-df47e17814a5")));
                addToMap(new TypeNode("IrdaHardware", "An IrDA device.", "42c7997c-0c56-4f93-956f-5fd0778c47c6", 0x0073)
                        .setDescriptor("speedKbit", "number", true, "Speed in Kbits.", null, null)
                        .setIsa(getFromMap("61328f83-db43-4efe-81be-df47e17814a5")));
                addToMap(new TypeNode("WifiHardware", "A WiFi device.", "78ff02be-2abe-4a8f-a082-a0d9c3cdae70", 0x00FB)
                        .setDescriptor("speedMbit", "number", true, "Speed in Mbits.", null, null)
                        .setDescriptor("macAddr", "string", true, "A media access control address", null, null)
                        .setIsa(getFromMap("61328f83-db43-4efe-81be-df47e17814a5")));
                addToMap(new TypeNode("WwanHardware", "A wireless WAN device.", "8d6e0956-033e-4efc-8846-e85bfddc8f98", 0x0101)
                        .setDescriptor("speedKbit", "number", true, "", null, null)
                        .setIsa(getFromMap("61328f83-db43-4efe-81be-df47e17814a5")));
                addToMap(new TypeNode("ProcessingUnit", "A processing unit.", "b7b2a2ff-3af7-469b-afbf-e52f1b80be91", 0x00C3)
                        .setDescriptor("cacheSizeKb", "number", true, "The size of cache memory on the CPU.", null, null)
                        .setDescriptor("cpuCoreCount", "number", true, "Number of cores.", null, null)
                        .setDescriptor("cpuClockHz", "number", true, "CPU clock in hertz.", null, null)
                        .setDescriptor("smtEnabled", "boolean", true, "Simultaneous multithreading (for example hyper-threading) " +
                                "capable true/false.", null, null)
                        .setDescriptor("bits", "number", true, "Number of bit width supported.", null, null)
                        .setIsa(getFromMap("ea6a7487-867c-4a74-8849-fb8539b8169f")));
                addToMap(new TypeNode("AlphaProcessor", "An Alpha processor.", "95b9f7a2-1a17-4cb9-8088-3e2e478ce638")
                        .setIsa(getFromMap("b7b2a2ff-3af7-469b-afbf-e52f1b80be91")));
                addToMap(new TypeNode("ArmProcessor", "An ARM processor.", "f3b1b497-1e83-4a09-a280-04f46a83fe24", 0x0028)
                        .setIsa(getFromMap("b7b2a2ff-3af7-469b-afbf-e52f1b80be91")));
                addToMap(new TypeNode("ItaniumProcessor", "An Itanium processor.", "241015c9-72f5-491f-a870-a30d8f7902fd", 0x0074)
                        .setIsa(getFromMap("b7b2a2ff-3af7-469b-afbf-e52f1b80be91")));
                addToMap(new TypeNode("M68kProcessor", "A Motorola 68000 series processor.", "33920524-4989-4322-a0ca-1cea614d7fef", 0x007A)
                        .setIsa(getFromMap("b7b2a2ff-3af7-469b-afbf-e52f1b80be91")));
                addToMap(new TypeNode("MipsProcessor", "A processor using the MIPS ISA.", "cbe81157-97f6-401b-bbaa-cfbc4f479298", 0x00A3)
                        .setIsa(getFromMap("b7b2a2ff-3af7-469b-afbf-e52f1b80be91")));
                addToMap(new TypeNode("MosProcessor", "A MOS Technology processor.", "b4f67698-7eca-4ca1-a4cf-b7ef9737785c", 0x00A5)
                        .setIsa(getFromMap("b7b2a2ff-3af7-469b-afbf-e52f1b80be91")));
                addToMap(new TypeNode("PariscProcessor", "A PA Risc processor.", "689a4f33-e08d-4e7f-83a5-79fd310bd3f9", 0x00C0)
                        .setIsa(getFromMap("b7b2a2ff-3af7-469b-afbf-e52f1b80be91")));
                addToMap(new TypeNode("PowerProcessor", "A PowerPC processor.", "808f2737-feca-4b70-aa1a-53410b970b10", 0x00C1)
                        .setIsa(getFromMap("b7b2a2ff-3af7-469b-afbf-e52f1b80be91")));
                addToMap(new TypeNode("SparcProcessor", "A SPARC processor.", "798ba8d1-8da7-471a-9efa-30530569b6a1", 0x00DC)
                        .setIsa(getFromMap("b7b2a2ff-3af7-469b-afbf-e52f1b80be91")));
                addToMap(new TypeNode("X86Processor", "An x86-based processor.", "4d4971b6-43b3-4c14-a82a-c51d72ba23a5", 0x0102)
                        .setIsa(getFromMap("b7b2a2ff-3af7-469b-afbf-e52f1b80be91")));
                addToMap(new TypeNode("ZilogProcessor", "A ZiLOG processor.", "a68f42c5-2a1d-43cb-bebd-55339f5e61ee", 0x0105)
                        .setIsa(getFromMap("b7b2a2ff-3af7-469b-afbf-e52f1b80be91")));
                addToMap(new TypeNode("Z80", "A ZiLOG Z80 processor.", "99ce31e0-d540-4582-ae38-b84f4bf13868")
                        .setIsa(getFromMap("a68f42c5-2a1d-43cb-bebd-55339f5e61ee"))
                        .fillDescriptor("cacheSizeKb", mapper.createObjectNode().put("cacheSizeKb", 0))
                        .fillDescriptor("cpuCoreCount", mapper.createObjectNode().put("cpuCoreCount", 1))
                        .fillDescriptor("cpuClockHz", mapper.createObjectNode().put("cpuClockHz", 8000))
                        .fillDescriptor("smtEnabled", mapper.createObjectNode().put("smtEnabled", false))
                        .fillDescriptor("bits", mapper.createObjectNode().put("bits", 8))
                        .fillDescriptor("rev", mapper.createObjectNode().put("rev", "H")));
                addToMap(new TypeNode("Software", "An abstract software component.", "8db2b492-9a4a-41fe-97a5-bc625da042ea", 0x00D6)
                        .setDescriptor("ver", "string", true, "The version of the software.", null, null)
                        .setDescriptor("shortName", "string", true, "A short lowercase name for the Software without spaces.", null,
                                null)
                        .setDescriptor("longName", "string", true, "The full descriptive name of the Software.", null, null)
                        .setIsa(getFromMap("a79ce509-a021-4177-ae61-73abd01fb479")));
                addToMap(new TypeNode("CloudSystem", "A cloud (operating) system and/or environment.",
                        "86c0439a-4219-46e6-82a3-6fb2ee774aa4", 0x003C)
                        .setIsa(getFromMap("8db2b492-9a4a-41fe-97a5-bc625da042ea")));
                addToMap(new TypeNode("OperatingSystem", "An operating system.", "f05e325e-1aee-4892-96e4-8bcd6e97c528", 0x00B8)
                        .setIsa(getFromMap("8db2b492-9a4a-41fe-97a5-bc625da042ea")));

                // Service managers
                addToMap(new TypeNode("ServiceManager", "An operating system initialization system.",
                        "c3008eec-b454-4ef6-bf7e-0bd92c13d3c3", 0x00CF)
                        .setIsa(getFromMap("8db2b492-9a4a-41fe-97a5-bc625da042ea")));
                addToMap(new TypeNode("Systemctl", "Systemd Service Manager.", "c409c9de-e23d-429b-a59a-ac46ea296331")
                        .setIsa(getFromMap("c3008eec-b454-4ef6-bf7e-0bd92c13d3c3")));
                addToMap(new TypeNode("Rc", "The rc service manager, used on most BSD's.", "67d7aa27-e0d9-458c-89fb-82734a8def95")
                        .setIsa(getFromMap("c3008eec-b454-4ef6-bf7e-0bd92c13d3c3")));
                addToMap(new TypeNode("Services", "Windows Service Manager.", "21b82fbf-803c-4056-b914-502e2ff9952d", 0x00CE)
                        .setIsa(getFromMap("c3008eec-b454-4ef6-bf7e-0bd92c13d3c3")));

                addToMap(new TypeNode("PackageManager", "A package manager.", "e2b8b9ee-b66f-4252-8fba-4fb172f741bc", 0x00BE)
                        .setIsa(getFromMap("8db2b492-9a4a-41fe-97a5-bc625da042ea")));
                addToMap(new TypeNode("Interpreter", "A command or programming language interpreter.",
                        "d047f415-734c-4bd3-92b9-d68d5521ce69", 0x006C)
                        .setIsa(getFromMap("8db2b492-9a4a-41fe-97a5-bc625da042ea")));
                addToMap(new TypeNode("Hypervisor", "A virtualization hypervisor.", "c5c7fcb5-8ac3-40cc-b195-b2b6af368e81", 0x0067)
                        .setIsa(getFromMap("8db2b492-9a4a-41fe-97a5-bc625da042ea")));
                addToMap(new TypeNode("Emulator", "A hardware platform emulator.", "59aeae81-9741-4986-b9c2-931504cfc9e3", 0x0051)
                        .setIsa(getFromMap("8db2b492-9a4a-41fe-97a5-bc625da042ea")));
                addToMap(new TypeNode("Server", "A generic local/networked/daemonized server.",
                        "3fb4603b-281e-47c0-b338-768ea05d4660", 0x00CE)
                        .setIsa(getFromMap("8db2b492-9a4a-41fe-97a5-bc625da042ea")));
                addToMap(new TypeNode("Client", "A generic client.", "8f371b1b-52a3-4409-be70-9cd947bec8dd")
                        .setIsa(getFromMap("8db2b492-9a4a-41fe-97a5-bc625da042ea")));
                addToMap(new TypeNode("Grouping", "A group of references to things.", "5f1eb7c5-ec5e-4563-be6a-fc50774fc374", 0x005F)
                        .setIsa(getFromMap("b1bc963a-19cc-4518-aecb-a35d028c49e3")));
                addToMap(new TypeNode("Locational", "A place/location.", "f31b8dc9-7b9a-47cc-bd3d-20b94b6daac1", 0x0079)
                        .setDescriptor("code", "string", false, "An optional code designation.", null, null)
                        .setIsa(getFromMap("5f1eb7c5-ec5e-4563-be6a-fc50774fc374")));
                addToMap(new TypeNode("Building", "A building.", "44197148-678f-4421-a618-dc6807beaf30", 0x0035)
                        .setDescriptor("city", "string", true, "The city where the Building resides.", null, null)
                        .setDescriptor("streetAddress", "string", true, "The street address and number (if any) of the Building.",
                                null, null)
                        .setDescriptor("country", "string", true, "The country where the Building resides", null, null)
                        .setDescriptor("postalCode", "string", false, "'The postal code or the Building.", null, null)
                        .setIsa(getFromMap("f31b8dc9-7b9a-47cc-bd3d-20b94b6daac1")));
                addToMap(new TypeNode("Floor", "A floor.", "9ca8061a-898c-4f2f-aeb3-c77d1747559f", 0x0056)
                        .setIsa(getFromMap("f31b8dc9-7b9a-47cc-bd3d-20b94b6daac1")));
                addToMap(new TypeNode("Room", "A room.", "94721751-ab2f-4285-9c33-cd91cb919430", 0x00CB)
                        .setIsa(getFromMap("f31b8dc9-7b9a-47cc-bd3d-20b94b6daac1")));
                addToMap(new TypeNode("Organizational", "An organizational group.", "c94c2c32-8e10-48c3-a31b-6337c2ad4df4", 0x00BD)
                        .setIsa(getFromMap("5f1eb7c5-ec5e-4563-be6a-fc50774fc374")));
                addToMap(new TypeNode("Company", "A company.", "d7582691-8ccc-415c-886b-7d363c8b6e84", 0x003D)
                        .setIsa(getFromMap("c94c2c32-8e10-48c3-a31b-6337c2ad4df4")));
                addToMap(new TypeNode("Department", "A department.", "aa6bc876-33fe-4710-b409-edb31e1081de", 0x0047)
                        .setIsa(getFromMap("c94c2c32-8e10-48c3-a31b-6337c2ad4df4")));
                addToMap(new TypeNode("Team", "A team.", "9d68148a-28ec-4f24-b1d7-0fc2d407ab78", 0x00E6)
                        .setIsa(getFromMap("c94c2c32-8e10-48c3-a31b-6337c2ad4df4")));
                addToMap(new TypeNode("Componental", "A group for organizing references to components.",
                        "ee92c308-80f4-40b2-af3a-77d26cd99517", 0x003F)
                        .setIsa(getFromMap("5f1eb7c5-ec5e-4563-be6a-fc50774fc374")));
                addToMap(new TypeNode("Computer", "An assembled computer device.", "ec11db91-6973-4285-b018-b461fbb432d7", 0x0040)
                        .setIsa(getFromMap("ee92c308-80f4-40b2-af3a-77d26cd99517")));
                addToMap(new TypeNode("Actor", "Something or someone that interacts/starts/acts within the system.",
                        "3175d762-dd97-434f-a288-21e4591480cf", 0x0022)
                        .setIsa(getFromMap("b1bc963a-19cc-4518-aecb-a35d028c49e3")));
                addToMap(new TypeNode("User", "A user.", "88d8630d-54ce-4aa5-8505-2068c31df5b1", 0x00F2)
                        .setDescriptor("emailAddress", "string", true, "The email address of the User.", null, null)
                        .setDescriptor("title", "string", false, "The title of the user.", null, null)
                        .setDescriptor("firstName", "string", true, "The first name of the User.", null, null)
                        .setDescriptor("lastName", "string", true, "The last name of the User.", null, null)
                        .setDescriptor("birthDate", "number", false, "The birth date of the User", null, null)
                        .setIsa(getFromMap("3175d762-dd97-434f-a288-21e4591480cf")));
                addToMap(new TypeNode("Batch", "A batch user.", "395d3e08-842b-4640-8b57-751e1b0c9e4b", 0x002D)
                        .setDescriptor("emailAddress", "string", true, "The email address of the Batch user.", null, null)
                        .setIsa(getFromMap("3175d762-dd97-434f-a288-21e4591480cf")));

                // Capabilities
            ObjectNode systemFalse = mapper.createObjectNode().put("system", false);
        ObjectNode systemTrue = mapper.createObjectNode().put("system", true);
                addToMap(new TypeNode("Capability", "A software component gives capabilities to its parent or to siblings.",
                        "916e04a9-c773-4eaf-9c08-450016426e5a", 0x0036)
                        .setDescriptor("system", "boolean", true, "Is this capability for internal use?", null, null)
                        .setIsa(getFromMap("b1bc963a-19cc-4518-aecb-a35d028c49e3")));
                addToMap(new TypeNode("State", "A capability that makes it possible to state-check.",
                        "b3cfd296-1090-43c0-ac2e-00e5d7fe7779", 0x0039)
                        .setIsa(getFromMap("916e04a9-c773-4eaf-9c08-450016426e5a"))
                        .fillDescriptor("system", systemFalse));
                addToMap(new TypeNode("Stop", "A capability that makes it possible to stop something.",
                        "632ca701-2648-4df5-87ee-a41d3d1ef3da", 0x00E0)
                        .setIsa(getFromMap("916e04a9-c773-4eaf-9c08-450016426e5a"))
                        .fillDescriptor("system", systemFalse));
                addToMap(new TypeNode("Start", "A capability that makes it possible to stop something.",
                        "347ca5da-db19-4d98-9fed-60e72d5d6bba", 0x00DF)
                        .setIsa(getFromMap("916e04a9-c773-4eaf-9c08-450016426e5a"))
                        .fillDescriptor("system", systemFalse));
                addToMap(new TypeNode("Restart", "A capability that makes it possible to restart something.",
                        "cc558715-63dc-4162-95a4-9c51792c7953", 0x00C8)
                        .setIsa(getFromMap("916e04a9-c773-4eaf-9c08-450016426e5a"))
                        .fillDescriptor("system", systemFalse));
                addToMap(new TypeNode("Install", "A capability that makes it possible to install something.",
                        "3c077211-0ffa-452f-8d84-5bd5be5598f5", 0x006B)
                        .setIsa(getFromMap("916e04a9-c773-4eaf-9c08-450016426e5a"))
                        .fillDescriptor("system", systemFalse));
                addToMap(new TypeNode("Configure", "A capability that makes it possible to configure something.",
                        "3dc7b06a-69a0-4650-897a-34c5d069e3c5", 0x0041)
                        .setIsa(getFromMap("916e04a9-c773-4eaf-9c08-450016426e5a"))
                        .fillDescriptor("system", systemFalse));
                addToMap(new TypeNode("Uninstall", "A capability that makes it possible to uninstall something.",
                        "ea1b32de-cf31-4c1b-9b2b-f1a6d9983418", 0x00EB)
                        .setIsa(getFromMap("916e04a9-c773-4eaf-9c08-450016426e5a"))
                        .fillDescriptor("system", systemFalse));
                addToMap(new TypeNode("CreateVm", "A capability that makes it possible to create a virtual machine.",
                        "b0995f9f-90af-4066-9d00-6b7b33f9a19b", 0x00F4)
                        .setIsa(getFromMap("916e04a9-c773-4eaf-9c08-450016426e5a"))
                        .fillDescriptor("system", systemFalse));
                addToMap(new TypeNode("DestroyVm", "A capability that makes it possible to destroy a virtual machine.",
                        "cdb08b91-5998-4f7c-9b82-b345b8052ecd", 0x00F4)
                        .setIsa(getFromMap("916e04a9-c773-4eaf-9c08-450016426e5a"))
                        .fillDescriptor("system", systemFalse));
                addToMap(new TypeNode("Execute", "A capability that makes it possible to run commands.",
                        "76fb3116-d4a8-459a-b62b-a2641bcb7e7d", 0x0054)
                        .setIsa(getFromMap("916e04a9-c773-4eaf-9c08-450016426e5a"))
                        .fillDescriptor("system", systemTrue));
                addToMap(new TypeNode("Discover", "A capability that makes it possible to discover something.",
                        "3c170a27-05ee-4999-97d4-e7cc9135cb96", 0x0049)
                        .setIsa(getFromMap("916e04a9-c773-4eaf-9c08-450016426e5a"))
                        .fillDescriptor("system", systemFalse));
                addToMap(new TypeNode("Identify", "A capability that makes it possible to identify (version, name, etc) something.",
                        "ca49051c-d7d1-45fa-b327-0f4dca810897", 0x0069)
                        .setIsa(getFromMap("916e04a9-c773-4eaf-9c08-450016426e5a"))
                        .fillDescriptor("system", systemFalse));
                addToMap(new TypeNode("SendString", "A capability that makes it possible to send a string over a protocol.",
                        "64ca8ff9-8071-4933-95d0-bbacb97bd4a8")
                        .setIsa(getFromMap("916e04a9-c773-4eaf-9c08-450016426e5a"))
                        .fillDescriptor("system", systemTrue));
                addToMap(new TypeNode("ReceiveString", "A capability that makes it possible to receive a string over a protocol.",
                        "8292da1f-75de-41d6-a8fb-cdcca6700ad9")
                        .setIsa(getFromMap("916e04a9-c773-4eaf-9c08-450016426e5a"))
                        .fillDescriptor("system", systemTrue));
                addToMap(new TypeNode("SendFile", "A capability that makes it possible to send a file over a protocol.",
                        "371d336a-d328-4468-9b0f-40877bb5b646")
                        .setIsa(getFromMap("916e04a9-c773-4eaf-9c08-450016426e5a"))
                        .fillDescriptor("system", systemTrue));
                addToMap(new TypeNode("ReceiveFile", "A capability that makes it possible to receive a file over a protocol.",
                        "f2ba58a6-0934-4f0e-b9cf-1a7430a3fa7a")
                        .setIsa(getFromMap("916e04a9-c773-4eaf-9c08-450016426e5a"))
                        .fillDescriptor("system", systemTrue));
                addToMap(new TypeNode("Invoke", "A capability that makes it possible to invoke a command to execute.",
                        "4251040d-f05c-4711-8094-59ce773a8c35", 0x00CC)
                        .setIsa(getFromMap("916e04a9-c773-4eaf-9c08-450016426e5a"))
                        .fillDescriptor("system", systemTrue));
                addToMap(new TypeNode("Access", "A capability that makes it possible to access a textual or graphical console.",
                        "20d9b0db-92b2-42a7-93d9-f86e00312fef", 0x0021)
                        .setIsa(getFromMap("916e04a9-c773-4eaf-9c08-450016426e5a"))
                        .fillDescriptor("system", systemFalse));

                // Protocols
                addToMap(new TypeNode("Protocol", "A protocol.", "db0c3c65-52fd-4dec-907c-6c8a669e88f3", 0x00C4)
                        .setIsa(getFromMap("b1bc963a-19cc-4518-aecb-a35d028c49e3")));
                addToMap(new TypeNode("HTTP", "The hypertext transfer protocol.", "9fd04885-179f-4c72-99a2-52c692e61b19", 0x0065)
                        .setIsa(getFromMap("db0c3c65-52fd-4dec-907c-6c8a669e88f3")));
                addToMap(new TypeNode("HTTP09", "The hypertext transfer protocol, version 0.9.",
                        "09102127-1803-48b6-a9f4-2fd1b891293d")
                        .setIsa(getFromMap("9fd04885-179f-4c72-99a2-52c692e61b19")));
                addToMap(new TypeNode("HTTP10", "The hypertext transfer protocol, version 1.0.",
                        "256d493d-3b7a-4e28-8e64-8f81de4d92de")
                        .setIsa(getFromMap("9fd04885-179f-4c72-99a2-52c692e61b19")));
                addToMap(new TypeNode("HTTP11", "The hypertext transfer protocol, version 1.1.",
                        "b11b10d7-6f46-49c0-bc61-3ecdd4342cd7")
                        .setIsa(getFromMap("9fd04885-179f-4c72-99a2-52c692e61b19")));
                addToMap(new TypeNode("SMB", "The server message block protocol.", "0771de77-c3d8-4db1-92fa-7ac96205f9bb", 0x00D3)
                        .setIsa(getFromMap("db0c3c65-52fd-4dec-907c-6c8a669e88f3")));
                addToMap(new TypeNode("SMB10", "The server message block protocol, version 1.0.",
                        "70960db9-f085-499d-9d22-2569badc41c7")
                        .setIsa(getFromMap("0771de77-c3d8-4db1-92fa-7ac96205f9bb")));
                addToMap(new TypeNode("SMB20", "The server message block protocol, version 2.0.",
                        "aeefff52-0300-4611-be60-e745bfbd3d37")
                        .setIsa(getFromMap("0771de77-c3d8-4db1-92fa-7ac96205f9bb")));
                addToMap(new TypeNode("SMB21", "The server message block protocol, version 2.1.",
                        "5c7b0507-dfd9-4617-a45f-3d617cd61e68")
                        .setIsa(getFromMap("0771de77-c3d8-4db1-92fa-7ac96205f9bb")));
                addToMap(new TypeNode("SMB30", "The server message block protocol, version 3.0.",
                        "dbfb220e-b22c-460e-b27d-65de22501618")
                        .setIsa(getFromMap("0771de77-c3d8-4db1-92fa-7ac96205f9bb")));
                addToMap(new TypeNode("SSH", "The secure shell protocol.", "008b9566-6317-4ddd-977c-9ddca00b79b8", 0x00DD)
                        .setIsa(getFromMap("db0c3c65-52fd-4dec-907c-6c8a669e88f3")));
                addToMap(new TypeNode("SSH10", "The secure shell protocol, version 1.0.", "000b0fd3-a121-4194-a441-0857abca490d")
                        .setIsa(getFromMap("008b9566-6317-4ddd-977c-9ddca00b79b8")));
                addToMap(new TypeNode("SSH20", "The secure shell protocol, version 2.0.", "07b23795-140f-4a72-9c86-e1d3b86cd149")
                        .setIsa(getFromMap("008b9566-6317-4ddd-977c-9ddca00b79b8")));
                addToMap(new TypeNode("TELNET", "The telnet protocol.", "27fc7ed2-beb3-41c8-aa69-900ad7bb989f", 0x00E7)
                        .setIsa(getFromMap("db0c3c65-52fd-4dec-907c-6c8a669e88f3")));

                addToMap(new TypeNode("Instrument", "A representation of a software instrument that gives end user function.",
                        "14dbfbfa-b50a-466f-8539-55d5d3f15e4a", 0x0060)
                        .setDescriptor("fileLocation", "string", true, "The filesystem location of the instrument.", null, null)
                        .setDescriptor("type", "string", true, "The type of instrument, this can be \"runner|marker|parser\".", null, "^runner|marker|parser$")
                        .setDescriptor("weight", "number", false, "The weight of a instrument, used choose between instruments.", null, null)
                        .setDescriptor("extension", "string", false, "Possible extension in case of a scriptable instrument.", null, "^\\.[a-zA-Z0-9]+$")
                        .setDescriptor("arguments", "string", false, "A string representation of the arguments object.", null, null)
                        .setDescriptor("subjectIsLocal", "boolean", true, "Is the subject for this instrument local or remote?", null, null)
                        .setDescriptor("template", "string", true, "The handlebars template resulting in a command.", null, null)
                        .setDescriptor("resultParser", "string", false, "The parser able to convert the raw results of this instrument.", null, null)
                        .setIsa(getFromMap("b1bc963a-19cc-4518-aecb-a35d028c49e3")));
                addToMap(new TypeNode("Result", "A result from an operation.", "f3498c02-58c6-486b-87c6-9d54607fdb12", 0x00C9)
                        .setDescriptor("task", "string", true, "The task ID.", null, null)
                        .setDescriptor("process", "string", false, "Process that resulted in this result.", null, null)
                        .setDescriptor("returnCode", "number", false, "The returncode of a process", null, null)
                        .setDescriptor("stdout", "string", false, "The stdout of a program.", null, null)
                        .setDescriptor("stderr", "string", false, "The stdout of a program.", null, null)
                        .setDescriptor("dateCreated", "number", true, "The date + time when the result was created.", null, null)
                        .setIsa(getFromMap("b1bc963a-19cc-4518-aecb-a35d028c49e3")));
    }
}
