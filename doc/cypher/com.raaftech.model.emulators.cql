// com.raaftech.model.emulators-0.0.1


// ServiceModel 0.0.1 - Extended emulators.
MERGE (raafemumodel:Object:Model:EmulatorModel {
    uuid: '911d42bb-1097-4a9a-8b0e-66783f703c88',
    name: 'EmulatorModel',
    desc: 'Extended emulator types.',
    ver: '0.0.1',
    ns: 'com.raaftech.model.emulators'
    });

MATCH (raafemumodel:EmulatorModel {uuid: '911d42bb-1097-4a9a-8b0e-66783f703c88'}),
    (model:Model {uuid: '4e2dccfa-b5ca-4992-a76e-c390b7d90084'})
MERGE (raafemumodel)-[:ISA]->(model);

MATCH (raafemumodel:EmulatorModel {uuid: '911d42bb-1097-4a9a-8b0e-66783f703c88'}),
    (opcfemumodel:EmulatorModel {uuid: 'ce00fbd8-b9e6-4e93-ae5e-ce0d9da5b3b4'})
MERGE (raafemumodel)-[:REQUIRES]->(opcfemumodel);


// TBD
