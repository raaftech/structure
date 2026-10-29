# Structure

Structure models computing environments in a Neo4j graph. All knowledge is data:
types for operating systems, services, addresses and nodes, plus instances for
each item in use.

Bundles define types in JSON with various relational links, constraints and
associated schemas. Instruments are instances too, and each one states what it
applies to and which capability it gives. New systems thus mean new bundles,
with no change to the backend. The backend interacts with the graph, runs tasks
and serves results.

The following sections below show you how to [build](#building) Structure, how
to [run](#running) it, some [example](#examples) calls using `curl` and how to
run [tests](#testing). We also have an extensive [notes](#notes) section with
various details about Structure.

## Requirements

To build and run Structure, you need:

- [Java](https://adoptium.net/) 25 or later.
- [Maven](https://maven.apache.org/download.cgi), to build and run tests.
- [Node.js](https://nodejs.org/) 22.4 or later on the Structure host, to run
  bootstrap and the network, address and OS discovery instruments. npm installs
  their JavaScript dependencies.
- [Python](https://www.python.org/) on hosts where `PythonDiscover` runs. This
  instrument uses bundle profiles to identify components through files, TCP
  ports and command output. Its script supports Python 2 and Python 3.
- [Neo4j](https://neo4j.com/download/) server 5.19 or later, to store the graph.
- [Docker](https://docs.docker.com/get-docker/), to run the integration tests.

Node.js and Python are needed only for these instruments. The Java backend,
build and tests do not need them.

## Building

Build the jar without the tests (see [Testing](#testing)). The below will result
in `target/structure-*-fat.jar`:

```bash
mvn package -DskipTests
```

Then install the dependencies of the bundle payloads:

```bash
for bundle in structure nmap os; do
  (cd "src/main/resources/bundles/$bundle/payload" && npm ci)
done
```

## Running

To run Structure, first start Neo4j and then Structure itself.

### Neo4j

Structure needs a Neo4j server, version 5.19 or later. Neo4j needs passwords of
8 or more characters. Set the password once, then start Neo4j:

```bash
neo4j-admin dbms set-initial-password structure
neo4j start
```

### Structure Backend

Specify the Neo4j database password and start Structure:

```bash
export STRUCTURE_NEO4J_PASS=structure
java -jar target/structure-*-fat.jar --log-level debug --unsafe
```

Structure always uses HTTPS. At the first start, it makes the keystore
`structure.jks` in `state-dir`, which is `target` in development mode. The
keystore has a self-signed certificate for `localhost` and `https-host`.
Structure exports the certificate to `structure.crt` and the key to
`structure.key`, next to the keystore. Clients trust this certificate with
`curl --cacert` or `NODE_EXTRA_CA_CERTS`. To make a new certificate, for example
after you change `https-host`, delete the keystore. `mvn clean` also deletes it.

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

`mvn test` runs the unit tests. `mvn verify` also runs the integration tests,
which are the subclasses of `GraphTest`, and checks that line coverage is 60% or
higher. The integration tests need Docker. They start their own Neo4j server
from the image `neo4j:latest` with Testcontainers, and do not use the server of
a normal run. Testcontainers removes the container when the tests stop.

## Notes

What follows are various notes about various aspects of Structure.

### Bundle loading

Each bundle path must be a directory. An empty directory is valid. Each entity
in a `.type.json` or `.inst.json` file must have a `meta.uuid`. UUIDs must be
unique across all configured bundle paths, including entities in the same file.
The loader reads files in overlapping bundle paths once.

If a bundle file cannot be read, Structure stops and reports the file name. If
dependencies cannot be resolved, it reports each unsorted entity and its missing
targets. These errors are visible at the default log level.

Bundle loading adds new entities and updates entities whose checksum changed. It
retains graph entities that are absent from disk. Removal of a bundle file does
not delete its entities or their instances from the graph.

### Command discovery

Send `{"msgh":"discover","args":{}}` to
`tech.raaf.structure.bus.manager.structuremanager` through the event bus bridge.
The reply has `result.data`, an object keyed by deployment ID. Each deployment
has `addresses` and `handlers`. Each handler has `schemaUri`, its argument
`schema` document and `help`, which is null if no help is available. Schema
references are relative to `schemaUri`.

Discovery includes node UUID addresses and type aliases. An actor registers its
metadata when it starts and removes it when it stops. Call discovery again to
refresh the command list. The `list` handler still returns deployment IDs.

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
| `--bundle-path`    | `STRUCTURE_BUNDLE_PATH`    | `structure.bundle.path`    | [Home and mode](#home-and-mode)     | Comma-separated bundle paths, relative to Structure's home                 |
| `--home`           | `STRUCTURE_HOME`           | `structure.home`           | [Home and mode](#home-and-mode)     | Structure's home directory                                                 |
| `--state-dir`      | `STRUCTURE_STATE_DIR`      | `structure.state.dir`      | [State directory](#state-directory) | Directory of the files that Structure writes, relative to Structure's home |
| `--jks-file`       | `STRUCTURE_JKS_FILE`       | `structure.jks.file`       | `structure.jks`                     | JKS keystore for HTTPS, relative to `state-dir`; made if missing           |
| `--jks-pass`       | `STRUCTURE_JKS_PASS`       | `structure.jks.pass`       | `changeit`                          | Keystore password                                                          |
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
| Bundles        | `src/main/resources` in Structure's home | Structure's home                                                             |
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
settings from the source directory. Structure sets this variable, `NODE_PATH`
for the shared client in `core/payload`, and `NODE_EXTRA_CA_CERTS` for the
server certificate when it starts a process.

Templates use `bundleFile` to find a payload relative to the instrument JSON
file. A rescan updates these paths, including for bundles outside Structure's
home. Install each payload's npm dependencies after you place its bundle. The
core bundle supplies the shared event bus client, settings and graph writer.

#### Bootstrap and discovery

Bootstrap creates the graph representation of the Structure host. The
`StructureBootstrap` instrument is provided by the existing `Structure` type and
gives `Detect`, the capability that discovers graph components. Before the host
is registered, `selfDiscovery` starts this instrument's payload directly. It
uses the bundle file path and the same process settings as normal instrument
execution. After registration, normal instrument execution is available.

`PythonDiscover` is also provided by `Structure`. It needs Python on its subject
and uses bundle profiles to check files, TCP ports and command output.

Payloads connect directly to `wss://https-host:https-port/eventbus/websocket`.
For manual use from Structure's home, set the runtime paths explicitly:

```bash
export STRUCTURE_STATE_DIR=$PWD/target
export NODE_PATH=$PWD/src/main/resources/bundles/core/payload
export NODE_EXTRA_CA_CERTS=$STRUCTURE_STATE_DIR/structure.crt
node src/main/resources/bundles/structure/payload/main.js -c -h my-host.example
node src/main/resources/bundles/nmap/payload/main.js -c 192.0.2.0/24
node src/main/resources/bundles/nmap/payload/main.js -i 192.0.2.10
node src/main/resources/bundles/os/payload/main.js -i 192.0.2.10
```

The `-c` option of bootstrap uses compatibility mode without Ansible. OS
discovery runs on the Structure host and uses Ansible to inspect a remote host.
Network discovery scans a CIDR range with nmap. Address discovery scans one
existing IPv4 address. A failed connection, scan or write causes a nonzero exit
status.

#### Passwords

`structure.json` can have passwords. Thus only its owner can read it, as for
`structure.jks` and `structure.key`. Since users can see command lines with
`ps`, prefer to give passwords in the environment or in a settings file that
only you can read.
