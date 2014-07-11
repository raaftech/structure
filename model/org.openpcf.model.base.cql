// org.openpcf.model.base-0.0.1


// Object - All, has uuid constraints, name indexes.
MERGE (object:Object {
    uuid: 'b1bc963a-19cc-4518-aecb-a35d028c49e3',
    name: 'Object',
    desc: 'The root object.'
    });

CREATE INDEX ON :Object(name);
CREATE CONSTRAINT ON (object:Object)
    ASSERT object.uuid IS UNIQUE;


// Model - Housekeeping stuff for the database, versioning.
MERGE (model:Object:Model {
    uuid: '4e2dccfa-b5ca-4992-a76e-c390b7d90084',
    name: 'Model',
    namespace: 'Specifies a namespace for the model.',
    version: 'Specifies a version number for the model.',
    desc: 'A model object.'
    });

MATCH (model:Model {uuid: '4e2dccfa-b5ca-4992-a76e-c390b7d90084'}),
    (object:Object {uuid: 'b1bc963a-19cc-4518-aecb-a35d028c49e3'})
MERGE (model)-[:ISA]->(object);


// Base Model 0.0.1 - The first model object.
MERGE (basemodel:Object:Model:BaseModel {
    uuid: '48c8698b-3afd-44e5-baf7-73f55494cb31',
    name: 'BaseModel',
    namespace: 'org.openpcf.model.base',
    version: '0.0.1',
    desc: 'The first model object, all base types.'
    });

MATCH (basemodel:BaseModel {uuid: '48c8698b-3afd-44e5-baf7-73f55494cb31'}),
    (model:Model {uuid: '4e2dccfa-b5ca-4992-a76e-c390b7d90084'})
MERGE (basemodel)-[:ISA]->(model);


// Component - Something that can be combined/built.
MERGE (component:Object:Component {
    uuid: 'a79ce509-a021-4177-ae61-73abd01fb479',
    name: 'Component',
    desc: 'An abstract base component.'
    });

MATCH (component:Component {uuid: 'a79ce509-a021-4177-ae61-73abd01fb479'}),
    (object:Object {uuid: 'b1bc963a-19cc-4518-aecb-a35d028c49e3'})
MERGE (component)-[:ISA]->(object);


// Device - Any kind of device.
MERGE (device:Object:Component:Device {
    uuid: 'ea6a7487-867c-4a74-8849-fb8539b8169f',
    name: 'Device',
    desc: 'An abstract device component.'
    });

MATCH (device:Device {uuid: 'ea6a7487-867c-4a74-8849-fb8539b8169f'}),
    (component:Component {uuid: 'a79ce509-a021-4177-ae61-73abd01fb479'})
MERGE (device)-[:ISA]->(component);


// AssembledDevice - Any assembled device.
MERGE (assembleddevice:Object:Component:Device:AssembledDevice {
    uuid: 'dbb2884f-7c31-4f7d-a92e-d6a76dd1b7fe',
    name: 'AssembledDevice',
    desc: 'An assembled device, containing various components.'
    });

MATCH (assembleddevice:AssembledDevice {uuid: 'dbb2884f-7c31-4f7d-a92e-d6a76dd1b7fe'}),
    (device:Device {uuid: 'ea6a7487-867c-4a74-8849-fb8539b8169f'})
MERGE (assembleddevice)-[:ISA]->(device);


// Computer - An assembled computer device.
MERGE (computer:Object:Component:Device:AssembledDevice:Computer {
    uuid: 'ec11db91-6973-4285-b018-b461fbb432d7',
    name: 'Computer',
    desc: 'An assembled computer device.'
    });

MATCH (computer:Computer {uuid: 'ec11db91-6973-4285-b018-b461fbb432d7'}),
    (assembleddevice:AssembledDevice {uuid: 'dbb2884f-7c31-4f7d-a92e-d6a76dd1b7fe'})
MERGE (computer)-[:ISA]->(assembleddevice);


// ConsoleDevice - A console device.
MERGE (consoledevice:Object:Component:Device:ConsoleDevice {
    uuid: '3d0865a2-3ecf-4035-886b-0bb603c0e2c2',
    name: 'ConsoleDevice',
    desc: 'A console device.'
    });

MATCH (consoledevice:ConsoleDevice {uuid: '3d0865a2-3ecf-4035-886b-0bb603c0e2c2'}),
    (device:Device {uuid: 'ea6a7487-867c-4a74-8849-fb8539b8169f'})
MERGE (consoledevice)-[:ISA]->(device);


// GenericSerial - A serial communication device.
MERGE (genericserial:Object:Component:Device:ConsoleDevice:GenericSerial {
    uuid: 'f5b25736-ec12-4a7e-b198-548b97ebc36f',
    name: 'GenericSerial',
    desc: 'A serial communication device.'
    });

MATCH (genericserial:GenericSerial {uuid: 'f5b25736-ec12-4a7e-b198-548b97ebc36f'}),
    (consoledevice:ConsoleDevice {uuid: '3d0865a2-3ecf-4035-886b-0bb603c0e2c2'})
MERGE (genericserial)-[:ISA]->(consoledevice);


// GraphicsDevice - A grapical device.
MERGE (graphicsdevice:Object:Component:Device:GraphicsDevice {
    uuid: 'a642b995-c073-4c27-a611-c5bf3854958f',
    name: 'GraphicsDevice',
    desc: 'A graphics device.'
    });

MATCH (graphicsdevice:GraphicsDevice {uuid: 'a642b995-c073-4c27-a611-c5bf3854958f'}),
    (device:Device {uuid: 'ea6a7487-867c-4a74-8849-fb8539b8169f'})
MERGE (graphicsdevice)-[:ISA]->(device);


// GenericVga - A VGA graphics device.
MERGE (genericvga:Object:Component:Device:GraphicsDevice:GenericVga {
    uuid: 'b3a2ac7c-ad8a-4047-a460-2ac41a7ddfef',
    name: 'GenericVga',
    desc: 'A VGA graphics device.'
    });

MATCH (genericvga:GenericVga {uuid: 'b3a2ac7c-ad8a-4047-a460-2ac41a7ddfef'}),
    (graphicsdevice:GraphicsDevice {uuid: 'a642b995-c073-4c27-a611-c5bf3854958f'})
MERGE (genericvga)-[:ISA]->(graphicsdevice);


// MemoryDevice - A memory device.
MERGE (memorydevice:Object:Component:Device:MemoryDevice {
    uuid: '1c87c672-1f69-4a02-9bc8-9eff28dda5e5',
    name: 'MemoryDevice',
    desc: 'A memory device'
    });

MATCH (memorydevice:MemoryDevice {uuid: '1c87c672-1f69-4a02-9bc8-9eff28dda5e5'}),
    (device:Device {uuid: 'ea6a7487-867c-4a74-8849-fb8539b8169f'})
MERGE (memorydevice)-[:ISA]->(device);


// VolatileMemory - A volatile memory device.
MERGE (volatilememory:Object:Component:Device:MemoryDevice:VolatileMemory {
    uuid: '44131d46-c1b0-4030-ae5a-9a3108627749',
    name: 'VolatileMemory',
    desc: 'A generic volatile memory device.'
    });

MATCH (volatilememory:VolatileMemory {uuid: '44131d46-c1b0-4030-ae5a-9a3108627749'}),
    (memorydevice:MemoryDevice {uuid: '1c87c672-1f69-4a02-9bc8-9eff28dda5e5'})
MERGE (volatilememory)-[:ISA]->(memorydevice);


// RandomAccessMemory - Random access memory.
MERGE (randomaccessmemory:Object:Component:Device:MemoryDevice:VolatileMemory:RandomAccessMemory {
    uuid: '4c0a0638-1fa7-4f5b-9d69-1d199dcedccc',
    name: 'RandomAccessMemory',
    desc: 'Random access memory.'
    });

MATCH (randomaccessmemory:RandomAccessMemory {uuid: '4c0a0638-1fa7-4f5b-9d69-1d199dcedccc'}),
    (volatilememory:VolatileMemory {uuid: '44131d46-c1b0-4030-ae5a-9a3108627749'})
MERGE (randomaccessmemory)-[:ISA]->(volatilememory);


// NonVolatileMemory - A volatile memory device.
MERGE (nonvolatilememory:Object:Component:Device:MemoryDevice:NonVolatileMemory {
    uuid: '7f129e53-ff07-4ed8-9062-daf5f733a3a6',
    name: 'NonVolatileMemory',
    desc: 'A generic non-volatile memory device.'
    });

MATCH (nonvolatilememory:NonVolatileMemory {uuid: '7f129e53-ff07-4ed8-9062-daf5f733a3a6'}),
    (memorydevice:MemoryDevice {uuid: '1c87c672-1f69-4a02-9bc8-9eff28dda5e5'})
MERGE (nonvolatilememory)-[:ISA]->(memorydevice);


// HardDiskDrive - A non-volatile harddisk-based storage device.
MERGE (harddiskdrive:Object:Component:Device:MemoryDevice:NonVolatileMemory:HardDiskDrive {
    uuid: '24cc3545-0d1a-4d85-8d08-a83c3dbe5912',
    name: 'HardDiskDrive',
    desc: 'A non-volatile harddisk-based storage device.'
    });

MATCH (harddiskdrive:HardDiskDrive {uuid: '24cc3545-0d1a-4d85-8d08-a83c3dbe5912'}),
    (nonvolatilememory:NonVolatileMemory {uuid: '7f129e53-ff07-4ed8-9062-daf5f733a3a6'})
MERGE (harddiskdrive)-[:ISA]->(nonvolatilememory);


// SolidStateDrive - A non-volatile solid-state storage device.
MERGE (solidstatedrive:Object:Component:Device:MemoryDevice:NonVolatileMemory:SolidStateDrive {
    uuid: '7a60ec32-0a9a-40b9-bf1f-d69674d4a233',
    name: 'SolidStateDrive',
    desc: 'A non-volatile solid-state storage device.'
    });

MATCH (solidstatedrive:SolidStateDrive {uuid: '7a60ec32-0a9a-40b9-bf1f-d69674d4a233'}),
    (nonvolatilememory:NonVolatileMemory {uuid: '7f129e53-ff07-4ed8-9062-daf5f733a3a6'})
MERGE (solidstatedrive)-[:ISA]->(nonvolatilememory);


// FloppyDrive - A floppy disk drive.
MERGE (floppydrive:Object:Component:Device:MemoryDevice:NonVolatileMemory:FloppyDrive {
    uuid: '118ec961-a823-465b-be17-b706f1c8c2f1',
    name: 'FloppyDrive',
    desc: 'A floppy disk drive.'
    });

MATCH (floppydrive:FloppyDrive {uuid: '118ec961-a823-465b-be17-b706f1c8c2f1'}),
    (nonvolatilememory:NonVolatileMemory {uuid: '7f129e53-ff07-4ed8-9062-daf5f733a3a6'})
MERGE (floppydrive)-[:ISA]->(nonvolatilememory);


// TapeDrive - A floppy disk drive.
MERGE (tapedrive:Object:Component:Device:MemoryDevice:NonVolatileMemory:TapeDrive {
    uuid: 'a507461f-647b-4b79-936a-721ca2dba145',
    name: 'TapeDrive',
    desc: 'A floppy disk drive.'
    });

MATCH (tapedrive:TapeDrive {uuid: 'a507461f-647b-4b79-936a-721ca2dba145'}),
    (nonvolatilememory:NonVolatileMemory {uuid: '7f129e53-ff07-4ed8-9062-daf5f733a3a6'})
MERGE (tapedrive)-[:ISA]->(nonvolatilememory);


// CdDrive - A Compact Disc drive.
MERGE (cddrive:Object:Component:Device:MemoryDevice:NonVolatileMemory:CdDrive {
    uuid: '30af2fe3-aa05-4912-a3d5-dddfdacf74e2',
    name: 'CdDrive',
    desc: 'A Compact Disc drive.'
    });

MATCH (cddrive:CdDrive {uuid: '30af2fe3-aa05-4912-a3d5-dddfdacf74e2'}),
    (nonvolatilememory:NonVolatileMemory {uuid: '7f129e53-ff07-4ed8-9062-daf5f733a3a6'})
MERGE (cddrive)-[:ISA]->(nonvolatilememory);


// DvdDrive - A Digital Video Disc drive.
MERGE (dvddrive:Object:Component:Device:MemoryDevice:NonVolatileMemory:DvdDrive {
    uuid: '23ab95d2-2815-4121-a81c-63aca40cdcfa',
    name: 'DvdDrive',
    desc: 'A Digital Video Disc drive.'
    });

MATCH (dvddrive:DvdDrive {uuid: '23ab95d2-2815-4121-a81c-63aca40cdcfa'}),
    (nonvolatilememory:NonVolatileMemory {uuid: '7f129e53-ff07-4ed8-9062-daf5f733a3a6'})
MERGE (dvddrive)-[:ISA]->(nonvolatilememory);


// BdDrive - A Blu-Ray drive.
MERGE (bddrive:Object:Component:Device:MemoryDevice:NonVolatileMemory:BdDrive {
    uuid: 'cbc27482-11d1-4890-a3e1-e9f80baea82c',
    name: 'BdDrive',
    desc: 'A Digital Video Disc drive.'
    });

MATCH (bddrive:BdDrive {uuid: 'cbc27482-11d1-4890-a3e1-e9f80baea82c'}),
    (nonvolatilememory:NonVolatileMemory {uuid: '7f129e53-ff07-4ed8-9062-daf5f733a3a6'})
MERGE (bddrive)-[:ISA]->(nonvolatilememory);


// NetworkDevice - A network device.
MERGE (networkdevice:Object:Component:Device:NetworkDevice {
    uuid: '86786ed6-d62a-4011-9ae8-0082000e48fa',
    name: 'NetworkDevice',
    desc: 'A network device.'
    });

MATCH (networkdevice:NetworkDevice {uuid: '86786ed6-d62a-4011-9ae8-0082000e48fa'}),
    (device:Device {uuid: 'ea6a7487-867c-4a74-8849-fb8539b8169f'})
MERGE (networkdevice)-[:ISA]->(device);


// WiredNetworkDevice - A wired networking device.
MERGE (wirednetworkdevice:Object:Component:Device:NetworkDevice:WiredNetworkDevice {
    uuid: '0d4c880d-d888-435f-b3a3-3d3eb9e70ddf',
    name: 'WiredNetworkDevice',
    desc: 'A wired network device.'
    });

MATCH (wirednetworkdevice:WiredNetworkDevice {uuid: '0d4c880d-d888-435f-b3a3-3d3eb9e70ddf'}),
    (networkdevice:NetworkDevice {uuid: '86786ed6-d62a-4011-9ae8-0082000e48fa'})
MERGE (wirednetworkdevice)-[:ISA]->(networkdevice);


// ModemDevice - A wired modem networking device.
MERGE (modemdevice:Object:Component:Device:NetworkDevice:WiredNetworkDevice:ModemDevice {
    uuid: 'f34b76e4-f5a6-4bae-968e-f22ef09b1e35',
    name: 'ModemDevice',
    desc: 'A modem networking device.'
    });

MATCH (modemdevice:ModemDevice {uuid: 'f34b76e4-f5a6-4bae-968e-f22ef09b1e35'}),
    (wirednetworkdevice:WiredNetworkDevice {uuid: '0d4c880d-d888-435f-b3a3-3d3eb9e70ddf'})
MERGE (modemdevice)-[:ISA]->(wirednetworkdevice);


// EthernetDevice - A wired ethernet device.
MERGE (ethernetdevice:Object:Component:Device:NetworkDevice:WiredNetworkDevice:EthernetDevice {
    uuid: '0dbd26f2-baff-446c-9241-46eaa3e03736',
    name: 'EthernetDevice',
    desc: 'A wired ethernet device.'
    });

MATCH (ethernetdevice:EthernetDevice {uuid: '0dbd26f2-baff-446c-9241-46eaa3e03736'}),
    (wirednetworkdevice:WiredNetworkDevice {uuid: '0d4c880d-d888-435f-b3a3-3d3eb9e70ddf'})
MERGE (ethernetdevice)-[:ISA]->(wirednetworkdevice);


// WirelessNetworkDevice - A wireless device.
MERGE (wirelessnetworkdevice:Object:Component:Device:NetworkDevice:WirelessNetworkDevice {
    uuid: '61328f83-db43-4efe-81be-df47e17814a5',
    name: 'WirelessNetworkDevice',
    desc: 'A wireless network device.'
    });

MATCH (wirelessnetworkdevice:WirelessNetworkDevice {uuid: '61328f83-db43-4efe-81be-df47e17814a5'}),
    (networkdevice:NetworkDevice {uuid: '86786ed6-d62a-4011-9ae8-0082000e48fa'})
MERGE (wirelessnetworkdevice)-[:ISA]->(networkdevice);


// BluetoothDevice - A Bluetooth device.
MERGE (bluetoothdevice:Object:Component:Device:NetworkDevice:WirelessNetworkDevice:BluetoothDevice {
    uuid: '009cf823-39c0-4b17-ae81-ad98ed0d4585',
    name: 'BluetoothDevice',
    desc: 'A Bluetooth device.'
    });

MATCH (bluetoothdevice:BluetoothDevice {uuid: '009cf823-39c0-4b17-ae81-ad98ed0d4585'}),
    (wirelessnetworkdevice:WirelessNetworkDevice {uuid: '61328f83-db43-4efe-81be-df47e17814a5'})
MERGE (bluetoothdevice)-[:ISA]->(wirelessnetworkdevice);


// IrdaDevice - An IrDA device.
MERGE (irdadevice:Object:Component:Device:NetworkDevice:WirelessNetworkDevice:IrdaDevice {
    uuid: '42c7997c-0c56-4f93-956f-5fd0778c47c6',
    name: 'IrdaDevice',
    desc: 'An IrDA device.'
    });

MATCH (irdadevice:IrdaDevice {uuid: '42c7997c-0c56-4f93-956f-5fd0778c47c6'}),
    (wirelessnetworkdevice:WirelessNetworkDevice {uuid: '61328f83-db43-4efe-81be-df47e17814a5'})
MERGE (irdadevice)-[:ISA]->(wirelessnetworkdevice);


// WifiDevice - A WiFi device.
MERGE (wifidevice:Object:Component:Device:NetworkDevice:WirelessNetworkDevice:WifiDevice {
    uuid: '78ff02be-2abe-4a8f-a082-a0d9c3cdae70',
    name: 'WifiDevice',
    desc: 'A WiFi device.'
    });

MATCH (wifidevice:WifiDevice {uuid: '78ff02be-2abe-4a8f-a082-a0d9c3cdae70'}),
    (wirelessnetworkdevice:WirelessNetworkDevice {uuid: '61328f83-db43-4efe-81be-df47e17814a5'})
MERGE (wifidevice)-[:ISA]->(wirelessnetworkdevice);


// WwanDevice - A Wireless WAN device.
MERGE (wwandevice:Object:Component:Device:NetworkDevice:WirelessNetworkDevice:WwanDevice {
    uuid: '8d6e0956-033e-4efc-8846-e85bfddc8f98',
    name: 'WwanDevice',
    desc: 'A wireless WAN device.'
    });

MATCH (wwandevice:WwanDevice {uuid: '8d6e0956-033e-4efc-8846-e85bfddc8f98'}),
    (wirelessnetworkdevice:WirelessNetworkDevice {uuid: '61328f83-db43-4efe-81be-df47e17814a5'})
MERGE (wwandevice)-[:ISA]->(wirelessnetworkdevice);


// ProcessingUnit - A processing unit.
MERGE (processingunit:Object:Component:Device:ProcessingUnit {
    uuid: 'b7b2a2ff-3af7-469b-afbf-e52f1b80be91',
    name: 'ProcessingUnit',
    desc: 'A processing unit.'
    });

MATCH (processingunit:ProcessingUnit {uuid: 'b7b2a2ff-3af7-469b-afbf-e52f1b80be91'}),
    (device:Device {uuid: 'ea6a7487-867c-4a74-8849-fb8539b8169f'})
MERGE (processingunit)-[:ISA]->(device);


// ArmProcessor- An ARM processor.
MERGE (armprocessor:Object:Component:Device:ProcessingUnit:ArmProcessor {
    uuid: 'f3b1b497-1e83-4a09-a280-04f46a83fe24',
    name: 'ArmProcessor',
    desc: 'An ARM processor.'
    });

MATCH (armprocessor:ArmProcessor {uuid: 'f3b1b497-1e83-4a09-a280-04f46a83fe24'}),
    (processingunit:ProcessingUnit {uuid: 'b7b2a2ff-3af7-469b-afbf-e52f1b80be91'})
MERGE (armprocessor)-[:ISA]->(processingunit);


// ItaniumProcessor - An Itanium processor.
MERGE (itaniumprocessor:Object:Component:Device:ProcessingUnit:ItaniumProcessor {
    uuid: '241015c9-72f5-491f-a870-a30d8f7902fd',
    name: 'ItaniumProcessor',
    desc: 'An Itanium processor.'
    });

MATCH (itaniumprocessor:ItaniumProcessor {uuid: '241015c9-72f5-491f-a870-a30d8f7902fd'}),
    (processingunit:ProcessingUnit {uuid: 'b7b2a2ff-3af7-469b-afbf-e52f1b80be91'})
MERGE (itaniumprocessor)-[:ISA]->(processingunit);


// M68KProcessor - A Motorola 68000 series processor.
MERGE (m68kprocessor:Object:Component:Device:ProcessingUnit:M68kProcessor {
    uuid: '33920524-4989-4322-a0ca-1cea614d7fef',
    name: 'M68kProcessor',
    desc: 'A Motorola 68000 series processor.'
    });

MATCH (m68kprocessor:M68kProcessor {uuid: '33920524-4989-4322-a0ca-1cea614d7fef'}),
    (processingunit:ProcessingUnit {uuid: 'b7b2a2ff-3af7-469b-afbf-e52f1b80be91'})
MERGE (m68kprocessor)-[:ISA]->(processingunit);


// MipsProcessor - A MIPS processor.
MERGE (mipsprocessor:Object:Component:Device:ProcessingUnit:MipsProcessor {
    uuid: 'cbe81157-97f6-401b-bbaa-cfbc4f479298',
    name: 'MipsProcessor',
    desc: 'A Motorola 68000 series processor.'
    });

MATCH (mipsprocessor:MipsProcessor {uuid: 'cbe81157-97f6-401b-bbaa-cfbc4f479298'}),
    (processingunit:ProcessingUnit {uuid: 'b7b2a2ff-3af7-469b-afbf-e52f1b80be91'})
MERGE (mipsprocessor)-[:ISA]->(processingunit);


// MosProcessor - A MOS Technology processor.
MERGE (mosprocessor:Object:Component:Device:ProcessingUnit:MosProcessor {
    uuid: 'b4f67698-7eca-4ca1-a4cf-b7ef9737785c',
    name: 'MosProcessor',
    desc: 'A MOS Technology processor.'
    });

MATCH (mosprocessor:MosProcessor {uuid: 'b4f67698-7eca-4ca1-a4cf-b7ef9737785c'}),
    (processingunit:ProcessingUnit {uuid: 'b7b2a2ff-3af7-469b-afbf-e52f1b80be91'})
MERGE (mosprocessor)-[:ISA]->(processingunit);


// PowerProcessor - A PowerPC processor.
MERGE (powerprocessor:Object:Component:Device:ProcessingUnit:PowerProcessor {
    uuid: '808f2737-feca-4b70-aa1a-53410b970b10',
    name: 'PowerProcessor',
    desc: 'A PowerPC processor.'
    });

MATCH (powerprocessor:PowerProcessor {uuid: '808f2737-feca-4b70-aa1a-53410b970b10'}),
    (processingunit:ProcessingUnit {uuid: 'b7b2a2ff-3af7-469b-afbf-e52f1b80be91'})
MERGE (powerprocessor)-[:ISA]->(processingunit);


// SparcProcessor - A SPARC processor.
MERGE (sparcprocessor:Object:Component:Device:ProcessingUnit:SparcProcessor {
    uuid: '798ba8d1-8da7-471a-9efa-30530569b6a1',
    name: 'SparcProcessor',
    desc: 'A SPARC processor.'
    });

MATCH (sparcprocessor:SparcProcessor {uuid: '798ba8d1-8da7-471a-9efa-30530569b6a1'}),
    (processingunit:ProcessingUnit {uuid: 'b7b2a2ff-3af7-469b-afbf-e52f1b80be91'})
MERGE (sparcprocessor)-[:ISA]->(processingunit);


// X86Processor - An x86-based processor.
MERGE (x86processor:Object:Component:Device:ProcessingUnit:X86Processor {
    uuid: '4d4971b6-43b3-4c14-a82a-c51d72ba23a5',
    name: 'X86Processor',
    desc: 'An x86-based processor.'
    });

MATCH (x86processor:X86Processor {uuid: '4d4971b6-43b3-4c14-a82a-c51d72ba23a5'}),
    (processingunit:ProcessingUnit {uuid: 'b7b2a2ff-3af7-469b-afbf-e52f1b80be91'})
MERGE (x86processor)-[:ISA]->(processingunit);


// ZilogProcessor - A ZiLOG processor.
MERGE (zilogprocessor:Object:Component:Device:ProcessingUnit:ZilogProcessor {
    uuid: 'a68f42c5-2a1d-43cb-bebd-55339f5e61ee',
    name: 'ZilogProcessor',
    desc: 'A ZiLOG processor.'
    });

MATCH (zilogprocessor:ZilogProcessor {uuid: 'a68f42c5-2a1d-43cb-bebd-55339f5e61ee'}),
    (processingunit:ProcessingUnit {uuid: 'b7b2a2ff-3af7-469b-afbf-e52f1b80be91'})
MERGE (zilogprocessor)-[:ISA]->(processingunit);


// Software - Any kind of software.
MERGE (software:Object:Component:Software {
    uuid: '8db2b492-9a4a-41fe-97a5-bc625da042ea',
    name: 'Software',
    desc: 'An abstract software component.'
    });

MATCH (software:Software {uuid: '8db2b492-9a4a-41fe-97a5-bc625da042ea'}),
    (component:Component {uuid: 'a79ce509-a021-4177-ae61-73abd01fb479'})
MERGE (software)-[:ISA]->(component);


// CloudSystem - Any cloud (operating) system, either IAAS or PAAS.
MERGE (cloudsystem:Object:Component:Software:CloudSystem {
    uuid: '86c0439a-4219-46e6-82a3-6fb2ee774aa4',
    name: 'CloudSystem',
    desc: 'A cloud (operating) system and/or environment.'
    });

MATCH (cloudsystem:CloudSystem {uuid: '86c0439a-4219-46e6-82a3-6fb2ee774aa4'}),
    (software:Software {uuid: '8db2b492-9a4a-41fe-97a5-bc625da042ea'})
MERGE (cloudsystem)-[:ISA]->(software);


// OperatingSystem - Any operating system.
MERGE (operatingsystem:Object:Component:Software:OperatingSystem {
    uuid: 'f05e325e-1aee-4892-96e4-8bcd6e97c528',
    name: 'OperatingSystem',
    desc: 'An operating system.'
    });

MATCH (operatingsystem:OperatingSystem {uuid: 'f05e325e-1aee-4892-96e4-8bcd6e97c528'}),
    (software:Software {uuid: '8db2b492-9a4a-41fe-97a5-bc625da042ea'})
MERGE (operatingsystem)-[:ISA]->(software);


// ServiceManager - Any service/boot initialization system.
MERGE (servicemanager:Object:Component:Software:ServiceManager {
    uuid: 'c3008eec-b454-4ef6-bf7e-0bd92c13d3c3',
    name: 'ServiceManager',
    desc: 'An operating system initialization system.'
    });

MATCH (servicemanager:ServiceManager {uuid: 'c3008eec-b454-4ef6-bf7e-0bd92c13d3c3'}),
    (software:Software {uuid: '8db2b492-9a4a-41fe-97a5-bc625da042ea'})
MERGE (servicemanager)-[:ISA]->(software);


// PackageManager - Any packaging system.
MERGE (packagemanager:Object:Component:Software:PackageManager {
    uuid: 'e2b8b9ee-b66f-4252-8fba-4fb172f741bc',
    name: 'PackageManager',
    desc: 'A package manager.'
    });

MATCH (packagemanager:PackageManager {uuid: 'e2b8b9ee-b66f-4252-8fba-4fb172f741bc'}),
    (software:Software {uuid: '8db2b492-9a4a-41fe-97a5-bc625da042ea'})
MERGE (packagemanager)-[:ISA]->(software);


// Interpreter - Any interpreter.
MERGE (interpreter:Object:Component:Software:Interpreter {
    uuid: 'd047f415-734c-4bd3-92b9-d68d5521ce69',
    name: 'Interpreter',
    desc: 'A command or programming language interpreter.'
    });

MATCH (interpreter:Interpreter {uuid: 'd047f415-734c-4bd3-92b9-d68d5521ce69'}),
    (software:Software {uuid: '8db2b492-9a4a-41fe-97a5-bc625da042ea'})
MERGE (interpreter)-[:ISA]->(software);


// Hypervisor - Any hypervisor.
MERGE (hypervisor:Object:Component:Software:Hypervisor {
    uuid: 'c5c7fcb5-8ac3-40cc-b195-b2b6af368e81',
    name: 'Hypervisor',
    desc: 'A virtualization hypervisor.'
    });

MATCH (hypervisor:Hypervisor {uuid: 'c5c7fcb5-8ac3-40cc-b195-b2b6af368e81'}),
    (software:Software {uuid: '8db2b492-9a4a-41fe-97a5-bc625da042ea'})
MERGE (hypervisor)-[:ISA]->(software);


// Service - Any generic local/networked service.
MERGE (service:Object:Component:Software:Service {
    uuid: '3fb4603b-281e-47c0-b338-768ea05d4660',
    name: 'Service',
    desc: 'A generic local/networked service.'
    });

MATCH (service:Service {uuid: '3fb4603b-281e-47c0-b338-768ea05d4660'}),
    (software:Software {uuid: '8db2b492-9a4a-41fe-97a5-bc625da042ea'})
MERGE (service)-[:ISA]->(software);


// Grouping - A group of references to things.
MERGE (grouping:Object:Grouping {
    uuid: '5f1eb7c5-ec5e-4563-be6a-fc50774fc374',
    name: 'Grouping',
    desc: 'A group of references to things.'
    });

MATCH (grouping:Grouping {uuid: '5f1eb7c5-ec5e-4563-be6a-fc50774fc374'}),
    (object:Object {uuid: 'b1bc963a-19cc-4518-aecb-a35d028c49e3'})
MERGE (grouping)-[:ISA]->(object);

// Locational - Any kind of place/location.
MERGE (locational:Object:Grouping:Locational {
    uuid: 'f31b8dc9-7b9a-47cc-bd3d-20b94b6daac1',
    name: 'Locational',
    desc: 'A place/location.'
    });

MATCH (locational:Locational {uuid: 'f31b8dc9-7b9a-47cc-bd3d-20b94b6daac1'}),
    (grouping:Grouping {uuid: '5f1eb7c5-ec5e-4563-be6a-fc50774fc374'})
MERGE (locational)-[:ISA]->(grouping);


// Building - A building, has address, containing references to floors, rooms, devices.
MERGE (building:Object:Grouping:Locational:Building {
    uuid: '44197148-678f-4421-a618-dc6807beaf30',
    name: 'Building',
    desc: 'A building.'
    });

MATCH (building:Building {uuid: '44197148-678f-4421-a618-dc6807beaf30'}),
    (locational:Locational {uuid: 'f31b8dc9-7b9a-47cc-bd3d-20b94b6daac1'})
MERGE (building)-[:ISA]->(locational);


// Floor - A floor containing references to rooms, devices.
MERGE (floor:Object:Grouping:Locational:Floor {
    uuid: '9ca8061a-898c-4f2f-aeb3-c77d1747559f',
    name: 'Floor',
    desc: 'A floor.'
    });

MATCH (floor:Floor {uuid: '9ca8061a-898c-4f2f-aeb3-c77d1747559f'}),
    (locational:Locational {uuid: 'f31b8dc9-7b9a-47cc-bd3d-20b94b6daac1'})
MERGE (floor)-[:ISA]->(locational);


// Room - A room containing references to devices.
MERGE (room:Object:Grouping:Locational:Room {
    uuid: '94721751-ab2f-4285-9c33-cd91cb919430',
    name: 'Room',
    desc: 'A room.'
    });

MATCH (room:Room {uuid: '94721751-ab2f-4285-9c33-cd91cb919430'}),
    (locational:Locational {uuid: 'f31b8dc9-7b9a-47cc-bd3d-20b94b6daac1'})
MERGE (room)-[:ISA]->(locational);


// Organizational - Any kind of organizational group.
MERGE (organizational:Object:Grouping:Organizational {
    uuid: 'c94c2c32-8e10-48c3-a31b-6337c2ad4df4',
    name: 'Organizational',
    desc: 'An organizational group.'
    });

MATCH (organizational:Organizational {uuid: 'c94c2c32-8e10-48c3-a31b-6337c2ad4df4'}),
    (grouping:Grouping {uuid: '5f1eb7c5-ec5e-4563-be6a-fc50774fc374'})
MERGE (organizational)-[:ISA]->(grouping);


// Company - A company can contain references to departments, teams, users.
MERGE (company:Object:Grouping:Organizational:Company {
    uuid: 'd7582691-8ccc-415c-886b-7d363c8b6e84',
    name: 'Company',
    desc: 'A company.'
    });

MATCH (company:Company {uuid: 'd7582691-8ccc-415c-886b-7d363c8b6e84'}),
    (organizational:Organizational {uuid: 'c94c2c32-8e10-48c3-a31b-6337c2ad4df4'})
MERGE (company)-[:ISA]->(organizational);


// Department - A department can contain references to teams, users.
MERGE (department:Object:Grouping:Organizational:Department {
    uuid: 'aa6bc876-33fe-4710-b409-edb31e1081de',
    name: 'Department',
    desc: 'A department.'
    });

MATCH (department:Department {uuid: 'aa6bc876-33fe-4710-b409-edb31e1081de'}),
    (organizational:Organizational {uuid: 'c94c2c32-8e10-48c3-a31b-6337c2ad4df4'})
MERGE (department)-[:ISA]->(organizational);


// Team - A team can contain references to users.
MERGE (team:Object:Grouping:Organizational:Team {
    uuid: '9d68148a-28ec-4f24-b1d7-0fc2d407ab78',
    name: 'Team',
    desc: 'A team.'
    });

MATCH (team:Team {uuid: '9d68148a-28ec-4f24-b1d7-0fc2d407ab78'}),
    (organizational:Organizational {uuid: 'c94c2c32-8e10-48c3-a31b-6337c2ad4df4'})
MERGE (team)-[:ISA]->(organizational);


// Componental - A convenience group for organizing references to components.
MERGE (componental:Object:Grouping:Componental {
    uuid: 'ee92c308-80f4-40b2-af3a-77d26cd99517',
    name: 'Componental',
    desc: 'A group for organizing references to components.'
    });

MATCH (componental:Componental {uuid: 'ee92c308-80f4-40b2-af3a-77d26cd99517'}),
    (grouping:Grouping {uuid: '5f1eb7c5-ec5e-4563-be6a-fc50774fc374'})
MERGE (componental)-[:ISA]->(grouping);


// Actor - Something or someone that interacts/starts/acts.
MERGE (actor:Object:Actor {
    uuid: '3175d762-dd97-434f-a288-21e4591480cf',
    name: 'Actor',
    desc: 'Something or someone that interacts/starts/acts within the system.'
    });

MATCH (actor:Actor {uuid: '3175d762-dd97-434f-a288-21e4591480cf'}),
    (object:Object {uuid: 'b1bc963a-19cc-4518-aecb-a35d028c49e3'})
MERGE (actor)-[:ISA]->(object);


// User - Users. Interact with the system.
MERGE (user:Object:Actor:User {
    uuid: '88d8630d-54ce-4aa5-8505-2068c31df5b1',
    name: 'User',
    desc: 'A user.'
    });

MATCH (user:User {uuid: '88d8630d-54ce-4aa5-8505-2068c31df5b1'}),
    (actor:Actor {uuid: '3175d762-dd97-434f-a288-21e4591480cf'})
MERGE (user)-[:ISA]->(actor);


// Batch - An automated entity type. Interacts with system in an automated way.
MERGE (batch:Object:Actor:Batch {
    uuid: '395d3e08-842b-4640-8b57-751e1b0c9e4b',
    name: 'Batch',
    desc: 'A batch user.'
    });

MATCH (batch:Batch {uuid: '395d3e08-842b-4640-8b57-751e1b0c9e4b'}),
    (actor:Actor {uuid: '3175d762-dd97-434f-a288-21e4591480cf'})
MERGE (batch)-[:ISA]->(actor);


// Capability - A software component gives capabilities to its parent or to siblings.
MERGE (capability:Object:Capability {
    uuid: '916e04a9-c773-4eaf-9c08-450016426e5a',
    name: 'Capability',
    desc: 'A software component gives capabilities to its parent or to siblings.'
    });

MATCH (capability:Capability {uuid: '916e04a9-c773-4eaf-9c08-450016426e5a'}),
    (object:Object {uuid: 'b1bc963a-19cc-4518-aecb-a35d028c49e3'})
MERGE (capability)-[:ISA]->(object);


// Checkable - Makes it possible to state-check.
MERGE (checkable:Object:Capability:Checkable {
    uuid: 'b3cfd296-1090-43c0-ac2e-00e5d7fe7779',
    name: 'Checkable',
    desc: 'A capability that makes it possible to state-check.'
    });

MATCH (checkable:Checkable {uuid: 'b3cfd296-1090-43c0-ac2e-00e5d7fe7779'}),
    (capability:Capability {uuid: '916e04a9-c773-4eaf-9c08-450016426e5a'})
MERGE (checkable)-[:ISA]->(capability);


// Stopable - Gives a stop capability.
MERGE (stopable:Object:Capability:Stopable {
    uuid: '632ca701-2648-4df5-87ee-a41d3d1ef3da',
    name: 'Stopable',
    desc: 'A capability that makes it possible to stop something.'
    });

MATCH (stopable:Stopable {uuid: '632ca701-2648-4df5-87ee-a41d3d1ef3da'}),
    (capability:Capability {uuid: '916e04a9-c773-4eaf-9c08-450016426e5a'})
MERGE (stopable)-[:ISA]->(capability);


// Startable - Gives a start capability.
MERGE (startable:Object:Capability:Startable {
    uuid: '347ca5da-db19-4d98-9fed-60e72d5d6bba',
    name: 'Startable',
    desc: 'A capability that makes it possible to stop something.'
    });

MATCH (startable:Startable {uuid: '347ca5da-db19-4d98-9fed-60e72d5d6bba'}),
    (capability:Capability {uuid: '916e04a9-c773-4eaf-9c08-450016426e5a'})
MERGE (startable)-[:ISA]->(capability);


// Restartable - Gives a restart capability.
MERGE (restartable:Object:Capability:Restartable {
    uuid: 'cc558715-63dc-4162-95a4-9c51792c7953',
    name: 'Restartable',
    desc: 'A capability that makes it possible to restart something.'
    });

MATCH (restartable:Restartable {uuid: 'cc558715-63dc-4162-95a4-9c51792c7953'}),
    (capability:Capability {uuid: '916e04a9-c773-4eaf-9c08-450016426e5a'})
MERGE (restartable)-[:ISA]->(capability);


// Installable - Gives an install capability.
MERGE (installable:Object:Capability:Installable {
    uuid: '3c077211-0ffa-452f-8d84-5bd5be5598f5',
    name: 'Installable',
    desc: 'A capability that makes it possible to install something.'
    });

MATCH (installable:Installable {uuid: '3c077211-0ffa-452f-8d84-5bd5be5598f5'}),
    (capability:Capability {uuid: '916e04a9-c773-4eaf-9c08-450016426e5a'})
MERGE (installable)-[:ISA]->(capability);


// Configurable - Gives a configure capability.
MERGE (configurable:Object:Capability:Configurable {
    uuid: '3dc7b06a-69a0-4650-897a-34c5d069e3c5',
    name: 'Configurable',
    desc: 'A capability that makes it possible to configure something.'
    });

MATCH (configurable:Configurable {uuid: '3dc7b06a-69a0-4650-897a-34c5d069e3c5'}),
    (capability:Capability {uuid: '916e04a9-c773-4eaf-9c08-450016426e5a'})
MERGE (configurable)-[:ISA]->(capability);


// Uninstallable - Gives an uninstall capability.
MERGE (uninstallable:Object:Capability:Uninstallable {
    uuid: 'ea1b32de-cf31-4c1b-9b2b-f1a6d9983418',
    name: 'Uninstallable',
    desc: 'A capability that makes it possible to uninstall something.'
    });

MATCH (uninstallable:Uninstallable {uuid: 'ea1b32de-cf31-4c1b-9b2b-f1a6d9983418'}),
    (capability:Capability {uuid: '916e04a9-c773-4eaf-9c08-450016426e5a'})
MERGE (uninstallable)-[:ISA]->(capability);

// Virtualizable - Gives a virtualization capability.
MERGE (virtualizable:Object:Capability:Virtualizable {
    uuid: 'b0995f9f-90af-4066-9d00-6b7b33f9a19b',
    name: 'Virtualizable',
    desc: 'A capability that makes it possible to virtualize something.'
    });

MATCH (virtualizable:Virtualizable {uuid: 'b0995f9f-90af-4066-9d00-6b7b33f9a19b'}),
    (capability:Capability {uuid: '916e04a9-c773-4eaf-9c08-450016426e5a'})
MERGE (virtualizable)-[:ISA]->(capability);


// Commandable - Can be used to run a command.
MERGE (commandable:Object:Capability:Commandable {
    uuid: '76fb3116-d4a8-459a-b62b-a2641bcb7e7d',
    name: 'Commandable',
    desc: 'A capability that makes it possible to run commands.'
    });

MATCH (commandable:Commandable {uuid: '76fb3116-d4a8-459a-b62b-a2641bcb7e7d'}),
    (capability:Capability {uuid: '916e04a9-c773-4eaf-9c08-450016426e5a'})
MERGE (commandable)-[:ISA]->(capability);


// Protocol - Services implement protocols, Handlers interact with services using protocols.
MERGE (protocol:Object:Protocol {
    uuid: 'db0c3c65-52fd-4dec-907c-6c8a669e88f3',
    name: 'Protocol',
    desc: 'A protocol.'
    });

MATCH (protocol:Protocol {uuid: 'db0c3c65-52fd-4dec-907c-6c8a669e88f3'}),
    (object:Object {uuid: 'b1bc963a-19cc-4518-aecb-a35d028c49e3'})
MERGE (protocol)-[:ISA]->(object);


// HTTP - Can speak the HTTP protocol.
MERGE (http:Object:Protocol:HTTP {
    uuid: '9fd04885-179f-4c72-99a2-52c692e61b19',
    name: 'HTTP',
    desc: 'The hypertext transfer protocol.'
    });

MATCH (http:HTTP {uuid: '9fd04885-179f-4c72-99a2-52c692e61b19'}),
    (protocol:Protocol {uuid: 'db0c3c65-52fd-4dec-907c-6c8a669e88f3'})
MERGE (http)-[:ISA]->(protocol);


// HTTP09 - Can speak the HTTP 0.9 protocol.
MERGE (http09:Object:Protocol:HTTP:HTTP09 {
    uuid: '09102127-1803-48b6-a9f4-2fd1b891293d',
    name: 'HTTP09',
    desc: 'The hypertext transfer protocol, version 0.9.'
    });

MATCH (http09:HTTP09 {uuid: '09102127-1803-48b6-a9f4-2fd1b891293d'}),
    (http:HTTP {uuid: '9fd04885-179f-4c72-99a2-52c692e61b19'})
MERGE (http09)-[:ISA]->(http);


// HTTP10 - Can speak the HTTP 1.0 protocol.
MERGE (http10:Object:Protocol:HTTP:HTTP10 {
    uuid: '256d493d-3b7a-4e28-8e64-8f81de4d92de',
    name: 'HTTP10',
    desc: 'The hypertext transfer protocol, version 1.0.'
    });

MATCH (http10:HTTP10 {uuid: '256d493d-3b7a-4e28-8e64-8f81de4d92de'}),
    (http:HTTP {uuid: '9fd04885-179f-4c72-99a2-52c692e61b19'})
MERGE (http10)-[:ISA]->(http);


// HTTP11 - Can speak the HTTP 1.1 protocol.
MERGE (http11:Object:Protocol:HTTP:HTTP11 {
    uuid: 'b11b10d7-6f46-49c0-bc61-3ecdd4342cd7',
    name: 'HTTP11',
    desc: 'The hypertext transfer protocol, version 1.1.'
    });

MATCH (http11:HTTP11 {uuid: 'b11b10d7-6f46-49c0-bc61-3ecdd4342cd7'}),
    (http:HTTP {uuid: '9fd04885-179f-4c72-99a2-52c692e61b19'})
MERGE (http11)-[:ISA]->(http);


// SMB - Can speak the SMB protocol.
MERGE (smb:Object:Protocol:SMB {
    uuid: '0771de77-c3d8-4db1-92fa-7ac96205f9bb',
    name: 'SMB',
    desc: 'The server message block protocol.'
    });

MATCH (smb:SMB {uuid: '0771de77-c3d8-4db1-92fa-7ac96205f9bb'}),
    (protocol:Protocol {uuid: 'db0c3c65-52fd-4dec-907c-6c8a669e88f3'})
MERGE (smb)-[:ISA]->(protocol);


// SMB10 - Can speak the SMB 1.0 protocol.
MERGE (smb10:Object:Protocol:SMB:SMB10 {
    uuid: '70960db9-f085-499d-9d22-2569badc41c7',
    name: 'SMB10',
    desc: 'The server message block protocol, version 1.0.'
    });

MATCH (smb10:SMB10 {uuid: '70960db9-f085-499d-9d22-2569badc41c7'}),
    (smb:SMB {uuid: '0771de77-c3d8-4db1-92fa-7ac96205f9bb'})
MERGE (smb10)-[:ISA]->(smb);


// SMB20 - Can speak the SMB 2.0 protocol.
MERGE (smb20:Object:Protocol:SMB:SMB20 {
    uuid: 'aeefff52-0300-4611-be60-e745bfbd3d37',
    name: 'SMB20',
    desc: 'The server message block protocol, version 2.0.'
    });

MATCH (smb20:SMB20 {uuid: 'aeefff52-0300-4611-be60-e745bfbd3d37'}),
    (smb:SMB {uuid: '0771de77-c3d8-4db1-92fa-7ac96205f9bb'})
MERGE (smb20)-[:ISA]->(smb);


// SMB21 - Can speak the SMB 2.1 protocol.
MERGE (smb21:Object:Protocol:SMB21 {
    uuid: '5c7b0507-dfd9-4617-a45f-3d617cd61e68',
    name: 'SMB21',
    desc: 'The server message block protocol, version 2.1.'
    });

MATCH (smb21:SMB21 {uuid: '5c7b0507-dfd9-4617-a45f-3d617cd61e68'}),
    (smb:SMB {uuid: '0771de77-c3d8-4db1-92fa-7ac96205f9bb'})
MERGE (smb21)-[:ISA]->(smb);


// SMB30 - Can speak the SMB 3.0 protocol.
MERGE (smb30:Object:Protocol:SMB30 {
    uuid: 'dbfb220e-b22c-460e-b27d-65de22501618',
    name: 'SMB30',
    desc: 'The server message block protocol, version 3.0.'
    });

MATCH (smb30:SMB30 {uuid: 'dbfb220e-b22c-460e-b27d-65de22501618'}),
    (smb:SMB {uuid: '0771de77-c3d8-4db1-92fa-7ac96205f9bb'})
MERGE (smb30)-[:ISA]->(smb);


// SSH - Can speak the SSH protocol.
MERGE (ssh:Object:Protocol:SSH {
    uuid: '008b9566-6317-4ddd-977c-9ddca00b79b8',
    name: 'SSH',
    desc: 'The secure shell protocol.'
    });

MATCH (ssh:SSH {uuid: '008b9566-6317-4ddd-977c-9ddca00b79b8'}),
    (protocol:Protocol {uuid: 'db0c3c65-52fd-4dec-907c-6c8a669e88f3'})
MERGE (ssh)-[:ISA]->(protocol);


// SSH10 - Can speak the SSH 1.0 protocol.
MERGE (ssh10:Object:Protocol:SSH10 {
    uuid: '000b0fd3-a121-4194-a441-0857abca490d',
    name: 'SSH10',
    desc: 'The secure shell protocol, version 1.0.'
    });

MATCH (ssh10:SSH10 {uuid: '000b0fd3-a121-4194-a441-0857abca490d'}),
    (ssh:SSH {uuid: '008b9566-6317-4ddd-977c-9ddca00b79b8'})
MERGE (ssh10)-[:ISA]->(ssh);


// SSH20 - Can speak the SSH 2.0 protocol.
MERGE (ssh20:Object:Protocol:SSH20 {
    uuid: '07b23795-140f-4a72-9c86-e1d3b86cd149',
    name: 'SSH20',
    desc: 'The secure shell protocol, version 2.0.'
    });

MATCH (ssh20:SSH20 {uuid: '07b23795-140f-4a72-9c86-e1d3b86cd149'}),
    (ssh:SSH {uuid: '008b9566-6317-4ddd-977c-9ddca00b79b8'})
MERGE (ssh20)-[:ISA]->(ssh);


// TELNET - Can speak the SSH protocol.
MERGE (telnet:Object:Protocol:TELNET {
    uuid: '27fc7ed2-beb3-41c8-aa69-900ad7bb989f',
    name: 'TELNET',
    desc: 'The telnet protocol.'
    });

MATCH (telnet:TELNET {uuid: '27fc7ed2-beb3-41c8-aa69-900ad7bb989f'}),
    (protocol:Protocol {uuid: 'db0c3c65-52fd-4dec-907c-6c8a669e88f3'})
MERGE (telnet)-[:ISA]->(protocol);


// Operation - A command or method that can be called, with or without schedule.
MERGE (operation:Object:Operation {
    uuid: '46294722-b14e-438e-9fbc-95ff137b6b18',
    name: 'Operation',
    desc: 'A command or method that can be called, with or without schedule.'
    });

MATCH (operation:Operation {uuid: '46294722-b14e-438e-9fbc-95ff137b6b18'}),
    (object:Object {uuid: 'b1bc963a-19cc-4518-aecb-a35d028c49e3'})
MERGE (operation)-[:ISA]->(object);


// Result - A result from an operation.
MERGE (result:Object:Result {
    uuid: 'f3498c02-58c6-486b-87c6-9d54607fdb12',
    name: 'Result',
    desc: 'A result from an operation.'
    });

MATCH (result:Result {uuid: 'f3498c02-58c6-486b-87c6-9d54607fdb12'}),
    (object:Object {uuid: 'b1bc963a-19cc-4518-aecb-a35d028c49e3'})
MERGE (result)-[:ISA]->(object);


// Handler - A representation of a software handler that gives end user function.
MERGE (handler:Object:Handler {
    uuid: '14dbfbfa-b50a-466f-8539-55d5d3f15e4a',
    name: 'Handler',
    desc: 'A representation of a software handler that gives end user function.'
    });

MATCH (handler:Handler {uuid: '14dbfbfa-b50a-466f-8539-55d5d3f15e4a'}),
    (object:Object {uuid: 'b1bc963a-19cc-4518-aecb-a35d028c49e3'})
MERGE (handler)-[:ISA]->(object);
