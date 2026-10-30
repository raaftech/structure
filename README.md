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
