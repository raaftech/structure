// org.openpcf.model.servicemanagers-0.0.1


// ServiceManagerModel 0.0.1 - Main service manager systems.
MERGE (opcfsvcmgrmodel:Object:Model:ServiceManagerModel {
    uuid: '4c305d8f-a9bb-4cfa-a512-4cc514c63349',
    name: 'ServiceManagerModel',
    namespace: 'org.openpcf.model.servicemanagers',
    version: '0.0.1',
    desc: 'Main service manager types.'
    });

MATCH (opcfsvcmgrmodel:ServiceManagerModel {uuid: '4c305d8f-a9bb-4cfa-a512-4cc514c63349'}),
    (model:Model {uuid: '4e2dccfa-b5ca-4992-a76e-c390b7d90084'})
MERGE (opcfsvcmgrmodel)-[:ISA]->(model);

MATCH (opcfsvcmgrmodel:ServiceManagerModel {uuid: '4c305d8f-a9bb-4cfa-a512-4cc514c63349'}),
    (basemodel:BaseModel {uuid: '48c8698b-3afd-44e5-baf7-73f55494cb31'})
MERGE (opcfsvcmgrmodel)-[:REQUIRES]->(basemodel);


// SysV Init
MERGE (sysvinit:Object:Component:Software:ServiceManager:SysvInit {
    uuid: '608bfaf1-9edc-4609-b5cd-56d8aeff8174',
    name: 'SysvInit',
    desc: 'SysV Init Service Manager.'
    });

MATCH (sysvinit:SysvInit {uuid: '608bfaf1-9edc-4609-b5cd-56d8aeff8174'}),
    (servicemanager:ServiceManager {uuid: 'c3008eec-b454-4ef6-bf7e-0bd92c13d3c3'})
MERGE (sysvinit)-[:ISA]->(servicemanager);


// BSD RC Init
MERGE (bsdrcinit:Object:Component:Software:ServiceManager:BsdrcInit {
    uuid: 'b4c067de-e3cf-4b5b-90ca-588725d1b6c4',
    name: 'BsdrcInit',
    desc: 'BSD RC Init Service Manager.'
    });

MATCH (bsdrcinit:BsdrcInit {uuid: 'b4c067de-e3cf-4b5b-90ca-588725d1b6c4'}),
    (servicemanager:ServiceManager {uuid: 'c3008eec-b454-4ef6-bf7e-0bd92c13d3c3'})
MERGE (bsdrcinit)-[:ISA]->(servicemanager);


// Linux Upstart
MERGE (upstart:Object:Component:Software:ServiceManager:Upstart {
    uuid: '3aac3563-a493-4efa-a31b-e751ee7d0a6f',
    name: 'Upstart',
    desc: 'Upstart Service Manager.'
    });

MATCH (upstart:Upstart {uuid: '3aac3563-a493-4efa-a31b-e751ee7d0a6f'}),
    (servicemanager:ServiceManager {uuid: 'c3008eec-b454-4ef6-bf7e-0bd92c13d3c3'})
MERGE (upstart)-[:ISA]->(servicemanager);


// Linux Systemd
MERGE (systemd:Object:Component:Software:ServiceManager:Systemd {
    uuid: '0c189a0e-6b78-4177-832d-5d9b35b8a6af',
    name: 'Systemd',
    desc: 'Systemd Service Manager.'
    });

MATCH (systemd:Systemd {uuid: '0c189a0e-6b78-4177-832d-5d9b35b8a6af'}),
    (servicemanager:ServiceManager {uuid: 'c3008eec-b454-4ef6-bf7e-0bd92c13d3c3'})
MERGE (systemd)-[:ISA]->(servicemanager);


// Mac OS X SystemStarter
MERGE (systemstarter:Object:Component:Software:ServiceManager:SystemStarter {
    uuid: 'fd13bd74-6682-4494-92af-a6caf5e64f12',
    name: 'SystemStarter',
    desc: 'Mac OS X SystemStarter.'
    });

MATCH (systemstarter:SystemStarter {uuid: 'fd13bd74-6682-4494-92af-a6caf5e64f12'}),
    (servicemanager:ServiceManager {uuid: 'c3008eec-b454-4ef6-bf7e-0bd92c13d3c3'})
MERGE (systemstarter)-[:ISA]->(servicemanager);


// Mac OS X Launchd
MERGE (launchd:Object:Component:Software:ServiceManager:Launchd {
    uuid: '7b0bfefb-2212-4c58-a7c5-46ff66f0166a',
    name: 'Launchd',
    desc: 'Mac OS X Launchd.'
    });

MATCH (launchd:Launchd {uuid: '7b0bfefb-2212-4c58-a7c5-46ff66f0166a'}),
    (servicemanager:ServiceManager {uuid: 'c3008eec-b454-4ef6-bf7e-0bd92c13d3c3'})
MERGE (launchd)-[:ISA]->(servicemanager);


// Windows Service Control Manager
MERGE (scm:Object:Component:Software:ServiceManager:Scm {
    uuid: '198435e1-cf1d-4d0a-9fb8-c760b5e8ac2b',
    name: 'Scm',
    desc: 'Service Control Manager.'
    });

MATCH (scm:Scm {uuid: '198435e1-cf1d-4d0a-9fb8-c760b5e8ac2b'}),
    (servicemanager:ServiceManager {uuid: 'c3008eec-b454-4ef6-bf7e-0bd92c13d3c3'})
MERGE (scm)-[:ISA]->(servicemanager);
