// org.openpcf.model.cloudsystems-0.0.1


// CloudSystemModel 0.0.1 - Main cloudsystems.
MERGE (opcfcloudmodel:Object:Model:CloudSystemModel {
    uuid: '4a8e288a-696d-4616-bb77-970dcf3e150d',
    name: 'CloudSystemModel',
    namespace: 'org.openpcf.model.cloudsystems',
    version: '0.0.1',
    desc: 'Main cloud (operating) system types.'
    });

MATCH (opcfcloudmodel:CloudSystemModel {uuid: '4a8e288a-696d-4616-bb77-970dcf3e150d'}),
    (model:Model {uuid: '4e2dccfa-b5ca-4992-a76e-c390b7d90084'})
MERGE (opcfcloudmodel)-[:ISA]->(model);

MATCH (opcfcloudmodel:CloudSystemModel {uuid: '4a8e288a-696d-4616-bb77-970dcf3e150d'}),
    (basemodel:BaseModel {uuid: '48c8698b-3afd-44e5-baf7-73f55494cb31'})
MERGE (opcfcloudmodel)-[:REQUIRES]->(basemodel);


// ApacheCloudStack -The Apache CloudStack platform.
MERGE (cloudstack:Object:Component:Software:CloudSystem:ApacheCloudStack {
    uuid: 'c1d0eada-0d22-4026-aa2d-d865c1583482',
    name: 'ApacheCloudStack',
    desc: 'The Apache CloudStack platform.',
    shortName: 'cloudstack',
    longName: 'Apache CloudStack'
    });

MATCH (cloudstack:ApacheCloudStack {uuid: 'c1d0eada-0d22-4026-aa2d-d865c1583482'}),
    (cloudsystem:CloudSystem {uuid: '86c0439a-4219-46e6-82a3-6fb2ee774aa4'})
MERGE (cloudstack)-[:ISA]->(cloudsystem);


// VmwareCloudFoundry - The VMware CloudFoundry platform.
MERGE (cloudfoundry:Object:Component:Software:CloudSystem:VmwareCloudFoundry {
    uuid: '6d080eef-d9c6-4be7-b81b-b9c57f0fe515',
    name: 'VmwareCloudFoundry',
    desc: 'The VMware CloudFoundry platform.',
    shortName: 'cloudfoundry',
    longName: 'VMware CloudFoundry'
    });

MATCH (cloudfoundry:VmwareCloudFoundry {uuid: '6d080eef-d9c6-4be7-b81b-b9c57f0fe515'}),
    (cloudsystem:CloudSystem {uuid: '86c0439a-4219-46e6-82a3-6fb2ee774aa4'})
MERGE (cloudfoundry)-[:ISA]->(cloudsystem);


// Eucalyptus - The Eucalyptus Cloud platform.
MERGE (eucalyptus:Object:Component:Software:CloudSystem:Eucalyptus {
    uuid: '87c66de2-e24b-4586-8170-5fcf6cc28424',
    name: 'Eucalyptus',
    desc: 'The Eucalyptus Cloud platform.',
    shortName: 'eucalyptus',
    longName: 'Eucalyptus'
    });

MATCH (eucalyptus:Eucalyptus {uuid: '87c66de2-e24b-4586-8170-5fcf6cc28424'}),
    (cloudsystem:CloudSystem {uuid: '86c0439a-4219-46e6-82a3-6fb2ee774aa4'})
MERGE (eucalyptus)-[:ISA]->(cloudsystem);


// OpenNebula
MERGE (opennebula:Object:Component:Software:CloudSystem:OpenNebula {
    uuid: '0e5b0e12-5975-43f5-9e92-5298a75a5f7a',
    name: 'OpenNebula',
    desc: 'The OpenNebula Cloud platform.',
    shortName: 'opennebula',
    longName: 'OpenNebula'
    });

MATCH (opennebula:OpenNebula {uuid: '0e5b0e12-5975-43f5-9e92-5298a75a5f7a'}),
    (cloudsystem:CloudSystem {uuid: '86c0439a-4219-46e6-82a3-6fb2ee774aa4'})
MERGE (opennebula)-[:ISA]->(cloudsystem);


// OpenShift
MERGE (openshift:Object:Component:Software:CloudSystem:OpenShift {
    uuid: 'e57b8fbb-05a0-4c64-830a-8da17801e757',
    name: 'OpenShift',
    desc: 'The OpenShift Cloud platform.',
    shortName: 'openshift',
    longName: 'OpenShift'
    });

MATCH (openshift:OpenShift {uuid: 'e57b8fbb-05a0-4c64-830a-8da17801e757'}),
    (cloudsystem:CloudSystem {uuid: '86c0439a-4219-46e6-82a3-6fb2ee774aa4'})
MERGE (openshift)-[:ISA]->(cloudsystem);


// OpenStack
MERGE (openstack:Object:Component:Software:CloudSystem:OpenStack {
    uuid: '55e9f170-9a98-42e3-ba58-f35c592eaeb0',
    name: 'OpenStack',
    desc: 'The OpenStack Cloud platform.',
    shortName: 'openstack',
    longName: 'OpenStack'
    });

MATCH (openstack:OpenStack {uuid: '55e9f170-9a98-42e3-ba58-f35c592eaeb0'}),
    (cloudsystem:CloudSystem {uuid: '86c0439a-4219-46e6-82a3-6fb2ee774aa4'})
MERGE (openstack)-[:ISA]->(cloudsystem);
