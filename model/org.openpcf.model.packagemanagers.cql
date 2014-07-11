// org.openpcf.model.packagemanagers-0.0.1


// PackageManagerModel 0.0.1 - Main packaging systems.
MERGE (opcfpkgmodel:Object:Model:PackageManagerModel {
    uuid: '88846580-7ca5-4d7c-aba4-f350e61dd5ce',
    name: 'PackageManagerModel',
    namespace: 'org.openpcf.model.packagemanagers',
    version: '0.0.1',
    desc: 'Main package manager types.'
    });

MATCH (opcfpkgmodel:PackageManagerModel {uuid: '88846580-7ca5-4d7c-aba4-f350e61dd5ce'}),
    (model:Model {uuid: '4e2dccfa-b5ca-4992-a76e-c390b7d90084'})
MERGE (opcfpkgmodel)-[:ISA]->(model);

MATCH (opcfpkgmodel:PackageManagerModel {uuid: '88846580-7ca5-4d7c-aba4-f350e61dd5ce'}),
    (basemodel:BaseModel {uuid: '48c8698b-3afd-44e5-baf7-73f55494cb31'})
MERGE (opcfpkgmodel)-[:REQUIRES]->(basemodel);


// PacMan - Arch Linux Pacman Package Manager.
MERGE (pacman:Object:Component:Software:PackageManager:PacMan {
    uuid: 'ad047d85-792a-4385-a14c-b85266bc5176',
    name: 'PacMan',
    desc: 'Arch Linux Pacman Package Manager.'
    });

MATCH (pacman:PacMan {uuid: 'ad047d85-792a-4385-a14c-b85266bc5176'}),
    (packagemanager:PackageManager {uuid: 'e2b8b9ee-b66f-4252-8fba-4fb172f741bc'})
MERGE (pacman)-[:ISA]->(packagemanager);


// BsdPkg - BSD pkg Package Manager.
MERGE (bsdpkg:Object:Component:Software:PackageManager:BsdPkg {
    uuid: '95d6093b-448b-4a62-8642-dc8d32b43622',
    name: 'BsdPkg',
    desc: 'BSD pkg Package Manager.'
    });

MATCH (bsdpkg:BsdPkg {uuid: '95d6093b-448b-4a62-8642-dc8d32b43622'}),
    (packagemanager:PackageManager {uuid: 'e2b8b9ee-b66f-4252-8fba-4fb172f741bc'})
MERGE (bsdpkg)-[:ISA]->(packagemanager);


// BsdPkgNg - BSD pkgng Package Manager.
MERGE (bsdpkgng:Object:Component:Software:PackageManager:BsdPkgNg {
    uuid: '1a879371-f680-4514-8189-5d6e227ca6cc',
    name: 'BsdPkgNg',
    desc: 'BSD pkgng Package Manager.'
    });

MATCH (bsdpkgng:BsdPkgNg {uuid: '1a879371-f680-4514-8189-5d6e227ca6cc'}),
    (packagemanager:PackageManager {uuid: 'e2b8b9ee-b66f-4252-8fba-4fb172f741bc'})
MERGE (bsdpkgng)-[:ISA]->(packagemanager);


// Apt - Debian Advanced Package Tool.
MERGE (apt:Object:Component:Software:PackageManager:Apt {
    uuid: '74528ec1-d223-40ad-8e2f-703b7f6d33b0',
    name: 'Apt',
    desc: 'Debian Advanced Package Tool.'
    });

MATCH (apt:Apt {uuid: '74528ec1-d223-40ad-8e2f-703b7f6d33b0'}),
    (packagemanager:PackageManager {uuid: 'e2b8b9ee-b66f-4252-8fba-4fb172f741bc'})
MERGE (apt)-[:ISA]->(packagemanager);


// Yum - YellowDog Updater, Modified.
MERGE (yum:Object:Component:Software:PackageManager:Yum {
    uuid: '27a03a73-d654-4999-8888-72043c84172d',
    name: 'Yum',
    desc: 'YellowDog Updater, Modified.'
    });

MATCH (yum:Yum {uuid: '27a03a73-d654-4999-8888-72043c84172d'}),
    (packagemanager:PackageManager {uuid: 'e2b8b9ee-b66f-4252-8fba-4fb172f741bc'})
MERGE (yum)-[:ISA]->(packagemanager);


// Zypp - ZYpp Package Manager.
MERGE (zypp:Object:Component:Software:PackageManager:Zypp {
    uuid: '8a016a93-73f3-4b3d-9129-b3708f5d1afe',
    name: 'Zypp',
    desc: 'ZYpp Package Manager.'
    });

MATCH (zypp:Zypp {uuid: '8a016a93-73f3-4b3d-9129-b3708f5d1afe'}),
    (packagemanager:PackageManager {uuid: 'e2b8b9ee-b66f-4252-8fba-4fb172f741bc'})
MERGE (zypp)-[:ISA]->(packagemanager);


// OptZip - The very simple unzip-in /opt package manager.
MERGE (optzip:Object:Component:Software:PackageManager:OptZip {
    uuid: '2115cfc1-a2e8-4efe-aa58-60ef64a729ca',
    name: 'OptZip',
    desc: 'The very simple unzip-in /opt package manager.'
    });

MATCH (optzip:OptZip {uuid: '2115cfc1-a2e8-4efe-aa58-60ef64a729ca'}),
    (packagemanager:PackageManager {uuid: 'e2b8b9ee-b66f-4252-8fba-4fb172f741bc'})
MERGE (optzip)-[:ISA]->(packagemanager);
