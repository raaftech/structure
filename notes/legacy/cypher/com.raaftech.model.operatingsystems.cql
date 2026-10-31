// com.raaftech.model.operatingsystems-0.0.1


// OperatingSystemModel 0.0.1 - Extended operating systems.
MERGE (raafosmodel:Object:Model:OperatingSystemModel {
    uuid: '15a87f5e-6c6a-4ba3-9033-a82dcc92899c',
    name: 'OperatingSystemModel',
    desc: 'Extended operating system types.',
    ver: '0.0.1',
    ns: 'com.raaftech.model.operatingsystems'
    });

MATCH (raafosmodel:OperatingSystemModel {uuid: '15a87f5e-6c6a-4ba3-9033-a82dcc92899c'}),
    (model:Model {uuid: '4e2dccfa-b5ca-4992-a76e-c390b7d90084'})
MERGE (raafosmodel)-[:ISA]->(model);

MATCH (raafosmodel:OperatingSystemModel {uuid: '15a87f5e-6c6a-4ba3-9033-a82dcc92899c'}),
    (opcfosmodel:OperatingSystemModel {uuid: '90d2c69e-7199-45f4-bafa-5e0f848cf17d'})
MERGE (raafosmodel)-[:REQUIRES]->(opcfosmodel);


// FreeDOS
MERGE (fdos:Object:Component:Software:OperatingSystem:DosLike:FreeDos {
    uuid: '0481d915-9d3c-4dbc-a55d-a688652934ef',
    name: 'FreeDos',
    desc: 'A free DOS-like operating system.',
    shortName: 'fdos',
    longName: 'FreeDOS'
    });

MATCH (fdos:FreeDos {uuid: '0481d915-9d3c-4dbc-a55d-a688652934ef'}),
    (doslike:DosLike {uuid: 'e181e9e8-7f30-42a3-b87f-b8e183ddfd6f'})
MERGE (fdos)-[:ISA]->(doslike);


// FreeDOS 1.x
MERGE (fdos1:Object:Component:Software:OperatingSystem:DosLike:FreeDos:FreeDos1 {
    uuid: 'fd4984a4-e593-491d-ad1a-986fc203ff0e',
    name: 'FreeDos1',
    desc: 'A free DOS-like operating system, version 1.x.',
    ver: '1.x',
    shortName: 'fdos1',
    longName: 'FreeDOS 1.x'
    });

MATCH (fdos1:FreeDos1 {uuid: 'fd4984a4-e593-491d-ad1a-986fc203ff0e'}),
    (fdos:FreeDos {uuid: '0481d915-9d3c-4dbc-a55d-a688652934ef'})
MERGE (fdos1)-[:ISA]->(fdos);


//MS-DOS
MERGE (msdos:Object:Component:Software:OperatingSystem:DosLike:MsDos {
    uuid: 'dfbe4ce9-75be-4431-9629-3ab7d4a311e5',
    name: 'MsDos',
    desc: 'The Microsoft Disk Operating System.',
    shortName: 'msdos',
    longName: 'MS-DOS'
    });

MATCH (msdos:MsDos {uuid: 'dfbe4ce9-75be-4431-9629-3ab7d4a311e5'}),
    (doslike:DosLike {uuid: 'e181e9e8-7f30-42a3-b87f-b8e183ddfd6f'})
MERGE (msdos)-[:ISA]->(doslike);


// MS-DOS 5.x
MERGE (msdos5:Object:Component:Software:OperatingSystem:DosLike:MsDos:MsDos5 {
    uuid: '50dcae6d-48b0-432f-8f26-1cbf302bf504',
    name: 'MsDos5',
    desc: 'The Microsoft Disk Operating System, version 5.x.',
    ver: '5.x',
    shortName: 'msdos5',
    longName: 'MS-DOS 5.x'
    });

MATCH (msdos5:MsDos5 {uuid: '50dcae6d-48b0-432f-8f26-1cbf302bf504'}),
    (msdos:MsDos {uuid: 'dfbe4ce9-75be-4431-9629-3ab7d4a311e5'})
MERGE (msdos5)-[:ISA]->(msdos);


// MS-DOS 6.x
MERGE (msdos6:Object:Component:Software:OperatingSystem:DosLike:MsDos:MsDos6 {
    uuid: '7d9ce51f-5ff4-40a7-b089-770aa0c29f71',
    name: 'MsDos6',
    desc: 'The Microsoft Disk Operating System, version 6.x.',
    ver: '6.x',
    shortName: 'msdos6',
    longName: 'MS-DOS 6.x'
    });

MATCH (msdos6:MsDos6 {uuid: '7d9ce51f-5ff4-40a7-b089-770aa0c29f71'}),
    (msdos:MsDos {uuid: 'dfbe4ce9-75be-4431-9629-3ab7d4a311e5'})
MERGE (msdos6)-[:ISA]->(msdos);


// Windows 9x
MERGE (win9x:Object:Component:Software:OperatingSystem:WindowsLike:Windows:Windows9x {
    uuid: 'f936f4df-d1a6-4596-808a-6e2891a0688e',
    name: 'Windows9x',
    desc: 'The Microsoft Windows 95/98/ME based operating systems.',
    shortName: 'win9x',
    longName: 'Windows 95/98/ME'
    });

MATCH (win9x:Windows9x {uuid: 'f936f4df-d1a6-4596-808a-6e2891a0688e'}),
    (windows:Windows {uuid: '060a519d-08d0-4d12-82ac-1e3f14277009'})
MERGE (win9x)-[:ISA]->(windows);


// Windows 95.
MERGE (win95:Object:Component:Software:OperatingSystem:WindowsLike:Windows:Windows9x:Windows95 {
    uuid: '37730c0d-e581-4089-8fc0-c848094223a7',
    name: 'Windows95',
    desc: 'The Microsoft Windows 95 operating system.',
    ver: '4.0',
    shortName: 'win95',
    longName: 'Windows 95'
    });

MATCH (win95:Windows95 {uuid: '37730c0d-e581-4089-8fc0-c848094223a7'}),
    (win9x:Windows9x {uuid: 'f936f4df-d1a6-4596-808a-6e2891a0688e'})
MERGE (win95)-[:ISA]->(win9x);


// Windows 98.
MERGE (win98:Object:Component:Software:OperatingSystem:WindowsLike:Windows:Windows9x:Windows98 {
    uuid: '7d7616b2-2fae-4f47-83cf-0e7add8950c5',
    name: 'Windows98',
    desc: 'The Microsoft Windows 98 operating system.',
    ver: '4.10',
    shortName: 'win98',
    longName: 'Windows 98'
    });

MATCH (win98:Windows98 {uuid: '7d7616b2-2fae-4f47-83cf-0e7add8950c5'}),
    (win9x:Windows9x {uuid: 'f936f4df-d1a6-4596-808a-6e2891a0688e'})
MERGE (win98)-[:ISA]->(win9x);


// Windows ME.
MERGE (winme:Object:Component:Software:OperatingSystem:WindowsLike:Windows:Windows9x:WindowsMe {
    uuid: '231a2bd9-1b36-42e9-97aa-9144a05d9ade',
    name: 'WindowsMe',
    desc: 'The Microsoft Windows ME operating system.',
    ver: '4.90',
    shortName: 'winme',
    longName: 'Windows ME'
    });

MATCH (winme:WindowsMe {uuid: '231a2bd9-1b36-42e9-97aa-9144a05d9ade'}),
    (win9x:Windows9x {uuid: 'f936f4df-d1a6-4596-808a-6e2891a0688e'})
MERGE (winme)-[:ISA]->(win9x);


// Windows NT.
MERGE (winnt:Object:Component:Software:OperatingSystem:WindowsLike:Windows:WindowsNt {
    uuid: '3d97107a-fb41-4a45-bd98-e93190ea4e83',
    name: 'WindowsNt',
    desc: 'The Microsoft Windows NT based operating systems.',
    shortName: 'winnt',
    longName: 'Windows NT'
    });

MATCH (winnt:WindowsNt {uuid: '3d97107a-fb41-4a45-bd98-e93190ea4e83'}),
    (windows:Windows {uuid: '060a519d-08d0-4d12-82ac-1e3f14277009'})
MERGE (winnt)-[:ISA]->(windows);


// Windows NT 4.0.
MERGE (winnt4:Object:Component:Software:OperatingSystem:WindowsLike:Windows:WindowsNt:WindowsNt4 {
    uuid: 'da560e60-4958-4dee-ab8a-d62d9cdf3593',
    name: 'WindowsNt4',
    desc: 'The Microsoft Windows NT 4.0 operating system.',
    ver: '4.0',
    shortName: 'winnt4',
    longName: 'Windows NT 4.0'
    });

MATCH (winnt4:WindowsNt4 {uuid: 'da560e60-4958-4dee-ab8a-d62d9cdf3593'}),
    (winnt:WindowsNt {uuid: '3d97107a-fb41-4a45-bd98-e93190ea4e83'})
MERGE (winnt4)-[:ISA]->(winnt);


// Windows 2000.
MERGE (win2k:Object:Component:Software:OperatingSystem:WindowsLike:Windows:WindowsNt:Windows2000 {
    uuid: '9c97e86c-24b5-44b8-bef2-4ad465bb5c82',
    name: 'Windows2000',
    desc: 'The Microsoft Windows 2000 operating system.',
    ver: '5.0',
    shortName: 'win2k',
    longName: 'Windows 2000'
    });

MATCH (win2k:Windows2000 {uuid: '9c97e86c-24b5-44b8-bef2-4ad465bb5c82'}),
    (winnt:WindowsNt {uuid: '3d97107a-fb41-4a45-bd98-e93190ea4e83'})
MERGE (win2k)-[:ISA]->(winnt);


// Windows XP.
MERGE (winxp:Object:Component:Software:OperatingSystem:WindowsLike:Windows:WindowsNt:WindowsXp {
    uuid: '31a0496d-9cb4-4aab-b3ed-86ddb566c0f4',
    name: 'WindowsXp',
    desc: 'The Microsoft Windows XP operating system.',
    ver: '5.1',
    shortName: 'winxp',
    longName: 'Windows XP'
    });

MATCH (winxp:WindowsXp {uuid: '31a0496d-9cb4-4aab-b3ed-86ddb566c0f4'}),
    (winnt:WindowsNt {uuid: '3d97107a-fb41-4a45-bd98-e93190ea4e83'})
MERGE (winxp)-[:ISA]->(winnt);


// Windows Server 2003.
MERGE (win2k3:Object:Component:Software:OperatingSystem:WindowsLike:Windows:WindowsNt:Windows2003 {
    uuid: '8eb5e304-7d31-4645-89e2-8c39fefc7278',
    name: 'Windows2003',
    desc: 'The Microsoft Windows Server 2003 operating system.',
    ver: '5.2',
    shortName: 'win2k3',
    longName: 'Windows Server 2003'
    });

MATCH (win2k3:Windows2003 {uuid: '8eb5e304-7d31-4645-89e2-8c39fefc7278'}),
    (winnt:WindowsNt {uuid: '3d97107a-fb41-4a45-bd98-e93190ea4e83'})
MERGE (win2k3)-[:ISA]->(winnt);


// Windows Vista.
MERGE (winv:Object:Component:Software:OperatingSystem:WindowsLike:Windows:WindowsNt:WindowsVista {
    uuid: 'ec9e7f41-cda5-48df-9c22-8f320fff398b',
    name: 'WindowsVista',
    desc: 'The Microsoft Windows Vista operating system.',
    ver: '6.0',
    shortName: 'winv',
    longName: 'Windows Vista'
    });

MATCH (winv:WindowsVista {uuid: 'ec9e7f41-cda5-48df-9c22-8f320fff398b'}),
    (winnt:WindowsNt {uuid: '3d97107a-fb41-4a45-bd98-e93190ea4e83'})
MERGE (winv)-[:ISA]->(winnt);


// Windows 7.
MERGE (win7:Object:Component:Software:OperatingSystem:WindowsLike:Windows:WindowsNt:Windows7 {
    uuid: 'a3b0f390-7257-43cb-9b9d-1cef8a4092bf',
    name: 'Windows7',
    desc: 'The Microsoft Windows 7 operating system.',
    ver: '6.1',
    shortName: 'win7',
    longName: 'Windows 7'
    });

MATCH (win7:Windows7 {uuid: 'a3b0f390-7257-43cb-9b9d-1cef8a4092bf'}),
    (winnt:WindowsNt {uuid: '3d97107a-fb41-4a45-bd98-e93190ea4e83'})
MERGE (win7)-[:ISA]->(winnt);


// Windows Server 2008.
MERGE (win2k8:Object:Component:Software:OperatingSystem:WindowsLike:Windows:WindowsNt:Windows2008 {
    uuid: '0cb2b9f2-3dfe-4183-8c0d-519a2b16e68a',
    name: 'Windows2008',
    desc: 'The Microsoft Windows Server 2008 operating system.',
    ver: '6.0',
    shortName: 'win2k8',
    longName: 'Windows Server 2008'
    });

MATCH (win2k8:Windows2008 {uuid: '0cb2b9f2-3dfe-4183-8c0d-519a2b16e68a'}),
    (winnt:WindowsNt {uuid: '3d97107a-fb41-4a45-bd98-e93190ea4e83'})
MERGE (win2k8)-[:ISA]->(winnt);


// Windows 8.
MERGE (win8:Object:Component:Software:OperatingSystem:WindowsLike:Windows:WindowsNt:Windows8 {
    uuid: '16934de7-609f-4068-a18e-7eb7452b5f01',
    name: 'Windows8',
    desc: 'The Microsoft Windows 8 operating system.',
    ver: '6.2',
    shortName: 'win8',
    longName: 'Windows 8'
    });

MATCH (win8:Windows8 {uuid: '16934de7-609f-4068-a18e-7eb7452b5f01'}),
    (winnt:WindowsNt {uuid: '3d97107a-fb41-4a45-bd98-e93190ea4e83'})
MERGE (win8)-[:ISA]->(winnt);


// Windows Server 2012.
MERGE (win2k12:Object:Component:Software:OperatingSystem:WindowsLike:Windows:WindowsNt:Windows2012 {
    uuid: '46758eba-4463-4779-91d9-9095f9d87097',
    name: 'Windows2012',
    desc: 'The Microsoft Windows Server 2012 operating system.',
    ver: '6.3',
    shortName: 'win2k12',
    longName: 'Windows Server 2012'
    });

MATCH (win2k12:Windows2012 {uuid: '46758eba-4463-4779-91d9-9095f9d87097'}),
    (winnt:WindowsNt {uuid: '3d97107a-fb41-4a45-bd98-e93190ea4e83'})
MERGE (win2k12)-[:ISA]->(winnt);


// ReactOS.
MERGE (reactos:Object:Component:Software:OperatingSystem:WindowsLike:ReactOs {
    uuid: '3a80082c-173f-44fc-b42a-1fb381d2e3bc',
    name: 'ReactOs',
    desc: 'The ReactOS Operating System.',
    shortName: 'reactos',
    longName: 'ReactOS'
    });

MATCH (reactos:ReactOs {uuid: '3a80082c-173f-44fc-b42a-1fb381d2e3bc'}),
    (windowslike:WindowsLike {uuid: '4edbaf6a-da58-4644-abac-ae80328ddb49'})
MERGE (reactos)-[:ISA]->(windowslike);


// ReactOS 0.3.x.
MERGE (reactos03:Object:Component:Software:OperatingSystem:WindowsLike:ReactOs:ReactOs03 {
    uuid: '0a6a1bd2-1a6b-461b-aa28-cca977b8faf1',
    name: 'ReactOs03',
    desc: 'The ReactOS 0.3.x Operating System.',
    ver: '0.3.x',
    shortName: 'reactos03',
    longName: 'ReactOS 0.3.x'
    });

MATCH (reactos03:ReactOs03 {uuid: '0a6a1bd2-1a6b-461b-aa28-cca977b8faf1'}),
    (reactos:ReactOs {uuid: '3a80082c-173f-44fc-b42a-1fb381d2e3bc'})
MERGE (reactos03)-[:ISA]->(reactos);


// ReactOS 0.4.
MERGE (reactos04:Object:Component:Software:OperatingSystem:WindowsLike:ReactOs:ReactOs04 {
    uuid: '94708506-de30-4827-b9ef-84f6694c4e3f',
    name: 'ReactOs04',
    desc: 'The ReactOS 0.4.x Operating System.',
    ver: '0.4.x',
    shortName: 'reactos04',
    longName: 'ReactOS 0.4.x'
    });

MATCH (reactos04:ReactOs04 {uuid: '94708506-de30-4827-b9ef-84f6694c4e3f'}),
    (reactos:ReactOs {uuid: '3a80082c-173f-44fc-b42a-1fb381d2e3bc'})
MERGE (reactos04)-[:ISA]->(reactos);


// AIX.
MERGE (aix:Object:Component:Software:OperatingSystem:UnixLike:Unix:Aix {
    uuid: '9cf01e40-6be5-48bc-88c5-e9c3c8caaa4b',
    name: 'Aix',
    desc: 'The IBM AIX Operating System.',
    shortName: 'aix',
    longName: 'IBM AIX'
    });

MATCH (aix:Aix {uuid: '9cf01e40-6be5-48bc-88c5-e9c3c8caaa4b'}),
    (unix:Unix {uuid: 'a3b228b6-f737-460c-a3bc-bc653167b36c'})
MERGE (aix)-[:ISA]->(unix);


// AIX 5.
MERGE (aix5:Object:Component:Software:OperatingSystem:UnixLike:Unix:Aix:Aix5 {
    uuid: '154fd2d1-db50-4318-bfdf-49a62f11bdb6',
    name: 'Aix5',
    desc: 'The IBM AIX 5.x Operating System.',
    ver: '5.x',
    shortName: 'aix5',
    longName: 'IBM AIX 5.x'
    });

MATCH (aix5:Aix5 {uuid: '154fd2d1-db50-4318-bfdf-49a62f11bdb6'}),
    (aix:Aix {uuid: '9cf01e40-6be5-48bc-88c5-e9c3c8caaa4b'})
MERGE (aix5)-[:ISA]->(aix);


// AIX 6.
MERGE (aix6:Object:Component:Software:OperatingSystem:UnixLike:Unix:Aix:Aix6 {
    uuid: '843ff854-ee2a-4279-8dca-7caa24fc0727',
    name: 'Aix6',
    desc: 'The IBM AIX 6.x Operating System.',
    ver: '6.1',
    shortName: 'aix6',
    longName: 'IBM AIX 6.x'
    });

MATCH (aix6:Aix6 {uuid: '843ff854-ee2a-4279-8dca-7caa24fc0727'}),
    (aix:Aix {uuid: '9cf01e40-6be5-48bc-88c5-e9c3c8caaa4b'})
MERGE (aix6)-[:ISA]->(aix);


// AIX 7.
MERGE (aix7:Object:Component:Software:OperatingSystem:UnixLike:Unix:Aix:Aix7 {
    uuid: 'a3f1be8b-f222-4c9d-ad77-d2a5365f418d',
    name: 'Aix7',
    desc: 'The IBM AIX 7.x Operating System.',
    ver: '7.1',
    shortName: 'aix7',
    longName: 'IBM AIX 7.x'
    });

MATCH (aix7:Aix7 {uuid: 'a3f1be8b-f222-4c9d-ad77-d2a5365f418d'}),
    (aix:Aix {uuid: '9cf01e40-6be5-48bc-88c5-e9c3c8caaa4b'})
MERGE (aix7)-[:ISA]->(aix);


// HP/UX.
MERGE (hpux:Object:Component:Software:OperatingSystem:UnixLike:Unix:HpUx {
    uuid: '465b42e6-a7c5-4cf8-96c5-54257f054274',
    name: 'HpUx',
    desc: 'The HP/UX Operating System.',
    shortName: 'hpux',
    longName: 'HP/UX'
    });

MATCH (hpux:HpUx {uuid: '465b42e6-a7c5-4cf8-96c5-54257f054274'}),
    (unix:Unix {uuid: 'a3b228b6-f737-460c-a3bc-bc653167b36c'})
MERGE (hpux)-[:ISA]->(unix);


// HP/UX 10.
MERGE (hpux10:Object:Component:Software:OperatingSystem:UnixLike:Unix:HpUx:HpUx10 {
    uuid: '416e6138-b048-4ee5-9554-6481ea996edf',
    name: 'HpUx10',
    desc: 'The HP/UX 10.x Operating System.',
    ver: '10.0',
    shortName: 'hpux10',
    longName: 'HP/UX 10.x'
    });

MATCH (hpux10:HpUx10 {uuid: '416e6138-b048-4ee5-9554-6481ea996edf'}),
    (hpux:HpUx {uuid: '465b42e6-a7c5-4cf8-96c5-54257f054274'})
MERGE (hpux10)-[:ISA]->(hpux);


// HP/UX 10.20.
MERGE (hpux1020:Object:Component:Software:OperatingSystem:UnixLike:Unix:HpUx:HPUx10:HpUx1020 {
    uuid: 'ac59c16a-89d2-499e-8341-5161ffb4f176',
    name: 'HpUx1020',
    desc: 'The HP/UX 10.20 Operating System.',
    ver: '10.20',
    shortName: 'hpux1020',
    longName: 'HP/UX 10.20'
    });

MATCH (hpux1020:HpUx1020 {uuid: 'ac59c16a-89d2-499e-8341-5161ffb4f176'}),
    (hpux10:HpUx10 {uuid: '416e6138-b048-4ee5-9554-6481ea996edf'})
MERGE (hpux1020)-[:ISA]->(hpux10);


// HP/UX 11.
MERGE (hpux11:Object:Component:Software:OperatingSystem:UnixLike:Unix:HpUx:HpUx11 {
    uuid: '1d34dabf-b096-4e43-80c4-7318c81bc1fe',
    name: 'HpUx11',
    desc: 'The HP/UX 11.x Operating System.',
    ver: '11.0',
    shortName: 'hpux11',
    longName: 'HP/UX 11.x'
    });

MATCH (hpux11:HpUx11 {uuid: '1d34dabf-b096-4e43-80c4-7318c81bc1fe'}),
    (hpux:HpUx {uuid: '465b42e6-a7c5-4cf8-96c5-54257f054274'})
MERGE (hpux11)-[:ISA]->(hpux);

// HP/UX 11.23.
MERGE (hpux1123:Object:Component:Software:OperatingSystem:UnixLike:Unix:HpUx:HpUx11:HpUx1123 {
    uuid: 'd5312463-4313-4aea-9ac0-552201a7302c',
    name: 'HpUx1123',
    desc: 'The HP/UX 11.23 Operating System.',
    ver: '11.23',
    shortName: 'hpux1123',
    longName: 'HP/UX 11.23'
    });

MATCH (hpux1123:HpUx1123 {uuid: 'd5312463-4313-4aea-9ac0-552201a7302c'}),
    (hpux11:HpUx11 {uuid: '1d34dabf-b096-4e43-80c4-7318c81bc1fe'})
MERGE (hpux1123)-[:ISA]->(hpux11);


// HP/UX 11.31.
MERGE (hpux1131:Object:Component:Software:OperatingSystem:UnixLike:Unix:HpUx:HpUx11:HpUx1131 {
    uuid: '591557b5-f13a-4070-a043-782830560664',
    name: 'HpUx1131',
    desc: 'The HP/UX 11.31 Operating System.',
    ver: '11.31',
    shortName: 'hpux1131',
    longName: 'HP/UX 11.31'
    });

MATCH (hpux1131:HpUx1131 {uuid: '591557b5-f13a-4070-a043-782830560664'}),
    (hpux11:HpUx11 {uuid: '1d34dabf-b096-4e43-80c4-7318c81bc1fe'})
MERGE (hpux1131)-[:ISA]->(hpux11);


// Solaris.
MERGE (solaris:Object:Component:Software:OperatingSystem:UnixLike:Unix:Solaris {
    uuid: '4190b02e-a832-4316-87c6-d9e9b694e284',
    name: 'Solaris',
    desc: 'The Solaris Operating System.',
    shortName: 'solaris',
    longName: 'Solaris'
    });

MATCH (solaris:Solaris {uuid: '4190b02e-a832-4316-87c6-d9e9b694e284'}),
    (unix:Unix {uuid: 'a3b228b6-f737-460c-a3bc-bc653167b36c'})
MERGE (solaris)-[:ISA]->(unix);


// Solaris 8.
MERGE (solaris8:Object:Component:Software:OperatingSystem:UnixLike:Unix:Solaris:Solaris8 {
    uuid: '8e4b0333-3d59-455f-8de9-1bdd8af5ba96',
    name: 'Solaris8',
    desc: 'The Solaris 8 Operating System.',
    ver: '8',
    shortName: 'solaris8',
    longName: 'Solaris 8'
    });

MATCH (solaris8:Solaris8 {uuid: '8e4b0333-3d59-455f-8de9-1bdd8af5ba96'}),
    (solaris:Solaris {uuid: '4190b02e-a832-4316-87c6-d9e9b694e284'})
MERGE (solaris8)-[:ISA]->(solaris);

// Solaris 9.
MERGE (solaris9:Object:Component:Software:OperatingSystem:UnixLike:Unix:Solaris:Solaris9 {
    uuid: 'd5156f08-91e5-4608-8c97-cd76434b7636',
    name: 'Solaris9',
    desc: 'The Solaris 9 Operating System.',
    ver: '9',
    shortName: 'solaris9',
    longName: 'Solaris 9'
    });

MATCH (solaris9:Solaris9 {uuid: 'd5156f08-91e5-4608-8c97-cd76434b7636'}),
    (solaris:Solaris {uuid: '4190b02e-a832-4316-87c6-d9e9b694e284'})
MERGE (solaris9)-[:ISA]->(solaris);


// Solaris 10.
MERGE (solaris10:Object:Component:Software:OperatingSystem:UnixLike:Unix:Solaris:Solaris10 {
    uuid: 'e10cd458-1348-488b-8cc4-01af672828ae',
    name: 'Solaris10',
    desc: 'The Solaris 10 Operating System.',
    ver: '10',
    shortName: 'solaris10',
    longName: 'Solaris 10'
    });

MATCH (solaris10:Solaris10 {uuid: 'e10cd458-1348-488b-8cc4-01af672828ae'}),
    (solaris:Solaris {uuid: '4190b02e-a832-4316-87c6-d9e9b694e284'})
MERGE (solaris10)-[:ISA]->(solaris);


// Solaris 11.
MERGE (solaris11:Object:Component:Software:OperatingSystem:UnixLike:Unix:Solaris:Solaris11 {
    uuid: '3a81f815-a426-4269-bdd9-a7ea7527325c',
    name: 'Solaris11',
    desc: 'The Solaris 11 Operating System.',
    ver: '11.x',
    shortName: 'solaris11',
    longName: 'Solaris 11.x'
    });

MATCH (solaris11:Solaris11 {uuid: '3a81f815-a426-4269-bdd9-a7ea7527325c'}),
    (solaris:Solaris {uuid: '4190b02e-a832-4316-87c6-d9e9b694e284'})
MERGE (solaris11)-[:ISA]->(solaris);


// OpenIndiana
MERGE (openindiana:Object:Component:Software:OperatingSystem:UnixLike:Unix:Illumos:OpenIndiana {
    uuid: 'f4c41e3a-c59a-42f6-b6c8-b7c9b0df3f0e',
    name: 'OpenIndiana',
    desc: 'The OpenIndiana Operating System.',
    shortName: 'openindiana',
    longName: 'OpenIndiana'
    });

MATCH (openindiana:OpenIndiana {uuid: 'f4c41e3a-c59a-42f6-b6c8-b7c9b0df3f0e'}),
    (illumos:Illumos {uuid: 'f0bcc651-3ca6-4d4c-ac9d-5a60b2b08520'})
MERGE (openindiana)-[:ISA]->(illumos);


// OpenIndiana 151a8
MERGE (openindiana151a8:Object:Component:Software:OperatingSystem:UnixLike:Unix:Illumos:OpenIndiana:OpenIndiana151a8 {
    uuid: 'c98dc9c8-bcf3-4a88-a24f-ea4b6a6a0951',
    name: 'OpenIndiana151a8',
    desc: 'The OpenIndiana 151a8 Operating System.',
    ver: '151a8',
    shortName: 'openindiana151a8',
    longName: 'OpenIndiana 151a8'
    });

MATCH (openindiana151a8:OpenIndiana151a8 {uuid: 'c98dc9c8-bcf3-4a88-a24f-ea4b6a6a0951'}),
    (openindiana:OpenIndiana {uuid: 'f4c41e3a-c59a-42f6-b6c8-b7c9b0df3f0e'})
MERGE (openindiana151a8)-[:ISA]->(openindiana);


// NexentaStor
MERGE (nexentastor:Object:Component:Software:OperatingSystem:UnixLike:Unix:Illumos:NexentaStor {
    uuid: '0d73cb36-0f99-417b-a150-cc193ba044b8',
    name: 'NexentaStor',
    desc: 'The NexentaStor Operating System.',
    shortName: 'nexentastor',
    longName: 'NexentaStor'
    });

MATCH (nexentastor:NexentaStor {uuid: '0d73cb36-0f99-417b-a150-cc193ba044b8'}),
    (illumos:Illumos {uuid: 'f0bcc651-3ca6-4d4c-ac9d-5a60b2b08520'})
MERGE (nexentastor)-[:ISA]->(illumos);


// NexentaStor 3.
MERGE (nexentastor3:Object:Component:Software:OperatingSystem:UnixLike:Unix:Illumos:NexentaStor:NexentaStor3 {
    uuid: '6d90bb5c-bcb7-4c21-b7b9-8ba4aeb753b8',
    name: 'NexentaStor3',
    desc: 'The NexentaStor 3 Operating System.',
    ver: '3.x',
    shortName: 'nexentastor3',
    longName: 'NexentaStor 3.x'
    });

MATCH (nexentastor3:NexentaStor3 {uuid: '6d90bb5c-bcb7-4c21-b7b9-8ba4aeb753b8'}),
    (nexentastor:NexentaStor {uuid: '0d73cb36-0f99-417b-a150-cc193ba044b8'})
MERGE (nexentastor3)-[:ISA]->(nexentastor);


// NexentaStor 4.
MERGE (nexentastor4:Object:Component:Software:OperatingSystem:UnixLike:Unix:Illumos:NexentaStor:NexentaStor4 {
    uuid: 'ef584c3a-b8ec-41da-9435-a9d1f8474dab',
    name: 'NexentaStor4',
    desc: 'The NexentaStor 4 Operating System.',
    ver: '4.x',
    shortName: 'nexentastor4',
    longName: 'NexentaStor 4.x'
    });

MATCH (nexentastor4:NexentaStor4 {uuid: 'ef584c3a-b8ec-41da-9435-a9d1f8474dab'}),
    (nexentastor:NexentaStor {uuid: '0d73cb36-0f99-417b-a150-cc193ba044b8'})
MERGE (nexentastor4)-[:ISA]->(nexentastor);


// SmartOS
MERGE (smartos:Object:Component:Software:OperatingSystem:UnixLike:Unix:Illumos:SmartOs {
    uuid: 'a7fba50f-5ab5-46d2-acf7-08fbe7c41b44',
    name: 'SmartOs',
    desc: 'The SmartOS Operating System.',
    shortName: 'smartos',
    longName: 'SmartOS'
    });

MATCH (smartos:SmartOs {uuid: 'a7fba50f-5ab5-46d2-acf7-08fbe7c41b44'}),
    (illumos:Illumos {uuid: 'f0bcc651-3ca6-4d4c-ac9d-5a60b2b08520'})
MERGE (smartos)-[:ISA]->(illumos);


// Dyson
MERGE (dyson:Object:Component:Software:OperatingSystem:UnixLike:Unix:Illumos:Dyson {
    uuid: '70e37ec8-e807-4a82-b968-f596dac97d62',
    name: 'Dyson',
    desc: 'The Dyson Operating System.',
    shortName: 'dyson',
    longName: 'Dyson'
    });

MATCH (dyson:Dyson {uuid: '70e37ec8-e807-4a82-b968-f596dac97d62'}),
    (illumos:Illumos {uuid: 'f0bcc651-3ca6-4d4c-ac9d-5a60b2b08520'})
MERGE (dyson)-[:ISA]->(illumos);


// OmniOS
MERGE (omnios:Object:Component:Software:OperatingSystem:UnixLike:Unix:Illumos:OmniOs {
    uuid: 'e3e34862-6d23-4922-81cd-fb0df4624cb5',
    name: 'OmniOs',
    desc: 'The OmniOS Operating System.',
    shortName: 'omnios',
    longName: 'OmniOS'
    });

MATCH (omnios:OmniOs {uuid: 'e3e34862-6d23-4922-81cd-fb0df4624cb5'}),
    (illumos:Illumos {uuid: 'f0bcc651-3ca6-4d4c-ac9d-5a60b2b08520'})
MERGE (omnios)-[:ISA]->(illumos);


// OmniOS r151006
MERGE (omniosr151006:Object:Component:Software:OperatingSystem:UnixLike:Unix:Illumos:OmniOs:OmniOsR151006 {
    uuid: '8e008afd-d1eb-48bd-8f71-684270750927',
    name: 'OmniOsR151006',
    desc: 'The OmniOS r151006 Operating System.',
    ver: 'r151006',
    shortName: 'omniosr151006',
    longName: 'OmniOS r151006'
    });

MATCH (omniosr151006:OmniOsR151006 {uuid: '8e008afd-d1eb-48bd-8f71-684270750927'}),
    (omnios:OmniOs {uuid: 'e3e34862-6d23-4922-81cd-fb0df4624cb5'})
MERGE (omniosr151006)-[:ISA]->(omnios);


// OmniOS r151010
MERGE (omniosr151010:Object:Component:Software:OperatingSystem:UnixLike:Unix:Illumos:OmniOs:OmniOsR151010 {
    uuid: 'e164b0ef-707f-48da-a5b7-b185746c4fd0',
    name: 'OmniOsR151010',
    desc: 'The OmniOS r151010 Operating System.',
    ver: 'r151010',
    shortName: 'omniosr151010',
    longName: 'OmniOS r151010'
    });

MATCH (omniosr151010:OmniOsR151010 {uuid: 'e164b0ef-707f-48da-a5b7-b185746c4fd0'}),
    (omnios:OmniOs {uuid: 'e3e34862-6d23-4922-81cd-fb0df4624cb5'})
MERGE (omniosr151010)-[:ISA]->(omnios);


// OpenSXCE
MERGE (opensxce:Object:Component:Software:OperatingSystem:UnixLike:Unix:Illumos:OpenSxce {
    uuid: '2bd660a2-1783-4433-81a0-23d82dea1cce',
    name: 'OpenSxce',
    desc: 'The OpenSXCE Operating System.',
    shortName: 'opensxce',
    longName: 'OpenSXCE'
    });

MATCH (opensxce:OpenSxce {uuid: '2bd660a2-1783-4433-81a0-23d82dea1cce'}),
    (illumos:Illumos {uuid: 'f0bcc651-3ca6-4d4c-ac9d-5a60b2b08520'})
MERGE (opensxce)-[:ISA]->(illumos);


// OpenSXCE 2013
MERGE (opensxce2013:Object:Component:Software:OperatingSystem:UnixLike:Unix:Illumos:OpenSxce:OpenSxce2013 {
    uuid: '1724894c-bb76-494f-b999-b467b0f4ccb6',
    name: 'OpenSxce2013',
    desc: 'The OpenSXCE 2013 Operating System.',
    ver: '2013',
    shortName: 'opensxce2013',
    longName: 'OpenSXCE 2013'
    });

MATCH (opensxce2013:OpenSxce2013 {uuid: '1724894c-bb76-494f-b999-b467b0f4ccb6'}),
    (opensxce:OpenSxce {uuid: '2bd660a2-1783-4433-81a0-23d82dea1cce'})
MERGE (opensxce2013)-[:ISA]->(opensxce);


// OpenSXCE 2014
MERGE (opensxce2014:Object:Component:Software:OperatingSystem:UnixLike:Unix:Illumos:OpenSxce:OpenSxce2014 {
    uuid: 'c94f703d-7a1b-4ce4-aac5-c786e59117e0',
    name: 'OpenSxce2014',
    desc: 'The OpenSXCE 2014 Operating System.',
    ver: '2014',
    shortName: 'opensxce2014',
    longName: 'OpenSXCE 2014'
    });

MATCH (opensxce2014:OpenSxce2014 {uuid: 'c94f703d-7a1b-4ce4-aac5-c786e59117e0'}),
    (opensxce:OpenSxce {uuid: '2bd660a2-1783-4433-81a0-23d82dea1cce'})
MERGE (opensxce2014)-[:ISA]->(opensxce);


// Mac OS X 10.3.
MERGE (macosx3:Object:Component:Software:OperatingSystem:UnixLike:Unix:MacOsX:MacOsX3 {
    uuid: 'edcd8303-03f6-459c-a5c3-2ea00c20f29c',
    name: 'MacOsX3',
    desc: 'The Apple Mac OS X 10.3 Operating System.',
    ver: '10.3',
    shortName: 'macosx3',
    longName: 'Mac OS X 10.3'
    });

MATCH (macosx3:MacOsX3 {uuid: 'edcd8303-03f6-459c-a5c3-2ea00c20f29c'}),
    (macosx:MacOsX {uuid: '0f81d6dc-d221-464a-ab85-1e3e5d98c3f0'})
MERGE (macosx3)-[:ISA]->(macosx);


// Mac OS X 10.4.
MERGE (macosx4:Object:Component:Software:OperatingSystem:UnixLike:Unix:MacOsX:MacOsX4 {
    uuid: '3d986bda-2109-4e91-a0ec-ad2be3bae792',
    name: 'MacOsX4',
    desc: 'The Apple Mac OS X 10.4 Operating System.',
    ver: '10.4',
    shortName: 'macosx4',
    longName: 'Mac OS X 10.4'
    });

MATCH (macosx4:MacOsX4 {uuid: '3d986bda-2109-4e91-a0ec-ad2be3bae792'}),
    (macosx:MacOsX {uuid: '0f81d6dc-d221-464a-ab85-1e3e5d98c3f0'})
MERGE (macosx4)-[:ISA]->(macosx);


// Mac OS X 10.5.
MERGE (macosx5:Object:Component:Software:OperatingSystem:UnixLike:Unix:MacOsX:MacOsX5 {
    uuid: '1cbf8f53-496b-4421-a805-e5bc86c2a56c',
    name: 'MacOsX5',
    desc: 'The Apple Mac OS X 10.5 Operating System.',
    ver: '10.5',
    shortName: 'macosx5',
    longName: 'Mac OS X 10.5'
    });

MATCH (macosx5:MacOsX5 {uuid: '1cbf8f53-496b-4421-a805-e5bc86c2a56c'}),
    (macosx:MacOsX {uuid: '0f81d6dc-d221-464a-ab85-1e3e5d98c3f0'})
MERGE (macosx5)-[:ISA]->(macosx);


// Mac OS X 10.6.
MERGE (macosx6:Object:Component:Software:OperatingSystem:UnixLike:Unix:MacOsX:MacOsX6 {
    uuid: 'eae5479b-1c29-41c9-9b8c-28f6663f85b6',
    name: 'MacOsX6',
    desc: 'The Apple Mac OS X 10.6 Operating System.',
    ver: '10.6',
    shortName: 'macosx6',
    longName: 'Mac OS X 10.6'
    });

MATCH (macosx6:MacOsX6 {uuid: 'eae5479b-1c29-41c9-9b8c-28f6663f85b6'}),
    (macosx:MacOsX {uuid: '0f81d6dc-d221-464a-ab85-1e3e5d98c3f0'})
MERGE (macosx6)-[:ISA]->(macosx);


// Mac OS X 10.7.
MERGE (macosx7:Object:Component:Software:OperatingSystem:UnixLike:Unix:MacOsX:MacOsX7 {
    uuid: '9c9d4b4c-4896-4a67-873f-2638a3ce4616',
    name: 'MacOsX7',
    desc: 'The Apple Mac OS X 10.7 Operating System.',
    ver: '10.7',
    shortName: 'macosx7',
    longName: 'Mac OS X 10.7'
    });

MATCH (macosx7:MacOsX7 {uuid: '9c9d4b4c-4896-4a67-873f-2638a3ce4616'}),
    (macosx:MacOsX {uuid: '0f81d6dc-d221-464a-ab85-1e3e5d98c3f0'})
MERGE (macosx7)-[:ISA]->(macosx);


// Mac OS X 10.8.
MERGE (macosx8:Object:Component:Software:OperatingSystem:UnixLike:Unix:MacOsX:MacOsX8 {
    uuid: '6d7fc436-1e34-4538-ad6c-e3d8d47aa4b0',
    name: 'MacOsX8',
    desc: 'The Apple Mac OS X 10.8 Operating System.',
    ver: '10.8',
    shortName: 'macosx8',
    longName: 'Mac OS X 10.8'
    });

MATCH (macosx8:MacOsX8 {uuid: '6d7fc436-1e34-4538-ad6c-e3d8d47aa4b0'}),
    (macosx:MacOsX {uuid: '0f81d6dc-d221-464a-ab85-1e3e5d98c3f0'})
MERGE (macosx8)-[:ISA]->(macosx);


// Mac OS X 10.9.
MERGE (macosx9:Object:Component:Software:OperatingSystem:UnixLike:Unix:MacOsX:MacOsX9 {
    uuid: '8453b53d-4c0c-4892-b7c0-390314978a2b',
    name: 'MacOsX9',
    desc: 'The Apple Mac OS X 10.9 Operating System.',
    ver: '10.9',
    shortName: 'macosx9',
    longName: 'Mac OS X 10.9'
    });

MATCH (macosx9:MacOsX9 {uuid: '8453b53d-4c0c-4892-b7c0-390314978a2b'}),
    (macosx:MacOsX {uuid: '0f81d6dc-d221-464a-ab85-1e3e5d98c3f0'})
MERGE (macosx9)-[:ISA]->(macosx);


// Mac OS X 10.10.
MERGE (macosx10:Object:Component:Software:OperatingSystem:UnixLike:Unix:MacOsX:MacOsX10 {
    uuid: '66ef29ff-374d-492f-99fe-e6573fcbacdf',
    name: 'MacOsX10',
    desc: 'The Apple Mac OS X 10.10 Operating System.',
    ver: '10.10',
    shortName: 'macosx10',
    longName: 'Mac OS X 10.10'
    });

MATCH (macosx10:MacOsX10 {uuid: '66ef29ff-374d-492f-99fe-e6573fcbacdf'}),
    (macosx:MacOsX {uuid: '0f81d6dc-d221-464a-ab85-1e3e5d98c3f0'})
MERGE (macosx10)-[:ISA]->(macosx);


// FreeBSD.
MERGE (freebsd:Object:Component:Software:OperatingSystem:UnixLike:Bsd:FreeBsd {
    uuid: '6bccb300-554a-43ce-981b-fd07b22c83f0',
    name: 'FreeBsd',
    desc: 'The FreeBSD Operating System.',
    shortName: 'freebsd',
    longName: 'FreeBSD'
    });

MATCH (freebsd:FreeBsd {uuid: '6bccb300-554a-43ce-981b-fd07b22c83f0'}),
    (bsd:Bsd {uuid: '6004436f-7c88-4c2e-a29d-e8077bad10e9'})
MERGE (freebsd)-[:ISA]->(bsd);


// FreeBSD 4.
MERGE (freebsd4:Object:Component:Software:OperatingSystem:UnixLike:Bsd:FreeBsd:FreeBsd4 {
    uuid: 'd636ce7f-0da9-434c-b200-e7e409465076',
    name: 'FreeBsd4',
    desc: 'The FreeBSD 4.x Operating System.',
    ver: '4.x',
    shortName: 'freebsd4',
    longName: 'FreeBSD 4.x'
    });

MATCH (freebsd4:FreeBsd4 {uuid: 'd636ce7f-0da9-434c-b200-e7e409465076'}),
    (freebsd:FreeBsd {uuid: '6bccb300-554a-43ce-981b-fd07b22c83f0'})
MERGE (freebsd4)-[:ISA]->(freebsd);


// FreeBSD 5.
MERGE (freebsd5:Object:Component:Software:OperatingSystem:UnixLike:Bsd:FreeBsd:FreeBsd5 {
    uuid: '931e52b3-97b8-43bb-bf4c-3424ba4cce0f',
    name: 'FreeBsd5',
    desc: 'The FreeBSD 5.x Operating System.',
    ver: '5.x',
    shortName: 'freebsd5',
    longName: 'FreeBSD 5.x'
    });

MATCH (freebsd5:FreeBsd5 {uuid: '931e52b3-97b8-43bb-bf4c-3424ba4cce0f'}),
    (freebsd:FreeBsd {uuid: '6bccb300-554a-43ce-981b-fd07b22c83f0'})
MERGE (freebsd5)-[:ISA]->(freebsd);


// FreeBSD 6.
MERGE (freebsd6:Object:Component:Software:OperatingSystem:UnixLike:Bsd:FreeBsd:FreeBsd6 {
    uuid: 'ac25708a-4b29-41c7-9739-c41fbdc38782',
    name: 'FreeBsd6',
    desc: 'The FreeBSD 6.x Operating System.',
    ver: '6.x',
    shortName: 'freebsd6',
    longName: 'FreeBSD 6.x'
    });

MATCH (freebsd6:FreeBsd6 {uuid: 'ac25708a-4b29-41c7-9739-c41fbdc38782'}),
    (freebsd:FreeBsd {uuid: '6bccb300-554a-43ce-981b-fd07b22c83f0'})
MERGE (freebsd6)-[:ISA]->(freebsd);


// FreeBSD 7.
MERGE (freebsd7:Object:Component:Software:OperatingSystem:UnixLike:Bsd:FreeBsd:FreeBsd7 {
    uuid: 'a05c4070-c1f7-400f-8ff4-00a2ebf613fa',
    name: 'FreeBsd7',
    desc: 'The FreeBSD 7.x Operating System.',
    ver: '7.x',
    shortName: 'freebsd7',
    longName: 'FreeBSD 7.x'
    });

MATCH (freebsd7:FreeBsd7 {uuid: 'a05c4070-c1f7-400f-8ff4-00a2ebf613fa'}),
    (freebsd:FreeBsd {uuid: '6bccb300-554a-43ce-981b-fd07b22c83f0'})
MERGE (freebsd7)-[:ISA]->(freebsd);


// FreeBSD 8.
MERGE (freebsd8:Object:Component:Software:OperatingSystem:UnixLike:Bsd:FreeBsd:FreeBsd8 {
    uuid: '4eff136f-1f84-4cde-984d-ecd274cd2610',
    name: 'FreeBsd8',
    desc: 'The FreeBSD 8.x Operating System.',
    ver: '8.x',
    shortName: 'freebsd8',
    longName: 'FreeBSD 8.x'
    });

MATCH (freebsd8:FreeBsd8 {uuid: '4eff136f-1f84-4cde-984d-ecd274cd2610'}),
    (freebsd:FreeBsd {uuid: '6bccb300-554a-43ce-981b-fd07b22c83f0'})
MERGE (freebsd8)-[:ISA]->(freebsd);


// FreeBSD 9.
MERGE (freebsd9:Object:Component:Software:OperatingSystem:UnixLike:Bsd:FreeBsd:FreeBsd9 {
    uuid: 'f9a36f71-9b07-4813-ba2c-35448155e6f3',
    name: 'FreeBsd9',
    desc: 'The FreeBSD 9.x Operating System.',
    ver: '9.x',
    shortName: 'freebsd9',
    longName: 'FreeBSD 9.x'
    });

MATCH (freebsd9:FreeBsd9 {uuid: 'f9a36f71-9b07-4813-ba2c-35448155e6f3'}),
    (freebsd:FreeBsd {uuid: '6bccb300-554a-43ce-981b-fd07b22c83f0'})
MERGE (freebsd9)-[:ISA]->(freebsd);


// FreeBSD 10.
MERGE (freebsd10:Object:Component:Software:OperatingSystem:UnixLike:Bsd:FreeBsd:FreeBsd10 {
    uuid: 'e144cfb5-664a-4542-9125-74da72cc0a8a',
    name: 'FreeBsd10',
    desc: 'The FreeBSD 10.x Operating System.',
    ver: '10.x',
    shortName: 'freebsd10',
    longName: 'FreeBSD 10.x'
    });

MATCH (freebsd10:FreeBsd10 {uuid: 'e144cfb5-664a-4542-9125-74da72cc0a8a'}),
    (freebsd:FreeBsd {uuid: '6bccb300-554a-43ce-981b-fd07b22c83f0'})
MERGE (freebsd10)-[:ISA]->(freebsd);


// NetBSD.
MERGE (netbsd:Object:Component:Software:OperatingSystem:UnixLike:Bsd:NetBsd {
    uuid: '33b00a7e-0ddc-4d9e-87d2-876653655d72',
    name: 'NetBsd',
    desc: 'The NetBSD Operating System.',
    shortName: 'netbsd',
    longName: 'NetBSD'
    });

MATCH (netbsd:NetBsd {uuid: '33b00a7e-0ddc-4d9e-87d2-876653655d72'}),
    (bsd:Bsd {uuid: '6004436f-7c88-4c2e-a29d-e8077bad10e9'})
MERGE (netbsd)-[:ISA]->(bsd);

// NetBSD 4.
MERGE (netbsd4:Object:Component:Software:OperatingSystem:UnixLike:Bsd:NetBsd:NetBsd4 {
    uuid: 'e0dfa561-5500-49dd-a532-f5416385d66a',
    name: 'NetBsd4',
    desc: 'The NetBSD 4.x Operating System.',
    ver: '4.x',
    shortName: 'netbsd4',
    longName: 'NetBSD 4.x'
    });

MATCH (netbsd4:NetBsd4 {uuid: 'e0dfa561-5500-49dd-a532-f5416385d66a'}),
    (netbsd:NetBsd {uuid: '33b00a7e-0ddc-4d9e-87d2-876653655d72'})
MERGE (netbsd4)-[:ISA]->(netbsd);


// NetBSD 5.
MERGE (netbsd5:Object:Component:Software:OperatingSystem:UnixLike:Bsd:NetBsd:NetBsd5 {
    uuid: '64cfc48a-0626-4c72-9634-86dd33e94cde',
    name: 'NetBsd5',
    desc: 'The NetBSD 5.x Operating System.',
    ver: '5.x',
    shortName: 'netbsd5',
    longName: 'NetBSD 5.x'
    });

MATCH (netbsd5:NetBsd5 {uuid: '64cfc48a-0626-4c72-9634-86dd33e94cde'}),
    (netbsd:NetBsd {uuid: '33b00a7e-0ddc-4d9e-87d2-876653655d72'})
MERGE (netbsd5)-[:ISA]->(netbsd);


// NetBSD 6.
MERGE (netbsd6:Object:Component:Software:OperatingSystem:UnixLike:Bsd:NetBsd:NetBsd6 {
    uuid: '8ac7f26d-588d-44de-8792-26d6d429f9ff',
    name: 'NetBsd6',
    desc: 'The NetBSD 6.x Operating System.',
    ver: '6.x',
    shortName: 'netbsd6',
    longName: 'NetBSD 6.x'
    });

MATCH (netbsd6:NetBsd6 {uuid: '8ac7f26d-588d-44de-8792-26d6d429f9ff'}),
    (netbsd:NetBsd {uuid: '33b00a7e-0ddc-4d9e-87d2-876653655d72'})
MERGE (netbsd6)-[:ISA]->(netbsd);


// OpenBSD.
MERGE (openbsd:Object:Component:Software:OperatingSystem:UnixLike:Bsd:OpenBsd {
    uuid: '5609a5b8-b1fb-4ec3-a297-155c680a7e65',
    name: 'OpenBsd',
    desc: 'The OpenBSD Operating System.',
    shortName: 'openbsd',
    longName: 'OpenBSD'
    });

MATCH (openbsd:OpenBsd {uuid: '5609a5b8-b1fb-4ec3-a297-155c680a7e65'}),
    (bsd:Bsd {uuid: '6004436f-7c88-4c2e-a29d-e8077bad10e9'})
MERGE (openbsd)-[:ISA]->(bsd);


// OpenBSD 4.
MERGE (openbsd4:Object:Component:Software:OperatingSystem:UnixLike:Bsd:OpenBsd:OpenBsd4 {
    uuid: '03b4f697-c07a-473f-aae6-92d30e63c2f3',
    name: 'OpenBsd4',
    desc: 'The OpenBSD 4.x Operating System.',
    ver: '4.x',
    shortName: 'openbsd4',
    longName: 'OpenBSD 4.x'
    });

MATCH (openbsd4:OpenBsd4 {uuid: '03b4f697-c07a-473f-aae6-92d30e63c2f3'}),
    (openbsd:OpenBsd {uuid: '5609a5b8-b1fb-4ec3-a297-155c680a7e65'})
MERGE (openbsd4)-[:ISA]->(openbsd);


// OpenBSD 5.
MERGE (openbsd5:Object:Component:Software:OperatingSystem:UnixLike:Bsd:OpenBsd:OpenBsd5 {
    uuid: '6330dd4e-92bc-4cab-b218-1c18d37e9742',
    name: 'OpenBsd5',
    desc: 'The OpenBSD 5.x Operating System.',
    ver: '5.x',
    shortName: 'openbsd5',
    longName: 'OpenBSD 5.x'
    });

MATCH (openbsd5:OpenBsd5 {uuid: '6330dd4e-92bc-4cab-b218-1c18d37e9742'}),
    (openbsd:OpenBsd {uuid: '5609a5b8-b1fb-4ec3-a297-155c680a7e65'})
MERGE (openbsd5)-[:ISA]->(openbsd);


// DragonFlyBSD.
MERGE (dfbsd:Object:Component:Software:OperatingSystem:UnixLike:Bsd:DragonFlyBsd {
    uuid: '41d52588-c248-4018-ac34-a8dfc1204729',
    name: 'DragonFlyBsd',
    desc: 'The DragonFlyBSD Operating System.',
    shortName: 'dfbsd',
    longName: 'DragonFlyBSD'
    });

MATCH (dfbsd:DragonFlyBsd {uuid: '41d52588-c248-4018-ac34-a8dfc1204729'}),
    (bsd:Bsd {uuid: '6004436f-7c88-4c2e-a29d-e8077bad10e9'})
MERGE (dfbsd)-[:ISA]->(bsd);


// DragonFlyBSD 1.
MERGE (dfbsd1:Object:Component:Software:OperatingSystem:UnixLike:Bsd:DragonFlyBsd:DragonFlyBsd1 {
    uuid: '85d606c8-1d4b-4657-a67c-19ad78c51117',
    name: 'DragonFlyBsd1',
    desc: 'The DragonFlyBSD 1.x Operating System.',
    ver: '1.x',
    shortName: 'dfbsd1',
    longName: 'DragonFlyBSD 1.x'
    });

MATCH (dfbsd1:DragonFlyBsd1 {uuid: '85d606c8-1d4b-4657-a67c-19ad78c51117'}),
    (dfbsd:DragonFlyBsd {uuid: '41d52588-c248-4018-ac34-a8dfc1204729'})
MERGE (dfbsd1)-[:ISA]->(dfbsd);


// DragonFlyBSD 2.
MERGE (dfbsd2:Object:Component:Software:OperatingSystem:UnixLike:Bsd:DragonFlyBsd:DragonFlyBsd2 {
    uuid: '32fd5872-9894-4284-ac52-9b6fdf68f3a8',
    name: 'DragonFlyBsd2',
    desc: 'The DragonFlyBSD 2.x Operating System.',
    ver: '2.x',
    shortName: 'dfbsd2',
    longName: 'DragonFlyBSD 2.x'
    });

MATCH (dfbsd2:DragonFlyBsd2 {uuid: '32fd5872-9894-4284-ac52-9b6fdf68f3a8'}),
    (dfbsd:DragonFlyBsd {uuid: '41d52588-c248-4018-ac34-a8dfc1204729'})
MERGE (dfbsd2)-[:ISA]->(dfbsd);


// DragonFlyBSD 3.
MERGE (dfbsd3:Object:Component:Software:OperatingSystem:UnixLike:Bsd:DragonFlyBsd:DragonFlyBsd3 {
    uuid: '11666f58-6150-4a65-abea-b8c5fbddfde4',
    name: 'DragonFlyBsd3',
    desc: 'The DragonFlyBSD 3.x Operating System.',
    ver: '3.x',
    shortName: 'dfbsd3',
    longName: 'DragonFlyBSD 3.x'
    });

MATCH (dfbsd3:DragonFlyBsd3 {uuid: '11666f58-6150-4a65-abea-b8c5fbddfde4'}),
    (dfbsd:DragonFlyBsd {uuid: '41d52588-c248-4018-ac34-a8dfc1204729'})
MERGE (dfbsd3)-[:ISA]->(dfbsd);


// Arch Linux.
MERGE (archlinux:Object:Component:Software:OperatingSystem:UnixLike:Linux:ArchLinux {
    uuid: '42a991a2-2f25-479a-a21d-7a7e07f691cf',
    name: 'ArchLinux',
    desc: 'The Arch Linux Operating System.',
    shortName: 'archlinux',
    longName: 'Arch Linux'
    });

MATCH (archlinux:ArchLinux {uuid: '42a991a2-2f25-479a-a21d-7a7e07f691cf'}),
    (linux:Linux {uuid: 'a5613527-2f64-4481-83f2-80b0cb427e28'})
MERGE (archlinux)-[:ISA]->(linux);


// Debian.
MERGE (debian:Object:Component:Software:OperatingSystem:UnixLike:Linux:Debian {
    uuid: '021488da-3011-4861-97a4-420a7d647e5d',
    name: 'Debian',
    desc: 'The Debian Linux Operating System.',
    shortName: 'debian',
    longName: 'Debian Linux'
    });

MATCH (debian:Debian {uuid: '021488da-3011-4861-97a4-420a7d647e5d'}),
    (linux:Linux {uuid: 'a5613527-2f64-4481-83f2-80b0cb427e28'})
MERGE (debian)-[:ISA]->(linux);


// Debian 4.
MERGE (debian4:Object:Component:Software:OperatingSystem:UnixLike:Linux:Debian:Debian4 {
    uuid: '3de571df-b7cf-4673-8a31-e40e5dde624e',
    name: 'Debian4',
    desc: 'The Debian 4.x Linux Operating System.',
    ver: '4.x',
    shortName: 'debian4',
    longName: 'Debian 4.x'
    });

MATCH (debian4:Debian4 {uuid: '3de571df-b7cf-4673-8a31-e40e5dde624e'}),
    (debian:Debian {uuid: '021488da-3011-4861-97a4-420a7d647e5d'})
MERGE (debian4)-[:ISA]->(debian);


// Debian 5.
MERGE (debian5:Object:Component:Software:OperatingSystem:UnixLike:Linux:Debian:Debian5 {
    uuid: '4f764a05-7fbd-4810-a069-a7e76f4933cd',
    name: 'Debian5',
    desc: 'The Debian 5.x Linux Operating System.',
    ver: '5.x',
    shortName: 'debian5',
    longName: 'Debian 5.x'
    });

MATCH (debian5:Debian5 {uuid: '4f764a05-7fbd-4810-a069-a7e76f4933cd'}),
    (debian:Debian {uuid: '021488da-3011-4861-97a4-420a7d647e5d'})
MERGE (debian5)-[:ISA]->(debian);


// Debian 6.
MERGE (debian6:Object:Component:Software:OperatingSystem:UnixLike:Linux:Debian:Debian6 {
    uuid: '6cf7eed4-847a-4249-b4cd-e0668091336e',
    name: 'Debian6',
    desc: 'The Debian 6.x Linux Operating System.',
    ver: '6.x',
    shortName: 'debian6',
    longName: 'Debian 6.x'
    });

MATCH (debian6:Debian6 {uuid: '6cf7eed4-847a-4249-b4cd-e0668091336e'}),
    (debian:Debian {uuid: '021488da-3011-4861-97a4-420a7d647e5d'})
MERGE (debian6)-[:ISA]->(debian);


// Debian 7.
MERGE (debian7:Object:Component:Software:OperatingSystem:UnixLike:Linux:Debian:Debian7 {
    uuid: '368ea969-197b-4e18-8a73-c6b6b26beb9f',
    name: 'Debian7',
    desc: 'The Debian 7.x Linux Operating System.',
    ver: '7.x',
    shortName: 'debian7',
    longName: 'Debian 7.x'
    });

MATCH (debian7:Debian7 {uuid: '368ea969-197b-4e18-8a73-c6b6b26beb9f'}),
    (debian:Debian {uuid: '021488da-3011-4861-97a4-420a7d647e5d'})
MERGE (debian7)-[:ISA]->(debian);


// Debian 8.
MERGE (debian8:Object:Component:Software:OperatingSystem:UnixLike:Linux:Debian:Debian8 {
    uuid: 'e7cfb97e-b1b0-4696-92bf-a70499d3fb40',
    name: 'Debian8',
    desc: 'The Debian 8.x Linux Operating System.',
    ver: '8.x',
    shortName: 'debian8',
    longName: 'Debian 8.x'
    });

MATCH (debian8:Debian8 {uuid: 'e7cfb97e-b1b0-4696-92bf-a70499d3fb40'}),
    (debian:Debian {uuid: '021488da-3011-4861-97a4-420a7d647e5d'})
MERGE (debian8)-[:ISA]->(debian);


// Ubuntu.
MERGE (ubuntu:Object:Component:Software:OperatingSystem:UnixLike:Linux:Ubuntu {
    uuid: '1e13acac-4efb-4e62-894d-812492f6c266',
    name: 'Ubuntu',
    desc: 'The Ubuntu Linux Operating System.',
    shortName: 'ubuntu',
    longName: 'Ubuntu Linux'
    });

MATCH (ubuntu:Ubuntu {uuid: '1e13acac-4efb-4e62-894d-812492f6c266'}),
    (linux:Linux {uuid: 'a5613527-2f64-4481-83f2-80b0cb427e28'})
MERGE (ubuntu)-[:ISA]->(linux);


// Ubuntu 8.
MERGE (ubuntu8:Object:Component:Software:OperatingSystem:UnixLike:Linux:Ubuntu:Ubuntu8 {
    uuid: 'b41dabe4-e012-4c83-b073-4ca202950c44',
    name: 'Ubuntu8',
    desc: 'The Ubuntu 8.x Linux Operating System.',
    ver: '8.x',
    shortName: 'ubuntu8',
    longName: 'Ubuntu 8.x'
    });

MATCH (ubuntu8:Ubuntu8 {uuid: 'b41dabe4-e012-4c83-b073-4ca202950c44'}),
    (ubuntu:Ubuntu {uuid: '1e13acac-4efb-4e62-894d-812492f6c266'})
MERGE (ubuntu8)-[:ISA]->(ubuntu);


// Ubuntu 9.
MERGE (ubuntu9:Object:Component:Software:OperatingSystem:UnixLike:Linux:Ubuntu:Ubuntu9 {
    uuid: 'a404e00c-46d7-4c20-940e-e071967dd2fe',
    name: 'Ubuntu9',
    desc: 'The Ubuntu 9.x Linux Operating System.',
    ver: '9.x',
    shortName: 'ubuntu9',
    longName: 'Ubuntu 9.x'
    });

MATCH (ubuntu9:Ubuntu9 {uuid: 'a404e00c-46d7-4c20-940e-e071967dd2fe'}),
    (ubuntu:Ubuntu {uuid: '1e13acac-4efb-4e62-894d-812492f6c266'})
MERGE (ubuntu9)-[:ISA]->(ubuntu);


// Ubuntu 10.
MERGE (ubuntu10:Object:Component:Software:OperatingSystem:UnixLike:Linux:Ubuntu:Ubuntu10 {
    uuid: 'd98635d0-a229-4755-88e2-30f891ce692d',
    name: 'Ubuntu10',
    desc: 'The Ubuntu 10.x Linux Operating System.',
    ver: '10.x',
    shortName: 'ubuntu10',
    longName: 'Ubuntu 10.x'
    });

MATCH (ubuntu10:Ubuntu10 {uuid: 'd98635d0-a229-4755-88e2-30f891ce692d'}),
    (ubuntu:Ubuntu {uuid: '1e13acac-4efb-4e62-894d-812492f6c266'})
MERGE (ubuntu10)-[:ISA]->(ubuntu);


// Ubuntu 11.
MERGE (ubuntu11:Object:Component:Software:OperatingSystem:UnixLike:Linux:Ubuntu:Ubuntu11 {
    uuid: 'dbb94e1f-72ca-4d2d-a5fd-38d7639df5c1',
    name: 'Ubuntu11',
    desc: 'The Ubuntu 11.x Linux Operating System.',
    ver: '11.x',
    shortName: 'ubuntu11',
    longName: 'Ubuntu 11.x'
    });

MATCH (ubuntu11:Ubuntu11 {uuid: 'dbb94e1f-72ca-4d2d-a5fd-38d7639df5c1'}),
    (ubuntu:Ubuntu {uuid: '1e13acac-4efb-4e62-894d-812492f6c266'})
MERGE (ubuntu11)-[:ISA]->(ubuntu);


// Ubuntu 12.
MERGE (ubuntu12:Object:Component:Software:OperatingSystem:UnixLike:Linux:Ubuntu:Ubuntu12 {
    uuid: '0cf76620-de1f-4e7b-b51b-74ecd980c253',
    name: 'Ubuntu12',
    desc: 'The Ubuntu 12.x Linux Operating System.',
    ver: '12.x',
    shortName: 'ubuntu12',
    longName: 'Ubuntu 12.x'
    });

MATCH (ubuntu12:Ubuntu12 {uuid: '0cf76620-de1f-4e7b-b51b-74ecd980c253'}),
    (ubuntu:Ubuntu {uuid: '1e13acac-4efb-4e62-894d-812492f6c266'})
MERGE (ubuntu12)-[:ISA]->(ubuntu);


// Ubuntu 13.
MERGE (ubuntu13:Object:Component:Software:OperatingSystem:UnixLike:Linux:Ubuntu:Ubuntu13 {
    uuid: 'ab6a2022-57f1-476c-8013-3cda7554280e',
    name: 'Ubuntu13',
    desc: 'The Ubuntu 13.x Linux Operating System.',
    ver: '13.x',
    shortName: 'ubuntu13',
    longName: 'Ubuntu 13.x'
    });

MATCH (ubuntu13:Ubuntu13 {uuid: 'ab6a2022-57f1-476c-8013-3cda7554280e'}),
    (ubuntu:Ubuntu {uuid: '1e13acac-4efb-4e62-894d-812492f6c266'})
MERGE (ubuntu13)-[:ISA]->(ubuntu);


// Ubuntu 14.
MERGE (ubuntu14:Object:Component:Software:OperatingSystem:UnixLike:Linux:Ubuntu:Ubuntu14 {
    uuid: '61a034bb-19e3-4d8f-bda9-35c90e874dc8',
    name: 'Ubuntu14',
    desc: 'The Ubuntu 14.x Linux Operating System.',
    ver: '14.x',
    shortName: 'ubuntu14',
    longName: 'Ubuntu 14.x'
    });

MATCH (ubuntu14:Ubuntu14 {uuid: '61a034bb-19e3-4d8f-bda9-35c90e874dc8'}),
    (ubuntu:Ubuntu {uuid: '1e13acac-4efb-4e62-894d-812492f6c266'})
MERGE (ubuntu14)-[:ISA]->(ubuntu);


// Linux Mint.
MERGE (mint:Object:Component:Software:OperatingSystem:UnixLike:Linux:Mint {
    uuid: '7d5b895a-a076-491d-b6ca-58eed0d0c960',
    name: 'Mint',
    desc: 'The Linux Mint Operating System.',
    shortName: 'mint',
    longName: 'Linux Mint'
    });

MATCH (mint:Mint {uuid: '7d5b895a-a076-491d-b6ca-58eed0d0c960'}),
    (linux:Linux {uuid: 'a5613527-2f64-4481-83f2-80b0cb427e28'})
MERGE (mint)-[:ISA]->(linux);


// Linux Mint 10.
MERGE (mint10:Object:Component:Software:OperatingSystem:UnixLike:Linux:Mint:Mint10 {
    uuid: 'ab5e364d-51c3-4f77-b1d7-a12128e62f9f',
    name: 'Mint10',
    desc: 'The Linux Mint 10.x Operating System.',
    ver: '10',
    shortName: 'mint10',
    longName: ':Linux Mint 10.x'
    });

MATCH (mint10:Mint10 {uuid: 'ab5e364d-51c3-4f77-b1d7-a12128e62f9f'}),
    (mint:Mint {uuid: '7d5b895a-a076-491d-b6ca-58eed0d0c960'})
MERGE (mint10)-[:ISA]->(mint);


// Linux Mint 11.
MERGE (mint11:Object:Component:Software:OperatingSystem:UnixLike:Linux:Mint:Mint11 {
    uuid: '27a69b62-9e0b-49e1-bb5e-30cd83e6cc68',
    name: 'Mint11',
    desc: 'The Linux Mint 11.x Operating System.',
    ver: '11',
    shortName: 'mint11',
    longName: ':Linux Mint 11.x'
    });

MATCH (mint11:Mint11 {uuid: '27a69b62-9e0b-49e1-bb5e-30cd83e6cc68'}),
    (mint:Mint {uuid: '7d5b895a-a076-491d-b6ca-58eed0d0c960'})
MERGE (mint11)-[:ISA]->(mint);


// Linux Mint 12.
MERGE (mint12:Object:Component:Software:OperatingSystem:UnixLike:Linux:Mint:Mint12 {
    uuid: '8beb8f4b-cf59-4f2c-81b5-74041d379fc6',
    name: 'Mint12',
    desc: 'The Linux Mint 12.x Operating System.',
    ver: '12',
    shortName: 'mint12',
    longName: ':Linux Mint 12.x'
    });

MATCH (mint12:Mint12 {uuid: '8beb8f4b-cf59-4f2c-81b5-74041d379fc6'}),
    (mint:Mint {uuid: '7d5b895a-a076-491d-b6ca-58eed0d0c960'})
MERGE (mint12)-[:ISA]->(mint);


// Linux Mint 13.
MERGE (mint13:Object:Component:Software:OperatingSystem:UnixLike:Linux:Mint:Mint13 {
    uuid: 'f204c30e-46be-49d3-82fd-7f455e1d3ac3',
    name: 'Mint13',
    desc: 'The Linux Mint 13.x Operating System.',
    ver: '13',
    shortName: 'mint13',
    longName: ':Linux Mint 13.x'
    });

MATCH (mint13:Mint13 {uuid: 'f204c30e-46be-49d3-82fd-7f455e1d3ac3'}),
    (mint:Mint {uuid: '7d5b895a-a076-491d-b6ca-58eed0d0c960'})
MERGE (mint13)-[:ISA]->(mint);


// Linux Mint 14.
MERGE (mint14:Object:Component:Software:OperatingSystem:UnixLike:Linux:Mint:Mint14 {
    uuid: 'c4ea3a0d-8bdc-48e0-b985-719453831834',
    name: 'Mint14',
    desc: 'The Linux Mint 14.x Operating System.',
    ver: '14',
    shortName: 'mint14',
    longName: ':Linux Mint 14.x'
    });

MATCH (mint14:Mint14 {uuid: 'c4ea3a0d-8bdc-48e0-b985-719453831834'}),
    (mint:Mint {uuid: '7d5b895a-a076-491d-b6ca-58eed0d0c960'})
MERGE (mint14)-[:ISA]->(mint);


// Linux Mint 15.
MERGE (mint15:Object:Component:Software:OperatingSystem:UnixLike:Linux:Mint:Mint15 {
    uuid: '4b5a4f58-6f90-404f-a7bf-dd805506b197',
    name: 'Mint15',
    desc: 'The Linux Mint 15.x Operating System.',
    ver: '15',
    shortName: 'mint15',
    longName: ':Linux Mint 15.x'
    });

MATCH (mint15:Mint15 {uuid: '4b5a4f58-6f90-404f-a7bf-dd805506b197'}),
    (mint:Mint {uuid: '7d5b895a-a076-491d-b6ca-58eed0d0c960'})
MERGE (mint15)-[:ISA]->(mint);


// Linux Mint 16.
MERGE (mint16:Object:Component:Software:OperatingSystem:UnixLike:Linux:Mint:Mint16 {
    uuid: '5a1a5259-3387-4a43-9329-0b090f2b6473',
    name: 'Mint16',
    desc: 'The Linux Mint 16.x Operating System.',
    ver: '16',
    shortName: 'mint16',
    longName: ':Linux Mint 16.x'
    });

MATCH (mint16:Mint16 {uuid: '5a1a5259-3387-4a43-9329-0b090f2b6473'}),
    (mint:Mint {uuid: '7d5b895a-a076-491d-b6ca-58eed0d0c960'})
MERGE (mint16)-[:ISA]->(mint);


// Linux Mint 17.
MERGE (mint17:Object:Component:Software:OperatingSystem:UnixLike:Linux:Mint:Mint17 {
    uuid: 'b495af93-5b98-4746-ba29-2ec74f530cc6',
    name: 'Mint17',
    desc: 'The Linux Mint 17.x Operating System.',
    ver: '17',
    shortName: 'mint17',
    longName: ':Linux Mint 17.x'
    });

MATCH (mint17:Mint17 {uuid: 'b495af93-5b98-4746-ba29-2ec74f530cc6'}),
    (mint:Mint {uuid: '7d5b895a-a076-491d-b6ca-58eed0d0c960'})
MERGE (mint17)-[:ISA]->(mint);


// Red Hat Enterprise Linux.
MERGE (rhel:Object:Component:Software:OperatingSystem:UnixLike:Linux:Rhel {
    uuid: '98741859-5dd5-4c8d-a64a-69598f5ccb2f',
    name: 'Rhel',
    desc: 'The Red Hat Enterprise Linux Operating System.',
    shortName: 'rhel',
    longName: 'Red Hat Enterprise Linux'
    });

MATCH (rhel:Rhel {uuid: '98741859-5dd5-4c8d-a64a-69598f5ccb2f'}),
    (linux:Linux {uuid: 'a5613527-2f64-4481-83f2-80b0cb427e28'})
MERGE (rhel)-[:ISA]->(linux);


// Red Hat Enterprise Linux 3.
MERGE (rhel3:Object:Component:Software:OperatingSystem:UnixLike:Linux:Rhel:Rhel3 {
    uuid: '200eed01-43fe-4e9f-a740-a1bf50933751',
    name: 'Rhel3',
    desc: 'The Red Hat Enterprise Linux 3.x Operating System.',
    ver: '3.x',
    shortName: 'rhel3',
    longName: 'Red Hat Enterprise Linux 3.x'
    });

MATCH (rhel3:Rhel3 {uuid: '200eed01-43fe-4e9f-a740-a1bf50933751'}),
    (rhel:Rhel {uuid: '98741859-5dd5-4c8d-a64a-69598f5ccb2f'})
MERGE (rhel3)-[:ISA]->(rhel);


// Red Hat Enterprise Linux 4.
MERGE (rhel4:Object:Component:Software:OperatingSystem:UnixLike:Linux:Rhel:Rhel4 {
    uuid: '078f7adf-2647-4f35-8742-96a7c59c992b',
    name: 'Rhel4',
    desc: 'The Red Hat Enterprise Linux 4.x Operating System.',
    ver: '4.x',
    shortName: 'rhel4',
    longName: 'Red Hat Enterprise Linux 4.x'
    });

MATCH (rhel4:Rhel4 {uuid: '078f7adf-2647-4f35-8742-96a7c59c992b'}),
    (rhel:Rhel {uuid: '98741859-5dd5-4c8d-a64a-69598f5ccb2f'})
MERGE (rhel4)-[:ISA]->(rhel);


// Red Hat Enterprise Linux 5.
MERGE (rhel5:Object:Component:Software:OperatingSystem:UnixLike:Linux:Rhel:Rhel5 {
    uuid: '04202e27-f9ff-4ce0-8418-0d4aaecd6257',
    name: 'Rhel5',
    desc: 'The Red Hat Enterprise Linux 5.x Operating System.',
    ver: '5.x',
    shortName: 'rhel5',
    longName: 'Red Hat Enterprise Linux 5.x'
    });

MATCH (rhel5:Rhel5 {uuid: '04202e27-f9ff-4ce0-8418-0d4aaecd6257'}),
    (rhel:Rhel {uuid: '98741859-5dd5-4c8d-a64a-69598f5ccb2f'})
MERGE (rhel5)-[:ISA]->(rhel);


// Red Hat Enterprise Linux 6.
MERGE (rhel6:Object:Component:Software:OperatingSystem:UnixLike:Linux:Rhel:Rhel6 {
    uuid: 'c19c8786-1ed0-4fc1-9ee9-7f0d49022d2c',
    name: 'Rhel6',
    desc: 'The Red Hat Enterprise Linux 6.x Operating System.',
    ver: '6.x',
    shortName: 'rhel6',
    longName: 'Red Hat Enterprise Linux 6.x'
    });

MATCH (rhel6:Rhel6 {uuid: 'c19c8786-1ed0-4fc1-9ee9-7f0d49022d2c'}),
    (rhel:Rhel {uuid: '98741859-5dd5-4c8d-a64a-69598f5ccb2f'})
MERGE (rhel6)-[:ISA]->(rhel);


// Red Hat Enterprise Linux 7.
MERGE (rhel7:Object:Component:Software:OperatingSystem:UnixLike:Linux:Rhel:Rhel7 {
    uuid: 'd4fd6992-f34c-473b-9e05-929d7179ff73',
    name: 'Rhel7',
    desc: 'The Red Hat Enterprise Linux 7.x Operating System.',
    ver: '7.x',
    shortName: 'rhel7',
    longName: 'Red Hat Enterprise Linux 7.x'
    });

MATCH (rhel7:Rhel7 {uuid: 'd4fd6992-f34c-473b-9e05-929d7179ff73'}),
    (rhel:Rhel {uuid: '98741859-5dd5-4c8d-a64a-69598f5ccb2f'})
MERGE (rhel7)-[:ISA]->(rhel);


// CentOS.
MERGE (centos:Object:Component:Software:OperatingSystem:UnixLike:Linux:Centos {
    uuid: 'c8b320a0-c5fd-43c0-b52e-305f2daccfec',
    name: 'Centos',
    desc: 'The Community Enterprise Operating System.',
    shortName: 'centos',
    longName: 'CentOS'
    });

MATCH (centos:Centos {uuid: 'c8b320a0-c5fd-43c0-b52e-305f2daccfec'}),
    (linux:Linux {uuid: 'a5613527-2f64-4481-83f2-80b0cb427e28'})
MERGE (centos)-[:ISA]->(linux);


// CentOS 3.
MERGE (centos3:Object:Component:Software:OperatingSystem:UnixLike:Linux:Centos:Centos3 {
    uuid: '4a43eead-c3a1-4e10-b8c1-b23e0b5b26ef',
    name: 'Centos3',
    desc: 'The Community Enterprise Operating System, version 3.x',
    ver: '3.x',
    shortName: 'centos3',
    longName: 'CentOS 3.x'
    });

MATCH (centos3:Centos3 {uuid: '4a43eead-c3a1-4e10-b8c1-b23e0b5b26ef'}),
    (centos:Centos {uuid: 'c8b320a0-c5fd-43c0-b52e-305f2daccfec'})
MERGE (centos3)-[:ISA]->(centos);


// CentOS 4.
MERGE (centos4:Object:Component:Software:OperatingSystem:UnixLike:Linux:Centos:Centos4 {
    uuid: '8e49d933-dcd4-439f-9d10-91cb48a8e2d4',
    name: 'Centos4',
    desc: 'The Community Enterprise Operating System, version 4.x',
    ver: '4.x',
    shortName: 'centos4',
    longName: 'CentOS 4.x'
    });

MATCH (centos4:Centos4 {uuid: '8e49d933-dcd4-439f-9d10-91cb48a8e2d4'}),
    (centos:Centos {uuid: 'c8b320a0-c5fd-43c0-b52e-305f2daccfec'})
MERGE (centos4)-[:ISA]->(centos);


// CentOS 5.
MERGE (centos5:Object:Component:Software:OperatingSystem:UnixLike:Linux:Centos:Centos5 {
    uuid: 'caaee566-d43e-44b8-a59d-a87c37300562',
    name: 'Centos5',
    desc: 'The Community Enterprise Operating System, version 5.x',
    ver: '5.x',
    shortName: 'centos5',
    longName: 'CentOS 5.x'
    });

MATCH (centos5:Centos5 {uuid: 'caaee566-d43e-44b8-a59d-a87c37300562'}),
    (centos:Centos {uuid: 'c8b320a0-c5fd-43c0-b52e-305f2daccfec'})
MERGE (centos5)-[:ISA]->(centos);


// CentOS 6.
MERGE (centos6:Object:Component:Software:OperatingSystem:UnixLike:Linux:Centos:Centos6 {
    uuid: 'f3122393-1f4f-4916-96d7-a64b8be5736b',
    name: 'Centos6',
    desc: 'The Community Enterprise Operating System, version 6.x',
    ver: '6.x',
    shortName: 'centos6',
    longName: 'CentOS 6.x'
    });

MATCH (centos6:Centos6 {uuid: 'f3122393-1f4f-4916-96d7-a64b8be5736b'}),
    (centos:Centos {uuid: 'c8b320a0-c5fd-43c0-b52e-305f2daccfec'})
MERGE (centos6)-[:ISA]->(centos);


// CentOS 7.
MERGE (centos7:Object:Component:Software:OperatingSystem:UnixLike:Linux:Centos:Centos7 {
    uuid: '9c13f597-4cd8-4311-a884-6efe24f10c81',
    name: 'Centos7',
    desc: 'The Community Enterprise Operating System, version 7.x',
    ver: '7.x',
    shortName: 'centos7',
    longName: 'CentOS 7.x'
    });

MATCH (centos7:Centos7 {uuid: '9c13f597-4cd8-4311-a884-6efe24f10c81'}),
    (centos:Centos {uuid: 'c8b320a0-c5fd-43c0-b52e-305f2daccfec'})
MERGE (centos7)-[:ISA]->(centos);


// Fedora.
MERGE (fedora:Object:Component:Software:OperatingSystem:UnixLike:Linux:Fedora {
    uuid: '3f90173a-ae4d-4fd5-8c80-5659262d3aef',
    name: 'Fedora',
    desc: 'The Fedora Linux Operating System.',
    shortName: 'fedora',
    longName: 'Fedora'
    });

MATCH (fedora:Fedora {uuid: '3f90173a-ae4d-4fd5-8c80-5659262d3aef'}),
    (linux:Linux {uuid: 'a5613527-2f64-4481-83f2-80b0cb427e28'})
MERGE (fedora)-[:ISA]->(linux);


// Fedora 12.
MERGE (fedora12:Object:Component:Software:OperatingSystem:UnixLike:Linux:Fedora:Fedora12 {
    uuid: '342aecd0-163c-48fa-b0da-8e25b2c27a06',
    name: 'Fedora12',
    desc: 'The Fedora 12 Linux Operating System.',
    ver: '12',
    shortName: 'fedora12',
    longName: 'Fedora 12'
    });

MATCH (fedora12:Fedora12 {uuid: '342aecd0-163c-48fa-b0da-8e25b2c27a06'}),
    (fedora:Fedora {uuid: '3f90173a-ae4d-4fd5-8c80-5659262d3aef'})
MERGE (fedora12)-[:ISA]->(fedora);


// Fedora 13.
MERGE (fedora13:Object:Component:Software:OperatingSystem:UnixLike:Linux:Fedora:Fedora13 {
    uuid: 'f536b385-373e-45d2-9fd5-a41d5171cc45',
    name: 'Fedora13',
    desc: 'The Fedora 13 Linux Operating System.',
    ver: '13',
    shortName: 'fedora13',
    longName: 'Fedora 13'
    });

MATCH (fedora13:Fedora13 {uuid: 'f536b385-373e-45d2-9fd5-a41d5171cc45'}),
    (fedora:Fedora {uuid: '3f90173a-ae4d-4fd5-8c80-5659262d3aef'})
MERGE (fedora13)-[:ISA]->(fedora);


// Fedora 14.
MERGE (fedora14:Object:Component:Software:OperatingSystem:UnixLike:Linux:Fedora:Fedora14 {
    uuid: '68b7effb-35c0-4b0d-adce-4ea0af5b0c97',
    name: 'Fedora14',
    desc: 'The Fedora 14 Linux Operating System.',
    ver: '14',
    shortName: 'fedora14',
    longName: 'Fedora 14'
    });

MATCH (fedora14:Fedora14 {uuid: '68b7effb-35c0-4b0d-adce-4ea0af5b0c97'}),
    (fedora:Fedora {uuid: '3f90173a-ae4d-4fd5-8c80-5659262d3aef'})
MERGE (fedora14)-[:ISA]->(fedora);


// Fedora 15.
MERGE (fedora15:Object:Component:Software:OperatingSystem:UnixLike:Linux:Fedora:Fedora15 {
    uuid: '193c78d5-ad8f-48a4-ac10-8ab775cc7eb9',
    name: 'Fedora15',
    desc: 'The Fedora 15 Linux Operating System.',
    ver: '15',
    shortName: 'fedora15',
    longName: 'Fedora 15'
    });

MATCH (fedora15:Fedora15 {uuid: '193c78d5-ad8f-48a4-ac10-8ab775cc7eb9'}),
    (fedora:Fedora {uuid: '3f90173a-ae4d-4fd5-8c80-5659262d3aef'})
MERGE (fedora15)-[:ISA]->(fedora);


// Fedora 16.
MERGE (fedora16:Object:Component:Software:OperatingSystem:UnixLike:Linux:Fedora:Fedora16 {
    uuid: 'f25ebc43-5f20-469f-90fe-8b907258f648',
    name: 'Fedora16',
    desc: 'The Fedora 16 Linux Operating System.',
    ver: '16',
    shortName: 'fedora16',
    longName: 'Fedora 16'
    });

MATCH (fedora16:Fedora16 {uuid: 'f25ebc43-5f20-469f-90fe-8b907258f648'}),
    (fedora:Fedora {uuid: '3f90173a-ae4d-4fd5-8c80-5659262d3aef'})
MERGE (fedora16)-[:ISA]->(fedora);


// Fedora 17.
MERGE (fedora17:Object:Component:Software:OperatingSystem:UnixLike:Linux:Fedora:Fedora17 {
    uuid: 'e279d091-8dbe-45af-a41b-02f64800844f',
    name: 'Fedora17',
    desc: 'The Fedora 17 Linux Operating System.',
    ver: '17',
    shortName: 'fedora17',
    longName: 'Fedora 17'
    });

MATCH (fedora17:Fedora17 {uuid: 'e279d091-8dbe-45af-a41b-02f64800844f'}),
    (fedora:Fedora {uuid: '3f90173a-ae4d-4fd5-8c80-5659262d3aef'})
MERGE (fedora17)-[:ISA]->(fedora);


// Fedora 18.
MERGE (fedora18:Object:Component:Software:OperatingSystem:UnixLike:Linux:Fedora:Fedora18 {
    uuid: 'ef0cc490-f3cf-48f9-be47-a7ce9a2618db',
    name: 'Fedora18',
    desc: 'The Fedora 18 Linux Operating System.',
    ver: '18',
    shortName: 'fedora18',
    longName: 'Fedora 18'
    });

MATCH (fedora18:Fedora18 {uuid: 'ef0cc490-f3cf-48f9-be47-a7ce9a2618db'}),
    (fedora:Fedora {uuid: '3f90173a-ae4d-4fd5-8c80-5659262d3aef'})
MERGE (fedora18)-[:ISA]->(fedora);


// Fedora 19.
MERGE (fedora19:Object:Component:Software:OperatingSystem:UnixLike:Linux:Fedora:Fedora19 {
    uuid: '725c35f8-fbf0-442d-a4ec-0d83304e1b2a',
    name: 'Fedora19',
    desc: 'The Fedora 19 Linux Operating System.',
    ver: '19',
    shortName: 'fedora19',
    longName: 'Fedora 19'
    });

MATCH (fedora19:Fedora19 {uuid: '725c35f8-fbf0-442d-a4ec-0d83304e1b2a'}),
    (fedora:Fedora {uuid: '3f90173a-ae4d-4fd5-8c80-5659262d3aef'})
MERGE (fedora19)-[:ISA]->(fedora);


// Fedora 20.
MERGE (fedora20:Object:Component:Software:OperatingSystem:UnixLike:Linux:Fedora:Fedora20 {
    uuid: 'ca39cf3a-43b2-46b9-8440-cf350c730d29',
    name: 'Fedora20',
    desc: 'The Fedora 20 Linux Operating System.',
    ver: '20',
    shortName: 'fedora20',
    longName: 'Fedora 20'
    });

MATCH (fedora20:Fedora20 {uuid: 'ca39cf3a-43b2-46b9-8440-cf350c730d29'}),
    (fedora:Fedora {uuid: '3f90173a-ae4d-4fd5-8c80-5659262d3aef'})
MERGE (fedora20)-[:ISA]->(fedora);


// Fedora 21.
MERGE (fedora21:Object:Component:Software:OperatingSystem:UnixLike:Linux:Fedora:Fedora21 {
    uuid: '45284c82-2da0-48ae-a698-5f4f707fab94',
    name: 'Fedora21',
    desc: 'The Fedora 21 Linux Operating System.',
    ver: '21',
    shortName: 'fedora21',
    longName: 'Fedora 21'
    });

MATCH (fedora21:Fedora21 {uuid: '45284c82-2da0-48ae-a698-5f4f707fab94'}),
    (fedora:Fedora {uuid: '3f90173a-ae4d-4fd5-8c80-5659262d3aef'})
MERGE (fedora21)-[:ISA]->(fedora);


// Mageia.
MERGE (mageia:Object:Component:Software:OperatingSystem:UnixLike:Linux:Mageia {
    uuid: '26b0d6c8-4d6b-4ec2-8d15-6e71758cc06c',
    name: 'Mageia',
    desc: 'The Mageia Linux Operating System.',
    shortName: 'mageia',
    longName: 'Mageia'
    });

MATCH (mageia:Mageia {uuid: '26b0d6c8-4d6b-4ec2-8d15-6e71758cc06c'}),
    (linux:Linux {uuid: 'a5613527-2f64-4481-83f2-80b0cb427e28'})
MERGE (mageia)-[:ISA]->(linux);


// Mageia 3.
MERGE (mageia3:Object:Component:Software:OperatingSystem:UnixLike:Linux:Mageia:Mageia3 {
    uuid: '43a6757b-5fc1-4781-9521-b028af50defc',
    name: 'Mageia3',
    desc: 'The Mageia 3.x Linux Operating System.',
    ver: '3.x',
    shortName: 'mageia3',
    longName: 'Mageia 3.x'
    });

MATCH (mageia3:Mageia3 {uuid: '43a6757b-5fc1-4781-9521-b028af50defc'}),
    (mageia:Mageia {uuid: '26b0d6c8-4d6b-4ec2-8d15-6e71758cc06c'})
MERGE (mageia3)-[:ISA]->(mageia);


// Mageia 4.
MERGE (mageia4:Object:Component:Software:OperatingSystem:UnixLike:Linux:Mageia:Mageia4 {
    uuid: '5a516ba5-2d9d-40bb-ba45-c79f630f1953',
    name: 'Mageia4',
    desc: 'The Mageia 4.x Linux Operating System.',
    ver: '4.x',
    shortName: 'mageia4',
    longName: 'Mageia 4.x'
    });

MATCH (mageia4:Mageia4 {uuid: '5a516ba5-2d9d-40bb-ba45-c79f630f1953'}),
    (mageia:Mageia {uuid: '26b0d6c8-4d6b-4ec2-8d15-6e71758cc06c'})
MERGE (mageia4)-[:ISA]->(mageia);


// openSUSE.
MERGE (opensuse:Object:Component:Software:OperatingSystem:UnixLike:Linux:OpenSuse {
    uuid: 'cdabdf0d-5994-43ce-803b-c4686ed16f48',
    name: 'OpenSuse',
    desc: 'The openSUSE Linux Operating System.',
    shortName: 'opensuse',
    longName: 'openSUSE'
    });

MATCH (opensuse:OpenSuse {uuid: 'cdabdf0d-5994-43ce-803b-c4686ed16f48'}),
    (linux:Linux {uuid: 'a5613527-2f64-4481-83f2-80b0cb427e28'})
MERGE (opensuse)-[:ISA]->(linux);


// openSUSE 10.
MERGE (opensuse10:Object:Component:Software:OperatingSystem:UnixLike:Linux:OpenSuse:OpenSuse10 {
    uuid: 'aa0c5261-b3cd-49b8-adf1-243087416064',
    name: 'OpenSuse10',
    desc: 'The openSUSE 10.x Linux Operating System.',
    ver: '10.x',
    shortName: 'opensuse10',
    longName: 'openSUSE 10.x'
    });

MATCH (opensuse10:OpenSuse10 {uuid: 'aa0c5261-b3cd-49b8-adf1-243087416064'}),
    (opensuse:OpenSuse {uuid: 'cdabdf0d-5994-43ce-803b-c4686ed16f48'})
MERGE (opensuse10)-[:ISA]->(opensuse);


// openSUSE 11.
MERGE (opensuse11:Object:Component:Software:OperatingSystem:UnixLike:Linux:OpenSuse:OpenSuse11 {
    uuid: 'dbe9b27e-5436-4b5b-a851-f0ba0cb04d69',
    name: 'OpenSuse11',
    desc: 'The openSUSE 11.x Linux Operating System.',
    ver: '11.x',
    shortName: 'opensuse11',
    longName: 'openSUSE 11.x'
    });

MATCH (opensuse11:OpenSuse11 {uuid: 'dbe9b27e-5436-4b5b-a851-f0ba0cb04d69'}),
    (opensuse:OpenSuse {uuid: 'cdabdf0d-5994-43ce-803b-c4686ed16f48'})
MERGE (opensuse11)-[:ISA]->(opensuse);


// openSUSE 12.
MERGE (opensuse12:Object:Component:Software:OperatingSystem:UnixLike:Linux:OpenSuse:OpenSuse12 {
    uuid: '90fa3322-60c3-4808-9d83-3af9b5a69b62',
    name: 'OpenSuse12',
    desc: 'The openSUSE 12.x Linux Operating System.',
    ver: '12.x',
    shortName: 'opensuse12',
    longName: 'openSUSE 12.x'
    });

MATCH (opensuse12:OpenSuse12 {uuid: '90fa3322-60c3-4808-9d83-3af9b5a69b62'}),
    (opensuse:OpenSuse {uuid: 'cdabdf0d-5994-43ce-803b-c4686ed16f48'})
MERGE (opensuse12)-[:ISA]->(opensuse);


// openSUSE 13.
MERGE (opensuse13:Object:Component:Software:OperatingSystem:UnixLike:Linux:OpenSuse:OpenSuse13 {
    uuid: '09ad6070-a602-4e00-ba9c-25a181f7f0df',
    name: 'OpenSuse13',
    desc: 'The openSUSE 13.x Linux Operating System.',
    ver: '13.x',
    shortName: 'opensuse13',
    longName: 'openSUSE 13.x'
    });

MATCH (opensuse13:OpenSuse13 {uuid: '09ad6070-a602-4e00-ba9c-25a181f7f0df'}),
    (opensuse:OpenSuse {uuid: 'cdabdf0d-5994-43ce-803b-c4686ed16f48'})
MERGE (opensuse13)-[:ISA]->(opensuse);


// SUSE Linux Enterprise Server.
MERGE (sles:Object:Component:Software:OperatingSystem:UnixLike:Linux:Sles {
    uuid: '6cbfc14e-6835-43bc-a29d-aafc8589de4b',
    name: 'Sles',
    desc: 'The SUSE Linux Enterprise Server Operating System.',
    shortName: 'sles',
    longName: 'SUSE Linux Enterprise Server'
    });

MATCH (sles:Sles {uuid: '6cbfc14e-6835-43bc-a29d-aafc8589de4b'}),
    (linux:Linux {uuid: 'a5613527-2f64-4481-83f2-80b0cb427e28'})
MERGE (sles)-[:ISA]->(linux);


// SUSE Linux Enterprise Server 9.
MERGE (sles9:Object:Component:Software:OperatingSystem:UnixLike:Linux:Sles:Sles9 {
    uuid: '019b3997-f28e-4b81-8e5c-b1077fdf86cb',
    name: 'Sles9',
    desc: 'The SUSE Linux Enterprise Server 9.x Operating System.',
    ver: '9.x',
    shortName: 'sles9',
    longName: 'SUSE Linux Enterprise Server 9.x'
    });

MATCH (sles9:Sles9 {uuid: '019b3997-f28e-4b81-8e5c-b1077fdf86cb'}),
    (sles:Sles {uuid: '6cbfc14e-6835-43bc-a29d-aafc8589de4b'})
MERGE (sles9)-[:ISA]->(sles);


// SUSE Linux Enterprise Server 10.
MERGE (sles10:Object:Component:Software:OperatingSystem:UnixLike:Linux:Sles:Sles10 {
    uuid: '3f1076ed-dbe7-4ca6-8138-5a909bef9ce8',
    name: 'Sles10',
    desc: 'The SUSE Linux Enterprise Server 10.x Operating System.',
    ver: '10.x',
    shortName: 'sles10',
    longName: 'SUSE Linux Enterprise Server 10.x'
    });

MATCH (sles10:Sles10 {uuid: '3f1076ed-dbe7-4ca6-8138-5a909bef9ce8'}),
    (sles:Sles {uuid: '6cbfc14e-6835-43bc-a29d-aafc8589de4b'})
MERGE (sles10)-[:ISA]->(sles);


// SUSE Linux Enterprise Server 11.
MERGE (sles11:Object:Component:Software:OperatingSystem:UnixLike:Linux:Sles:Sles11 {
    uuid: 'db95db3d-fe10-448a-9f30-2a05bf06e699',
    name: 'Sles11',
    desc: 'The SUSE Linux Enterprise Server 11.x Operating System.',
    ver: '11.x',
    shortName: 'sles11',
    longName: 'SUSE Linux Enterprise Server 11.x'
    });

MATCH (sles11:Sles11 {uuid: 'db95db3d-fe10-448a-9f30-2a05bf06e699'}),
    (sles:Sles {uuid: '6cbfc14e-6835-43bc-a29d-aafc8589de4b'})
MERGE (sles11)-[:ISA]->(sles);


// SUSE Linux Enterprise Server 12.
MERGE (sles12:Object:Component:Software:OperatingSystem:UnixLike:Linux:Sles:Sles12 {
    uuid: 'e3c6338f-ece3-448e-867b-49a8c16c9974',
    name: 'Sles12',
    desc: 'The SUSE Linux Enterprise Server 12.x Operating System.',
    ver: '12.x',
    shortName: 'sles12',
    longName: 'SUSE Linux Enterprise Server 12.x'
    });

MATCH (sles12:Sles12 {uuid: 'e3c6338f-ece3-448e-867b-49a8c16c9974'}),
    (sles:Sles {uuid: '6cbfc14e-6835-43bc-a29d-aafc8589de4b'})
MERGE (sles12)-[:ISA]->(sles);
