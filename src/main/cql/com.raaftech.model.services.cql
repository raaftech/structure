// com.raaftech.model.services-0.0.1


// ServiceModel 0.0.1 - Extended services.
MERGE (raafsvcmodel:Object:Model:ServiceModel {
    uuid: 'a82f6282-2067-4091-b6f2-18bd9cc80cda',
    name: 'ServiceModel',
    desc: 'Extended service types.',
    ver: '0.0.1',
    ns: 'com.raaftech.model.services'
    });

MATCH (raafsvcmodel:ServiceModel {uuid: 'a82f6282-2067-4091-b6f2-18bd9cc80cda'}),
    (model:Model {uuid: '4e2dccfa-b5ca-4992-a76e-c390b7d90084'})
MERGE (raafsvcmodel)-[:ISA]->(model);

MATCH (raafsvcmodel:ServiceModel {uuid: 'a82f6282-2067-4091-b6f2-18bd9cc80cda'}),
    (opcfsvcmodel:ServiceModel {uuid: 'f947e94a-f9b2-4170-90e1-107c2a946bd7'})
MERGE (raafsvcmodel)-[:REQUIRES]->(opcfsvcmodel);


// TBD
