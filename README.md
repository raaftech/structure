# Structure

Structure is a systems management platform in which capabilities emerge from an
environment's components and their relationships. You request a capability, such
as `Start` or `Execute`, but you don't say how. Structure then uses its model to
select and combine the tools needed to perform it, including the full command
construction, transport, and result collection.

A typed Neo4j graph represents both the environment and the knowledge used to
manage it. Operating systems, services, addresses and installed tools share this
model with types, protocols, capabilities and instruments. Each instrument
declares which component provides it, what it applies to and which capability it
gives. Type inheritance and relationship constraints let these definitions apply
across families of systems.

For example, the `Start` capability for a remote service can combine a service
manager, a command interpreter and available command transfer client/server
tools on the two hosts. Structure uses the graph to select these parts and
assemble the steps to transfer and run the command. The `Start` capability can
use different compatible interpreters and transports without a separate
implementation for each combination.

The `Detect` capability updates the graph with discovered components and
relationships, which can make further, new capabilities available. Bundles
supply type definitions, schemas, discovery profiles and instruments as JSON,
together with command templates and scripts. Support for new tools and services
can build on existing definitions and execution mechanisms without changes to
the Java backend.

The following section starts with giving a short explanation about how these
[capabilities](#capabilities) work, and then continues with showing you how to
[build](#building) Structure, how to [run](#running) it, how to do some
[example](#examples) commands, and finally how to run our [tests](#testing). 
We also have an extensive [notes](#notes) section with various details about 
Structure.

## Capabilities

Structure initially defines these capabilities:

| Capability      | Purpose                                                              |
| --------------- | -------------------------------------------------------------------- |
| `State`         | Check the state of an entity.                                        |
| `Start`         | Start an entity.                                                     |
| `Stop`          | Stop an entity.                                                      |
| `Restart`       | Restart an entity.                                                   |
| `Create`        | Create related entities.                                             |
| `Destroy`       | Destroy an entity or its children.                                   |
| `Execute`       | Run commands.                                                        |
| `Detect`        | Detect a component.                                                  |
| `List`          | List child entities.                                                 |
| `Identify`      | Identify details such as a name or version.                          |
| `SendString`    | Send a string over a protocol.                                       |
| `ReceiveString` | Receive a string over a protocol.                                    |
| `SendFile`      | Send a file over a protocol.                                         |
| `ReceiveFile`   | Receive a file over a protocol.                                      |
| `Invoke`        | Invoke a command for execution.                                      |
| `Verify`        | Check whether an entity's type is correct.                           |
| `Show`          | Return an entity's current graph representation.                     |
| `Update`        | Verify and identify an entity, then update its graph representation. |
| `Guess`         | Infer an abstract entity that cannot yet be detected directly.       |

Capabilities can work together to perform an operation. For example, `Execute`
runs a command, and `Invoke` lets an interpreter run a script. To run a command
on a (remote or local) subject, Structure can combine them as follows:

0. Structure executes a query between the source and target systems to figure
   out what they have. What they have gives you a list of capabilities that you
   can run.
1. An `Execute` instrument puts a command you want to run in a template.
2. An `Invoke` instrument, such as `BashInvoke`, wraps the command in a script.
3. A `SendFile` instrument transfers the script, and a `SendString` instrument
   sends a command to run it. The subject must have matching receive
   capabilities for the protocols used.

Other operations, such as `Start` (starting a service, or a virtual machine, or
a container), can use the same interpreter and transport capabilities. Structure
selects compatible instruments from the graph and combines them through its
execution logic.

## Requirements

To build and run Structure, you need:

- [Maven](https://maven.apache.org/download.cgi) to build, and to run tests.
- [Java](https://adoptium.net/) 25 or later, to compile and run the code.
- [Python](https://www.python.org/) on the Structure host, to run bootstrap
  and the network, address and OS discovery instruments.
- [Ansible](https://docs.ansible.com/) on the Structure host, for OS discovery.
  Note that its `setup` module needs Python 2 or Python 3 on remote hosts.
- [Nmap](https://nmap.org/) on the Structure host, for network and address
  scanning and discovery.
- [Docker](https://docs.docker.com/get-docker/) to run the integration tests,
  and (optionally) to run a Neo4j container image.
- [Neo4j](https://neo4j.com/download/) (optional, you can also use Docker). You
  need version 5.19 or later. Used to store the Structure graph.

## Building

Build the jar without the tests (see [Testing](#testing)). The below will result
in `target/structure-*-fat.jar`:

```bash
mvn package -DskipTests
```

## Running

To run Structure, first start Neo4j and then Structure itself.

### Neo4j

Structure needs a Neo4j server, version 5.19 or later; let's start one using
Docker:

```bash
docker run -d --rm --name neo4j -p 7474:7474 -p 7687:7687 \
  -e NEO4J_AUTH=neo4j/structure --tmpfs /data --tmpfs /logs \
  --tmpfs /plugins \--tmpfs /import --tmpfs /var/lib/neo4j/run \
  neo4j:latest
```

### Structure Backend

Specify the Neo4j database password and start Structure:

```bash
export STRUCTURE_NEO4J_PASS=structure
java -jar target/structure-*-fat.jar --unsafe
```

### Structure REPL

Issue [#468](https://github.com/raaftech/structure/issues/468) adds a Java
client for the event bus bridge.

## Examples

Some requests to try:

```bash
# The JSON schema of a type, from Structure
curl -s --cacert target/structure.crt https://localhost:8443/schemas/linux.schema.json | jq .

# The number of types in the graph, from the Neo4j Query API
curl -s -u neo4j:structure -H 'Content-Type: application/json' \
  -d '{"statement": "MATCH (t:Type) RETURN count(t)"}' \
  http://localhost:7474/db/neo4j/query/v2 | jq .data.values
```

## Testing

To run regular unit tests:

```bash
mvn test
```

To run integration tests:

```bash
mvn integration-test
```

To run all of the above, including coverage report:

```bash
mvn verify
```

## Notes

What follows are various notes about various aspects of Structure.

### Bundle loading

The `bundle-dir` setting selects one directory. An empty directory is valid.
Each entity in a `.type.json` or `.inst.json` file must have a `meta.uuid`.
UUIDs must be unique across the bundle directory, including entities in the same file.
The graph stores entity paths relative to this directory, for example
`svcmgr/servicemanager.type.json`.
Use `bundle-dir` in place of the old `bundle-path` setting. A bundle reload
updates stored paths, including when the entity content has no changes.

If a bundle file cannot be read, Structure stops and reports the file name. If
dependencies cannot be resolved, it reports each unsorted entity and its missing
targets. These errors are visible at the default log level.

Bundle loading adds new entities and updates entities whose checksum changed. It
retains graph entities that are absent from disk. Removal of a bundle file does
not delete its entities or their instances from the graph.

### User input

The caller of an instrument can give values for the static arguments that have
`canBeUserSupplied: true`, for example `username` of `OpenSshClientSendString`.
Structure gives the other static arguments, and refuses a task when the caller
gives one of these. Structure does not sanitize the values of the caller, but it
quotes them:

- Templates for a Unix shell give each value through the `sh` helper, and
  templates for `cmd` through the `cmd` helper. Thus each value is one word of
  the command. `BundleConsistencyTest` checks each template in `bundles`.
- The `cmd` helper cannot escape a double quote or a line break. Structure
  refuses a value for `cmd` that has one of these characters.
- Script arguments are quoted two times, because the remote shell of `ssh`
  parses them again.
- A value for a secret file, such as a password, must be one line.

Some values are code on purpose, and are not quoted. A user who can call these
instruments can run each command on the subject:

- `command` of `UnixExecute` and `WindowsExecute`;
- `script` of the invoke instruments, for example `ShInvoke` and
  `CmdShellInvoke`.

`payloadDirectory` and `payloadFileName` are not quoted either. Structure makes
these names, with word characters, dots and dashes only.

Instruments with `subjectIsLocal: false`, for example those of `nmap` and
`openssh`, run on the Structure host, as the user that runs Structure. Other
instruments run on their subject, through a send string instrument such as
`OpenSshClientSendString`.

### HTTPS

Structure always uses HTTPS. At the first start, it makes the keystore
`structure.jks` in `state-dir`, which is `target` in development mode. The
keystore has a self-signed certificate for `localhost` and `https-host`.
Structure exports the certificate to `structure.crt` and the key to
`structure.key`, next to the keystore. Clients trust this certificate with
`curl --cacert`. Python payloads read `ca-cert` from `structure.json`. To make a
new certificate, for example after you change `https-host`, delete the keystore.
`mvn clean` also deletes it.

### Configuration

Each setting has a command line option, an environment variable and a property.
Structure reads the value of a setting from these sources, highest priority
first:

1. the command line option, for example `--neo4j-bolt-uri`;
2. the environment variable, for example `STRUCTURE_NEO4J_BOLT_URI`;
3. the system property, for example `-Dstructure.neo4j.bolt.uri`;
4. the key in a settings file, for example `structure.neo4j.bolt.uri` (see
   [Settings files](#settings-files));
5. the default.

| Option             | Environment variable       | Property                   | Default                             | Description                                                                |
| ------------------ | -------------------------- | -------------------------- | ----------------------------------- | -------------------------------------------------------------------------- |
| `--help`           | `STRUCTURE_HELP`           | `structure.help`           |                                     | Print usage and exit                                                       |
| `--log-level`      | `STRUCTURE_LOG_LEVEL`      | `structure.log.level`      | `info`                              | `info`, `debug` or `trace`; `trace` also prints stack traces               |
| `--unsafe`         | `STRUCTURE_UNSAFE`         | `structure.unsafe`         |                                     | Allow all event bus addresses and log argument values                      |
| `--bundle-dir`     | `STRUCTURE_BUNDLE_DIR`     | `structure.bundle.dir`     | [Home and mode](#home-and-mode)     | Bundle directory, relative to Structure's home                             |
| `--home`           | `STRUCTURE_HOME`           | `structure.home`           | [Home and mode](#home-and-mode)     | Structure's home directory                                                 |
| `--state-dir`      | `STRUCTURE_STATE_DIR`      | `structure.state.dir`      | [State directory](#state-directory) | Directory of the files that Structure writes, relative to Structure's home |
| `--jks-file`       | `STRUCTURE_JKS_FILE`       | `structure.jks.file`       | `structure.jks`                     | JKS keystore for HTTPS, relative to `state-dir`; made if missing           |
| `--jks-pass`       | `STRUCTURE_JKS_PASS`       | `structure.jks.pass`       | `changeit`                          | Keystore password                                                          |
| `--ca-cert`        | `STRUCTURE_CA_CERT`        | `structure.ca.cert`        | `structure.crt` beside `jks-file`   | Certificate trusted by bundle payloads; relative paths use `state-dir`     |
| `--neo4j-bolt-uri` | `STRUCTURE_NEO4J_BOLT_URI` | `structure.neo4j.bolt.uri` | `bolt://localhost:7687`             | Neo4j Bolt server                                                          |
| `--neo4j-http-uri` | `STRUCTURE_NEO4J_HTTP_URI` | `structure.neo4j.http.uri` | `http://localhost:7474`             | Neo4j Query API                                                            |
| `--neo4j-user`     | `STRUCTURE_NEO4J_USER`     | `structure.neo4j.user`     | `neo4j`                             | Neo4j user                                                                 |
| `--neo4j-pass`     | `STRUCTURE_NEO4J_PASS`     | `structure.neo4j.pass`     | required                            | Neo4j password                                                             |
| `--https-host`     | `STRUCTURE_HTTPS_HOST`     | `structure.https.host`     | `localhost`                         | HTTPS server host; also used in schema URIs                                |
| `--https-port`     | `STRUCTURE_HTTPS_PORT`     | `structure.https.port`     | `8443`                              | HTTPS server port                                                          |
| `--worker-timeout` | `STRUCTURE_WORKER_TIMEOUT` | `structure.worker.timeout` | `300`                               | Maximum worker run time, in seconds                                        |
| `--job-pool`       | `STRUCTURE_JOB_POOL`       | `structure.job.pool`       | 4 × CPU cores                       | Maximum number of jobs that run at the same time                           |

`--help` and `--unsafe` are flags. On the command line they have no value. In
the other sources, their value is `true` or `false`. Structure prints the usage
and stops if it does not know an option, or if a setting has no valid value.

#### Home and mode

Structure finds its files in its own home directory, which is a directory two
levels above where the jar file is. Some examples:

| Jar                                          | Structure's home  | Mode        |
| -------------------------------------------- | ----------------- | ----------- |
| `~/src/structure/target/structure-*-fat.jar` | `~/src/structure` | development |
| `/opt/structure/lib/structure.jar`           | `/opt/structure`  | release     |

Structure runs in development mode when it runs from a build in this repository.
It checks all of these to determine if it is running from within a development
environment:

- the jar is in the `target` directory within Structure's home;
- Structure's home has a `pom.xml` file;
- Structure's home has a `src/main/java` directory.

In all other cases, Structure runs in release mode.

You can set Structure's home yourself, with `--home`, `STRUCTURE_HOME` or
`-Dstructure.home`. Structure then uses that directory in place of the one of
the jar, for all of this section: the mode, the bundles, the settings files and
`state-dir`. A settings file can not set Structure's home, as Structure finds
the settings files in Structure's home.

Relative paths in settings are relative to Structure's home. Structure never
uses the current working directory, so you can start it from any directory. The
mode tells Structure where its files are:

|                | Development mode                         | Release mode                                                                 |
| -------------- | ---------------------------------------- | ---------------------------------------------------------------------------- |
| `bundle-dir`   | `src/main/resources/bundles` in Structure's home | `bundles` in Structure's home                                          |
| Settings files | `src/main/resources` in Structure's home | `/etc/structure`, `etc` in Structure's home, `$XDG_CONFIG_HOME/structure`    |
| `state-dir`    | `target` in Structure's home             | `/var/lib/structure`, `var` in Structure's home, `$XDG_STATE_HOME/structure` |

#### Settings files

A settings file is a `config.properties` file with keys as `structure.<name>`,
with a dot for each dash. For example: `structure.https.port=8443`.

Structure reads all the settings files that exist and that it can read, as
layers. A key in a later file has priority over the same key in an earlier file.
The order, lowest priority first:

1. `config.properties` on the class path, if a package adds one. The jar of this
   repository has none.
2. In development mode: `src/main/resources/config.properties` in Structure's
   home. Git ignores this file, and Maven does not put it in the jar.
3. In release mode:
   1. `/etc/structure/config.properties`, for all users of the machine;
   2. `etc/config.properties` in Structure's home, for this installation;
   3. `$XDG_CONFIG_HOME/structure/config.properties`, for one user.

#### State directory

`state-dir` is one directory. In it, Structure writes the keystore,
`structure.crt`, `structure.key`, `structure.json`, the generated scripts and
the file cache of Vert.x. Structure also runs its commands in `state-dir`. The
default:

- In development mode: `target` in Structure's home. `mvn clean` deletes it.
- In release mode, the first of these that exists and that Structure can write
  to:
  1. `/var/lib/structure`, for a system service;
  2. `var` in Structure's home, for this installation;
  3. `$XDG_STATE_HOME/structure`, for one user (created if it does not exist).

Structure does not create `/var/lib/structure` or `var` in Structure's home. An
installer or an administrator usually creates those, hopefully with correct
owner and permission settings.

#### Bundle payloads

At each start, Structure writes its settings to `structure.json` in `state-dir`.
Payloads require `STRUCTURE_STATE_DIR` as an absolute path. They do not infer
settings from the source directory. Structure sets this variable when it starts
a process. Python payloads find the shared client in the sibling `core/payload`
directory relative to their own files. The client reads the server address and
certificate trust from `structure.json`, as written by `Configuration`. Set
`ca-cert` through the normal configuration sources to use another certificate
file.

Templates use `bundleFile` to find a payload relative to the instrument JSON
file. A rescan updates these paths, including for bundles outside Structure's
home. The core bundle supplies the shared event bus client and graph helpers in
`payload/structure_client.py`. All payloads use the Python standard library. The
client connects with TLS certificate and host name checks, sends event bus
heartbeats and handles WebSocket messages, control frames and timeouts.

#### Bootstrap and discovery

Bootstrap creates the graph representation of the Structure host. The
`StructureBootstrap` instrument is provided by the existing `Structure` type and
gives `Detect`, the capability that discovers graph components. Before the host
is registered, `selfDiscovery` starts this instrument's payload directly. It
uses the bundle file path and the same process settings as normal instrument
execution. After registration, normal instrument execution is available.

Bootstrap registers a generic OS, the host name, a local IPv4 address and
Structure. It also registers the tools that it finds on the host. It does not
need Ansible. If Ansible is available, self-discovery then runs `OsDiscover`
through the normal instrument path to enrich the same OS and host group.
Existing UUIDs and components remain in the graph.

`Structure` is a `SystemManager`. `Ansible` is a `ConfigurationManager`; both
manager types extend `Software`. The `ansible` bundle provides `OsDiscover`
through `Ansible`. The `os` bundle defines OS types. Network and address
discovery remain in the `nmap` bundle, provided by `Nmap`.

`PythonDiscover` is also provided by `Structure`. It needs Python on its subject
and uses bundle profiles to check files, TCP ports and command output.

Payloads connect directly to `wss://https-host:https-port/eventbus/websocket`.
For manual use from Structure's home, set the runtime paths explicitly:

```bash
export STRUCTURE_STATE_DIR=$PWD/target
python3 src/main/resources/bundles/structure/payload/main.py -h my-host.example
python3 src/main/resources/bundles/nmap/payload/main.py -c 192.0.2.0/24
python3 src/main/resources/bundles/nmap/payload/main.py -i 192.0.2.10
python3 src/main/resources/bundles/ansible/payload/main.py -i 192.0.2.10
```

OS discovery needs Python 3 and Ansible on the Structure host. Remote hosts can
use Python 2 or Python 3 if the installed Ansible version supports it. It uses
Ansible's `setup` module with a local connection for the Structure host and SSH
as root for remote hosts. The `OsDiscover` command passes the Structure host
group UUID with `--self-id` so the payload can select the connection. For manual
local discovery, add `--self-id <uuid>` with the registered Structure host group
UUID. Manual remote discovery can omit this argument. Ansible uses the host's
SSH settings and checks remote host keys. Network discovery scans a CIDR range
with nmap. Address discovery scans one existing IPv4 address. A failed
connection, scan or write causes a nonzero exit status.

#### Passwords

`structure.json` can have passwords. Thus only its owner can read it, as for
`structure.jks` and `structure.key`. Since users can see command lines with
`ps`, prefer to give passwords in the environment or in a settings file that
only you can read.
