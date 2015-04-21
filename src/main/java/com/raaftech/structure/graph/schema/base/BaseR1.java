package com.raaftech.structure.graph.schema.base;

import com.raaftech.structure.graph.SchemaNode;
import com.raaftech.structure.graph.TypeNode;
import com.raaftech.structure.graph.schema.SchemaUpdate;

import java.util.Arrays;

/**
 * Give a description of Base here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-12
 */
public class BaseR1 extends SchemaUpdate {

    protected void setUpdateInformation() {
        updateInformation = SchemaNode.getOrCreateSchemaNode("BaseModel", "The first model object, all base types.",
                "1d924647-6815-4772-a38d-ca86717185ee", 1);
    }

    protected void initNodes() {
        // raaftech.com.model.base
        typeNodeList = Arrays.asList(
	        new TypeNode("Type", "The root type.", "b1bc963a-19cc-4518-aecb-a35d028c49e3"),
	        new TypeNode("Component", "An abstract base component.", "a79ce509-a021-4177-ae61-73abd01fb479")
	                .setIsa("b1bc963a-19cc-4518-aecb-a35d028c49e3"),
	        new TypeNode("Configuration", "An abstract configuration component.", "3f4a43f7-b558-4bf0-ba7e" +
	                "-66b99cffaf16")
	                .setIsa("a79ce509-a021-4177-ae61-73abd01fb479"),
	        new TypeNode("IpAddress", "An IP address (address only, can be assigned to a network interface).",
	                "299001a6-1091-4c0d-87be-c512af03e179")
	                .setDescriptor("addr", "string", true, "IP addresses should at least have the addr field.", null, null)
	                .setIsa("3f4a43f7-b558-4bf0-ba7e-66b99cffaf16"),
	        new TypeNode("Ipv4Address", "An IPv4 address (address only, can be assigned to a network " +
	                "interface).",
	                "f7fbe917-9d49-4df5-95f6-6e132067a424")
	                .setIsa("299001a6-1091-4c0d-87be-c512af03e179"),
	        new TypeNode("Ipv6Address", "An IPv6 address (address only, can be assigned to a network interface).",
	                "47b5804d-5f5d-4bf8-89f6-a189c29b05ae")
	                .setIsa("299001a6-1091-4c0d-87be-c512af03e179"),
	        new TypeNode("IpListener", "An IP listener (address and port, can be assigned to a network service).",
	                "91c999a0-33ec-42ee-bae0-6f8adae42535")
	                .setDescriptor("addr", "string", true, "IP listeners should at least have the addr and port fields.", null,
							null)
	                .setDescriptor("port", "number", true, "IP listeners should at least have the addr and port fields.", null,
							null)
	                .setIsa("3f4a43f7-b558-4bf0-ba7e-66b99cffaf16"),
	        new TypeNode("Ipv4Listener", "An IPv4 listener (address and port, can be assigned to a network service).",
	                "5ca95f7a-5175-4e7f-8a61-862e24485af3")
	                .setIsa("91c999a0-33ec-42ee-bae0-6f8adae42535"),
	        new TypeNode("Ipv6Listener", "An IPv6 listener (address and port, can be assigned to a network service).",
	                "6a79b4e9-13ce-4a09-bc5b-b429d23b34d7")
	                .setIsa("91c999a0-33ec-42ee-bae0-6f8adae42535"),
	        new TypeNode("Url", "a uniform resource locator.", "0e9f72bd-5bc5-43dc-ab86-30a8f7171bc6")
	                .setDescriptor("url", "string", true, "scheme://domain:port/path?query_string#fragment_id", null, null)
	                .setIsa("3f4a43f7-b558-4bf0-ba7e-66b99cffaf16"),
	        new TypeNode("DnsName", "Either a short name or a fully-qualified domain name.",
	                "99c0da37-3599-418d-8ecc-de36d37ae9f4")
	                .setDescriptor("dnsname", "string", true, "somename", null, null)
	                .setIsa("3f4a43f7-b558-4bf0-ba7e-66b99cffaf16"),
	        new TypeNode("Map", "A configuration object that maps keys to multi-valued attributes in the JSON format" +
	                ".", "c970f6d3-4898-4596-bfac-9d6d1f6b2541")
	                .setDescriptor("map", "string", true, "JSON string containing the Map.", null, null)
	                .setIsa("3f4a43f7-b558-4bf0-ba7e-66b99cffaf16"),
	        new TypeNode("DirectoryMap", "A generic map of possible directories this software has. Key: os-shortname, " +
	                "Values: list of directories, Format: JSON.", "64d50e90-8be2-4d97-9dba-dd8518eb3f84")
	                .setIsa("c970f6d3-4898-4596-bfac-9d6d1f6b2541"),
	        new TypeNode("PrefixDirectoryMap", "A map of possible/known prefix directories for this software. Key: " +
	                "os-shortname, Values: list of directory prefixes, Format: JSON.", "d47e9ecd-d735-49a0-8d94" +
	                "-c4cb948ea226")
	                .setIsa("64d50e90-8be2-4d97-9dba-dd8518eb3f84"),
	        new TypeNode("ConfigDirectoryMap", "A map of possible/known configuration directories for this software. " +
	                "Key: os-shortname, Values: list of config directories, Format: JSON.", "fb83aefa-ec5e-4ad9-a807" +
	                "-4a37ffaa404b")
	                .setIsa("64d50e90-8be2-4d97-9dba-dd8518eb3f84"),
	        new TypeNode("BinaryDirectoryMap", "A map of possible/known binary executable directories for this " +
	                "software. Key: os-shortname, Values: list of binary directories, Format: JSON.",
	                "33e5e211-88db-4a78-95cd-2f2a3cd9e297")
	                .setIsa("64d50e90-8be2-4d97-9dba-dd8518eb3f84"),
	        new TypeNode("LibraryDirectoryMap", "A map of possible/known dynamic link library directories for this " +
	                "software. Key: os-shortname, Values: list of library directories, Format: JSON.",
	                "8ccf2b1c-3650-408f-8560-4ab9f3a57bbe")
	                .setIsa("64d50e90-8be2-4d97-9dba-dd8518eb3f84"),
	        new TypeNode("DataDirectoryMap", "A map of possible/known data directories for this software. Key: " +
	                "os-shortname, Values: list of data directories, Format: JSON.", "07aaa5dd-a747-4474-850d-01ad00a2f533")
	                .setIsa("64d50e90-8be2-4d97-9dba-dd8518eb3f84"),
	        new TypeNode("LogDirectoryMap", "A map of possible/known log directories for this software. Key: " +
	                "os-shortname, Values: list of log directories, Format: JSON.", "f8a9ce96-2a50-4a80-8deb-ec6fb294638e")
	                .setIsa("64d50e90-8be2-4d97-9dba-dd8518eb3f84"),
	        new TypeNode("FileMap", "A generic map of possible/known files this software has. Key: os-shortname, " +
	                "Values: list of files, Format: JSON.", "35407e44-ec50-4d66-83f9-dacf1f1b49dd")
	                .setIsa("c970f6d3-4898-4596-bfac-9d6d1f6b2541"),
	        new TypeNode("ConfigFileMap", "A map of possible/known configuration files for this software, non-full " +
	                "path, checked within ConfigDirectoryMap entries.", "a6125034-55e0-4604-bf21-407837d0b419")
	                .setIsa("35407e44-ec50-4d66-83f9-dacf1f1b49dd"),
	        new TypeNode("BinaryFileMap", "A map of possible/known binary executable files for this software, non-full" +
	                " path, checked within BinaryDirectoryMap entries.", "5ffab714-fa0e-416c-a237-9b023fe27bd7")
	                .setIsa("35407e44-ec50-4d66-83f9-dacf1f1b49dd"),
	        new TypeNode("LibraryFileMap", "A map of possible/known dynamic link library files for this software, " +
	                "non-full path, checked within LibraryDirectoryMap entries.", "99a6e35b-912e-41f1-b404-d24299e51fb2")
	                .setIsa("35407e44-ec50-4d66-83f9-dacf1f1b49dd"),
	        new TypeNode("DataFileMap", "A map of possible/known data files for this software, non-full path, checked " +
	                "within DataDirectoryMap entries.", "b19412ae-dffe-4fe8-9f00-3207ab686fb4")
	                .setIsa("35407e44-ec50-4d66-83f9-dacf1f1b49dd"),
	        new TypeNode("LogFileMap", "A map of possible/known log files for this software, non-full path, checked " +
	                "within LogDirectoryMap entries.", "ac583c10-f77c-4d40-84eb-4ffed77c79e0")
	                .setIsa("35407e44-ec50-4d66-83f9-dacf1f1b49dd"),
	        new TypeNode("NameMap", "A generic map of possible/known names this software has. Key: os-shortname, " +
	                "Values: list of names, Format: JSON.", "14372d63-a428-46b9-8359-9982ef22cfce")
	                .setIsa("c970f6d3-4898-4596-bfac-9d6d1f6b2541"),
	        new TypeNode("PackageNameMap", "A generic map of possible/known package names this software has. Key: " +
	                "os-shortname, Values: list of names, Format: JSON.", "1d691f61-3c9f-46ad-b98b-b968352d59b3")
	                .setIsa("14372d63-a428-46b9-8359-9982ef22cfce"),
	        new TypeNode("ServiceNameMap", "A generic map of possible/known service names this software has. Key: " +
	                "os-shortname, Values: list of names, Format: JSON.", "ead61277-e797-406b-96e1-95c155655fac")
	                .setIsa("14372d63-a428-46b9-8359-9982ef22cfce"),
	        new TypeNode("StringMap", "A generic map of possible/known strings this software has. Key: *, Values: " +
	                "string pattern, Format: JSON.", "0d554a04-67d6-428f-8e6f-6cdcdaca39cb")
	                .setIsa("c970f6d3-4898-4596-bfac-9d6d1f6b2541"),
	        new TypeNode("VersionStringMap", "A map of possible/known version strings this software has. Key: *, " +
	                "Values: version string pattern, Format: JSON.", "1c1af482-f0e0-45f1-9aa1-f7702040067d")
	                .setIsa("0d554a04-67d6-428f-8e6f-6cdcdaca39cb"),
	        new TypeNode("Device", "An abstract device component.", "ea6a7487-867c-4a74-8849-fb8539b8169f")
	                .setDescriptor("rev", "string", false, "The revision of the device.", null, null)
	                .setIsa("a79ce509-a021-4177-ae61-73abd01fb479"),
	        new TypeNode("AssembledDevice", "An assembled device, containing various components.",
	                "dbb2884f-7c31-4f7d-a92e-d6a76dd1b7fe")
	                .setIsa("ea6a7487-867c-4a74-8849-fb8539b8169f"),
	        new TypeNode("Computer", "An assembled computer device.", "ec11db91-6973-4285-b018-b461fbb432d7")
	                .setIsa("dbb2884f-7c31-4f7d-a92e-d6a76dd1b7fe"),
	        new TypeNode("ConsoleDevice", "A console device.", "3d0865a2-3ecf-4035-886b-0bb603c0e2c2")
	                .setDescriptor("baudRate", "number", false, "110,300,600,1200,2400,4800,9600,14400,19200,28800,38400,56000," +
							"57600 or 115200.", null, null)
	                .setDescriptor("stopBits", "number", false, "1, 1.5 or 2.", null, null)
	                .setDescriptor("parity", "string", false, "none (N), odd (O), even (E), mark (M), or space (S).", null, null)
	                .setDescriptor("dataBits", "number", false, "5,6,7,8 or 9.", null, null)
	                .setIsa("ea6a7487-867c-4a74-8849-fb8539b8169f"),
	        new TypeNode("GenericSerial", "A serial communication device.",
	                "f5b25736-ec12-4a7e-b198-548b97ebc36f")
	                .setIsa("3d0865a2-3ecf-4035-886b-0bb603c0e2c2"),
	        new TypeNode("GraphicsDevice", "A graphics device.", "a642b995-c073-4c27-a611-c5bf3854958f")
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
	                .setIsa("ea6a7487-867c-4a74-8849-fb8539b8169f"),
	        new TypeNode("GenericVga", "A VGA graphics device.", "b3a2ac7c-ad8a-4047-a460-2ac41a7ddfef")
	                .setIsa("a642b995-c073-4c27-a611-c5bf3854958f"),
	        new TypeNode("MemoryDevice", "A memory device", "1c87c672-1f69-4a02-9bc8-9eff28dda5e5")
	                .setDescriptor("memSizeKb", "number", true, "Memory size in Kb.", null, null)
	                .setIsa("ea6a7487-867c-4a74-8849-fb8539b8169f"),
	        new TypeNode("VolatileMemory", "A generic volatile memory device.", "44131d46-c1b0-4030-ae5a" +
	                "-9a3108627749")
	                .setDescriptor("memClockHz", "number", true, "Memory speed in Hz.", null, null)
	                .setIsa("1c87c672-1f69-4a02-9bc8-9eff28dda5e5"),
	        new TypeNode("RandomAccessMemory", "Random access memory.", "4c0a0638-1fa7-4f5b-9d69-1d199dcedccc")
	                .setIsa("44131d46-c1b0-4030-ae5a-9a3108627749"),
	        new TypeNode("NonVolatileMemory", "A generic non-volatile memory device.",
	                "7f129e53-ff07-4ed8-9062-daf5f733a3a6")
	                .setIsa("1c87c672-1f69-4a02-9bc8-9eff28dda5e5"),
	        new TypeNode("HardDiskDrive", "A non-volatile harddisk-based storage device.",
	                "24cc3545-0d1a-4d85-8d08-a83c3dbe5912")
	                .setIsa("7f129e53-ff07-4ed8-9062-daf5f733a3a6"),
	        new TypeNode("SolidStateDrive", "A non-volatile solid-state storage device.",
	                "7a60ec32-0a9a-40b9-bf1f-d69674d4a233")
	                .setIsa("7f129e53-ff07-4ed8-9062-daf5f733a3a6"),
	        new TypeNode("FloppyDrive", "A floppy disk drive.", "118ec961-a823-465b-be17-b706f1c8c2f1")
	                .setIsa("7f129e53-ff07-4ed8-9062-daf5f733a3a6"),
	        new TypeNode("TapeDrive", "A tape drive.", "a507461f-647b-4b79-936a-721ca2dba145")
	                .setIsa("7f129e53-ff07-4ed8-9062-daf5f733a3a6"),
	        new TypeNode("CdDrive", "A Compact Disc drive.", "30af2fe3-aa05-4912-a3d5-dddfdacf74e2")
	                .setIsa("7f129e53-ff07-4ed8-9062-daf5f733a3a6"),
	        new TypeNode("DvdDrive", "A Digital Video Disc drive.", "23ab95d2-2815-4121-a81c-63aca40cdcfa")
	                .setIsa("7f129e53-ff07-4ed8-9062-daf5f733a3a6"),
	        new TypeNode("BdDrive", "A Digital Video Disc drive.", "cbc27482-11d1-4890-a3e1-e9f80baea82c")
	                .setIsa("7f129e53-ff07-4ed8-9062-daf5f733a3a6"),
	        new TypeNode("NetworkDevice", "A network device.", "86786ed6-d62a-4011-9ae8-0082000e48fa")
	                .setIsa("ea6a7487-867c-4a74-8849-fb8539b8169f"),
	        new TypeNode("WiredNetworkDevice", "A wired network device.", "0d4c880d-d888-435f-b3a3-3d3eb9e70ddf")
	                .setIsa("86786ed6-d62a-4011-9ae8-0082000e48fa"),
	        new TypeNode("ModemDevice", "A modem networking device.", "f34b76e4-f5a6-4bae-968e-f22ef09b1e35")
	                .setDescriptor("baudRate", "number", true, "110,300,600,1200,2400,4800,9600,14400,19200,28800,38400,56000," +
							"57600 or 115200.", null, null)
	                .setIsa("0d4c880d-d888-435f-b3a3-3d3eb9e70ddf"),
	        new TypeNode("EthernetDevice", "A wired ethernet device.", "0dbd26f2-baff-446c-9241-46eaa3e03736")
	                .setDescriptor("speedMbit", "number", true, "Speed in Mbits.", null, null)
	                .setDescriptor("macAddr", "string", true, "A media access control address.", null, null)
	                .setIsa("0d4c880d-d888-435f-b3a3-3d3eb9e70ddf"),
	        new TypeNode("WirelessNetworkDevice", "A wireless network device.", "61328f83-db43-4efe-81be-df47e17814a5")
	                .setIsa("86786ed6-d62a-4011-9ae8-0082000e48fa"),
	        new TypeNode("BluetoothDevice", "A Bluetooth device.", "009cf823-39c0-4b17-ae81-ad98ed0d4585")
	                .setDescriptor("macAddr", "string", true, "A media access control address.", null, null)
	                .setDescriptor("speedKbit", "number", true, "Speed in Kbits.", null, null)
	                .setIsa("61328f83-db43-4efe-81be-df47e17814a5"),
	        new TypeNode("IrdaDevice", "An IrDA device.", "42c7997c-0c56-4f93-956f-5fd0778c47c6")
	                .setDescriptor("speedKbit", "number", true, "Speed in Kbits.", null, null)
	                .setIsa("61328f83-db43-4efe-81be-df47e17814a5"),
	        new TypeNode("WifiDevice", "A WiFi device.", "78ff02be-2abe-4a8f-a082-a0d9c3cdae70")
	                .setDescriptor("speedMbit", "number", true, "Speed in Mbits.", null, null)
	                .setDescriptor("macAddr", "string", true, "A media access control address", null, null)
	                .setIsa("61328f83-db43-4efe-81be-df47e17814a5"),
	        new TypeNode("WwanDevice", "A wireless WAN device.", "8d6e0956-033e-4efc-8846-e85bfddc8f98")
	                .setDescriptor("speedKbit", "number", true, "", null, null)
	                .setIsa("61328f83-db43-4efe-81be-df47e17814a5"),
	        new TypeNode("ProcessingUnit", "A processing unit.", "b7b2a2ff-3af7-469b-afbf-e52f1b80be91")
	                .setDescriptor("cacheSizeKb", "number", true, "The size of cache memory on the CPU.", null, null)
	                .setDescriptor("cpuCoreCount", "number", true, "Number of cores.", null, null)
	                .setDescriptor("cpuClockHz", "number", true, "CPU clock in hertz.", null, null)
	                .setDescriptor("smtEnabled", "boolean", true, "Simultaneous multithreading (for example hyper-threading) " +
							"capable true/false.", null, null)
	                .setDescriptor("bits", "number", true, "Number of bit width supported.", null, null)
	                .setIsa("ea6a7487-867c-4a74-8849-fb8539b8169f"),
	        new TypeNode("AlphaProcessor", "An Alpha processor.", "95b9f7a2-1a17-4cb9-8088-3e2e478ce638")
	                .setIsa("b7b2a2ff-3af7-469b-afbf-e52f1b80be91"),
	        new TypeNode("ArmProcessor", "An ARM processor.", "f3b1b497-1e83-4a09-a280-04f46a83fe24")
	                .setIsa("b7b2a2ff-3af7-469b-afbf-e52f1b80be91"),
	        new TypeNode("ItaniumProcessor", "An Itanium processor.", "241015c9-72f5-491f-a870-a30d8f7902fd")
	                .setIsa("b7b2a2ff-3af7-469b-afbf-e52f1b80be91"),
	        new TypeNode("M68kProcessor", "A Motorola 68000 series processor.", "33920524-4989-4322-a0ca-1cea614d7fef")
	                .setIsa("b7b2a2ff-3af7-469b-afbf-e52f1b80be91"),
	        new TypeNode("MipsProcessor", "A Motorola 68000 series processor.", "cbe81157-97f6-401b-bbaa-cfbc4f479298")
	                .setIsa("b7b2a2ff-3af7-469b-afbf-e52f1b80be91"),
	        new TypeNode("MosProcessor", "A MOS Technology processor.", "b4f67698-7eca-4ca1-a4cf-b7ef9737785c")
	                .setIsa("b7b2a2ff-3af7-469b-afbf-e52f1b80be91"),
	        new TypeNode("PariscProcessor", "A PA Risc processor.", "689a4f33-e08d-4e7f-83a5-79fd310bd3f9")
	                .setIsa("b7b2a2ff-3af7-469b-afbf-e52f1b80be91"),
	        new TypeNode("PowerProcessor", "A PowerPC processor.", "808f2737-feca-4b70-aa1a-53410b970b10")
	                .setIsa("b7b2a2ff-3af7-469b-afbf-e52f1b80be91"),
	        new TypeNode("SparcProcessor", "A SPARC processor.", "798ba8d1-8da7-471a-9efa-30530569b6a1")
	                .setIsa("b7b2a2ff-3af7-469b-afbf-e52f1b80be91"),
	        new TypeNode("X86Processor", "An x86-based processor.", "4d4971b6-43b3-4c14-a82a-c51d72ba23a5")
	                .setIsa("b7b2a2ff-3af7-469b-afbf-e52f1b80be91"),
	        new TypeNode("ZilogProcessor", "A ZiLOG processor.", "a68f42c5-2a1d-43cb-bebd-55339f5e61ee")
	                .setIsa("b7b2a2ff-3af7-469b-afbf-e52f1b80be91"),
	        new TypeNode("Software", "An abstract software component.", "8db2b492-9a4a-41fe-97a5-bc625da042ea")
	                .setDescriptor("ver", "string", true, "The version of the software.", null, null)
	                .setDescriptor("shortName", "string", true, "A short lowercase name for the Software without spaces.", null,
							null)
	                .setDescriptor("longName", "string", true, "The full descriptive name of the Software.", null, null)
	                .setIsa("a79ce509-a021-4177-ae61-73abd01fb479"),
	        new TypeNode("CloudSystem", "A cloud (operating) system and/or environment.",
	                "86c0439a-4219-46e6-82a3-6fb2ee774aa4")
	                .setIsa("8db2b492-9a4a-41fe-97a5-bc625da042ea"),
	        new TypeNode("OperatingSystem", "An operating system.", "f05e325e-1aee-4892-96e4-8bcd6e97c528")
	                .setIsa("8db2b492-9a4a-41fe-97a5-bc625da042ea"),
	        new TypeNode("ServiceManager", "An operating system initialization system.",
	                "c3008eec-b454-4ef6-bf7e-0bd92c13d3c3")
	                .setIsa("8db2b492-9a4a-41fe-97a5-bc625da042ea"),
	        new TypeNode("PackageManager", "A package manager.", "e2b8b9ee-b66f-4252-8fba-4fb172f741bc")
	                .setIsa("8db2b492-9a4a-41fe-97a5-bc625da042ea"),
	        new TypeNode("Interpreter", "A command or programming language interpreter.",
	                "d047f415-734c-4bd3-92b9-d68d5521ce69")
	                .setIsa("8db2b492-9a4a-41fe-97a5-bc625da042ea"),
	        new TypeNode("Hypervisor", "A virtualization hypervisor.", "c5c7fcb5-8ac3-40cc-b195-b2b6af368e81")
	                .setIsa("8db2b492-9a4a-41fe-97a5-bc625da042ea"),
	        new TypeNode("Emulator", "A hardware platform emulator.", "59aeae81-9741-4986-b9c2-931504cfc9e3")
	                .setIsa("8db2b492-9a4a-41fe-97a5-bc625da042ea"),
	        new TypeNode("Service", "A generic local/networked/daemonized service.",
	                "3fb4603b-281e-47c0-b338-768ea05d4660")
	                .setIsa("8db2b492-9a4a-41fe-97a5-bc625da042ea"),
	        new TypeNode("Grouping", "A group of references to things.", "5f1eb7c5-ec5e-4563-be6a-fc50774fc374")
	                .setIsa("b1bc963a-19cc-4518-aecb-a35d028c49e3"),
	        new TypeNode("Locational", "A place/location.", "f31b8dc9-7b9a-47cc-bd3d-20b94b6daac1")
	                .setDescriptor("code", "string", false, "An optional code designation.", null, null)
	                .setIsa("5f1eb7c5-ec5e-4563-be6a-fc50774fc374"),
	        new TypeNode("Building", "A building.", "44197148-678f-4421-a618-dc6807beaf30")
	                .setDescriptor("city", "string", true, "The city where the Building resides.", null, null)
	                .setDescriptor("streetAddress", "string", true, "The street address and number (if any) of the Building.",
							null, null)
	                .setDescriptor("country", "string", true, "The country where the Building resides", null, null)
	                .setDescriptor("postalCode", "string", false, "'The postal code or the Building.", null, null)
	                .setIsa("f31b8dc9-7b9a-47cc-bd3d-20b94b6daac1"),
	        new TypeNode("Floor", "A floor.", "9ca8061a-898c-4f2f-aeb3-c77d1747559f")
	                .setIsa("f31b8dc9-7b9a-47cc-bd3d-20b94b6daac1"),
	        new TypeNode("Room", "A room.", "94721751-ab2f-4285-9c33-cd91cb919430")
	                .setIsa("f31b8dc9-7b9a-47cc-bd3d-20b94b6daac1"),
	        new TypeNode("Organizational", "An organizational group.", "c94c2c32-8e10-48c3-a31b-6337c2ad4df4")
	                .setIsa("5f1eb7c5-ec5e-4563-be6a-fc50774fc374"),
	        new TypeNode("Company", "A company.", "d7582691-8ccc-415c-886b-7d363c8b6e84")
	                .setIsa("c94c2c32-8e10-48c3-a31b-6337c2ad4df4"),
	        new TypeNode("Department", "A department.", "aa6bc876-33fe-4710-b409-edb31e1081de")
	                .setIsa("c94c2c32-8e10-48c3-a31b-6337c2ad4df4"),
	        new TypeNode("Team", "A team.", "9d68148a-28ec-4f24-b1d7-0fc2d407ab78")
	                .setIsa("c94c2c32-8e10-48c3-a31b-6337c2ad4df4"),
	        new TypeNode("Componental", "A group for organizing references to components.",
	                "ee92c308-80f4-40b2-af3a-77d26cd99517")
	                .setIsa("5f1eb7c5-ec5e-4563-be6a-fc50774fc374"),
	        new TypeNode("Actor", "Something or someone that interacts/starts/acts within the system.",
	                "3175d762-dd97-434f-a288-21e4591480cf")
	                .setIsa("b1bc963a-19cc-4518-aecb-a35d028c49e3"),
	        new TypeNode("User", "A user.", "88d8630d-54ce-4aa5-8505-2068c31df5b1")
	                .setDescriptor("emailAddress", "string", true, "The email address of the User.", null, null)
	                .setDescriptor("title", "string", false, "The title of the user.", null, null)
	                .setDescriptor("firstName", "string", true, "The first name of the User.", null, null)
	                .setDescriptor("lastName", "string", true, "The last name of the User.", null, null)
	                .setDescriptor("birthDate", "number", false, "The birth date of the User", null, null)
	                .setIsa("3175d762-dd97-434f-a288-21e4591480cf"),
	        new TypeNode("Batch", "A batch user.", "395d3e08-842b-4640-8b57-751e1b0c9e4b")
	                .setDescriptor("emailAddress", "string", true, "The email address of the Batch user.", null, null)
	                .setIsa("3175d762-dd97-434f-a288-21e4591480cf"),
	        new TypeNode("Capability", "A software component gives capabilities to its parent or to siblings.",
	                "916e04a9-c773-4eaf-9c08-450016426e5a")
	                .setIsa("b1bc963a-19cc-4518-aecb-a35d028c49e3"),
	        new TypeNode("Checkable", "A capability that makes it possible to state-check.",
	                "b3cfd296-1090-43c0-ac2e-00e5d7fe7779")
	                .setIsa("916e04a9-c773-4eaf-9c08-450016426e5a"),
	        new TypeNode("Stopable", "A capability that makes it possible to stop something.",
	                "632ca701-2648-4df5-87ee-a41d3d1ef3da")
	                .setIsa("916e04a9-c773-4eaf-9c08-450016426e5a"),
	        new TypeNode("Startable", "A capability that makes it possible to stop something.",
	                "347ca5da-db19-4d98-9fed-60e72d5d6bba")
	                .setIsa("916e04a9-c773-4eaf-9c08-450016426e5a"),
	        new TypeNode("Restartable", "A capability that makes it possible to restart something.",
	                "cc558715-63dc-4162-95a4-9c51792c7953")
	                .setIsa("916e04a9-c773-4eaf-9c08-450016426e5a"),
	        new TypeNode("Installable", "A capability that makes it possible to install something.",
	                "3c077211-0ffa-452f-8d84-5bd5be5598f5")
	                .setIsa("916e04a9-c773-4eaf-9c08-450016426e5a"),
	        new TypeNode("Configurable", "A capability that makes it possible to configure something.",
	                "3dc7b06a-69a0-4650-897a-34c5d069e3c5")
	                .setIsa("916e04a9-c773-4eaf-9c08-450016426e5a"),
	        new TypeNode("Uninstallable", "A capability that makes it possible to uninstall something.",
	                "ea1b32de-cf31-4c1b-9b2b-f1a6d9983418")
	                .setIsa("916e04a9-c773-4eaf-9c08-450016426e5a"),
	        new TypeNode("Virtualizable", "A capability that makes it possible to virtualize something.",
	                "b0995f9f-90af-4066-9d00-6b7b33f9a19b")
	                .setIsa("916e04a9-c773-4eaf-9c08-450016426e5a"),
	        new TypeNode("Commandable", "A capability that makes it possible to run commands.",
	                "76fb3116-d4a8-459a-b62b-a2641bcb7e7d")
	                .setIsa("916e04a9-c773-4eaf-9c08-450016426e5a"),
	        new TypeNode("Protocol", "A protocol.", "db0c3c65-52fd-4dec-907c-6c8a669e88f3")
	                .setIsa("b1bc963a-19cc-4518-aecb-a35d028c49e3"),
	        new TypeNode("HTTP", "The hypertext transfer protocol.", "9fd04885-179f-4c72-99a2-52c692e61b19")
	                .setIsa("db0c3c65-52fd-4dec-907c-6c8a669e88f3"),
	        new TypeNode("HTTP09", "The hypertext transfer protocol, version 0.9.",
	                "09102127-1803-48b6-a9f4-2fd1b891293d")
	                .setIsa("9fd04885-179f-4c72-99a2-52c692e61b19"),
	        new TypeNode("HTTP10", "The hypertext transfer protocol, version 1.0.",
	                "256d493d-3b7a-4e28-8e64-8f81de4d92de")
	                .setIsa("9fd04885-179f-4c72-99a2-52c692e61b19"),
	        new TypeNode("HTTP11", "The hypertext transfer protocol, version 1.1.",
	                "b11b10d7-6f46-49c0-bc61-3ecdd4342cd7")
	                .setIsa("9fd04885-179f-4c72-99a2-52c692e61b19"),
	        new TypeNode("SMB", "The server message block protocol.", "0771de77-c3d8-4db1-92fa-7ac96205f9bb")
	                .setIsa("db0c3c65-52fd-4dec-907c-6c8a669e88f3"),
	        new TypeNode("SMB10", "The server message block protocol, version 1.0.",
	                "70960db9-f085-499d-9d22-2569badc41c7")
	                .setIsa("0771de77-c3d8-4db1-92fa-7ac96205f9bb"),
	        new TypeNode("SMB20", "The server message block protocol, version 2.0.",
	                "aeefff52-0300-4611-be60-e745bfbd3d37")
	                .setIsa("0771de77-c3d8-4db1-92fa-7ac96205f9bb"),
	        new TypeNode("SMB21", "The server message block protocol, version 2.1.",
	                "5c7b0507-dfd9-4617-a45f-3d617cd61e68")
	                .setIsa("0771de77-c3d8-4db1-92fa-7ac96205f9bb"),
	        new TypeNode("SMB30", "The server message block protocol, version 3.0.",
	                "dbfb220e-b22c-460e-b27d-65de22501618")
	                .setIsa("0771de77-c3d8-4db1-92fa-7ac96205f9bb"),
	        new TypeNode("SSH", "The secure shell protocol.", "008b9566-6317-4ddd-977c-9ddca00b79b8")
	                .setIsa("db0c3c65-52fd-4dec-907c-6c8a669e88f3"),
	        new TypeNode("SSH10", "The secure shell protocol, version 1.0.", "000b0fd3-a121-4194-a441-0857abca490d")
	                .setIsa("008b9566-6317-4ddd-977c-9ddca00b79b8"),
	        new TypeNode("SSH20", "The secure shell protocol, version 2.0.", "07b23795-140f-4a72-9c86-e1d3b86cd149")
	                .setIsa("008b9566-6317-4ddd-977c-9ddca00b79b8"),
	        new TypeNode("TELNET", "The telnet protocol.", "27fc7ed2-beb3-41c8-aa69-900ad7bb989f")
	                .setIsa("db0c3c65-52fd-4dec-907c-6c8a669e88f3"),
	        new TypeNode("Handler", "A representation of a software handler that gives end user function.",
	                "14dbfbfa-b50a-466f-8539-55d5d3f15e4a")
	                .setIsa("b1bc963a-19cc-4518-aecb-a35d028c49e3"),
	        new TypeNode("CommandHandler", "Creates a CommandOperation, executes that CommandOperation, creates " +
	                "CommandResult.", "6ac7f3f8-4a1f-42aa-87b8-b2f5516b5d7c")
	                .setIsa("14dbfbfa-b50a-466f-8539-55d5d3f15e4a"),
	        new TypeNode("VersionHandler", "Looks up a VersionOperation, executes command, creates " +
	                "VersionResult.",
	                "a57f47c3-7e27-4347-806b-204239c61e65")
	                .setIsa("14dbfbfa-b50a-466f-8539-55d5d3f15e4a"),
	        new TypeNode("StatusHandler", "Executes generic code to get a status or executes CustomStatusOperation" +
	                " if " +
	                "it exists. Creates StatusResult.", "3b6555cd-abd4-4cc6-ab64-b7c031c2b008")
	                .setIsa("14dbfbfa-b50a-466f-8539-55d5d3f15e4a"),
	        new TypeNode("ExistenceHandler", "Executes generic code to get a existence information or executes" +
	                " " +
	                "CustomExistenceOperation if it exists. Creates ExistenceResult.", "6a8a8dd7-f8d3-4170-8c25-3866cfc2037c")
	                .setIsa("14dbfbfa-b50a-466f-8539-55d5d3f15e4a"),
	        new TypeNode("UsageHandler", "Executes generic code to derive usage of resources or executes " +
	                "CustomUsageOperation if it exists. Creates UsageResult.", "dd0878eb-b560-4dc0-89da-1d7ddfcbbced")
	                .setIsa("14dbfbfa-b50a-466f-8539-55d5d3f15e4a"),
	        new TypeNode("InstallHandler", "Executes generic code to derive install method and executes or " +
	                "executes " +
	                "CustomInstallOperation if it exists.", "3e33df40-8d9d-49d2-939d-6712d89ca70f")
	                .setIsa("14dbfbfa-b50a-466f-8539-55d5d3f15e4a"),
	        new TypeNode("UninstallHandler", "Executes generic code to derive uninstall method and executes or" +
	                " " +
	                "executes CustomUninstallOperation if it exists.", "6b80a87e-9d24-4a06-b792-2d13c0713b6c")
	                .setIsa("14dbfbfa-b50a-466f-8539-55d5d3f15e4a"),
	        new TypeNode("StartHandler", "Executes generic code to derive start method and executes or executes " +
	                "CustomStartOperation if it exists.", "db00edc2-5900-46d8-99fc-b379fd8538e2")
	                .setIsa("14dbfbfa-b50a-466f-8539-55d5d3f15e4a"),
	        new TypeNode("StopHandler", "Executes generic code to derive stop method and executes or executes " +
	                "CustomStopOperation if it exists.", "5d97322d-9113-4d7c-ac6c-2690a8c65d0d")
	                .setIsa("14dbfbfa-b50a-466f-8539-55d5d3f15e4a"),
	        new TypeNode("RestartHandler", "Executes generic code to derive restart method and executes or executes " +
	                "CustomRestartOperation if it exists.", "c61c2f8c-0ba2-4e0d-99f2-66a5580a59a6")
	                .setIsa("14dbfbfa-b50a-466f-8539-55d5d3f15e4a"),
	        new TypeNode("Operation", "A command or method that can be called, with or without schedule.",
	                "46294722-b14e-438e-9fbc-95ff137b6b18")
	                .setDescriptor("timesInvoked", "number", true, "The number of times this operation was invoked.", null, null)
	                .setDescriptor("data", "string", true, "A custom command, method or eventbus message that does a software " +
							"installation.", null, null)
	                .setDescriptor("dateCreated", "number", true, "The date + time when the operation was created.", null, null)
	                .setDescriptor("dateStarted", "number", true, "The date + time when the operation was started.", null, null)
	                .setDescriptor("dateFinished", "number", true, "The date + time when the operation was finished.", null, null)

	                .setIsa("b1bc963a-19cc-4518-aecb-a35d028c49e3"),
	        new TypeNode("CommandOperation", "An arbitrary command.", "de64c2f4-7fa3-4e68-ba10-6af8140dac36")
	                .setIsa("46294722-b14e-438e-9fbc-95ff137b6b18"),
	        new TypeNode("VersionOperation", "A command that displays a version string.",
	                "9598a74d-0293-459a-af63-e3563215162f")
	                .setIsa("46294722-b14e-438e-9fbc-95ff137b6b18"),
	        new TypeNode("CustomStatusOperation", "Custom command for a status check operation.",
	                "bfc01b44-075d-4d6e-82eb-474830bba06e")
	                .setIsa("46294722-b14e-438e-9fbc-95ff137b6b18"),
	        new TypeNode("CustomExistenceOperation", "Custom command for an existence operation.",
	                "97bf21ef-d112-4607-9392-83243e44577f")
	                .setIsa("46294722-b14e-438e-9fbc-95ff137b6b18"),
	        new TypeNode("CustomUsageOperation", "Custom command that shows/derives/fetches the usage of a " +
	                "resource.",
	                "32b9f847-3579-4750-b29b-c0f3af877adb")
	                .setIsa("46294722-b14e-438e-9fbc-95ff137b6b18"),
	        new TypeNode("CustomInstallOperation", "Custom command to install a software.",
	                "7fb7c8c0-6967-4af2-a4f9-e5342fc3fb24")
	                .setIsa("46294722-b14e-438e-9fbc-95ff137b6b18"),
	        new TypeNode("CustomUninstallOperation", "Custom command to uninstall a software.",
	                "1308c542-ea7a-4525-8a1b-65cf96033a62")
	                .setIsa("46294722-b14e-438e-9fbc-95ff137b6b18"),
	        new TypeNode("CustomStartOperation", "Custom command to start a software.",
	                "8cb7ac74-7923-4114-abd2-17f4c5f440d7")
	                .setIsa("46294722-b14e-438e-9fbc-95ff137b6b18"),
	        new TypeNode("CustomStopOperation", "Custom command to stop a software.",
	                "ffefceab-afd8-42da-b793-d65beb4f71eb")
	                .setIsa("46294722-b14e-438e-9fbc-95ff137b6b18"),
	        new TypeNode("CustomRestartOperation", "Custom command to restart a software.",
	                "3d5300bf-7b4d-443c-b33a-159c00572eb9")
	                .setIsa("46294722-b14e-438e-9fbc-95ff137b6b18"),
	        new TypeNode("Result", "A result from an operation.", "f3498c02-58c6-486b-87c6-9d54607fdb12")
	                .setDescriptor("data", "string", true, "The result data.", null, null)
	                .setDescriptor("dateCreated", "number", true, "The date + time when the result was created.", null, null)
	                .setIsa("b1bc963a-19cc-4518-aecb-a35d028c49e3"),
	        new TypeNode("CommandResult", "A generic textual result from a command operation.",
	                "6991d878-52be-467d-893a-6b95074f04c2")
	                .setIsa("f3498c02-58c6-486b-87c6-9d54607fdb12"),
	        new TypeNode("VersionResult", "A textual result representing a version string.",
	                "2f9703cb-3d68-44fa-99bf-935ed658ca5a")
	                .setIsa("f3498c02-58c6-486b-87c6-9d54607fdb12"),
	        new TypeNode("StatusResult", "A result that represents a status.", "48839d5c-8ebc-4989-80ec-f6fb90299a56")
	                .setIsa("f3498c02-58c6-486b-87c6-9d54607fdb12"),
	        new TypeNode("DerivedStatusResult", "A derived status. 1/ok, 0/not-ok.",
	                "9565a2ca-29e4-4c36-8f17-97e9a0074ded")
	                .setIsa("48839d5c-8ebc-4989-80ec-f6fb90299a56"),
	        new TypeNode("LocalDerivedStatusResult", "A derived 1/0 status from all statuses known to the software" +
	                " " +
	                "itself.", "9d007dd4-82df-4791-81f9-d91a65e9f3a3")
	                .setIsa("48839d5c-8ebc-4989-80ec-f6fb90299a56"),
	        new TypeNode("AggregateDerivedStatusResult", "A derived 1/0 status from all localderivedstatuses from " +
	                "its " +
	                "children.", "3f9ca86e-6286-4e59-9eb1-fa352103fe5c")
	                .setIsa("48839d5c-8ebc-4989-80ec-f6fb90299a56"),
	        new TypeNode("SoftwareInstalledStatusResult", "1/installed, 0/not-installed.",
	                "38b435e1-046c-415b-9cce-6578efb4eef6")
	                .setIsa("48839d5c-8ebc-4989-80ec-f6fb90299a56"),
	        new TypeNode("SoftwareRunningStatusResult", "1/running, 0/not-running.",
	                "4fda91ea-63bb-4689-bcf4-86bb02314c21")
	                .setIsa("48839d5c-8ebc-4989-80ec-f6fb90299a56"),
	        new TypeNode("IpAddressReachableStatusResult", "1/reachable, 0/unreachable.",
	                "729e0e03-3a27-47e2-8075-7e4a754d68a5")
	                .setIsa("48839d5c-8ebc-4989-80ec-f6fb90299a56"),
	        new TypeNode("ListenerAvailableStatusResult", "1/open, 0/closed.", "630e8753-7566-4c2f-9cdf" +
	                "-99f5b64492a1")
	                .setIsa("48839d5c-8ebc-4989-80ec-f6fb90299a56"),
	        new TypeNode("DnsNameResolvableStatusResult", "1/resolved, 0/not-resolved.",
	                "fffdce77-417f-4044-a570-6e6153db1dd0")
	                .setIsa("48839d5c-8ebc-4989-80ec-f6fb90299a56"),
	        new TypeNode("ExistenceResult", "A generic result from an ExistenceHandler.",
	                "6d23a2bc-9539-4914-aaf4-45406f59d8fc")
	                .setIsa("f3498c02-58c6-486b-87c6-9d54607fdb12"),
	        new TypeNode("DirectoryExistenceResult", "A generic directory existence result from an ExistenceHandler.",
	                "31736b3b-82da-4864-bc59-b49c196e3327")
	                .setIsa("6d23a2bc-9539-4914-aaf4-45406f59d8fc"),
	        new TypeNode("PrefixDirectoryExistenceResult", "A possible result from an ExistenceHandler, parsing a " +
	                "PrefixDirectoryMap.", "93e34319-fe7b-4768-b83d-58986a4cdb73")
	                .setIsa("31736b3b-82da-4864-bc59-b49c196e3327"),
	        new TypeNode("ConfigDirectoryExistenceResult", "A possible result from an ExistenceHandler, parsing a " +
	                "ConfigDirectoryMap.", "0182518c-2380-4abc-be84-5fa02679fe52")
	                .setIsa("31736b3b-82da-4864-bc59-b49c196e3327"),
	        new TypeNode("BinaryDirectoryExistenceResult", "A possible result from an ExistenceHandler, parsing a " +
	                "BinaryDirectoryMap.", "1b607b4a-5206-4d55-8743-0487f7b6d944")
	                .setIsa("31736b3b-82da-4864-bc59-b49c196e3327"),
	        new TypeNode("LibraryDirectoryExistenceResult", "A possible result from an ExistenceHandler, parsing a " +
	                "LibraryDirectoryMap.", "25a2e063-b7e7-4727-9725-63859005e6ed")
	                .setIsa("31736b3b-82da-4864-bc59-b49c196e3327"),
	        new TypeNode("DataDirectoryExistenceResult", "A possible result from an ExistenceHandler, parsing a " +
	                "DataDirectoryMap.", "22105b60-ea6d-4c1e-b3ad-14b8ff9e37a1")
	                .setIsa("31736b3b-82da-4864-bc59-b49c196e3327"),
	        new TypeNode("LogDirectoryExistenceResult", "A possible result from an ExistenceHandler, parsing a " +
	                "LogDirectoryMap.", "3b64fa0c-474e-4109-9f43-c861ac5fce19")
	                .setIsa("31736b3b-82da-4864-bc59-b49c196e3327"),
	        new TypeNode("FileExistenceResult", "A generic file existence result from an ExistenceHandler.",
	                "b72fd981-3473-4874-9a40-bc86d6b5362f")
	                .setIsa("6d23a2bc-9539-4914-aaf4-45406f59d8fc"),
	        new TypeNode("ConfigFileExistenceResult", "A possible result from an ExistenceHandler, parsing a " +
	                "ConfigFileMap.", "6615da84-00f9-4858-a753-38f5905e8c4c")
	                .setIsa("b72fd981-3473-4874-9a40-bc86d6b5362f"),
	        new TypeNode("BinaryFileExistenceResult", "A possible result from an ExistenceHandler, parsing a " +
	                "BinaryFileMap.", "6c510a61-53b1-4f16-a5cb-8816846062be")
	                .setIsa("b72fd981-3473-4874-9a40-bc86d6b5362f"),
	        new TypeNode("LibraryFileExistenceResult", "A possible result from an ExistenceHandler, parsing a " +
	                "LibraryFileMap.", "e4d04fd4-0d95-49db-9637-891d5f9b6e21")
	                .setIsa("b72fd981-3473-4874-9a40-bc86d6b5362f"),
	        new TypeNode("DataFileExistenceResult", "A possible result from an ExistenceHandler, parsing a DataFileMap" +
	                ".", "6a1d9396-707e-4224-965c-2dd8c5c97cfb")
	                .setIsa("b72fd981-3473-4874-9a40-bc86d6b5362f"),
	        new TypeNode("LogFileExistenceResult", "A possible result from an ExistenceHandler, parsing a LogFileMap" +
	                ".", "fbd1b321-3c95-4eb3-ad4f-16c1f7215e39")
	                .setIsa("b72fd981-3473-4874-9a40-bc86d6b5362f"),
	        new TypeNode("NameExistenceResult", "A possible result from an ExistenceHandler, parsing a NameMap.",
	                "db4c51ec-7455-4bba-b965-f1c067f22e37")
	                .setIsa("6d23a2bc-9539-4914-aaf4-45406f59d8fc"),
	        new TypeNode("PackageNameExistenceResult", "A possible result from an ExistenceHandler, parsing a " +
	                "PackageNameMap.", "7dd30b10-04bf-4875-8f98-2add74299648")
	                .setIsa("db4c51ec-7455-4bba-b965-f1c067f22e37"),
	        new TypeNode("ServiceNameExistenceResult", "A possible result from an ExistenceHandler, parsing a " +
	                "ServiceNameMap.", "9edc562a-f026-4ea8-861f-0dd0cfdf3c39")
	                .setIsa("db4c51ec-7455-4bba-b965-f1c067f22e37"),
	        new TypeNode("StringExistenceResult", "A possible result from an ExistenceHandler, parsing a StringMap.",
	                "7bcaa3ec-9ffb-468d-a5dd-21d26661078a")
	                .setIsa("6d23a2bc-9539-4914-aaf4-45406f59d8fc"),
	        new TypeNode("VersionStringExistenceResult", "A possible result from an ExistenceHandler, parsing a " +
	                "VersionStringMap.", "4b86cf7f-6a98-46cc-b78a-0e568d2afe1f")
	                .setIsa("7bcaa3ec-9ffb-468d-a5dd-21d26661078a"),
	        new TypeNode("UsageResult", "A generic usage result.", "61bc2f71-158c-450c-9346-b9100505054e")
	                .setIsa("f3498c02-58c6-486b-87c6-9d54607fdb12"),
	        new TypeNode("ProcessorUsageResult", "The processing unit usage results from a UsageHandler.",
	                "885e629b-3f03-4fa9-a876-67e3991711a0")
	                .setIsa("61bc2f71-158c-450c-9346-b9100505054e"),
	        new TypeNode("MemoryUsageResult", "The memory usage results from a UsageHandler.",
	                "c893bc3b-21de-47b8-96dd-0ba11006d35b")
	                .setIsa("61bc2f71-158c-450c-9346-b9100505054e"),
	        new TypeNode("StorageUsageResult", "The storage usage results from a UsageHandler.",
	                "2615131b-50ce-4699-a5ef-261fbb633fe3")
	                .setIsa("61bc2f71-158c-450c-9346-b9100505054e"),
	        new TypeNode("NetworkUsageResult", "The network bandwidth usage results from a UsageHandler.",
	                "ebaf3fae-351a-4d90-9b98-9970399160aa")
	                .setIsa("61bc2f71-158c-450c-9346-b9100505054e")
        );
    }
}
