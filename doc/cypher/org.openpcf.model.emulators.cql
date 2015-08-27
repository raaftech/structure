// org.openpcf.model.emulators-0.0.1


// EmulatorModel 0.0.1 - Main emulators.
MERGE (opcfemumodel:Object:Model:EmulatorModel {
    uuid: 'ce00fbd8-b9e6-4e93-ae5e-ce0d9da5b3b4',
    name: 'EmulatorModel',
    desc: 'Main emulator types.',
    ver: '0.0.1',
    ns: 'org.openpcf.model.emulators'
    });

MATCH (opcfemumodel:EmulatorModel {uuid: 'ce00fbd8-b9e6-4e93-ae5e-ce0d9da5b3b4'}),
    (model:Model {uuid: '4e2dccfa-b5ca-4992-a76e-c390b7d90084'})
MERGE (opcfemumodel)-[:ISA]->(model);

MATCH (opcfemumodel:EmulatorModel {uuid: 'ce00fbd8-b9e6-4e93-ae5e-ce0d9da5b3b4'}),
    (basemodel:BaseModel {uuid: '48c8698b-3afd-44e5-baf7-73f55494cb31'})
MERGE (opcfemumodel)-[:REQUIRES]->(basemodel);


// Bochs.
MERGE (bochs:Object:Component:Software:Emulator:Bochs {
    uuid: 'cd8f940e-219b-488e-8b75-d9fece8b3b35',
    name: 'Bochs',
    desc: 'The Bochs Emulator.',
    shortName: 'bochs',
    longName: 'Bochs'
    });

MATCH (bochs:Bochs {uuid: 'cd8f940e-219b-488e-8b75-d9fece8b3b35'}),
    (emulator:Emulator {uuid: '59aeae81-9741-4986-b9c2-931504cfc9e3'})
MERGE (bochs)-[:ISA]->(emulator);


// DOSBox.
MERGE (dosbox:Object:Component:Software:Emulator:DosBox {
    uuid: '7e0135dc-b720-440a-9f4e-76747f7a7fe5',
    name: 'DosBox',
    desc: 'The DOSBox Emulator.',
    shortName: 'dosbox',
    longName: 'DOSBox'
    });

MATCH (dosbox:DosBox {uuid: '7e0135dc-b720-440a-9f4e-76747f7a7fe5'}),
    (emulator:Emulator {uuid: '59aeae81-9741-4986-b9c2-931504cfc9e3'})
MERGE (dosbox)-[:ISA]->(emulator);


// Qemu.
MERGE (qemu:Object:Component:Software:Emulator:Qemu {
    uuid: 'c80fddb7-bdbf-44b9-ac54-f7dd7ce4dbd0',
    name: 'Qemu',
    desc: 'The QEMU Emulator.',
    shortName: 'qemu',
    longName: 'QEMU'
    });

MATCH (qemu:Qemu {uuid: 'c80fddb7-bdbf-44b9-ac54-f7dd7ce4dbd0'}),
    (emulator:Emulator {uuid: '59aeae81-9741-4986-b9c2-931504cfc9e3'})
MERGE (qemu)-[:ISA]->(emulator);
