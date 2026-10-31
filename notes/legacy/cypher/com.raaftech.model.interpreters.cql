// com.raaftech.model.interpreters-0.0.1


// InterpreterModel 0.0.1 - Main interpreters.
MERGE (raafintmodel:Object:Model:InterpreterModel {
    uuid: '970e9898-d5b9-480e-aaac-ecffb37a9c8a',
    name: 'InterpreterModel',
    desc: 'Extended interpreter types.',
    ver: '0.0.1',
    ns: 'com.raaftech.model.interpreters'
    });

MATCH (raafintmodel:InterpreterModel {uuid: '970e9898-d5b9-480e-aaac-ecffb37a9c8a'}),
    (model:Model {uuid: '4e2dccfa-b5ca-4992-a76e-c390b7d90084'})
MERGE (raafintmodel)-[:ISA]->(model);

MATCH (raafintmodel:InterpreterModel {uuid: '970e9898-d5b9-480e-aaac-ecffb37a9c8a'}),
    (opcfintmodel:InterpreterModel {uuid: '452269e5-922e-49af-8442-ef3b58348f61'})
MERGE (raafintmodel)-[:REQUIRES]->(opcfintmodel);


//TBD
