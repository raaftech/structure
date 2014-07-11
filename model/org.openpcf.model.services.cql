// org.openpcf.model.services-0.0.1


// ServiceModel 0.0.1 - Main services.
MERGE (opcfsvcmodel:Object:Model:ServiceModel {
    uuid: 'f947e94a-f9b2-4170-90e1-107c2a946bd7',
    name: 'ServiceModel',
    namespace: 'org.openpcf.model.services',
    version: '0.0.1',
    desc: 'Main service types.'
    });

MATCH (opcfsvcmodel:ServiceModel {uuid: 'f947e94a-f9b2-4170-90e1-107c2a946bd7'}),
    (model:Model {uuid: '4e2dccfa-b5ca-4992-a76e-c390b7d90084'})
MERGE (opcfsvcmodel)-[:ISA]->(model);

MATCH (opcfsvcmodel:ServiceModel {uuid: 'f947e94a-f9b2-4170-90e1-107c2a946bd7'}),
    (basemodel:BaseModel {uuid: '48c8698b-3afd-44e5-baf7-73f55494cb31'})
MERGE (opcfsvcmodel)-[:REQUIRES]->(basemodel);


// HttpServer - A generic HTTP Server.
MERGE (httpserver:Object:Component:Software:Service:HttpServer {
    uuid: 'b46cb520-caaf-4d26-b59d-58edca723b8d',
    name: 'HttpServer',
    desc: 'A generic HTTP Server.'
    });

MATCH (httpserver:HttpServer {uuid: 'b46cb520-caaf-4d26-b59d-58edca723b8d'}),
    (service:Service {uuid: '3fb4603b-281e-47c0-b338-768ea05d4660'})
MERGE (httpserver)-[:ISA]->(service);


// SmbServer - A generic SMB Server.
MERGE (smbserver:Object:Component:Software:Service:SmbServer {
    uuid: '65af133b-1a2c-4faa-8d2a-a288eaa4f412',
    name: 'SmbServer',
    desc: 'A generic SMB/CIFS Server.'
    });

MATCH (smbserver:SmbServer {uuid: '65af133b-1a2c-4faa-8d2a-a288eaa4f412'}),
    (service:Service {uuid: '3fb4603b-281e-47c0-b338-768ea05d4660'})
MERGE (smbserver)-[:ISA]->(service);


// SshServer - A generic SMB Server.
MERGE (sshserver:Object:Component:Software:Service:SshServer {
    uuid: '7db4a3ef-2d3c-4071-929b-e81505fbd5e2',
    name: 'SshServer',
    desc: 'A generic SSH Server.'
    });

MATCH (sshserver:SshServer {uuid: '7db4a3ef-2d3c-4071-929b-e81505fbd5e2'}),
    (service:Service {uuid: '3fb4603b-281e-47c0-b338-768ea05d4660'})
MERGE (sshserver)-[:ISA]->(service);


// TelnetServer - A generic SMB Server.
MERGE (telnetserver:Object:Component:Software:Service:TelnetServer {
    uuid: '4a09bcce-59c6-4e94-9fc4-ba020733a27c',
    name: 'TelnetServer',
    desc: 'A generic TELNET Server.'
    });

MATCH (telnetserver:TelnetServer {uuid: '4a09bcce-59c6-4e94-9fc4-ba020733a27c'}),
    (service:Service {uuid: '3fb4603b-281e-47c0-b338-768ea05d4660'})
MERGE (telnetserver)-[:ISA]->(service);
