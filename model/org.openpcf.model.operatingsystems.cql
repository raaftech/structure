// org.openpcf.model.operatingsystems-0.0.1


// OperatingSystemModel 0.0.1 - Main operating systems.
MERGE (opcfosmodel:Object:Model:OperatingSystemModel {
    uuid: '90d2c69e-7199-45f4-bafa-5e0f848cf17d',
    name: 'OperatingSystemModel',
    desc: 'Main operating system types.',
    ver: '0.0.1',
    ns: 'org.openpcf.model.operatingsystems'
    });

MATCH (opcfosmodel:OperatingSystemModel {uuid: '90d2c69e-7199-45f4-bafa-5e0f848cf17d'}),
    (model:Model {uuid: '4e2dccfa-b5ca-4992-a76e-c390b7d90084'})
MERGE (opcfosmodel)-[:ISA]->(model);

MATCH (opcfosmodel:OperatingSystemModel {uuid: '90d2c69e-7199-45f4-bafa-5e0f848cf17d'}),
    (basemodel:BaseModel {uuid: '48c8698b-3afd-44e5-baf7-73f55494cb31'})
MERGE (opcfosmodel)-[:REQUIRES]->(basemodel);


// DOS-Like.
MERGE (doslike:Object:Component:Software:OperatingSystem:DosLike {
    uuid: 'e181e9e8-7f30-42a3-b87f-b8e183ddfd6f',
    name: 'DosLike',
    desc: 'A DOS-like operating system.',
    shortName: 'doslike',
    longName: 'DOS-Like'
    });

MATCH (doslike:DosLike {uuid: 'e181e9e8-7f30-42a3-b87f-b8e183ddfd6f'}),
    (operatingsystem:OperatingSystem {uuid: 'f05e325e-1aee-4892-96e4-8bcd6e97c528'})
MERGE (doslike)-[:ISA]->(operatingsystem);


// Windows-Like.
MERGE (windowslike:Object:Component:Software:OperatingSystem:WindowsLike {
    uuid: '4edbaf6a-da58-4644-abac-ae80328ddb49',
    name: 'WindowsLike',
    desc: 'A Windows-like operating system.',
    shortName: 'windowslike',
    longName: 'Windows-Like'
    });

MATCH (windowslike:WindowsLike {uuid: '4edbaf6a-da58-4644-abac-ae80328ddb49'}),
    (operatingsystem:OperatingSystem {uuid: 'f05e325e-1aee-4892-96e4-8bcd6e97c528'})
MERGE (windowslike)-[:ISA]->(operatingsystem);


// Windows
MERGE (windows:Object:Component:Software:OperatingSystem:WindowsLike:Windows {
    uuid: '060a519d-08d0-4d12-82ac-1e3f14277009',
    name: 'Windows',
    desc: 'The Microsoft Windows Operating System.',
    shortName: 'windows',
    longName: 'Windows'
    });

MATCH (windows:Windows {uuid: '060a519d-08d0-4d12-82ac-1e3f14277009'}),
    (windowslike:WindowsLike {uuid: '4edbaf6a-da58-4644-abac-ae80328ddb49'})
MERGE (windows)-[:ISA]->(windowslike);


// UNIX-Like.
MERGE (unixlike:Object:Component:Software:OperatingSystem:UnixLike {
    uuid: '6a1eea8b-1462-4439-8c60-12edbee6c397',
    name: 'UnixLike',
    desc: 'A UNIX-like operating system.',
    shortName: 'unixlike',
    longName: 'UNIX-Like'
    });

MATCH (unixlike:UnixLike {uuid: '6a1eea8b-1462-4439-8c60-12edbee6c397'}),
    (operatingsystem:OperatingSystem {uuid: 'f05e325e-1aee-4892-96e4-8bcd6e97c528'})
MERGE (unixlike)-[:ISA]->(operatingsystem);


// UNIX.
MERGE (unix:Object:Component:Software:OperatingSystem:UnixLike:Unix {
    uuid: 'a3b228b6-f737-460c-a3bc-bc653167b36c',
    name: 'Unix',
    desc: 'The UNIX Operating System.',
    shortName: 'unix',
    longName: 'UNIX'
    });

MATCH (unix:Unix {uuid: 'a3b228b6-f737-460c-a3bc-bc653167b36c'}),
    (unixlike:UnixLike {uuid: '6a1eea8b-1462-4439-8c60-12edbee6c397'})
MERGE (unix)-[:ISA]->(unixlike);


// Illumos (the only active open-source System V derivative).
MERGE (illumos:Object:Component:Software:OperatingSystem:UnixLike:Unix:Illumos {
    uuid: 'f0bcc651-3ca6-4d4c-ac9d-5a60b2b08520',
    name: 'Illumos',
    desc: 'The Illumos Operating System.',
    shortName: 'illumos',
    longName: 'Illumos'
    });

MATCH (illumos:Illumos {uuid: 'f0bcc651-3ca6-4d4c-ac9d-5a60b2b08520'}),
    (unix:Unix {uuid: 'a3b228b6-f737-460c-a3bc-bc653167b36c'})
MERGE (illumos)-[:ISA]->(unix);


// Mac OS X.
MERGE (macosx:Object:Component:Software:OperatingSystem:UnixLike:Unix:MacOsX {
    uuid: '0f81d6dc-d221-464a-ab85-1e3e5d98c3f0',
    name: 'MacOsX',
    desc: 'The Apple Mac OS X Operating System.',
    shortName: 'macosx',
    longName: 'Mac OS X'
    });

MATCH (macosx:MacOsX {uuid: '0f81d6dc-d221-464a-ab85-1e3e5d98c3f0'}),
    (unix:Unix {uuid: 'a3b228b6-f737-460c-a3bc-bc653167b36c'})
MERGE (macosx)-[:ISA]->(unix);


// BSD.
MERGE (bsd:Object:Component:Software:OperatingSystem:UnixLike:Bsd {
    uuid: '6004436f-7c88-4c2e-a29d-e8077bad10e9',
    name: 'Bsd',
    desc: 'The Berkeley Software Distribution Operating System.',
    shortName: 'bsd',
    longName: 'BSD'
    });

MATCH (bsd:Bsd {uuid: '6004436f-7c88-4c2e-a29d-e8077bad10e9'}),
    (unixlike:UnixLike {uuid: '6a1eea8b-1462-4439-8c60-12edbee6c397'})
MERGE (bsd)-[:ISA]->(unixlike);


// Linux.
MERGE (linux:Object:Component:Software:OperatingSystem:UnixLike:Linux {
    uuid: 'a5613527-2f64-4481-83f2-80b0cb427e28',
    name: 'Linux',
    desc: 'The Linux Operating System.',
    shortName: 'linux',
    longName: 'Linux'
    });

MATCH (linux:Linux {uuid: 'a5613527-2f64-4481-83f2-80b0cb427e28'}),
    (unixlike:UnixLike {uuid: '6a1eea8b-1462-4439-8c60-12edbee6c397'})
MERGE (linux)-[:ISA]->(unixlike);
