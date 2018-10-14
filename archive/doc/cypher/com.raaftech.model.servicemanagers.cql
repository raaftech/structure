// com.raaftech.model.servicemanagers-0.0.1


// ServiceManagerModel 0.0.1 - Main service manager systems.
MERGE (raafsvcmgrmodel:Object:Model:ServiceManagerModel {
    uuid: 'd42ad52a-947d-421e-ad2e-3bbdf463ebaa',
    name: 'ServiceManagerModel',
    desc: 'Extended service manager types.',
    ver: '0.0.1',
    ns: 'com.raaftech.model.servicemanagers'
    });

MATCH (raafsvcmgrmodel:ServiceManagerModel {uuid: 'd42ad52a-947d-421e-ad2e-3bbdf463ebaa'}),
    (model:Model {uuid: '4e2dccfa-b5ca-4992-a76e-c390b7d90084'})
MERGE (raafsvcmgrmodel)-[:ISA]->(model);

MATCH (raafsvcmgrmodel:ServiceManagerModel {uuid: 'd42ad52a-947d-421e-ad2e-3bbdf463ebaa'}),
    (opcfsvcmgrmodel:ServiceManagerModel {uuid: '4c305d8f-a9bb-4cfa-a512-4cc514c63349'})
MERGE (raafsvcmgrmodel)-[:REQUIRES]->(opcfsvcmgrmodel);


// Solaris Service Management Facility
MERGE (smf:Object:Component:Software:ServiceManager:Smf {
    uuid: '300beaaf-5b4c-48b1-be5b-a5fe3fa547b2',
    name: 'Smf',
    desc: 'The Solaris Service Management Facility.',
    shortName: 'smf',
    longName: 'Service Management Facility'
    });

MATCH (smf:Smf {uuid: '300beaaf-5b4c-48b1-be5b-a5fe3fa547b2'}),
    (servicemanager:ServiceManager {uuid: 'c3008eec-b454-4ef6-bf7e-0bd92c13d3c3'})
MERGE (smf)-[:ISA]->(servicemanager);
