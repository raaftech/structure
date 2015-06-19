// com.raaftech.model.hypervisors-0.0.1


// HypervisorModel 0.0.1 - Main hypervisors.
MERGE (raafvmmodel:Object:Model:HypervisorModel {
    uuid: '67664a91-6ec8-4851-aa91-9af56fdffe48',
    name: 'HypervisorModel',
    desc: 'Extended hypervisor types.',
    ver: '0.0.1',
    ns: 'com.raaftech.model.hypervisors'
    });

MATCH (raafvmmodel:HypervisorModel {uuid: '67664a91-6ec8-4851-aa91-9af56fdffe48'}),
    (model:Model {uuid: '4e2dccfa-b5ca-4992-a76e-c390b7d90084'})
MERGE (raafvmmodel)-[:ISA]->(model);

MATCH (raafvmmodel:HypervisorModel {uuid: '67664a91-6ec8-4851-aa91-9af56fdffe48'}),
    (opcfvmmodel:HypervisorModel {uuid: '76fc46bb-8ff8-4e35-a8b5-80c44a82346a'})
MERGE (raafvmmodel)-[:REQUIRES]->(opcfvmmodel);


// HP Integrity Virtual Machines.
MERGE (hpvm:Object:Component:Software:Hypervisor:HpVm {
    uuid: 'af0ecba0-9c1f-43ec-b555-9a665f904dd3',
    name: 'HpVm',
    desc: 'The HP Integrity virtual machine.',
    shortName: 'hpvm',
    longName: 'HP Integrity VM'
    });

MATCH (hpvm:HpVm {uuid: 'af0ecba0-9c1f-43ec-b555-9a665f904dd3'}),
    (hypervisor:Hypervisor {uuid: 'c5c7fcb5-8ac3-40cc-b195-b2b6af368e81'})
MERGE (hpvm)-[:ISA]->(hypervisor);


// IBM PowerVM.
MERGE (powervm:Object:Component:Software:Hypervisor:PowerVm {
    uuid: '50a2e964-ed70-4a96-988d-62aa9bfec5a5',
    name: 'PowerVm',
    desc: 'The IBM PowerVM virtual machine.',
    shortName: 'powervm',
    longName: 'IBM PowerVM'
    });

MATCH (powervm:PowerVm {uuid: '50a2e964-ed70-4a96-988d-62aa9bfec5a5'}),
    (hypervisor:Hypervisor {uuid: 'c5c7fcb5-8ac3-40cc-b195-b2b6af368e81'})
MERGE (powervm)-[:ISA]->(hypervisor);


// Oracle VM Server for SPARC.
MERGE (oraclevm:Object:Component:Software:Hypervisor:OracleVm {
    uuid: 'b1722662-e3ca-423a-9217-d226e32ab6f4',
    name: 'OracleVm',
    desc: 'The Oracle VM Server for SPARC virtual machine.',
    shortName: 'oraclevm',
    longName: 'Oracle VM Server for SPARC'
    });

MATCH (oraclevm:OracleVm {uuid: 'b1722662-e3ca-423a-9217-d226e32ab6f4'}),
    (hypervisor:Hypervisor {uuid: 'c5c7fcb5-8ac3-40cc-b195-b2b6af368e81'})
MERGE (oraclevm)-[:ISA]->(hypervisor);


// VMware vSphere.
MERGE (vmv:Object:Component:Software:Hypervisor:VmwareVsphere {
    uuid: 'ea00099d-5284-4a34-940f-7c0844e137b1',
    name: 'VmwareVsphere',
    desc: 'The vSphere virtual machine manager.',
    shortName: 'vmv',
    longName: 'VMware vSphere'
    });

MATCH (vmv:VmwareVsphere {uuid: 'ea00099d-5284-4a34-940f-7c0844e137b1'}),
    (hypervisor:Hypervisor {uuid: 'c5c7fcb5-8ac3-40cc-b195-b2b6af368e81'})
MERGE (vmv)-[:ISA]->(hypervisor);
