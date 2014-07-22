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


// BaseModel - The first model object.
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


// Configuration - Any kind of configuration.
MERGE (configuration:Object:Component:Configuration {
    uuid: '3f4a43f7-b558-4bf0-ba7e-66b99cffaf16',
    name: 'Configuration',
    desc: 'An abstract configuration component.'
    });

MATCH (configuration:Configuration {uuid: '3f4a43f7-b558-4bf0-ba7e-66b99cffaf16'}),
    (component:Component {uuid: 'a79ce509-a021-4177-ae61-73abd01fb479'})
MERGE (configuration)-[:ISA]->(component);


// IpAddress - An IP address.
MERGE (ipaddress:Object:Component:Configuration:IpAddress {
    uuid: '299001a6-1091-4c0d-87be-c512af03e179',
    name: 'IpAddress',
    desc: 'An IP address (address only, can be assigned to a network interface).',
    addr: 'IP addresses should at least have the addr field.'
    });

MATCH (ipaddress:IpAddress {uuid: '299001a6-1091-4c0d-87be-c512af03e179'}),
    (configuration:Configuration {uuid: '3f4a43f7-b558-4bf0-ba7e-66b99cffaf16'})
MERGE (ipaddress)-[:ISA]->(configuration);


// Ipv4Address - An IPv4 address.
MERGE (ipv4addr:Object:Component:Configuration:IpAddress:Ipv4Address {
    uuid: 'f7fbe917-9d49-4df5-95f6-6e132067a424',
    name: 'Ipv4Address',
    desc: 'An IPv4 address (address only, can be assigned to a network interface).',
    addr: 'Fill in an IPv4 address here.'
    });

MATCH (ipv4addr:Ipv4Address {uuid: 'f7fbe917-9d49-4df5-95f6-6e132067a424'}),
    (ipaddress:IpAddress {uuid: '299001a6-1091-4c0d-87be-c512af03e179'})
MERGE (ipv4addr)-[:ISA]->(ipaddress);


// Ipv6Address - An IPv6 address.
MERGE (ipv6addr:Object:Component:Configuration:IpAddress:Ipv6Address {
    uuid: '47b5804d-5f5d-4bf8-89f6-a189c29b05ae',
    name: 'Ipv6Address',
    desc: 'An IPv6 address (address only, can be assigned to a network interface).',
    addr: 'Fill in an IPv6 address here.'
    });

MATCH (ipv6addr:Ipv6Address {uuid: '47b5804d-5f5d-4bf8-89f6-a189c29b05ae'}),
    (ipaddress:IpAddress {uuid: '299001a6-1091-4c0d-87be-c512af03e179'})
MERGE (ipv6addr)-[:ISA]->(ipaddress);


// IpListener - An IP listener.
MERGE (iplistener:Object:Component:Configuration:IpListener {
    uuid: '91c999a0-33ec-42ee-bae0-6f8adae42535',
    name: 'IpListener',
    desc: 'An IP listener (address and port, can be assigned to a network service).',
    addr: 'IP listeners should at least have the addr and port fields.',
    port: 'IP listeners should at least have the addr and port fields.'
    });

MATCH (iplistener:IpListener {uuid: '91c999a0-33ec-42ee-bae0-6f8adae42535'}),
    (configuration:Configuration {uuid: '3f4a43f7-b558-4bf0-ba7e-66b99cffaf16'})
MERGE (iplistener)-[:ISA]->(configuration);


// Ipv4Listener - An IPv4 listener.
MERGE (ip4vlistener:Object:Component:Configuration:IpListener:Ipv4Listener {
    uuid: '5ca95f7a-5175-4e7f-8a61-862e24485af3',
    name: 'Ipv4Listener',
    desc: 'An IPv4 listener (address and port, can be assigned to a network service).',
    addr: 'Fill in an IPv4 address here.',
    port: 'Fill in a port number between 0 and 65535 here.'
    });

MATCH (ip4vlistener:Ipv4Listener {uuid: '5ca95f7a-5175-4e7f-8a61-862e24485af3'}),
    (iplistener:IpListener {uuid: '91c999a0-33ec-42ee-bae0-6f8adae42535'})
MERGE (ip4vlistener)-[:ISA]->(iplistener);


// Ipv6Listener - An IPv6 listener.
MERGE (ipv6listener:Object:Component:Configuration:IpListener:Ipv6Listener {
    uuid: '6a79b4e9-13ce-4a09-bc5b-b429d23b34d7',
    name: 'Ipv6Listener',
    desc: 'An IPv6 listener (address and port, can be assigned to a network service).',
    addr: 'Fill in an IPv6 address here.',
    port: 'Fill in a port number between 0 and 65535 here.'
    });

MATCH (ipv6listener:Ipv6Listener {uuid: '6a79b4e9-13ce-4a09-bc5b-b429d23b34d7'}),
    (iplistener:IpListener {uuid: '91c999a0-33ec-42ee-bae0-6f8adae42535'})
MERGE (ipv6listener)-[:ISA]->(iplistener);


// Url - A uniform resource locator.
MERGE (url:Object:Component:Configuration:Url {
    uuid: '0e9f72bd-5bc5-43dc-ab86-30a8f7171bc6',
    name: 'Url',
    desc: 'a uniform resource locator.',
    url: 'scheme://domain:port/path?query_string#fragment_id'
    });

MATCH (url:Url {uuid: '0e9f72bd-5bc5-43dc-ab86-30a8f7171bc6'}),
    (configuration:Configuration {uuid: '3f4a43f7-b558-4bf0-ba7e-66b99cffaf16'})
MERGE (url)-[:ISA]->(configuration);


// DnsName - Either a short name or a fully-qualified domain name.
MERGE (dnsname:Object:Component:Configuration:DnsName {
    uuid: '99c0da37-3599-418d-8ecc-de36d37ae9f4',
    name: 'DnsName',
    desc: 'Either a short name or a fully-qualified domain name.',
    dnsname: 'somename'
    });

MATCH (dnsname:DnsName {uuid: '99c0da37-3599-418d-8ecc-de36d37ae9f4'}),
    (configuration:Configuration {uuid: '3f4a43f7-b558-4bf0-ba7e-66b99cffaf16'})
MERGE (dnsname)-[:ISA]->(configuration);


// Map - A configuration object that maps keys to multi-valued attributes in the JSON format.
MERGE (map:Object:Component:Configuration:Map {
    uuid: 'c970f6d3-4898-4596-bfac-9d6d1f6b2541',
    name: 'Map',
    desc: 'A configuration object that maps keys to multi-valued attributes in the JSON format.',
    map: 'JSON string containing the Map.'
    });

MATCH (map:Map {uuid: 'c970f6d3-4898-4596-bfac-9d6d1f6b2541'}),
    (configuration:Configuration {uuid: '3f4a43f7-b558-4bf0-ba7e-66b99cffaf16'})
MERGE (map)-[:ISA]->(configuration);


// DirectoryMap - A generic map of possible directories this software has. Key: os-shortname, Values: list of directories, Format: JSON.
MERGE (dirmap:Object:Component:Configuration:Map:DirectoryMap {
    uuid: '64d50e90-8be2-4d97-9dba-dd8518eb3f84',
    name: 'DirectoryMap',
    desc: 'A generic map of possible directories this software has. Key: os-shortname, Values: list of directories, Format: JSON.',
    map: 'JSON string containing the DirectoryMap.'
    });

MATCH (dirmap:DirectoryMap {uuid: '64d50e90-8be2-4d97-9dba-dd8518eb3f84'}),
    (map:Map {uuid: 'c970f6d3-4898-4596-bfac-9d6d1f6b2541'})
MERGE (dirmap)-[:ISA]->(map);


// PrefixDirectoryMap - A map of possible/known prefix directories for this software. Key: os-shortname, Values: list of directory prefixes, Format: JSON.
MERGE (prefixdirmap:Object:Component:Configuration:Map:DirectoryMap:PrefixDirectoryMap {
    uuid: 'd47e9ecd-d735-49a0-8d94-c4cb948ea226',
    name: 'PrefixDirectoryMap',
    desc: 'A map of possible/known prefix directories for this software. Key: os-shortname, Values: list of directory prefixes, Format: JSON.',
    map: 'JSON string containing the PrefixDirectoryMap.'
    });

MATCH (prefixdirmap:PrefixDirectoryMap {uuid: 'd47e9ecd-d735-49a0-8d94-c4cb948ea226'}),
    (dirmap:DirectoryMap {uuid: '64d50e90-8be2-4d97-9dba-dd8518eb3f84'})
MERGE (prefixdirmap)-[:ISA]->(dirmap);


// ConfigDirectoryMap - A map of possible/known configuration directories for this software. Key: os-shortname, Values: list of config directories, Format: JSON.
MERGE (configdirmap:Object:Component:Configuration:Map:DirectoryMap:ConfigDirectoryMap {
    uuid: 'fb83aefa-ec5e-4ad9-a807-4a37ffaa404b',
    name: 'ConfigDirectoryMap',
    desc: 'A map of possible/known configuration directories for this software. Key: os-shortname, Values: list of config directories, Format: JSON.',
    map: 'JSON string containing the ConfigDirectoryMap.'
    });

MATCH (configdirmap:ConfigDirectoryMap {uuid: 'fb83aefa-ec5e-4ad9-a807-4a37ffaa404b'}),
    (dirmap:DirectoryMap {uuid: '64d50e90-8be2-4d97-9dba-dd8518eb3f84'})
MERGE (configdirmap)-[:ISA]->(dirmap);


// BinaryDirectoryMap - A map of possible/known binary executable directories for this software. Key: os-shortname, Values: list of binary directories, Format: JSON.
MERGE (bindirmap:Object:Component:Configuration:Map:DirectoryMap:BinaryDirectoryMap {
    uuid: '33e5e211-88db-4a78-95cd-2f2a3cd9e297',
    name: 'BinaryDirectoryMap',
    desc: 'A map of possible/known binary executable directories for this software. Key: os-shortname, Values: list of binary directories, Format: JSON.',
    map: 'JSON string containing the BinaryDirectoryMap.'
    });

MATCH (bindirmap:BinaryDirectoryMap {uuid: '33e5e211-88db-4a78-95cd-2f2a3cd9e297'}),
    (dirmap:DirectoryMap {uuid: '64d50e90-8be2-4d97-9dba-dd8518eb3f84'})
MERGE (bindirmap)-[:ISA]->(dirmap);


// LibraryDirectoryMap - A map of possible/known dynamic link library directories for this software. Key: os-shortname, Values: list of library directories, Format: JSON.
MERGE (libdirmap:Object:Component:Configuration:Map:DirectoryMap:LibraryDirectoryMap {
    uuid: '8ccf2b1c-3650-408f-8560-4ab9f3a57bbe',
    name: 'LibraryDirectoryMap',
    desc: 'A map of possible/known dynamic link library directories for this software. Key: os-shortname, Values: list of library directories, Format: JSON.',
    map: 'JSON string containing the LibraryDirectoryMap.'
    });

MATCH (libdirmap:LibraryDirectoryMap {uuid: '8ccf2b1c-3650-408f-8560-4ab9f3a57bbe'}),
    (dirmap:DirectoryMap {uuid: '64d50e90-8be2-4d97-9dba-dd8518eb3f84'})
MERGE (libdirmap)-[:ISA]->(dirmap);


// DataDirectoryMap - A map of possible/known data directories for this software. Key: os-shortname, Values: list of data directories, Format: JSON.
MERGE (datadirmap:Object:Component:Configuration:Map:DirectoryMap:DataDirectoryMap {
    uuid: '07aaa5dd-a747-4474-850d-01ad00a2f533',
    name: 'DataDirectoryMap',
    desc: 'A map of possible/known data directories for this software. Key: os-shortname, Values: list of data directories, Format: JSON.',
    map: 'JSON string containing the DataDirectoryMap.'
    });

MATCH (datadirmap:DataDirectoryMap {uuid: '07aaa5dd-a747-4474-850d-01ad00a2f533'}),
    (dirmap:DirectoryMap {uuid: '64d50e90-8be2-4d97-9dba-dd8518eb3f84'})
MERGE (datadirmap)-[:ISA]->(dirmap);


// LogDirectoryMap - A map of possible/known log directories for this software. Key: os-shortname, Values: list of log directories, Format: JSON.
MERGE (logdirmap:Object:Component:Configuration:Map:DirectoryMap:LogDirectoryMap {
    uuid: 'f8a9ce96-2a50-4a80-8deb-ec6fb294638e',
    name: 'LogDirectoryMap',
    desc: 'A map of possible/known log directories for this software. Key: os-shortname, Values: list of log directories, Format: JSON.',
    map: 'JSON string containing the LogDirectoryMap.'
    });

MATCH (logdirmap:LogDirectoryMap {uuid: 'f8a9ce96-2a50-4a80-8deb-ec6fb294638e'}),
    (dirmap:DirectoryMap {uuid: '64d50e90-8be2-4d97-9dba-dd8518eb3f84'})
MERGE (logdirmap)-[:ISA]->(dirmap);


// FileMap - A generic map of possible/known files this software has. Key: os-shortname, Values: list of files, Format: JSON.
MERGE (filemap:Object:Component:Configuration:Map:FileMap {
    uuid: '35407e44-ec50-4d66-83f9-dacf1f1b49dd',
    name: 'FileMap',
    desc: 'A generic map of possible/known files this software has. Key: os-shortname, Values: list of files, Format: JSON.',
    map: 'JSON string containing the FileMap.'
    });

MATCH (filemap:FileMap {uuid: '35407e44-ec50-4d66-83f9-dacf1f1b49dd'}),
    (map:Map {uuid: 'c970f6d3-4898-4596-bfac-9d6d1f6b2541'})
MERGE (filemap)-[:ISA]->(map);


// ConfigFileMap - A map of possible/known configuration files for this software, non-full path, checked within ConfigDirectoryMap entries.
MERGE (configfilemap:Object:Component:Configuration:Map:FileMap:ConfigFileMap {
    uuid: 'a6125034-55e0-4604-bf21-407837d0b419',
    name: 'ConfigFileMap',
    desc: 'A map of possible/known configuration files for this software, non-full path, checked within ConfigDirectoryMap entries.',
    map: 'JSON string containing the ConfigFileMap.'
    });

MATCH (configfilemap:ConfigFileMap {uuid: 'a6125034-55e0-4604-bf21-407837d0b419'}),
    (filemap:FileMap {uuid: '35407e44-ec50-4d66-83f9-dacf1f1b49dd'})
MERGE (configfilemap)-[:ISA]->(filemap);


// BinaryFileMap - A map of possible/known binary executable files for this software, non-full path, checked within BinaryDirectoryMap entries.
MERGE (binfilemap:Object:Component:Configuration:Map:FileMap:BinaryFileMap {
    uuid: '5ffab714-fa0e-416c-a237-9b023fe27bd7',
    name: 'BinaryFileMap',
    desc: 'A map of possible/known binary executable files for this software, non-full path, checked within BinaryDirectoryMap entries.',
    map: 'JSON string containing the BinaryFileMap.'
    });

MATCH (binfilemap:BinaryFileMap {uuid: '5ffab714-fa0e-416c-a237-9b023fe27bd7'}),
    (filemap:FileMap {uuid: '35407e44-ec50-4d66-83f9-dacf1f1b49dd'})
MERGE (binfilemap)-[:ISA]->(filemap);


// LibraryFileMap - A map of possible/known dynamic link library files for this software, non-full path, checked within LibraryDirectoryMap entries.
MERGE (libfilemap:Object:Component:Configuration:Map:FileMap:LibraryFileMap {
    uuid: '99a6e35b-912e-41f1-b404-d24299e51fb2',
    name: 'LibraryFileMap',
    desc: 'A map of possible/known dynamic link library files for this software, non-full path, checked within LibraryDirectoryMap entries.',
    map: 'JSON string containing the LibraryFileMap.'
    });

MATCH (libfilemap:LibraryFileMap {uuid: '99a6e35b-912e-41f1-b404-d24299e51fb2'}),
    (filemap:FileMap {uuid: '35407e44-ec50-4d66-83f9-dacf1f1b49dd'})
MERGE (libfilemap)-[:ISA]->(filemap);


// DataFileMap - A map of possible/known data files for this software, non-full path, checked within DataDirectoryMap entries.
MERGE (datafilemap:Object:Component:Configuration:Map:FileMap:DataFileMap {
    uuid: 'b19412ae-dffe-4fe8-9f00-3207ab686fb4',
    name: 'DataFileMap',
    desc: 'A map of possible/known data files for this software, non-full path, checked within DataDirectoryMap entries.',
    map: 'JSON string containing the DataFileMap.'
    });

MATCH (datafilemap:DataFileMap {uuid: 'b19412ae-dffe-4fe8-9f00-3207ab686fb4'}),
    (filemap:FileMap {uuid: '35407e44-ec50-4d66-83f9-dacf1f1b49dd'})
MERGE (datafilemap)-[:ISA]->(filemap);


// LogFileMap - A map of possible/known log files for this software, non-full path, checked within LogDirectoryMap entries.
MERGE (logfilemap:Object:Component:Configuration:Map:FileMap:LogFileMap {
    uuid: 'ac583c10-f77c-4d40-84eb-4ffed77c79e0',
    name: 'LogFileMap',
    desc: 'A map of possible/known log files for this software, non-full path, checked within LogDirectoryMap entries.',
    map: 'JSON string containing the LogFileMap.'
    });

MATCH (logfilemap:LogFileMap {uuid: 'ac583c10-f77c-4d40-84eb-4ffed77c79e0'}),
    (filemap:FileMap {uuid: '35407e44-ec50-4d66-83f9-dacf1f1b49dd'})
MERGE (logfilemap)-[:ISA]->(filemap);


// NameMap - A generic map of possible/known names this software has. Key: os-shortname, Values: list of names, Format: JSON.
MERGE (namemap:Object:Component:Configuration:Map:NameMap {
    uuid: '14372d63-a428-46b9-8359-9982ef22cfce',
    name: 'NameMap',
    desc: 'A generic map of possible/known names this software has. Key: os-shortname, Values: list of names, Format: JSON.',
    map: 'JSON string containing the NameMap.'
    });

MATCH (namemap:NameMap {uuid: '14372d63-a428-46b9-8359-9982ef22cfce'}),
    (map:Map {uuid: 'c970f6d3-4898-4596-bfac-9d6d1f6b2541'})
MERGE (namemap)-[:ISA]->(map);


// PackageNameMap - A generic map of possible/known package names this software has. Key: os-shortname, Values: list of names, Format: JSON.
MERGE (pkgmap:Object:Component:Configuration:Map:NameMap:PackageNameMap {
    uuid: '1d691f61-3c9f-46ad-b98b-b968352d59b3',
    name: 'PackageNameMap',
    desc: 'A generic map of possible/known package names this software has. Key: os-shortname, Values: list of names, Format: JSON.',
    map: 'JSON string containing the PackageNameMap.'
    });

MATCH (pkgmap:PackageNameMap {uuid: '1d691f61-3c9f-46ad-b98b-b968352d59b3'}),
    (namemap:NameMap {uuid: '14372d63-a428-46b9-8359-9982ef22cfce'})
MERGE (pkgmap)-[:ISA]->(namemap);


// ServiceNameMap - A generic map of possible/known service names this software has. Key: os-shortname, Values: list of names, Format: JSON.
MERGE (svcmap:Object:Component:Configuration:Map:NameMap:ServiceNameMap {
    uuid: 'ead61277-e797-406b-96e1-95c155655fac',
    name: 'ServiceNameMap',
    desc: 'A generic map of possible/known service names this software has. Key: os-shortname, Values: list of names, Format: JSON.',
    map: 'JSON string containing the ServiceNameMap.'
    });

MATCH (svcmap:ServiceNameMap {uuid: 'ead61277-e797-406b-96e1-95c155655fac'}),
    (namemap:NameMap {uuid: '14372d63-a428-46b9-8359-9982ef22cfce'})
MERGE (svcmap)-[:ISA]->(namemap);


// Device - Any kind of device.
MERGE (device:Object:Component:Device {
    uuid: 'ea6a7487-867c-4a74-8849-fb8539b8169f',
    name: 'Device',
    desc: 'An abstract device component.',
    rev: 'The revision of the device.'
    });

MATCH (device:Device {uuid: 'ea6a7487-867c-4a74-8849-fb8539b8169f'}),
    (component:Component {uuid: 'a79ce509-a021-4177-ae61-73abd01fb479'})
MERGE (device)-[:ISA]->(component);


// AssembledDevice - Any assembled device.
MERGE (assembleddevice:Object:Component:Device:AssembledDevice {
    uuid: 'dbb2884f-7c31-4f7d-a92e-d6a76dd1b7fe',
    name: 'AssembledDevice',
    desc: 'An assembled device, containing various components.',
    rev: 'The revision of the assembled device.'
    });

MATCH (assembleddevice:AssembledDevice {uuid: 'dbb2884f-7c31-4f7d-a92e-d6a76dd1b7fe'}),
    (device:Device {uuid: 'ea6a7487-867c-4a74-8849-fb8539b8169f'})
MERGE (assembleddevice)-[:ISA]->(device);


// Computer - An assembled computer device.
MERGE (computer:Object:Component:Device:AssembledDevice:Computer {
    uuid: 'ec11db91-6973-4285-b018-b461fbb432d7',
    name: 'Computer',
    desc: 'An assembled computer device.',
    rev: 'The revision of the computer device.'
    });

MATCH (computer:Computer {uuid: 'ec11db91-6973-4285-b018-b461fbb432d7'}),
    (assembleddevice:AssembledDevice {uuid: 'dbb2884f-7c31-4f7d-a92e-d6a76dd1b7fe'})
MERGE (computer)-[:ISA]->(assembleddevice);


// ConsoleDevice - A console device.
MERGE (consoledevice:Object:Component:Device:ConsoleDevice {
    uuid: '3d0865a2-3ecf-4035-886b-0bb603c0e2c2',
    name: 'ConsoleDevice',
    desc: 'A console device.',
    rev: 'The revision of the console device.',
    baudRate: '110,300,600,1200,2400,4800,9600,14400,19200,28800,38400,56000,57600 or 115200.',
    dataBits: '5,6,7,8 or 9.',
    parity: 'none (N), odd (O), even (E), mark (M), or space (S).',
    stopBits: '1, 1.5 or 2.'
    });

MATCH (consoledevice:ConsoleDevice {uuid: '3d0865a2-3ecf-4035-886b-0bb603c0e2c2'}),
    (device:Device {uuid: 'ea6a7487-867c-4a74-8849-fb8539b8169f'})
MERGE (consoledevice)-[:ISA]->(device);


// GenericSerial - A serial communication device.
MERGE (genericserial:Object:Component:Device:ConsoleDevice:GenericSerial {
    uuid: 'f5b25736-ec12-4a7e-b198-548b97ebc36f',
    name: 'GenericSerial',
    desc: 'A serial communication device.',
    rev: 'A',
    baudRate: '9600',
    dataBits: '8',
    parity: 'N',
    stopBits: '1'
    });

MATCH (genericserial:GenericSerial {uuid: 'f5b25736-ec12-4a7e-b198-548b97ebc36f'}),
    (consoledevice:ConsoleDevice {uuid: '3d0865a2-3ecf-4035-886b-0bb603c0e2c2'})
MERGE (genericserial)-[:ISA]->(consoledevice);


// GraphicsDevice - A grapical device.
MERGE (graphicsdevice:Object:Component:Device:GraphicsDevice {
    uuid: 'a642b995-c073-4c27-a611-c5bf3854958f',
    name: 'GraphicsDevice',
    desc: 'A graphics device.',
    rev: 'The revision of the graphics device.',
    gpuClockHz: 'Clockspeed of the GPU in Hz.',
    gpuCoreCount: 'Number of GPU cores if applicable.',
    maxColorBitsPerChannel: 'Maximum number of bits to describe color in a color channel (R,G,B,A).',
    maxColorBitsPerScreen: 'Maximum number of bits per pixel on a screen.',
    maxVerticalRefreshRateHz: 'Max',
    memClockHz: 'Memory speed in Hz.',
    memSizeKb: 'Memory size in Kb.',
    memBusBits: 'Memory bus width in bits.',
    memBandwidthKbs: 'Memory bandwidth in kilobytes per second.',
    shadingUnits: 'Number of shading units.',
    textureMapUnits: 'Number of texture mapping units.',
    renderOutputCores: 'Number of render output cores.',
    pixelRatesPerSec: 'Pixel fillrate, in pixels per second.',
    texelRatesPerSec: 'Texture fillrate, in texels per second.',
    flops: 'Number of floating-point operations per second.'
    });

MATCH (graphicsdevice:GraphicsDevice {uuid: 'a642b995-c073-4c27-a611-c5bf3854958f'}),
    (device:Device {uuid: 'ea6a7487-867c-4a74-8849-fb8539b8169f'})
MERGE (graphicsdevice)-[:ISA]->(device);


// GenericVga - A VGA graphics device.
MERGE (genericvga:Object:Component:Device:GraphicsDevice:GenericVga {
    uuid: 'b3a2ac7c-ad8a-4047-a460-2ac41a7ddfef',
    name: 'GenericVga',
    desc: 'A VGA graphics device.',
    rev: 'A',
    gpuClockHz: '28322',
    gpuCoreCount: '1',
    maxColorBitsPerChannel: '6',
    maxColorBitsPerScreen: '8',
    maxVerticalRefreshRateHz: '70',
    memClockHz: '833',
    memSizeKb: '256',
    memBusBits: '16',
    memBandwidthKbs: '16384',
    shadingUnits: '0',
    textureMapUnits: '0',
    renderOutputCores: '0',
    pixelRatesPerSec: '',
    texelRatesPerSec: '0',
    flops: ''
    });

MATCH (genericvga:GenericVga {uuid: 'b3a2ac7c-ad8a-4047-a460-2ac41a7ddfef'}),
    (graphicsdevice:GraphicsDevice {uuid: 'a642b995-c073-4c27-a611-c5bf3854958f'})
MERGE (genericvga)-[:ISA]->(graphicsdevice);


// MemoryDevice - A memory device.
MERGE (memorydevice:Object:Component:Device:MemoryDevice {
    uuid: '1c87c672-1f69-4a02-9bc8-9eff28dda5e5',
    name: 'MemoryDevice',
    desc: 'A memory device',
    rev: 'The revision of the memory device.',
    memSizeKb: 'Memory size in Kb.'
    });

MATCH (memorydevice:MemoryDevice {uuid: '1c87c672-1f69-4a02-9bc8-9eff28dda5e5'}),
    (device:Device {uuid: 'ea6a7487-867c-4a74-8849-fb8539b8169f'})
MERGE (memorydevice)-[:ISA]->(device);


// VolatileMemory - A volatile memory device.
MERGE (volatilememory:Object:Component:Device:MemoryDevice:VolatileMemory {
    uuid: '44131d46-c1b0-4030-ae5a-9a3108627749',
    name: 'VolatileMemory',
    desc: 'A generic volatile memory device.',
    rev: 'The revision of the volatile device.',
    memSizeKb: 'Memory size in Kb.',
    memClockHz: 'Memory speed in Hz.'
    });

MATCH (volatilememory:VolatileMemory {uuid: '44131d46-c1b0-4030-ae5a-9a3108627749'}),
    (memorydevice:MemoryDevice {uuid: '1c87c672-1f69-4a02-9bc8-9eff28dda5e5'})
MERGE (volatilememory)-[:ISA]->(memorydevice);


// RandomAccessMemory - Random access memory.
MERGE (randomaccessmemory:Object:Component:Device:MemoryDevice:VolatileMemory:RandomAccessMemory {
    uuid: '4c0a0638-1fa7-4f5b-9d69-1d199dcedccc',
    name: 'RandomAccessMemory',
    desc: 'Random access memory.',
    rev: 'The revision of the random access memory device.',
    memSizeKb: 'Memory size in Kb.',
    memClockHz: 'Memory speed in Hz.'
    });

MATCH (randomaccessmemory:RandomAccessMemory {uuid: '4c0a0638-1fa7-4f5b-9d69-1d199dcedccc'}),
    (volatilememory:VolatileMemory {uuid: '44131d46-c1b0-4030-ae5a-9a3108627749'})
MERGE (randomaccessmemory)-[:ISA]->(volatilememory);


// NonVolatileMemory - A volatile memory device.
MERGE (nonvolatilememory:Object:Component:Device:MemoryDevice:NonVolatileMemory {
    uuid: '7f129e53-ff07-4ed8-9062-daf5f733a3a6',
    name: 'NonVolatileMemory',
    desc: 'A generic non-volatile memory device.',
    rev: 'The revision of the non-volatile memory device.',
    memSizeKb: 'Memory size in Kb.'
    });

MATCH (nonvolatilememory:NonVolatileMemory {uuid: '7f129e53-ff07-4ed8-9062-daf5f733a3a6'}),
    (memorydevice:MemoryDevice {uuid: '1c87c672-1f69-4a02-9bc8-9eff28dda5e5'})
MERGE (nonvolatilememory)-[:ISA]->(memorydevice);


// HardDiskDrive - A non-volatile harddisk-based storage device.
MERGE (harddiskdrive:Object:Component:Device:MemoryDevice:NonVolatileMemory:HardDiskDrive {
    uuid: '24cc3545-0d1a-4d85-8d08-a83c3dbe5912',
    name: 'HardDiskDrive',
    desc: 'A non-volatile harddisk-based storage device.',
    rev: 'The revision of the hard disk drive device.',
    memSizeKb: 'Memory size in Kb.'
    });

MATCH (harddiskdrive:HardDiskDrive {uuid: '24cc3545-0d1a-4d85-8d08-a83c3dbe5912'}),
    (nonvolatilememory:NonVolatileMemory {uuid: '7f129e53-ff07-4ed8-9062-daf5f733a3a6'})
MERGE (harddiskdrive)-[:ISA]->(nonvolatilememory);


// SolidStateDrive - A non-volatile solid-state storage device.
MERGE (solidstatedrive:Object:Component:Device:MemoryDevice:NonVolatileMemory:SolidStateDrive {
    uuid: '7a60ec32-0a9a-40b9-bf1f-d69674d4a233',
    name: 'SolidStateDrive',
    desc: 'A non-volatile solid-state storage device.',
    rev: 'The revision of the solid-state storage device.',
    memSizeKb: 'Memory size in Kb.'
    });

MATCH (solidstatedrive:SolidStateDrive {uuid: '7a60ec32-0a9a-40b9-bf1f-d69674d4a233'}),
    (nonvolatilememory:NonVolatileMemory {uuid: '7f129e53-ff07-4ed8-9062-daf5f733a3a6'})
MERGE (solidstatedrive)-[:ISA]->(nonvolatilememory);


// FloppyDrive - A floppy disk drive.
MERGE (floppydrive:Object:Component:Device:MemoryDevice:NonVolatileMemory:FloppyDrive {
    uuid: '118ec961-a823-465b-be17-b706f1c8c2f1',
    name: 'FloppyDrive',
    desc: 'A floppy disk drive.',
    rev: 'The revision of the floppy disk drive device.',
    memSizeKb: 'Memory size in Kb.'
    });

MATCH (floppydrive:FloppyDrive {uuid: '118ec961-a823-465b-be17-b706f1c8c2f1'}),
    (nonvolatilememory:NonVolatileMemory {uuid: '7f129e53-ff07-4ed8-9062-daf5f733a3a6'})
MERGE (floppydrive)-[:ISA]->(nonvolatilememory);


// TapeDrive - A floppy disk drive.
MERGE (tapedrive:Object:Component:Device:MemoryDevice:NonVolatileMemory:TapeDrive {
    uuid: 'a507461f-647b-4b79-936a-721ca2dba145',
    name: 'TapeDrive',
    desc: 'A tape drive.',
    rev: 'The revision of the tape drive device.',
    memSizeKb: 'Memory size in Kb.'
    });

MATCH (tapedrive:TapeDrive {uuid: 'a507461f-647b-4b79-936a-721ca2dba145'}),
    (nonvolatilememory:NonVolatileMemory {uuid: '7f129e53-ff07-4ed8-9062-daf5f733a3a6'})
MERGE (tapedrive)-[:ISA]->(nonvolatilememory);


// CdDrive - A Compact Disc drive.
MERGE (cddrive:Object:Component:Device:MemoryDevice:NonVolatileMemory:CdDrive {
    uuid: '30af2fe3-aa05-4912-a3d5-dddfdacf74e2',
    name: 'CdDrive',
    desc: 'A Compact Disc drive.',
    rev: 'The revision of the compact disk drive device.',
    memSizeKb: 'Memory size in Kb.'
    });

MATCH (cddrive:CdDrive {uuid: '30af2fe3-aa05-4912-a3d5-dddfdacf74e2'}),
    (nonvolatilememory:NonVolatileMemory {uuid: '7f129e53-ff07-4ed8-9062-daf5f733a3a6'})
MERGE (cddrive)-[:ISA]->(nonvolatilememory);


// DvdDrive - A Digital Video Disc drive.
MERGE (dvddrive:Object:Component:Device:MemoryDevice:NonVolatileMemory:DvdDrive {
    uuid: '23ab95d2-2815-4121-a81c-63aca40cdcfa',
    name: 'DvdDrive',
    desc: 'A Digital Video Disc drive.',
    rev: 'The revision of the digital video disk drive device.',
    memSizeKb: 'Memory size in Kb.'
    });

MATCH (dvddrive:DvdDrive {uuid: '23ab95d2-2815-4121-a81c-63aca40cdcfa'}),
    (nonvolatilememory:NonVolatileMemory {uuid: '7f129e53-ff07-4ed8-9062-daf5f733a3a6'})
MERGE (dvddrive)-[:ISA]->(nonvolatilememory);


// BdDrive - A Blu-Ray drive.
MERGE (bddrive:Object:Component:Device:MemoryDevice:NonVolatileMemory:BdDrive {
    uuid: 'cbc27482-11d1-4890-a3e1-e9f80baea82c',
    name: 'BdDrive',
    desc: 'A Digital Video Disc drive.',
    rev: 'The revision of the blu-ray disk drive device.',
    memSizeKb: 'Memory size in Kb.'
    });

MATCH (bddrive:BdDrive {uuid: 'cbc27482-11d1-4890-a3e1-e9f80baea82c'}),
    (nonvolatilememory:NonVolatileMemory {uuid: '7f129e53-ff07-4ed8-9062-daf5f733a3a6'})
MERGE (bddrive)-[:ISA]->(nonvolatilememory);


// NetworkDevice - A network device.
MERGE (networkdevice:Object:Component:Device:NetworkDevice {
    uuid: '86786ed6-d62a-4011-9ae8-0082000e48fa',
    name: 'NetworkDevice',
    desc: 'A network device.',
    rev: 'The revision of the network device.'
    });

MATCH (networkdevice:NetworkDevice {uuid: '86786ed6-d62a-4011-9ae8-0082000e48fa'}),
    (device:Device {uuid: 'ea6a7487-867c-4a74-8849-fb8539b8169f'})
MERGE (networkdevice)-[:ISA]->(device);


// WiredNetworkDevice - A wired networking device.
MERGE (wirednetworkdevice:Object:Component:Device:NetworkDevice:WiredNetworkDevice {
    uuid: '0d4c880d-d888-435f-b3a3-3d3eb9e70ddf',
    name: 'WiredNetworkDevice',
    desc: 'A wired network device.',
    rev: 'The revision of the wired network device.'
    });

MATCH (wirednetworkdevice:WiredNetworkDevice {uuid: '0d4c880d-d888-435f-b3a3-3d3eb9e70ddf'}),
    (networkdevice:NetworkDevice {uuid: '86786ed6-d62a-4011-9ae8-0082000e48fa'})
MERGE (wirednetworkdevice)-[:ISA]->(networkdevice);


// ModemDevice - A modem networking device.
MERGE (modemdevice:Object:Component:Device:NetworkDevice:WiredNetworkDevice:ModemDevice {
    uuid: 'f34b76e4-f5a6-4bae-968e-f22ef09b1e35',
    name: 'ModemDevice',
    desc: 'A modem networking device.',
    rev: 'The revision of the modem networking device.',
    baudRate: '110,300,600,1200,2400,4800,9600,14400,19200,28800,38400,56000,57600 or 115200.'
    });

MATCH (modemdevice:ModemDevice {uuid: 'f34b76e4-f5a6-4bae-968e-f22ef09b1e35'}),
    (wirednetworkdevice:WiredNetworkDevice {uuid: '0d4c880d-d888-435f-b3a3-3d3eb9e70ddf'})
MERGE (modemdevice)-[:ISA]->(wirednetworkdevice);


// EthernetDevice - A wired ethernet device.
MERGE (ethernetdevice:Object:Component:Device:NetworkDevice:WiredNetworkDevice:EthernetDevice {
    uuid: '0dbd26f2-baff-446c-9241-46eaa3e03736',
    name: 'EthernetDevice',
    desc: 'A wired ethernet device.',
    rev: 'The revision of the ethernet network device.',
    macAddr: 'A media access control address.',
    speedMbit: 'Speed in Mbits.'
    });

MATCH (ethernetdevice:EthernetDevice {uuid: '0dbd26f2-baff-446c-9241-46eaa3e03736'}),
    (wirednetworkdevice:WiredNetworkDevice {uuid: '0d4c880d-d888-435f-b3a3-3d3eb9e70ddf'})
MERGE (ethernetdevice)-[:ISA]->(wirednetworkdevice);


// WirelessNetworkDevice - A wireless device.
MERGE (wirelessnetworkdevice:Object:Component:Device:NetworkDevice:WirelessNetworkDevice {
    uuid: '61328f83-db43-4efe-81be-df47e17814a5',
    name: 'WirelessNetworkDevice',
    desc: 'A wireless network device.',
    rev: 'The revision of the wireless network device.'
    });

MATCH (wirelessnetworkdevice:WirelessNetworkDevice {uuid: '61328f83-db43-4efe-81be-df47e17814a5'}),
    (networkdevice:NetworkDevice {uuid: '86786ed6-d62a-4011-9ae8-0082000e48fa'})
MERGE (wirelessnetworkdevice)-[:ISA]->(networkdevice);


// BluetoothDevice - A Bluetooth device.
MERGE (bluetoothdevice:Object:Component:Device:NetworkDevice:WirelessNetworkDevice:BluetoothDevice {
    uuid: '009cf823-39c0-4b17-ae81-ad98ed0d4585',
    name: 'BluetoothDevice',
    desc: 'A Bluetooth device.',
    rev: 'The revision of the bluetooth network device.',
    macAddr: 'A media access control address.',
    speedKbit: 'Speed in Kbits.'
    });

MATCH (bluetoothdevice:BluetoothDevice {uuid: '009cf823-39c0-4b17-ae81-ad98ed0d4585'}),
    (wirelessnetworkdevice:WirelessNetworkDevice {uuid: '61328f83-db43-4efe-81be-df47e17814a5'})
MERGE (bluetoothdevice)-[:ISA]->(wirelessnetworkdevice);


// IrdaDevice - An IrDA device.
MERGE (irdadevice:Object:Component:Device:NetworkDevice:WirelessNetworkDevice:IrdaDevice {
    uuid: '42c7997c-0c56-4f93-956f-5fd0778c47c6',
    name: 'IrdaDevice',
    desc: 'An IrDA device.',
    rev: 'The revision of the irda network device.',
    speedKbit: 'Speed in Kbits.'
    });

MATCH (irdadevice:IrdaDevice {uuid: '42c7997c-0c56-4f93-956f-5fd0778c47c6'}),
    (wirelessnetworkdevice:WirelessNetworkDevice {uuid: '61328f83-db43-4efe-81be-df47e17814a5'})
MERGE (irdadevice)-[:ISA]->(wirelessnetworkdevice);


// WifiDevice - A WiFi device.
MERGE (wifidevice:Object:Component:Device:NetworkDevice:WirelessNetworkDevice:WifiDevice {
    uuid: '78ff02be-2abe-4a8f-a082-a0d9c3cdae70',
    name: 'WifiDevice',
    desc: 'A WiFi device.',
    rev: 'The revision of the WiFi network device.',
    macAddr: 'A media access control address.',
    speedMbit: 'Speed in Mbits.'
    });

MATCH (wifidevice:WifiDevice {uuid: '78ff02be-2abe-4a8f-a082-a0d9c3cdae70'}),
    (wirelessnetworkdevice:WirelessNetworkDevice {uuid: '61328f83-db43-4efe-81be-df47e17814a5'})
MERGE (wifidevice)-[:ISA]->(wirelessnetworkdevice);


// WwanDevice - A Wireless WAN device.
MERGE (wwandevice:Object:Component:Device:NetworkDevice:WirelessNetworkDevice:WwanDevice {
    uuid: '8d6e0956-033e-4efc-8846-e85bfddc8f98',
    name: 'WwanDevice',
    desc: 'A wireless WAN device.',
    rev: 'The revision of the wireless WAN network device.',
    speedKbit: 'Speed in Kbits.'
    });

MATCH (wwandevice:WwanDevice {uuid: '8d6e0956-033e-4efc-8846-e85bfddc8f98'}),
    (wirelessnetworkdevice:WirelessNetworkDevice {uuid: '61328f83-db43-4efe-81be-df47e17814a5'})
MERGE (wwandevice)-[:ISA]->(wirelessnetworkdevice);


// ProcessingUnit - A processing unit.
MERGE (processingunit:Object:Component:Device:ProcessingUnit {
    uuid: 'b7b2a2ff-3af7-469b-afbf-e52f1b80be91',
    name: 'ProcessingUnit',
    desc: 'A processing unit.',
    rev: 'The revision of the processing unit.',
    cpuClockHz: 'CPU clock in hertz.',
    cpuCoreCount: 'Number of cores.',
    smtEnabled: "Simultaneous multithreading (for example hyper-threading) capable true/false.",
    cacheSizeKb: 'The size of cache memory on the CPU.',
    bits: 'Number of bit width supported.'
    });

MATCH (processingunit:ProcessingUnit {uuid: 'b7b2a2ff-3af7-469b-afbf-e52f1b80be91'}),
    (device:Device {uuid: 'ea6a7487-867c-4a74-8849-fb8539b8169f'})
MERGE (processingunit)-[:ISA]->(device);


// AlphaProcessor - An Alpha processor.
MERGE (alphaprocessor:Object:Component:Device:ProcessingUnit:AlphaProcessor {
    uuid: '95b9f7a2-1a17-4cb9-8088-3e2e478ce638',
    name: 'AlphaProcessor',
    desc: 'An Alpha processor.',
    rev: 'The revision of the processing unit.',
    cpuClockHz: 'CPU clock in hertz.',
    cpuCoreCount: 'Number of cores.',
    smtEnabled: "Simultaneous multithreading (for example hyper-threading) capable true/false.",
    cacheSizeKb: 'The size of cache memory on the CPU.',
    bits: 'Number of bit width supported.'
    });

MATCH (alphaprocessor:AlphaProcessor {uuid: '95b9f7a2-1a17-4cb9-8088-3e2e478ce638'}),
    (processingunit:ProcessingUnit {uuid: 'b7b2a2ff-3af7-469b-afbf-e52f1b80be91'})
MERGE (alphaprocessor)-[:ISA]->(processingunit);


// ArmProcessor- An ARM processor.
MERGE (armprocessor:Object:Component:Device:ProcessingUnit:ArmProcessor {
    uuid: 'f3b1b497-1e83-4a09-a280-04f46a83fe24',
    name: 'ArmProcessor',
    desc: 'An ARM processor.',
    rev: 'The revision of the processing unit.',
    cpuClockHz: 'CPU clock in hertz.',
    cpuCoreCount: 'Number of cores.',
    smtEnabled: "Simultaneous multithreading (for example hyper-threading) capable true/false.",
    cacheSizeKb: 'The size of cache memory on the CPU.',
    bits: 'Number of bit width supported.'
    });

MATCH (armprocessor:ArmProcessor {uuid: 'f3b1b497-1e83-4a09-a280-04f46a83fe24'}),
    (processingunit:ProcessingUnit {uuid: 'b7b2a2ff-3af7-469b-afbf-e52f1b80be91'})
MERGE (armprocessor)-[:ISA]->(processingunit);


// ItaniumProcessor - An Itanium processor.
MERGE (itaniumprocessor:Object:Component:Device:ProcessingUnit:ItaniumProcessor {
    uuid: '241015c9-72f5-491f-a870-a30d8f7902fd',
    name: 'ItaniumProcessor',
    desc: 'An Itanium processor.',
    rev: 'The revision of the processing unit.',
    cpuClockHz: 'CPU clock in hertz.',
    cpuCoreCount: 'Number of cores.',
    smtEnabled: "Simultaneous multithreading (for example hyper-threading) capable true/false.",
    cacheSizeKb: 'The size of cache memory on the CPU.',
    bits: 'Number of bit width supported.'
    });

MATCH (itaniumprocessor:ItaniumProcessor {uuid: '241015c9-72f5-491f-a870-a30d8f7902fd'}),
    (processingunit:ProcessingUnit {uuid: 'b7b2a2ff-3af7-469b-afbf-e52f1b80be91'})
MERGE (itaniumprocessor)-[:ISA]->(processingunit);


// M68KProcessor - A Motorola 68000 series processor.
MERGE (m68kprocessor:Object:Component:Device:ProcessingUnit:M68kProcessor {
    uuid: '33920524-4989-4322-a0ca-1cea614d7fef',
    name: 'M68kProcessor',
    desc: 'A Motorola 68000 series processor.',
    rev: 'The revision of the processing unit.',
    cpuClockHz: 'CPU clock in hertz.',
    cpuCoreCount: 'Number of cores.',
    smtEnabled: "Simultaneous multithreading (for example hyper-threading) capable true/false.",
    cacheSizeKb: 'The size of cache memory on the CPU.',
    bits: 'Number of bit width supported.'
    });

MATCH (m68kprocessor:M68kProcessor {uuid: '33920524-4989-4322-a0ca-1cea614d7fef'}),
    (processingunit:ProcessingUnit {uuid: 'b7b2a2ff-3af7-469b-afbf-e52f1b80be91'})
MERGE (m68kprocessor)-[:ISA]->(processingunit);


// MipsProcessor - A MIPS processor.
MERGE (mipsprocessor:Object:Component:Device:ProcessingUnit:MipsProcessor {
    uuid: 'cbe81157-97f6-401b-bbaa-cfbc4f479298',
    name: 'MipsProcessor',
    desc: 'A Motorola 68000 series processor.',
    rev: 'The revision of the processing unit.',
    cpuClockHz: 'CPU clock in hertz.',
    cpuCoreCount: 'Number of cores.',
    smtEnabled: "Simultaneous multithreading (for example hyper-threading) capable true/false.",
    cacheSizeKb: 'The size of cache memory on the CPU.',
    bits: 'Number of bit width supported.'
    });

MATCH (mipsprocessor:MipsProcessor {uuid: 'cbe81157-97f6-401b-bbaa-cfbc4f479298'}),
    (processingunit:ProcessingUnit {uuid: 'b7b2a2ff-3af7-469b-afbf-e52f1b80be91'})
MERGE (mipsprocessor)-[:ISA]->(processingunit);


// MosProcessor - A MOS Technology processor.
MERGE (mosprocessor:Object:Component:Device:ProcessingUnit:MosProcessor {
    uuid: 'b4f67698-7eca-4ca1-a4cf-b7ef9737785c',
    name: 'MosProcessor',
    desc: 'A MOS Technology processor.',
    rev: 'The revision of the processing unit.',
    cpuClockHz: 'CPU clock in hertz.',
    cpuCoreCount: 'Number of cores.',
    smtEnabled: "Simultaneous multithreading (for example hyper-threading) capable true/false.",
    cacheSizeKb: 'The size of cache memory on the CPU.',
    bits: 'Number of bit width supported.'
    });

MATCH (mosprocessor:MosProcessor {uuid: 'b4f67698-7eca-4ca1-a4cf-b7ef9737785c'}),
    (processingunit:ProcessingUnit {uuid: 'b7b2a2ff-3af7-469b-afbf-e52f1b80be91'})
MERGE (mosprocessor)-[:ISA]->(processingunit);


// PariscProcessor - An PA Risc processor.
MERGE (pariscprocessor:Object:Component:Device:ProcessingUnit:PariscProcessor {
    uuid: '689a4f33-e08d-4e7f-83a5-79fd310bd3f9',
    name: 'PariscProcessor',
    desc: 'A PA Risc processor.',
    rev: 'The revision of the processing unit.',
    cpuClockHz: 'CPU clock in hertz.',
    cpuCoreCount: 'Number of cores.',
    smtEnabled: "Simultaneous multithreading (for example hyper-threading) capable true/false.",
    cacheSizeKb: 'The size of cache memory on the CPU.',
    bits: 'Number of bit width supported.'
    });

MATCH (pariscprocessor:PariscProcessor {uuid: '689a4f33-e08d-4e7f-83a5-79fd310bd3f9'}),
    (processingunit:ProcessingUnit {uuid: 'b7b2a2ff-3af7-469b-afbf-e52f1b80be91'})
MERGE (pariscprocessor)-[:ISA]->(processingunit);


// PowerProcessor - A PowerPC processor.
MERGE (powerprocessor:Object:Component:Device:ProcessingUnit:PowerProcessor {
    uuid: '808f2737-feca-4b70-aa1a-53410b970b10',
    name: 'PowerProcessor',
    desc: 'A PowerPC processor.',
    rev: 'The revision of the processing unit.',
    cpuClockHz: 'CPU clock in hertz.',
    cpuCoreCount: 'Number of cores.',
    smtEnabled: "Simultaneous multithreading (for example hyper-threading) capable true/false.",
    cacheSizeKb: 'The size of cache memory on the CPU.',
    bits: 'Number of bit width supported.'
    });

MATCH (powerprocessor:PowerProcessor {uuid: '808f2737-feca-4b70-aa1a-53410b970b10'}),
    (processingunit:ProcessingUnit {uuid: 'b7b2a2ff-3af7-469b-afbf-e52f1b80be91'})
MERGE (powerprocessor)-[:ISA]->(processingunit);


// SparcProcessor - A SPARC processor.
MERGE (sparcprocessor:Object:Component:Device:ProcessingUnit:SparcProcessor {
    uuid: '798ba8d1-8da7-471a-9efa-30530569b6a1',
    name: 'SparcProcessor',
    desc: 'A SPARC processor.',
    rev: 'The revision of the processing unit.',
    cpuClockHz: 'CPU clock in hertz.',
    cpuCoreCount: 'Number of cores.',
    smtEnabled: "Simultaneous multithreading (for example hyper-threading) capable true/false.",
    cacheSizeKb: 'The size of cache memory on the CPU.',
    bits: 'Number of bit width supported.'
    });

MATCH (sparcprocessor:SparcProcessor {uuid: '798ba8d1-8da7-471a-9efa-30530569b6a1'}),
    (processingunit:ProcessingUnit {uuid: 'b7b2a2ff-3af7-469b-afbf-e52f1b80be91'})
MERGE (sparcprocessor)-[:ISA]->(processingunit);


// X86Processor - An x86-based processor.
MERGE (x86processor:Object:Component:Device:ProcessingUnit:X86Processor {
    uuid: '4d4971b6-43b3-4c14-a82a-c51d72ba23a5',
    name: 'X86Processor',
    desc: 'An x86-based processor.',
    rev: 'The revision of the processing unit.',
    cpuClockHz: 'CPU clock in hertz.',
    cpuCoreCount: 'Number of cores.',
    smtEnabled: "Simultaneous multithreading (for example hyper-threading) capable true/false.",
    cacheSizeKb: 'The size of cache memory on the CPU.',
    bits: 'Number of bit width supported.'
    });

MATCH (x86processor:X86Processor {uuid: '4d4971b6-43b3-4c14-a82a-c51d72ba23a5'}),
    (processingunit:ProcessingUnit {uuid: 'b7b2a2ff-3af7-469b-afbf-e52f1b80be91'})
MERGE (x86processor)-[:ISA]->(processingunit);


// ZilogProcessor - A ZiLOG processor.
MERGE (zilogprocessor:Object:Component:Device:ProcessingUnit:ZilogProcessor {
    uuid: 'a68f42c5-2a1d-43cb-bebd-55339f5e61ee',
    name: 'ZilogProcessor',
    desc: 'A ZiLOG processor.',
    rev: 'The revision of the processing unit.',
    cpuClockHz: 'CPU clock in hertz.',
    cpuCoreCount: 'Number of cores.',
    smtEnabled: "Simultaneous multithreading (for example hyper-threading) capable true/false.",
    cacheSizeKb: 'The size of cache memory on the CPU.',
    bits: 'Number of bit width supported.'
    });

MATCH (zilogprocessor:ZilogProcessor {uuid: 'a68f42c5-2a1d-43cb-bebd-55339f5e61ee'}),
    (processingunit:ProcessingUnit {uuid: 'b7b2a2ff-3af7-469b-afbf-e52f1b80be91'})
MERGE (zilogprocessor)-[:ISA]->(processingunit);


// Software - Any kind of software.
MERGE (software:Object:Component:Software {
    uuid: '8db2b492-9a4a-41fe-97a5-bc625da042ea',
    name: 'Software',
    desc: 'An abstract software component.',
    ver: 'The version of the software.'
    });

MATCH (software:Software {uuid: '8db2b492-9a4a-41fe-97a5-bc625da042ea'}),
    (component:Component {uuid: 'a79ce509-a021-4177-ae61-73abd01fb479'})
MERGE (software)-[:ISA]->(component);


// CloudSystem - Any cloud (operating) system, either IAAS or PAAS.
MERGE (cloudsystem:Object:Component:Software:CloudSystem {
    uuid: '86c0439a-4219-46e6-82a3-6fb2ee774aa4',
    name: 'CloudSystem',
    desc: 'A cloud (operating) system and/or environment.',
    ver: 'The version of the cloud (operating) system environment.',
    shortName: 'A short lowercase name for the Cloud System without spaces.',
    longName: 'The full descriptive name of the Cloud System.'
    });

MATCH (cloudsystem:CloudSystem {uuid: '86c0439a-4219-46e6-82a3-6fb2ee774aa4'}),
    (software:Software {uuid: '8db2b492-9a4a-41fe-97a5-bc625da042ea'})
MERGE (cloudsystem)-[:ISA]->(software);


// OperatingSystem - Any operating system.
MERGE (operatingsystem:Object:Component:Software:OperatingSystem {
    uuid: 'f05e325e-1aee-4892-96e4-8bcd6e97c528',
    name: 'OperatingSystem',
    desc: 'An operating system.',
    ver: 'The version of the operating system software.',
    shortName: 'A short lowercase name for the Operating System without spaces.',
    longName: 'The full descriptive name of the Operating System.'
    });

MATCH (operatingsystem:OperatingSystem {uuid: 'f05e325e-1aee-4892-96e4-8bcd6e97c528'}),
    (software:Software {uuid: '8db2b492-9a4a-41fe-97a5-bc625da042ea'})
MERGE (operatingsystem)-[:ISA]->(software);


// ServiceManager - Any service/boot initialization system.
MERGE (servicemanager:Object:Component:Software:ServiceManager {
    uuid: 'c3008eec-b454-4ef6-bf7e-0bd92c13d3c3',
    name: 'ServiceManager',
    desc: 'An operating system initialization system.',
    ver: 'The version of the service manager software.'
    });

MATCH (servicemanager:ServiceManager {uuid: 'c3008eec-b454-4ef6-bf7e-0bd92c13d3c3'}),
    (software:Software {uuid: '8db2b492-9a4a-41fe-97a5-bc625da042ea'})
MERGE (servicemanager)-[:ISA]->(software);


// PackageManager - Any packaging system.
MERGE (packagemanager:Object:Component:Software:PackageManager {
    uuid: 'e2b8b9ee-b66f-4252-8fba-4fb172f741bc',
    name: 'PackageManager',
    desc: 'A package manager.',
    ver: 'The version of the package manager software.'
    });

MATCH (packagemanager:PackageManager {uuid: 'e2b8b9ee-b66f-4252-8fba-4fb172f741bc'}),
    (software:Software {uuid: '8db2b492-9a4a-41fe-97a5-bc625da042ea'})
MERGE (packagemanager)-[:ISA]->(software);


// Interpreter - Any interpreter.
MERGE (interpreter:Object:Component:Software:Interpreter {
    uuid: 'd047f415-734c-4bd3-92b9-d68d5521ce69',
    name: 'Interpreter',
    desc: 'A command or programming language interpreter.',
    ver: 'The version of the command or programming language interpreter software.'
    });

MATCH (interpreter:Interpreter {uuid: 'd047f415-734c-4bd3-92b9-d68d5521ce69'}),
    (software:Software {uuid: '8db2b492-9a4a-41fe-97a5-bc625da042ea'})
MERGE (interpreter)-[:ISA]->(software);


// Hypervisor - Any hypervisor.
MERGE (hypervisor:Object:Component:Software:Hypervisor {
    uuid: 'c5c7fcb5-8ac3-40cc-b195-b2b6af368e81',
    name: 'Hypervisor',
    desc: 'A virtualization hypervisor.',
    ver: 'The version of the hypervisor software.'
    });

MATCH (hypervisor:Hypervisor {uuid: 'c5c7fcb5-8ac3-40cc-b195-b2b6af368e81'}),
    (software:Software {uuid: '8db2b492-9a4a-41fe-97a5-bc625da042ea'})
MERGE (hypervisor)-[:ISA]->(software);


// Service - Any generic local/networked service.
MERGE (service:Object:Component:Software:Service {
    uuid: '3fb4603b-281e-47c0-b338-768ea05d4660',
    name: 'Service',
    desc: 'A generic local/networked/daemonized service.',
    ver: 'The version of the local/networked/daemonized software.'
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


// Handler - A representation of a software handler that gives end user function.
MERGE (handler:Object:Handler {
    uuid: '14dbfbfa-b50a-466f-8539-55d5d3f15e4a',
    name: 'Handler',
    desc: 'A representation of a software handler that gives end user function.'
    });

MATCH (handler:Handler {uuid: '14dbfbfa-b50a-466f-8539-55d5d3f15e4a'}),
    (object:Object {uuid: 'b1bc963a-19cc-4518-aecb-a35d028c49e3'})
MERGE (handler)-[:ISA]->(object);


// CommandHandler - Creates a CommandOperation, executes that CommandOperation, creates CommandResult.
MERGE (cmdhandler:Object:Handler:CommandHandler {
    uuid: '6ac7f3f8-4a1f-42aa-87b8-b2f5516b5d7c',
    name: 'CommandHandler',
    desc: 'Creates a CommandOperation, executes that CommandOperation, creates CommandResult.'
    });

MATCH (cmdhandler:CommandHandler {uuid: '6ac7f3f8-4a1f-42aa-87b8-b2f5516b5d7c'}),
    (handler:Handler {uuid: '14dbfbfa-b50a-466f-8539-55d5d3f15e4a'})
MERGE (cmdhandler)-[:ISA]->(handler);


// VersionHandler - Looks up a VersionOperation, executes command, creates VersionResult.
MERGE (verhandler:Object:Handler:VersionHandler {
    uuid: 'a57f47c3-7e27-4347-806b-204239c61e65',
    name: 'VersionHandler',
    desc: 'Looks up a VersionOperation, executes command, creates VersionResult.'
    });

MATCH (verhandler:VersionHandler {uuid: 'a57f47c3-7e27-4347-806b-204239c61e65'}),
    (handler:Handler {uuid: '14dbfbfa-b50a-466f-8539-55d5d3f15e4a'})
MERGE (verhandler)-[:ISA]->(handler);


// StatusHandler - Executes generic code to get a status or executes CustomStatusOperation if it exists. Creates StatusResult.
MERGE (stathandler:Object:Handler:StatusHandler {
    uuid: '3b6555cd-abd4-4cc6-ab64-b7c031c2b008',
    name: 'StatusHandler',
    desc: 'Executes generic code to get a status or executes CustomStatusOperation if it exists. Creates StatusResult.'
    });

MATCH (stathandler:StatusHandler {uuid: '3b6555cd-abd4-4cc6-ab64-b7c031c2b008'}),
    (handler:Handler {uuid: '14dbfbfa-b50a-466f-8539-55d5d3f15e4a'})
MERGE (stathandler)-[:ISA]->(handler);


// ExistenceHandler - Executes generic code to get a existence information or executes CustomExistenceOperation if it exists. Creates ExistenceResult.
MERGE (existhandler:Object:Handler:ExistenceHandler {
    uuid: '6a8a8dd7-f8d3-4170-8c25-3866cfc2037c',
    name: 'ExistenceHandler',
    desc: 'Executes generic code to get a existence information or executes CustomExistenceOperation if it exists. Creates ExistenceResult.'
    });

MATCH (existhandler:ExistenceHandler {uuid: '6a8a8dd7-f8d3-4170-8c25-3866cfc2037c'}),
    (handler:Handler {uuid: '14dbfbfa-b50a-466f-8539-55d5d3f15e4a'})
MERGE (existhandler)-[:ISA]->(handler);


// UsageHandler - Executes generic code to derive usage of resources or executes CustomUsageOperation if it exists. Creates UsageResult.
MERGE (usagehandler:Object:Handler:UsageHandler {
    uuid: 'dd0878eb-b560-4dc0-89da-1d7ddfcbbced',
    name: 'UsageHandler',
    desc: 'Executes generic code to derive usage of resources or executes CustomUsageOperation if it exists. Creates UsageResult.'
    });

MATCH (usagehandler:UsageHandler {uuid: 'dd0878eb-b560-4dc0-89da-1d7ddfcbbced'}),
    (handler:Handler {uuid: '14dbfbfa-b50a-466f-8539-55d5d3f15e4a'})
MERGE (usagehandler)-[:ISA]->(handler);


// InstallHandler - Executes generic code to derive install method and executes or executes CustomInstallOperation if it exists.
MERGE (installhandler:Object:Handler:InstallHandler {
    uuid: '3e33df40-8d9d-49d2-939d-6712d89ca70f',
    name: 'InstallHandler',
    desc: 'Executes generic code to derive install method and executes or executes CustomInstallOperation if it exists.'
    });

MATCH (installhandler:InstallHandler {uuid: '3e33df40-8d9d-49d2-939d-6712d89ca70f'}),
    (handler:Handler {uuid: '14dbfbfa-b50a-466f-8539-55d5d3f15e4a'})
MERGE (installhandler)-[:ISA]->(handler);


// UninstallHandler - Executes generic code to derive uninstall method and executes or executes CustomUninstallOperation if it exists.
MERGE (uninstallhandler:Object:Handler:UninstallHandler {
    uuid: '6b80a87e-9d24-4a06-b792-2d13c0713b6c',
    name: 'UninstallHandler',
    desc: 'Executes generic code to derive uninstall method and executes or executes CustomUninstallOperation if it exists.'
    });

MATCH (uninstallhandler:UninstallHandler {uuid: '6b80a87e-9d24-4a06-b792-2d13c0713b6c'}),
    (handler:Handler {uuid: '14dbfbfa-b50a-466f-8539-55d5d3f15e4a'})
MERGE (uninstallhandler)-[:ISA]->(handler);


// StartHandler - Executes generic code to derive start method and executes or executes CustomStartOperation if it exists.
MERGE (starthandler:Object:Handler:StartHandler {
    uuid: 'db00edc2-5900-46d8-99fc-b379fd8538e2',
    name: 'StartHandler',
    desc: 'Executes generic code to derive start method and executes or executes CustomStartOperation if it exists.'
    });

MATCH (starthandler:StartHandler {uuid: 'db00edc2-5900-46d8-99fc-b379fd8538e2'}),
    (handler:Handler {uuid: '14dbfbfa-b50a-466f-8539-55d5d3f15e4a'})
MERGE (starthandler)-[:ISA]->(handler);


// StopHandler - Executes generic code to derive stop method and executes or executes CustomStopOperation if it exists.
MERGE (stophandler:Object:Handler:StopHandler {
    uuid: '5d97322d-9113-4d7c-ac6c-2690a8c65d0d',
    name: 'StopHandler',
    desc: 'Executes generic code to derive stop method and executes or executes CustomStopOperation if it exists.'
    });

MATCH (stophandler:StopHandler {uuid: '5d97322d-9113-4d7c-ac6c-2690a8c65d0d'}),
    (handler:Handler {uuid: '14dbfbfa-b50a-466f-8539-55d5d3f15e4a'})
MERGE (stophandler)-[:ISA]->(handler);


// RestartHandler - Executes generic code to derive restart method and executes or executes CustomRestartOperation if it exists.
MERGE (restartHandler:Object:Handler:RestartHandler {
    uuid: 'c61c2f8c-0ba2-4e0d-99f2-66a5580a59a6',
    name: 'RestartHandler',
    desc: 'Executes generic code to derive restart method and executes or executes CustomRestartOperation if it exists.'
    });

MATCH (restartHandler:RestartHandler {uuid: 'c61c2f8c-0ba2-4e0d-99f2-66a5580a59a6'}),
    (handler:Handler {uuid: '14dbfbfa-b50a-466f-8539-55d5d3f15e4a'})
MERGE (restartHandler)-[:ISA]->(handler);


// Operation - A command or method that can be called, with or without schedule.
MERGE (operation:Object:Operation {
    uuid: '46294722-b14e-438e-9fbc-95ff137b6b18',
    name: 'Operation',
    desc: 'A command or method that can be called, with or without schedule.'
    });

MATCH (operation:Operation {uuid: '46294722-b14e-438e-9fbc-95ff137b6b18'}),
    (object:Object {uuid: 'b1bc963a-19cc-4518-aecb-a35d028c49e3'})
MERGE (operation)-[:ISA]->(object);


// CommandOperation - An arbitrary command.
MERGE (cmdoperation:Object:Operation:CommandOperation {
    uuid: 'de64c2f4-7fa3-4e68-ba10-6af8140dac36',
    name: 'CommandOperation',
    desc: 'An arbitrary command.'
    });

MATCH (cmdoperation:CommandOperation {uuid: 'de64c2f4-7fa3-4e68-ba10-6af8140dac36'}),
    (operation:Operation {uuid: '46294722-b14e-438e-9fbc-95ff137b6b18'})
MERGE (cmdoperation)-[:ISA]->(operation);


// VersionOperation - A command that displays a version string.
MERGE (veroperation:Object:Operation:VersionOperation {
    uuid: '9598a74d-0293-459a-af63-e3563215162f',
    name: 'VersionOperation',
    desc: 'A command that displays a version string.'
    });

MATCH (veroperation:VersionOperation {uuid: '9598a74d-0293-459a-af63-e3563215162f'}),
    (operation:Operation {uuid: '46294722-b14e-438e-9fbc-95ff137b6b18'})
MERGE (veroperation)-[:ISA]->(operation);


// CustomStatusOperation - Custom command for a status check operation.
MERGE (customstatusoperation:Object:Operation:CustomStatusOperation {
    uuid: 'bfc01b44-075d-4d6e-82eb-474830bba06e',
    name: 'CustomStatusOperation',
    desc: 'Custom command for a status check operation.'
    });

MATCH (customstatusoperation:CustomStatusOperation {uuid: 'bfc01b44-075d-4d6e-82eb-474830bba06e'}),
    (operation:Operation {uuid: '46294722-b14e-438e-9fbc-95ff137b6b18'})
MERGE (customstatusoperation)-[:ISA]->(operation);


// CustomExistenceOperation - Custom command for an existence operation.
MERGE (customexistenceoperation:Object:Operation:CustomExistenceOperation {
    uuid: '97bf21ef-d112-4607-9392-83243e44577f',
    name: 'CustomExistenceOperation',
    desc: 'Custom command for an existence operation.'
    });

MATCH (customexistenceoperation:CustomExistenceOperation {uuid: '97bf21ef-d112-4607-9392-83243e44577f'}),
    (operation:Operation {uuid: '46294722-b14e-438e-9fbc-95ff137b6b18'})
MERGE (customexistenceoperation)-[:ISA]->(operation);


// CustomUsageOperation - Custom command that shows/derives/fetches the usage of a resource.
MERGE (customusageoperation:Object:Operation:CustomUsageOperation {
    uuid: '32b9f847-3579-4750-b29b-c0f3af877adb',
    name: 'CustomUsageOperation',
    desc: 'Custom command that shows/derives/fetches the usage of a resource.'
    });

MATCH (customusageoperation:CustomUsageOperation {uuid: '32b9f847-3579-4750-b29b-c0f3af877adb'}),
    (operation:Operation {uuid: '46294722-b14e-438e-9fbc-95ff137b6b18'})
MERGE (customusageoperation)-[:ISA]->(operation);


// CustomInstallOperation - Custom command to install a software.
MERGE (custominstalloperation:Object:Operation:CustomInstallOperation {
    uuid: '7fb7c8c0-6967-4af2-a4f9-e5342fc3fb24',
    name: 'CustomInstallOperation',
    desc: 'Custom command to install a software.'
    });

MATCH (custominstalloperation:CustomInstallOperation {uuid: '7fb7c8c0-6967-4af2-a4f9-e5342fc3fb24'}),
    (operation:Operation {uuid: '46294722-b14e-438e-9fbc-95ff137b6b18'})
MERGE (custominstalloperation)-[:ISA]->(operation);


// CustomUninstallOperation - Custom command to uninstall a software.
MERGE (customuninstalloperation:Object:Operation:CustomUninstallOperation {
    uuid: '1308c542-ea7a-4525-8a1b-65cf96033a62',
    name: 'CustomUninstallOperation',
    desc: 'Custom command to uninstall a software.'
    });

MATCH (customuninstalloperation:CustomUninstallOperation {uuid: '1308c542-ea7a-4525-8a1b-65cf96033a62'}),
    (operation:Operation {uuid: '46294722-b14e-438e-9fbc-95ff137b6b18'})
MERGE (customuninstalloperation)-[:ISA]->(operation);


// CustomStartOperation - Custom command to start a software.
MERGE (customstartoperation:Object:Operation:CustomStartOperation {
    uuid: '8cb7ac74-7923-4114-abd2-17f4c5f440d7',
    name: 'CustomStartOperation',
    desc: 'Custom command to start a software.'
    });

MATCH (customstartoperation:CustomStartOperation {uuid: '8cb7ac74-7923-4114-abd2-17f4c5f440d7'}),
    (operation:Operation {uuid: '46294722-b14e-438e-9fbc-95ff137b6b18'})
MERGE (customstartoperation)-[:ISA]->(operation);


// CustomStopOperation - Custom command to stop a software.
MERGE (customstopoperation:Object:Operation:CustomStopOperation {
    uuid: 'ffefceab-afd8-42da-b793-d65beb4f71eb',
    name: 'CustomStopOperation',
    desc: 'Custom command to stop a software.'
    });

MATCH (customstopoperation:CustomStopOperation {uuid: 'ffefceab-afd8-42da-b793-d65beb4f71eb'}),
    (operation:Operation {uuid: '46294722-b14e-438e-9fbc-95ff137b6b18'})
MERGE (customstopoperation)-[:ISA]->(operation);


// CustomRestartOperation - Custom command to restart a software.
MERGE (customrestartoperation:Object:Operation:CustomRestartOperation {
    uuid: '3d5300bf-7b4d-443c-b33a-159c00572eb9',
    name: 'CustomRestartOperation',
    desc: 'Custom command to restart a software.'
    });

MATCH (customrestartoperation:CustomRestartOperation {uuid: '3d5300bf-7b4d-443c-b33a-159c00572eb9'}),
    (operation:Operation {uuid: '46294722-b14e-438e-9fbc-95ff137b6b18'})
MERGE (customrestartoperation)-[:ISA]->(operation);


// Result - A result from an operation.
MERGE (result:Object:Result {
    uuid: 'f3498c02-58c6-486b-87c6-9d54607fdb12',
    name: 'Result',
    desc: 'A result from an operation.'
    });

MATCH (result:Result {uuid: 'f3498c02-58c6-486b-87c6-9d54607fdb12'}),
    (object:Object {uuid: 'b1bc963a-19cc-4518-aecb-a35d028c49e3'})
MERGE (result)-[:ISA]->(object);


// CommandResult - A generic textual result from a command operation.
MERGE (commandresult:Object:Result:CommandResult {
    uuid: '6991d878-52be-467d-893a-6b95074f04c2',
    name: 'CommandResult',
    desc: 'A generic textual result from a command operation.'
    });

MATCH (commandresult:CommandResult {uuid: '6991d878-52be-467d-893a-6b95074f04c2'}),
    (result:Result {uuid: 'f3498c02-58c6-486b-87c6-9d54607fdb12'})
MERGE (commandresult)-[:ISA]->(result);


// VersionResult - A textual result representing a version string.
MERGE (versionresult:Object:Result:VersionResult {
    uuid: '2f9703cb-3d68-44fa-99bf-935ed658ca5a',
    name: 'VersionResult',
    desc: 'A textual result representing a version string.'
    });

MATCH (versionresult:VersionResult {uuid: '2f9703cb-3d68-44fa-99bf-935ed658ca5a'}),
    (result:Result {uuid: 'f3498c02-58c6-486b-87c6-9d54607fdb12'})
MERGE (versionresult)-[:ISA]->(result);


// StatusResult - A result that represents a status.
MERGE (statusresult:Object:Result:StatusResult {
    uuid: '48839d5c-8ebc-4989-80ec-f6fb90299a56',
    name: 'StatusResult',
    desc: 'A result that represents a status.'
    });

MATCH (statusresult:StatusResult {uuid: '48839d5c-8ebc-4989-80ec-f6fb90299a56'}),
    (result:Result {uuid: 'f3498c02-58c6-486b-87c6-9d54607fdb12'})
MERGE (statusresult)-[:ISA]->(result);


// DerivedStatusResult - A derived status. 1/ok, 0/not-ok.
MERGE (derivedstatusresult:Object:Result:StatusResult:DerivedStatusResult {
    uuid: '9565a2ca-29e4-4c36-8f17-97e9a0074ded',
    name: 'DerivedStatusResult',
    desc: 'A derived status. 1/ok, 0/not-ok.'
    });

MATCH (derivedstatusresult:DerivedStatusResult {uuid: '9565a2ca-29e4-4c36-8f17-97e9a0074ded'}),
    (statusresult:StatusResult {uuid: '48839d5c-8ebc-4989-80ec-f6fb90299a56'})
MERGE (derivedstatusresult)-[:ISA]->(statusresult);


// LocalDerivedStatusResult - A derived 1/0 status from all statuses known to the software itself.
MERGE (localderivedstatusresult:Object:Result:StatusResult:LocalDerivedStatusResult {
    uuid: '9d007dd4-82df-4791-81f9-d91a65e9f3a3',
    name: 'LocalDerivedStatusResult',
    desc: 'A derived 1/0 status from all statuses known to the software itself.'
    });

MATCH (localderivedstatusresult:LocalDerivedStatusResult {uuid: '9d007dd4-82df-4791-81f9-d91a65e9f3a3'}),
    (statusresult:StatusResult {uuid: '48839d5c-8ebc-4989-80ec-f6fb90299a56'})
MERGE (localderivedstatusresult)-[:ISA]->(statusresult);


// AggregateDerivedStatusResult - A derived 1/0 status from all localderivedstatuses from its children.
MERGE (aggregatederivedstatusresult:Object:Result:StatusResult:AggregateDerivedStatusResult {
    uuid: '3f9ca86e-6286-4e59-9eb1-fa352103fe5c',
    name: 'AggregateDerivedStatusResult',
    desc: 'A derived 1/0 status from all localderivedstatuses from its children.'
    });

MATCH (aggregatederivedstatusresult:AggregateDerivedStatusResult {uuid: '3f9ca86e-6286-4e59-9eb1-fa352103fe5c'}),
    (statusresult:StatusResult {uuid: '48839d5c-8ebc-4989-80ec-f6fb90299a56'})
MERGE (aggregatederivedstatusresult)-[:ISA]->(statusresult);


// SoftwareInstalledStatusResult - 1/installed, 0/not-installed.
MERGE (softwareinstalledstatusresult:Object:Result:StatusResult:SoftwareInstalledStatusResult {
    uuid: '38b435e1-046c-415b-9cce-6578efb4eef6',
    name: 'SoftwareInstalledStatusResult',
    desc: '1/installed, 0/not-installed.'
    });

MATCH (softwareinstalledstatusresult:SoftwareInstalledStatusResult {uuid: '38b435e1-046c-415b-9cce-6578efb4eef6'}),
    (statusresult:StatusResult {uuid: '48839d5c-8ebc-4989-80ec-f6fb90299a56'})
MERGE (softwareinstalledstatusresult)-[:ISA]->(statusresult);


// SoftwareRunningStatusResult - 1/running, 0/not-running.
MERGE (softwarerunningstatusresult:Object:Result:StatusResult:SoftwareRunningStatusResult {
    uuid: '4fda91ea-63bb-4689-bcf4-86bb02314c21',
    name: 'SoftwareRunningStatusResult',
    desc: '1/running, 0/not-running.'
    });

MATCH (softwarerunningstatusresult:SoftwareRunningStatusResult {uuid: '4fda91ea-63bb-4689-bcf4-86bb02314c21'}),
    (statusresult:StatusResult {uuid: '48839d5c-8ebc-4989-80ec-f6fb90299a56'})
MERGE (softwarerunningstatusresult)-[:ISA]->(statusresult);


// IpAddressReachableStatusResult - 1/reachable, 0/unreachable.
MERGE (ipaddressreachablestatusresult:Object:Result:StatusResult:IpAddressReachableStatusResult {
    uuid: '729e0e03-3a27-47e2-8075-7e4a754d68a5',
    name: 'IpAddressReachableStatusResult',
    desc: '1/reachable, 0/unreachable.'
    });

MATCH (ipaddressreachablestatusresult:IpAddressReachableStatusResult {uuid: '729e0e03-3a27-47e2-8075-7e4a754d68a5'}),
    (statusresult:StatusResult {uuid: '48839d5c-8ebc-4989-80ec-f6fb90299a56'})
MERGE (ipaddressreachablestatusresult)-[:ISA]->(statusresult);


// ListenerAvailableStatusResult - 1/open, 0/closed.
MERGE (listeneravailablestatusresult:Object:Result:StatusResult:ListenerAvailableStatusResult {
    uuid: '630e8753-7566-4c2f-9cdf-99f5b64492a1',
    name: 'ListenerAvailableStatusResult',
    desc: '1/open, 0/closed.'
    });

MATCH (listeneravailablestatusresult:ListenerAvailableStatusResult {uuid: '630e8753-7566-4c2f-9cdf-99f5b64492a1'}),
    (statusresult:StatusResult {uuid: '48839d5c-8ebc-4989-80ec-f6fb90299a56'})
MERGE (listeneravailablestatusresult)-[:ISA]->(statusresult);


// DnsNameResolvableStatusResult - 1/resolved, 0/not-resolved.
MERGE (dnsnameresolvablestatusresult:Object:Result:StatusResult:DnsNameResolvableStatusResult {
    uuid: 'fffdce77-417f-4044-a570-6e6153db1dd0',
    name: 'DnsNameResolvableStatusResult',
    desc: '1/resolved, 0/not-resolved.'
    });

MATCH (dnsnameresolvablestatusresult:DnsNameResolvableStatusResult {uuid: 'fffdce77-417f-4044-a570-6e6153db1dd0'}),
    (statusresult:StatusResult {uuid: '48839d5c-8ebc-4989-80ec-f6fb90299a56'})
MERGE (dnsnameresolvablestatusresult)-[:ISA]->(statusresult);


// ExistenceResult - A generic result from an ExistenceHandler.
MERGE (existenceresult:Object:Result:ExistenceResult {
    uuid: '6d23a2bc-9539-4914-aaf4-45406f59d8fc',
    name: 'ExistenceResult',
    desc: 'A generic result from an ExistenceHandler.'
    });

MATCH (existenceresult:ExistenceResult {uuid: '6d23a2bc-9539-4914-aaf4-45406f59d8fc'}),
    (result:Result {uuid: 'f3498c02-58c6-486b-87c6-9d54607fdb12'})
MERGE (existenceresult)-[:ISA]->(result);


// DirectoryExistenceResult - A generic directory existence result from an ExistenceHandler.
MERGE (directoryexistenceresult:Object:Result:ExistenceResult:DirectoryExistenceResult {
    uuid: '31736b3b-82da-4864-bc59-b49c196e3327',
    name: 'DirectoryExistenceResult',
    desc: 'A generic directory existence result from an ExistenceHandler.'
    });

MATCH (directoryexistenceresult:DirectoryExistenceResult {uuid: '31736b3b-82da-4864-bc59-b49c196e3327'}),
    (existenceresult:ExistenceResult {uuid: '6d23a2bc-9539-4914-aaf4-45406f59d8fc'})
MERGE (directoryexistenceresult)-[:ISA]->(existenceresult);


// PrefixDirectoryExistenceResult - A possible result from an ExistenceHandler, parsing a PrefixDirectoryMap.
MERGE (prefixdirectoryexistenceresult:Object:Result:ExistenceResult:DirectoryExistenceResult:PrefixDirectoryExistenceResult {
    uuid: '93e34319-fe7b-4768-b83d-58986a4cdb73',
    name: 'PrefixDirectoryExistenceResult',
    desc: 'A possible result from an ExistenceHandler, parsing a PrefixDirectoryMap.'
    });

MATCH (prefixdirectoryexistenceresult:PrefixDirectoryExistenceResult {uuid: '93e34319-fe7b-4768-b83d-58986a4cdb73'}),
    (directoryexistenceresult:DirectoryExistenceResult {uuid: '31736b3b-82da-4864-bc59-b49c196e3327'})
MERGE (prefixdirectoryexistenceresult)-[:ISA]->(directoryexistenceresult);


// ConfigDirectoryExistenceResult - A possible result from an ExistenceHandler, parsing a ConfigDirectoryMap.
MERGE (configdirectoryexistenceresult:Object:Result:ExistenceResult:DirectoryExistenceResult:ConfigDirectoryExistenceResult {
    uuid: '0182518c-2380-4abc-be84-5fa02679fe52',
    name: 'ConfigDirectoryExistenceResult',
    desc: 'A possible result from an ExistenceHandler, parsing a ConfigDirectoryMap.'
    });

MATCH (configdirectoryexistenceresult:ConfigDirectoryExistenceResult {uuid: '0182518c-2380-4abc-be84-5fa02679fe52'}),
    (directoryexistenceresult:DirectoryExistenceResult {uuid: '31736b3b-82da-4864-bc59-b49c196e3327'})
MERGE (configdirectoryexistenceresult)-[:ISA]->(directoryexistenceresult);


// BinaryDirectoryExistenceResult - A possible result from an ExistenceHandler, parsing a BinaryDirectoryMap.
MERGE (binarydirectoryexistenceresult:Object:Result:ExistenceResult:DirectoryExistenceResult:BinaryDirectoryExistenceResult {
    uuid: '1b607b4a-5206-4d55-8743-0487f7b6d944',
    name: 'BinaryDirectoryExistenceResult',
    desc: 'A possible result from an ExistenceHandler, parsing a BinaryDirectoryMap.'
    });

MATCH (binarydirectoryexistenceresult:BinaryDirectoryExistenceResult {uuid: '1b607b4a-5206-4d55-8743-0487f7b6d944'}),
    (directoryexistenceresult:DirectoryExistenceResult {uuid: '31736b3b-82da-4864-bc59-b49c196e3327'})
MERGE (binarydirectoryexistenceresult)-[:ISA]->(directoryexistenceresult);


// LibraryDirectoryExistenceResult - A possible result from an ExistenceHandler, parsing a LibraryDirectoryMap.
MERGE (librarydirectoryexistenceresult:Object:Result:ExistenceResult:DirectoryExistenceResult:LibraryDirectoryExistenceResult {
    uuid: '25a2e063-b7e7-4727-9725-63859005e6ed',
    name: 'LibraryDirectoryExistenceResult',
    desc: 'A possible result from an ExistenceHandler, parsing a LibraryDirectoryMap.'
    });

MATCH (librarydirectoryexistenceresult:LibraryDirectoryExistenceResult {uuid: '25a2e063-b7e7-4727-9725-63859005e6ed'}),
    (directoryexistenceresult:DirectoryExistenceResult {uuid: '31736b3b-82da-4864-bc59-b49c196e3327'})
MERGE (librarydirectoryexistenceresult)-[:ISA]->(directoryexistenceresult);


// DataDirectoryExistenceResult - A possible result from an ExistenceHandler, parsing a DataDirectoryMap.
MERGE (datadirectoryexistenceresult:Object:Result:ExistenceResult:DirectoryExistenceResult:DataDirectoryExistenceResult {
    uuid: '22105b60-ea6d-4c1e-b3ad-14b8ff9e37a1',
    name: 'DataDirectoryExistenceResult',
    desc: 'A possible result from an ExistenceHandler, parsing a DataDirectoryMap.'
    });

MATCH (datadirectoryexistenceresult:DataDirectoryExistenceResult {uuid: '22105b60-ea6d-4c1e-b3ad-14b8ff9e37a1'}),
    (directoryexistenceresult:DirectoryExistenceResult {uuid: '31736b3b-82da-4864-bc59-b49c196e3327'})
MERGE (datadirectoryexistenceresult)-[:ISA]->(directoryexistenceresult);


// LogDirectoryExistenceResult - A possible result from an ExistenceHandler, parsing a LogDirectoryMap.
MERGE (logdirectoryexistenceresult:Object:Result:ExistenceResult:DirectoryExistenceResult:LogDirectoryExistenceResult {
    uuid: '3b64fa0c-474e-4109-9f43-c861ac5fce19',
    name: 'LogDirectoryExistenceResult',
    desc: 'A possible result from an ExistenceHandler, parsing a LogDirectoryMap.'
    });

MATCH (logdirectoryexistenceresult:LogDirectoryExistenceResult {uuid: '3b64fa0c-474e-4109-9f43-c861ac5fce19'}),
    (directoryexistenceresult:DirectoryExistenceResult {uuid: '31736b3b-82da-4864-bc59-b49c196e3327'})
MERGE (logdirectoryexistenceresult)-[:ISA]->(directoryexistenceresult);


// FileExistenceResult - A generic file existence result from an ExistenceHandler.
MERGE (fileexistenceresult:Object:Result:ExistenceResult:FileExistenceResult {
    uuid: 'b72fd981-3473-4874-9a40-bc86d6b5362f',
    name: 'FileExistenceResult',
    desc: 'A generic file existence result from an ExistenceHandler.'
    });

MATCH (fileexistenceresult:FileExistenceResult {uuid: 'b72fd981-3473-4874-9a40-bc86d6b5362f'}),
    (existenceresult:ExistenceResult {uuid: '6d23a2bc-9539-4914-aaf4-45406f59d8fc'})
MERGE (fileexistenceresult)-[:ISA]->(existenceresult);


// ConfigFileExistenceResult - A possible result from an ExistenceHandler, parsing a ConfigFileMap.
MERGE (configfileexistenceresult:Object:Result:ExistenceResult:FileExistenceResult:ConfigFileExistenceResult {
    uuid: '6615da84-00f9-4858-a753-38f5905e8c4c',
    name: 'ConfigFileExistenceResult',
    desc: 'A possible result from an ExistenceHandler, parsing a ConfigFileMap.'
    });

MATCH (configfileexistenceresult:ConfigFileExistenceResult {uuid: '6615da84-00f9-4858-a753-38f5905e8c4c'}),
    (fileexistenceresult:FileExistenceResult {uuid: 'b72fd981-3473-4874-9a40-bc86d6b5362f'})
MERGE (configfileexistenceresult)-[:ISA]->(fileexistenceresult);


// BinaryFileExistenceResult - A possible result from an ExistenceHandler, parsing a BinaryFileMap.
MERGE (binaryfileexistenceresult:Object:Result:ExistenceResult:FileExistenceResult:BinaryFileExistenceResult {
    uuid: '6c510a61-53b1-4f16-a5cb-8816846062be',
    name: 'BinaryFileExistenceResult',
    desc: 'A possible result from an ExistenceHandler, parsing a BinaryFileMap.'
    });

MATCH (binaryfileexistenceresult:BinaryFileExistenceResult {uuid: '6c510a61-53b1-4f16-a5cb-8816846062be'}),
    (fileexistenceresult:FileExistenceResult {uuid: 'b72fd981-3473-4874-9a40-bc86d6b5362f'})
MERGE (binaryfileexistenceresult)-[:ISA]->(fileexistenceresult);


// LibraryFileExistenceResult - A possible result from an ExistenceHandler, parsing a LibraryFileMap.
MERGE (libraryfileexistenceresult:Object:Result:ExistenceResult:FileExistenceResult:LibraryFileExistenceResult {
    uuid: 'e4d04fd4-0d95-49db-9637-891d5f9b6e21',
    name: 'LibraryFileExistenceResult',
    desc: 'A possible result from an ExistenceHandler, parsing a LibraryFileMap.'
    });

MATCH (libraryfileexistenceresult:LibraryFileExistenceResult {uuid: 'e4d04fd4-0d95-49db-9637-891d5f9b6e21'}),
    (fileexistenceresult:FileExistenceResult {uuid: 'b72fd981-3473-4874-9a40-bc86d6b5362f'})
MERGE (libraryfileexistenceresult)-[:ISA]->(fileexistenceresult);


// DataFileExistenceResult - A possible result from an ExistenceHandler, parsing a DataFileMap.
MERGE (datafileexistenceresult:Object:Result:ExistenceResult:FileExistenceResult:DataFileExistenceResult {
    uuid: '6a1d9396-707e-4224-965c-2dd8c5c97cfb',
    name: 'DataFileExistenceResult',
    desc: 'A possible result from an ExistenceHandler, parsing a DataFileMap.'
    });

MATCH (datafileexistenceresult:DataFileExistenceResult {uuid: '6a1d9396-707e-4224-965c-2dd8c5c97cfb'}),
    (fileexistenceresult:FileExistenceResult {uuid: 'b72fd981-3473-4874-9a40-bc86d6b5362f'})
MERGE (datafileexistenceresult)-[:ISA]->(fileexistenceresult);


// LogFileExistenceResult - A possible result from an ExistenceHandler, parsing a LogFileMap.
MERGE (logfileexistenceresult:Object:Result:ExistenceResult:FileExistenceResult:LogFileExistenceResult {
    uuid: 'fbd1b321-3c95-4eb3-ad4f-16c1f7215e39',
    name: 'LogFileExistenceResult',
    desc: 'A possible result from an ExistenceHandler, parsing a LogFileMap.'
    });

MATCH (logfileexistenceresult:LogFileExistenceResult {uuid: 'fbd1b321-3c95-4eb3-ad4f-16c1f7215e39'}),
    (fileexistenceresult:FileExistenceResult {uuid: 'b72fd981-3473-4874-9a40-bc86d6b5362f'})
MERGE (logfileexistenceresult)-[:ISA]->(fileexistenceresult);


// NameExistenceResult - A possible result from an ExistenceHandler, parsing a NameMap.
MERGE (nameexistenceresult:Object:Result:ExistenceResult:NameExistenceResult {
    uuid: 'db4c51ec-7455-4bba-b965-f1c067f22e37',
    name: 'NameExistenceResult',
    desc: 'A possible result from an ExistenceHandler, parsing a NameMap.'
    });

MATCH (nameexistenceresult:NameExistenceResult {uuid: 'db4c51ec-7455-4bba-b965-f1c067f22e37'}),
    (existenceresult:ExistenceResult {uuid: '6d23a2bc-9539-4914-aaf4-45406f59d8fc'})
MERGE (nameexistenceresult)-[:ISA]->(existenceresult);


// PackageNameExistenceResult - A possible result from an ExistenceHandler, parsing a PackageNameMap.
MERGE (packagenameexistenceresult:Object:Result:ExistenceResult:NameExistenceResult:PackageNameExistenceResult {
    uuid: '7dd30b10-04bf-4875-8f98-2add74299648',
    name: 'PackageNameExistenceResult',
    desc: 'A possible result from an ExistenceHandler, parsing a PackageNameMap.'
    });

MATCH (packagenameexistenceresult:PackageNameExistenceResult {uuid: '7dd30b10-04bf-4875-8f98-2add74299648'}),
    (nameexistenceresult:NameExistenceResult {uuid: 'db4c51ec-7455-4bba-b965-f1c067f22e37'})
MERGE (packagenameexistenceresult)-[:ISA]->(nameexistenceresult);


// ServiceNameExistenceResult - A possible result from an ExistenceHandler, parsing a ServiceNameMap.
MERGE (servicenameexistenceresult:Object:Result:ExistenceResult:NameExistenceResult:ServiceNameExistenceResult {
    uuid: '9edc562a-f026-4ea8-861f-0dd0cfdf3c39',
    name: 'ServiceNameExistenceResult',
    desc: 'A possible result from an ExistenceHandler, parsing a ServiceNameMap.'
    });

MATCH (servicenameexistenceresult:ServiceNameExistenceResult {uuid: '9edc562a-f026-4ea8-861f-0dd0cfdf3c39'}),
    (nameexistenceresult:NameExistenceResult {uuid: 'db4c51ec-7455-4bba-b965-f1c067f22e37'})
MERGE (servicenameexistenceresult)-[:ISA]->(nameexistenceresult);


// UsageResult - A generic usage result.
MERGE (usageresult:Object:Result:UsageResult {
    uuid: '61bc2f71-158c-450c-9346-b9100505054e',
    name: 'UsageResult',
    desc: 'A generic usage result.'
    });

MATCH (usageresult:UsageResult {uuid: '61bc2f71-158c-450c-9346-b9100505054e'}),
    (result:Result {uuid: 'f3498c02-58c6-486b-87c6-9d54607fdb12'})
MERGE (usageresult)-[:ISA]->(result);


// ProcessorUsageResult - The processing unit usage results from a UsageHandler.
MERGE (processorusageresult:Object:Result:UsageResult:ProcessorUsageResult {
    uuid: '885e629b-3f03-4fa9-a876-67e3991711a0',
    name: 'ProcessorUsageResult',
    desc: 'The processing unit usage results from a UsageHandler.'
    });

MATCH (processorusageresult:ProcessorUsageResult {uuid: '885e629b-3f03-4fa9-a876-67e3991711a0'}),
    (usageresult:UsageResult {uuid: '61bc2f71-158c-450c-9346-b9100505054e'})
MERGE (processorusageresult)-[:ISA]->(usageresult);


// MemoryUsageResult - The memory usage results from a UsageHandler.
MERGE (memoryusageresult:Object:Result:UsageResult:MemoryUsageResult {
    uuid: 'c893bc3b-21de-47b8-96dd-0ba11006d35b',
    name: 'MemoryUsageResult',
    desc: 'The memory usage results from a UsageHandler.'
    });

MATCH (memoryusageresult:MemoryUsageResult {uuid: 'c893bc3b-21de-47b8-96dd-0ba11006d35b'}),
    (usageresult:UsageResult {uuid: '61bc2f71-158c-450c-9346-b9100505054e'})
MERGE (memoryusageresult)-[:ISA]->(usageresult);


// StorageUsageResult - The storage usage results from a UsageHandler.
MERGE (storageusageresult:Object:Result:UsageResult:StorageUsageResult {
    uuid: '2615131b-50ce-4699-a5ef-261fbb633fe3',
    name: 'StorageUsageResult',
    desc: 'The storage usage results from a UsageHandler.'
    });

MATCH (storageusageresult:StorageUsageResult {uuid: '2615131b-50ce-4699-a5ef-261fbb633fe3'}),
    (usageresult:UsageResult {uuid: '61bc2f71-158c-450c-9346-b9100505054e'})
MERGE (storageusageresult)-[:ISA]->(usageresult);


// NetworkUsageResult - The network bandwidth usage results from a UsageHandler.
MERGE (networkusageresult:Object:Result:UsageResult:NetworkUsageResult {
    uuid: 'ebaf3fae-351a-4d90-9b98-9970399160aa',
    name: 'NetworkUsageResult',
    desc: 'The network bandwidth usage results from a UsageHandler.'
    });

MATCH (networkusageresult:NetworkUsageResult {uuid: 'ebaf3fae-351a-4d90-9b98-9970399160aa'}),
    (usageresult:UsageResult {uuid: '61bc2f71-158c-450c-9346-b9100505054e'})
MERGE (networkusageresult)-[:ISA]->(usageresult);
