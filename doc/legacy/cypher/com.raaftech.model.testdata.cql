// com.raaftech.model.testdata-0.0.1


// My Memory.
MERGE (mymemory:Instance:Object:Component:Device:MemoryDevice:VolatileMemory:RandomAccessMemory {
    uuid: '7bc98f1b-a044-4d6e-ac98-3e5885f08063',
    name: 'MyMemory',
    desc: 'My Memory.'
    });

MATCH (mymemory:Instance {uuid: '7bc98f1b-a044-4d6e-ac98-3e5885f08063'}),
    (ram:RandomAccessMemory {uuid:'4c0a0638-1fa7-4f5b-9d69-1d199dcedccc'})
MERGE (mymemory)-[:ISA]->(ram);


// My HardDisk.
MERGE (myhdd:Instance:Object:Component:Device:MemoryDevice:NonVolatileMemory:HardDiskDrive {
    uuid: '84a4db29-bbbb-437e-be77-f851d59d1331',
    name: 'MyHardDisk',
    desc: 'My Hard Disk Drive.'
    });

MATCH (myhdd:Instance {uuid: '84a4db29-bbbb-437e-be77-f851d59d1331'}),
    (hdd:HardDiskDrive {uuid:'24cc3545-0d1a-4d85-8d08-a83c3dbe5912'})
MERGE (myhdd)-[:ISA]->(hdd);


// My Floppy.
MERGE (myfloppy:Instance:Object:Component:Device:MemoryDevice:NonVolatileMemory:FloppyDrive {
    uuid: '413c7152-77b8-4203-b2f4-0385fd01d259',
    name: 'MyFloppyDrive',
    desc: 'My Floppy Drive.'
    });

MATCH (myfloppy:Instance {uuid: '413c7152-77b8-4203-b2f4-0385fd01d259'}),
    (floppy:FloppyDrive {uuid:'118ec961-a823-465b-be17-b706f1c8c2f1'})
MERGE (myfloppy)-[:ISA]->(floppy);


// My SerialDevice.
MERGE (myserial:Instance:Object:Component:Device:ConsoleDevice:GenericSerial {
    uuid: '78e1f1ce-c375-4537-9bf6-0bfa09751700',
    name: 'MySerialDevice',
    desc: 'My Serial Device.'
    });

MATCH (myserial:Instance {uuid: '78e1f1ce-c375-4537-9bf6-0bfa09751700'}),
    (serial:GenericSerial {uuid:'f5b25736-ec12-4a7e-b198-548b97ebc36f'})
MERGE (myserial)-[:ISA]->(serial);


// My EthernetDevice.
MERGE (myeth:Instance:Object:Component:Device:NetworkDevice:WiredNetworkDevice:EthernetDevice {
    uuid: '8509643b-139b-4e4b-a77f-2c46c17cf46c',
    name: 'MyEthernetDevice',
    desc: 'My Ethernet Device.'
    });

MATCH (myeth:Instance {uuid: '8509643b-139b-4e4b-a77f-2c46c17cf46c'}),
    (eth:EthernetDevice {uuid:'0dbd26f2-baff-446c-9241-46eaa3e03736'})
MERGE (myeth)-[:ISA]->(eth);


// My Processor.
MERGE (myproc:Instance:Object:Component:Device:ProcessingUnit:ItaniumProcessor {
    uuid: '49822bf5-4432-4acd-93ed-dc1ee211e456',
    name: 'MyProcessor',
    desc: 'My Processor.'
    });

MATCH (myproc:Instance {uuid: '49822bf5-4432-4acd-93ed-dc1ee211e456'}),
    (proc:ItaniumProcessor {uuid:'241015c9-72f5-491f-a870-a30d8f7902fd'})
MERGE (myproc)-[:ISA]->(proc);


// My Operating System.
MERGE (myos:Instance:Object:Component:Software:OperatingSystem:UnixLike:Unix:HpUx:HpUx11:HpUx1131 {
    uuid: '4c2f144a-aa29-4c83-8a98-424608029781',
    name: 'MyOperatingSystem',
    desc: 'My HP/UX Operating System.'
    });

MATCH (myos:Instance {uuid: '4c2f144a-aa29-4c83-8a98-424608029781'}),
    (os:OperatingSystem {uuid:'591557b5-f13a-4070-a043-782830560664'})
MERGE (myos)-[:ISA]->(os);


// My ServiceManager.
MERGE (mysm:Instance:Object:Component:Software:ServiceManager:SysvInit {
    uuid: 'd83dc1c0-2d28-4f72-ad96-cfb91442a7cb',
    name: 'MyServiceManager',
    desc: 'My HP/UX SysvInit Service Manager.'
    });

MATCH (mysm:Instance {uuid: 'd83dc1c0-2d28-4f72-ad96-cfb91442a7cb'}),
    (sm:ServiceManager {uuid:'608bfaf1-9edc-4609-b5cd-56d8aeff8174'})
MERGE (mysm)-[:ISA]->(sm);


// My PackageManager.
MERGE (mypm:Instance:Object:Component:Software:PackageManager:SdUx {
    uuid: 'a98db2cf-4a1b-45e3-856f-57ca0d7ac138',
    name: 'MyPackageManager',
    desc: 'My SD/UX Package Manager.'
    });

MATCH (mypm:Instance {uuid: 'a98db2cf-4a1b-45e3-856f-57ca0d7ac138'}),
    (pm:PackageManager {uuid:'92e1ff01-8f01-4882-be63-ed9d0777924d'})
MERGE (mypm)-[:ISA]->(pm);


// My SshServer.
MERGE (myssh:Instance:Object:Component:Software:Service:SshServer {
    uuid: 'd7e5d1b8-c1bc-4bb8-887d-e3c3f5e1a847',
    name: 'MySsh',
    desc: 'My SSH Server.'
    });

MATCH (myssh:Instance {uuid: 'd7e5d1b8-c1bc-4bb8-887d-e3c3f5e1a847'}),
    (ssh:SshServer {uuid:'7db4a3ef-2d3c-4071-929b-e81505fbd5e2'})
MERGE (myssh)-[:ISA]->(ssh);


// My HttpServer.
MERGE (myhttp:Instance:Object:Component:Software:Service:HttpServer {
    uuid: 'bdea9a85-8821-4863-be3d-c8bec72f19ae',
    name: 'MyHttp',
    desc: 'My HTTP Server.'
    });

MATCH (myhttp:Instance {uuid: 'bdea9a85-8821-4863-be3d-c8bec72f19ae'}),
    (http:HttpServer {uuid:'b46cb520-caaf-4d26-b59d-58edca723b8d'})
MERGE (myhttp)-[:ISA]->(http);


// My Hypervisor.
MERGE (myhypervisor:Instance:Object:Component:Software:Hypervisor:HpVm {
    uuid: '546ad37f-b59b-415a-998b-4c9635482881',
    name: 'MyHypervisor',
    desc: 'My HPVM Hypervisor.'
    });

MATCH (myhypervisor:Instance {uuid: '546ad37f-b59b-415a-998b-4c9635482881'}),
    (hypervisor:Hypervisor {uuid:'af0ecba0-9c1f-43ec-b555-9a665f904dd3'})
MERGE (myhypervisor)-[:ISA]->(hypervisor);


// My Shell.
MERGE (mysh:Instance:Object:Component:Software:Interpreter:UnixShell:Ksh {
    uuid: '9a183456-5637-4e40-a13d-5e3a57733fde',
    name: 'MyShell',
    desc: 'My Korn Shell.'
    });

MATCH (mysh:Instance {uuid: '9a183456-5637-4e40-a13d-5e3a57733fde'}),
    (sh:UnixShell {uuid:'fd95e858-c788-4578-9223-122eceb2e6c6'})
MERGE (mysh)-[:ISA]->(sh);


// My IP address.
MERGE (myip:Instance:Object:Component:Configuration:IpAddress:Ipv4Address {
    uuid: '83c5ab5a-691b-4404-bdec-6602e2d816bb',
    name: 'MyIpAddress',
    ipaddr: '192.168.1.123',
    desc: 'My IP address.'
    });

MATCH (myip:Instance {uuid: '83c5ab5a-691b-4404-bdec-6602e2d816bb'}),
    (ipv4address:Ipv4Address {uuid:'f7fbe917-9d49-4df5-95f6-6e132067a424'})
MERGE (myip)-[:ISA]->(ipv4address);


// My IP listener.
MERGE (myls:Instance:Object:Component:Configuration:IpListener:Ipv4Listener {
    uuid: 'e17d5c2b-a331-40db-8a98-7f622b1ca279',
    name: 'MyListener',
    ipaddr: '0.0.0.0',
    port: "8080",
    desc: 'My IP listener for a HTTP Service.'
    });

MATCH (myls:Instance {uuid: 'e17d5c2b-a331-40db-8a98-7f622b1ca279'}),
    (ipv4listener:Ipv4Listener {uuid:'5ca95f7a-5175-4e7f-8a61-862e24485af3'})
MERGE (myls)-[:ISA]->(ipv4listener);


// My Computer.
MERGE (mycomputer:Instance:Object:Component:Device:AssembledDevice:Computer {
    uuid: 'e86c9ea5-a343-4db1-9104-22364fbcdd17',
    name: 'MyComputer',
    desc: 'My Computer'
    });

MATCH (mycomputer:Instance {uuid: 'e86c9ea5-a343-4db1-9104-22364fbcdd17'}),
    (computer:Computer {uuid:'ec11db91-6973-4285-b018-b461fbb432d7'})
MERGE (mycomputer)-[:ISA]->(computer);

MATCH (mycomputer:Instance {uuid: 'e86c9ea5-a343-4db1-9104-22364fbcdd17'}),
    (mymemory:Instance {uuid:'7bc98f1b-a044-4d6e-ac98-3e5885f08063'})
MERGE (mycomputer)-[:HAS]->(mymemory);

MATCH (mycomputer:Instance {uuid: 'e86c9ea5-a343-4db1-9104-22364fbcdd17'}),
    (myhdd:Instance {uuid:'84a4db29-bbbb-437e-be77-f851d59d1331'})
MERGE (mycomputer)-[:HAS]->(myhdd);

MATCH (mycomputer:Instance {uuid: 'e86c9ea5-a343-4db1-9104-22364fbcdd17'}),
    (myfloppy:Instance {uuid:'413c7152-77b8-4203-b2f4-0385fd01d259'})
MERGE (mycomputer)-[:HAS]->(myfloppy);

MATCH (mycomputer:Instance {uuid: 'e86c9ea5-a343-4db1-9104-22364fbcdd17'}),
    (myserial:Instance {uuid:'78e1f1ce-c375-4537-9bf6-0bfa09751700'})
MERGE (mycomputer)-[:HAS]->(myserial);

MATCH (mycomputer:Instance {uuid: 'e86c9ea5-a343-4db1-9104-22364fbcdd17'}),
    (myeth:Instance {uuid:'8509643b-139b-4e4b-a77f-2c46c17cf46c'})
MERGE (mycomputer)-[:HAS]->(myeth);

MATCH (mycomputer:Instance {uuid: 'e86c9ea5-a343-4db1-9104-22364fbcdd17'}),
    (myproc:Instance {uuid:'49822bf5-4432-4acd-93ed-dc1ee211e456'})
MERGE (mycomputer)-[:HAS]->(myproc);

MATCH (mycomputer:Instance {uuid: 'e86c9ea5-a343-4db1-9104-22364fbcdd17'}),
    (myos:Instance {uuid: '4c2f144a-aa29-4c83-8a98-424608029781'})
MERGE (mycomputer)-[:HAS]->(myos);

MATCH (myos:Instance {uuid: '4c2f144a-aa29-4c83-8a98-424608029781'}),
    (myip:Ipv4Address {uuid: '83c5ab5a-691b-4404-bdec-6602e2d816bb'})
MERGE (myos)-[:HAS]->(myip);

MATCH (myos:Instance {uuid: '4c2f144a-aa29-4c83-8a98-424608029781'}),
    (mysm:ServiceManager {uuid: 'd83dc1c0-2d28-4f72-ad96-cfb91442a7cb'})
MERGE (myos)-[:HAS]->(mysm);

MATCH (myos:Instance {uuid: '4c2f144a-aa29-4c83-8a98-424608029781'}),
    (mypm:PackageManager {uuid: 'a98db2cf-4a1b-45e3-856f-57ca0d7ac138'})
MERGE (myos)-[:HAS]->(mypm);

MATCH (myos:Instance {uuid: '4c2f144a-aa29-4c83-8a98-424608029781'}),
    (mysh:UnixShell {uuid: '9a183456-5637-4e40-a13d-5e3a57733fde'})
MERGE (myos)-[:HAS]->(mysh);

MATCH (myos:Instance {uuid: '4c2f144a-aa29-4c83-8a98-424608029781'}),
    (myhypervisor:Hypervisor {uuid: '546ad37f-b59b-415a-998b-4c9635482881'})
MERGE (myos)-[:HAS]->(myhypervisor);

MATCH (myos:Instance {uuid: '4c2f144a-aa29-4c83-8a98-424608029781'}),
    (myssh:SshServer {uuid: 'd7e5d1b8-c1bc-4bb8-887d-e3c3f5e1a847'})
MERGE (myos)-[:HAS]->(myssh);

MATCH (myos:Instance {uuid: '4c2f144a-aa29-4c83-8a98-424608029781'}),
    (myhttp:HttpServer {uuid: 'bdea9a85-8821-4863-be3d-c8bec72f19ae'})
MERGE (myos)-[:HAS]->(myhttp);

MATCH (myhttp:HttpServer {uuid: 'bdea9a85-8821-4863-be3d-c8bec72f19ae'}),
    (myls:Ipv4Listener {uuid: 'e17d5c2b-a331-40db-8a98-7f622b1ca279'})
MERGE (myhttp)-[:HAS]->(myls);
