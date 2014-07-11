// org.openpcf.model.interpreters-0.0.1


// InterpreterModel 0.0.1 - Main interpreters.
MERGE (opcfintmodel:Object:Model:InterpreterModel {
    uuid: '452269e5-922e-49af-8442-ef3b58348f61',
    name: 'InterpreterModel',
    namespace: 'org.openpcf.model.interpreters',
    version: '0.0.1',
    desc: 'Main interpreter types.'
    });

MATCH (opcfintmodel:InterpreterModel {uuid: '452269e5-922e-49af-8442-ef3b58348f61'}),
    (model:Model {uuid: '4e2dccfa-b5ca-4992-a76e-c390b7d90084'})
MERGE (opcfintmodel)-[:ISA]->(model);

MATCH (opcfintmodel:InterpreterModel {uuid: '452269e5-922e-49af-8442-ef3b58348f61'}),
    (basemodel:BaseModel {uuid: '48c8698b-3afd-44e5-baf7-73f55494cb31'})
MERGE (opcfintmodel)-[:REQUIRES]->(basemodel);


// UNIX Shell.
MERGE (unixshell:Object:Component:Software:Interpreter:UnixShell {
    uuid: '86168e4a-8c99-43c9-ad95-c8f88d96ac32',
    name: 'UnixShell',
    desc: 'A generic UNIX shell.'
    });

MATCH (unixshell:UnixShell {uuid: '86168e4a-8c99-43c9-ad95-c8f88d96ac32'}),
    (interpreter:Interpreter {uuid: 'd047f415-734c-4bd3-92b9-d68d5521ce69'})
MERGE (unixshell)-[:ISA]->(interpreter);


// Bash Shell.
MERGE (bash:Object:Component:Software:Interpreter:UnixShell:Bash {
    uuid: '6d467b28-449f-498c-9ca5-8660f522897f',
    name: 'Bash',
    desc: 'The Bourne Again Shell.'
    });

MATCH (bash:Bash {uuid: '6d467b28-449f-498c-9ca5-8660f522897f'}),
    (unixshell:UnixShell {uuid: '86168e4a-8c99-43c9-ad95-c8f88d96ac32'})
MERGE (bash)-[:ISA]->(unixshell);


// Korn Shell.
MERGE (ksh:Object:Component:Software:Interpreter:UnixShell:Ksh {
    uuid: 'fd95e858-c788-4578-9223-122eceb2e6c6',
    name: 'Ksh',
    desc: 'The Korn Shell.'
    });

MATCH (ksh:Ksh {uuid: 'fd95e858-c788-4578-9223-122eceb2e6c6'}),
    (unixshell:UnixShell {uuid: '86168e4a-8c99-43c9-ad95-c8f88d96ac32'})
MERGE (ksh)-[:ISA]->(unixshell);


// C Shell.
MERGE (csh:Object:Component:Software:Interpreter:UnixShell:Csh {
    uuid: '608c9a23-a194-49b0-a986-c34bd52bf691',
    name: 'Csh',
    desc: 'The C Shell.'
    });

MATCH (csh:Csh {uuid: '608c9a23-a194-49b0-a986-c34bd52bf691'}),
    (unixshell:UnixShell {uuid: '86168e4a-8c99-43c9-ad95-c8f88d96ac32'})
MERGE (csh)-[:ISA]->(unixshell);


// TENEX C Shell.
MERGE (tcsh:Object:Component:Software:Interpreter:UnixShell:Tcsh {
    uuid: 'bb3e3458-391d-4c75-8857-4a08be7e5905',
    name: 'Tcsh',
    desc: 'The TENEX C Shell.'
    });

MATCH (tcsh:Tcsh {uuid: 'bb3e3458-391d-4c75-8857-4a08be7e5905'}),
    (unixshell:UnixShell {uuid: '86168e4a-8c99-43c9-ad95-c8f88d96ac32'})
MERGE (tcsh)-[:ISA]->(unixshell);


// Bourne Shell.
MERGE (sh:Object:Component:Software:Interpreter:UnixShell:Sh {
    uuid: '7c1c3278-6eb4-4be5-8b8d-94204bfd2223',
    name: 'Sh',
    desc: 'The Bourne Shell.'
    });

MATCH (sh:Sh {uuid: '7c1c3278-6eb4-4be5-8b8d-94204bfd2223'}),
    (unixshell:UnixShell {uuid: '86168e4a-8c99-43c9-ad95-c8f88d96ac32'})
MERGE (sh)-[:ISA]->(unixshell);


// Zhong Shao Shell.
MERGE (zsh:Object:Component:Software:Interpreter:UnixShell:Zsh {
    uuid: '7d9afc14-8e12-4c56-8ca6-4c43fd9808ab',
    name: 'Zsh',
    desc: 'The Zhong Shao Shell.'
    });

MATCH (zsh:Zsh {uuid: '7d9afc14-8e12-4c56-8ca6-4c43fd9808ab'}),
    (unixshell:UnixShell {uuid: '86168e4a-8c99-43c9-ad95-c8f88d96ac32'})
MERGE (zsh)-[:ISA]->(unixshell);


// DOS Shell
MERGE (dosshell:Object:Component:Software:Interpreter:DosShell {
    uuid: 'bc101145-dde2-4363-a7ed-7091d2ff2621',
    name: 'DosShell',
    desc: 'A generic DOS shell.'
    });

MATCH (dosshell:DosShell {uuid: 'bc101145-dde2-4363-a7ed-7091d2ff2621'}),
    (interpreter:Interpreter {uuid: 'd047f415-734c-4bd3-92b9-d68d5521ce69'})
MERGE (dosshell)-[:ISA]->(interpreter);
