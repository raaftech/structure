// org.openpcf.model.hypervisors-0.0.1


// HypervisorModel 0.0.1 - Main hypervisors.
MERGE (opcfvmmodel:Object:Model:HypervisorModel {
    uuid: '76fc46bb-8ff8-4e35-a8b5-80c44a82346a',
    name: 'HypervisorModel',
    desc: 'Main hypervisor types.',
    ver: '0.0.1',
    ns: 'org.openpcf.model.hypervisors'
    });

MATCH (opcfvmmodel:HypervisorModel {uuid: '76fc46bb-8ff8-4e35-a8b5-80c44a82346a'}),
    (model:Model {uuid: '4e2dccfa-b5ca-4992-a76e-c390b7d90084'})
MERGE (opcfvmmodel)-[:ISA]->(model);

MATCH (opcfvmmodel:HypervisorModel {uuid: '76fc46bb-8ff8-4e35-a8b5-80c44a82346a'}),
    (basemodel:BaseModel {uuid: '48c8698b-3afd-44e5-baf7-73f55494cb31'})
MERGE (opcfvmmodel)-[:REQUIRES]->(basemodel);


// KVM.
MERGE (kvm:Object:Component:Software:Hypervisor:Kvm {
    uuid: '04b4bd73-fcec-42f6-bb03-7686ad019fbb',
    name: 'Kvm',
    desc: 'The Linux kernel-based virtual machine.',
    shortName: 'kvm',
    longName: 'Kernel-based Virtual Machine'
    });

MATCH (kvm:Kvm {uuid: '04b4bd73-fcec-42f6-bb03-7686ad019fbb'}),
    (hypervisor:Hypervisor {uuid: 'c5c7fcb5-8ac3-40cc-b195-b2b6af368e81'})
MERGE (kvm)-[:ISA]->(hypervisor);


// Oracle VM VirtualBox.
MERGE (vbox:Object:Component:Software:Hypervisor:VirtualBox {
    uuid: '3c47f326-d613-4f42-b4ac-804a6c22f076',
    name: 'VirtualBox',
    desc: 'The Oracle VM VirtualBox virtual machine.',
    shortName: 'vbox',
    longName: 'Oracle VM VirtualBox'
    });

MATCH (vbox:VirtualBox {uuid: '3c47f326-d613-4f42-b4ac-804a6c22f076'}),
    (hypervisor:Hypervisor {uuid: 'c5c7fcb5-8ac3-40cc-b195-b2b6af368e81'})
MERGE (vbox)-[:ISA]->(hypervisor);


// VMware ESXi.
MERGE (esxi:Object:Component:Software:Hypervisor:VmwareEsxi {
    uuid: 'b9739ee8-34a6-4308-b7e2-ba775c0e59cf',
    name: 'VmwareEsxi',
    desc: 'The VMware ESXi virtual machine.',
    shortName: 'esxi',
    longName: 'VMware ESXi'
    });

MATCH (esxi:VmwareEsxi {uuid: 'b9739ee8-34a6-4308-b7e2-ba775c0e59cf'}),
    (hypervisor:Hypervisor {uuid: 'c5c7fcb5-8ac3-40cc-b195-b2b6af368e81'})
MERGE (esxi)-[:ISA]->(hypervisor);


// VMware Workstation.
MERGE (vmw:Object:Component:Software:Hypervisor:VmwareWorkstation {
    uuid: '897281c1-05be-43aa-a49c-50fd9ed4fc88',
    name: 'VmwareWorkstation',
    desc: 'The VMware Workstation virtual machine.',
    shortName: 'vmw',
    longName: 'VMware Workstation'
    });

MATCH (vmw:VmwareWorkstation {uuid: '897281c1-05be-43aa-a49c-50fd9ed4fc88'}),
    (hypervisor:Hypervisor {uuid: 'c5c7fcb5-8ac3-40cc-b195-b2b6af368e81'})
MERGE (vmw)-[:ISA]->(hypervisor);


// Xen
MERGE (xen:Object:Component:Software:Hypervisor:Xen {
    uuid: '54916142-640a-41df-82ad-974e92d3371c',
    name: 'Xen',
    desc: 'The Xen Project virtual machine.',
    shortName: 'xen',
    longName: 'Xen'
    });

MATCH (xen:Xen {uuid: '54916142-640a-41df-82ad-974e92d3371c'}),
    (hypervisor:Hypervisor {uuid: 'c5c7fcb5-8ac3-40cc-b195-b2b6af368e81'})
MERGE (xen)-[:ISA]->(hypervisor);
