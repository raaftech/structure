// com.raaftech.model.packagemanagers-0.0.1


// PackageManagerModel 0.0.1 - Main packaging systems.
MERGE (raafpkgmodel:Object:Model:PackageManagerModel {
    uuid: 'd14a4acd-6b96-4f55-914c-3e61ac413fc2',
    name: 'PackageManagerModel',
    desc: 'Extended package manager types.',
    ver: '0.0.1',
    ns: 'com.raaftech.model.packagemanagers'
    });

MATCH (raafpkgmodel:PackageManagerModel {uuid: 'd14a4acd-6b96-4f55-914c-3e61ac413fc2'}),
    (model:Model {uuid: '4e2dccfa-b5ca-4992-a76e-c390b7d90084'})
MERGE (raafpkgmodel)-[:ISA]->(model);

MATCH (raafpkgmodel:PackageManagerModel {uuid: 'd14a4acd-6b96-4f55-914c-3e61ac413fc2'}),
    (opcfpkgmodel:PackageManagerModel {uuid: '88846580-7ca5-4d7c-aba4-f350e61dd5ce'})
MERGE (raafpkgmodel)-[:REQUIRES]->(opcfpkgmodel);


// SdUx - SD/UX, the HP/UX Software Distributor.
MERGE (sdux:Object:Component:Software:PackageManager:SdUx {
    uuid: '92e1ff01-8f01-4882-be63-ed9d0777924d',
    name: 'SdUx',
    desc: 'SD/UX, the HP/UX Software Distributor.',
    shortName: 'sdux',
    longName: 'SD/UX'
    });

MATCH (sdux:SdUx {uuid: '92e1ff01-8f01-4882-be63-ed9d0777924d'}),
    (packagemanager:PackageManager {uuid: 'e2b8b9ee-b66f-4252-8fba-4fb172f741bc'})
MERGE (sdux)-[:ISA]->(packagemanager);


// AixLpp - IBM Licensed Program Product Manager.
MERGE (aixlpp:Object:Component:Software:PackageManager:AixLpp {
    uuid: '5da69c0f-ac1f-4fbf-8365-77c2706874aa',
    name: 'AixLpp',
    desc: 'IBM Licensed Program Product Manager.',
    shortName: 'aixlpp',
    longName: 'AIX LPP'
    });

MATCH (aixlpp:AixLpp {uuid: '5da69c0f-ac1f-4fbf-8365-77c2706874aa'}),
    (packagemanager:PackageManager {uuid: 'e2b8b9ee-b66f-4252-8fba-4fb172f741bc'})
MERGE (aixlpp)-[:ISA]->(packagemanager);


// SolarisIps - Solaris Image Packaging System Manager.
MERGE (solips:Object:Component:Software:PackageManager:SolarisIps {
    uuid: 'e70e7353-4e74-4a58-a7ca-eef0005ad5ec',
    name: 'SolarisIps',
    desc: 'Solaris Image Packaging System Manager.',
    shortName: 'solips',
    longName: 'Solaris IPS'
    });

MATCH (solips:SolarisIps {uuid: 'e70e7353-4e74-4a58-a7ca-eef0005ad5ec'}),
    (packagemanager:PackageManager {uuid: 'e2b8b9ee-b66f-4252-8fba-4fb172f741bc'})
MERGE (solips)-[:ISA]->(packagemanager);


// SolarisPkg - Solaris SVR4 Package (.pkg) Manager.
MERGE (solpkg:Object:Component:Software:PackageManager:SolarisPkg {
    uuid: '7ed426f2-c163-4bdc-b1a0-0dc05e31f691',
    name: 'SolarisPkg',
    desc: 'Older Solaris Package System Manager.',
    shortName: 'solpkg',
    longName: 'Solaris PKG'
    });

MATCH (solpkg:SolarisPkg {uuid: '7ed426f2-c163-4bdc-b1a0-0dc05e31f691'}),
    (packagemanager:PackageManager {uuid: 'e2b8b9ee-b66f-4252-8fba-4fb172f741bc'})
MERGE (solpkg)-[:ISA]->(packagemanager);
