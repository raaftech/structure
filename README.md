# Structure

### Building

To build Structure, you need Java 8, Maven, Node.js with npm, and Bower. Maven
builds with JDK 8 using `~/.m2/toolchains.xml`:

```xml
<toolchains>
  <toolchain>
    <type>jdk</type>
    <provides><version>1.8</version></provides>
    <configuration><jdkHome>/usr/lib/jvm/java-8-openjdk</jdkHome></configuration>
  </toolchain>
</toolchains>
```

Build the jar without the tests (see Testing). The below will result in
`target/structure-*-fat.jar`:

```bash
mvn package -DskipTests
```

Then get the libraries of the web frontend and of the tools in `ext`:

```bash
(cd src/main/resources/webroot && bower install)
for dir in ext/rest ext/repl ext/util/*/; do (cd $dir && npm install); done
```

### Running

Structure needs a Neo4j server, version 5.19 or later for the Query API that the
tools in `ext` use. Neo4j needs passwords of 8 or more characters. Set the
password once, then start Neo4j:

```bash
neo4j-admin dbms set-initial-password structure
neo4j start
```

Start Structure. It starts from any directory, but the commands here run from
the root of the repository. Currently, Structure needs Java 8. If your system
has a later version of Java installed as default, you can simply point
`JAVA_HOME` at an installed JDK 8 and use the `java` binary from that location:

```bash
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk
export STRUCTURE_NEO4J_PASS=structure
"${JAVA_HOME}/bin/java" -jar target/structure-*-fat.jar --log-level debug --unsafe
```

Structure always uses HTTPS. At the first start, it makes the keystore
`structure.jks` in `state-dir`, which is `target` in development mode. The
keystore has a self-signed certificate for `localhost`, `https-host` and
`rest-host`. Structure exports the certificate to `structure.crt` and the key to
`structure.key`, next to the keystore. Clients trust this certificate with
`curl --cacert` or `NODE_EXTRA_CA_CERTS`. To make a new certificate, for example
after you change `https-host` or `rest-host`, delete the keystore. `mvn clean`
also deletes it.

Start the REST server in a second terminal. It listens at `rest-host` and
`rest-port`, with the key and certificate of Structure:

```bash
export NODE_EXTRA_CA_CERTS=$PWD/target/structure.crt
cd ext/rest && npm start
```

Start the REPL in a third terminal, and type `help` for its commands:

```bash
export NODE_EXTRA_CA_CERTS=$PWD/target/structure.crt
cd ext/repl && npm start
```

Structure starts `ext/util/bootstrap` itself, with `NODE_EXTRA_CA_CERTS` set to
its certificate. Some requests to try:

```bash
# The names of all types, from the REST server
curl -s --cacert target/structure.crt https://localhost:3443/api/Entities | jq -r '.[].name'

# One type, by name
curl -s --cacert target/structure.crt https://localhost:3443/api/Entities/name/Linux | jq '{name, desc}'

# The five newest instances
curl -s --cacert target/structure.crt https://localhost:3443/api/Instances/last/5 | jq -r '.[].name'

# The JSON schema of a type, from Structure
curl -s --cacert target/structure.crt https://localhost:8443/schemas/linux.schema.json | jq .

# The number of types in the graph, from the Neo4j Query API
curl -s -u neo4j:structure -H 'Content-Type: application/json' \
  -d '{"statement": "MATCH (t:Type) RETURN count(t)"}' \
  http://localhost:7474/db/neo4j/query/v2 | jq .data.values
```

### Testing

`mvn test` runs the unit tests. `mvn verify` also runs the integration tests,
which are the subclasses of `GraphTest`, and checks that line coverage is 60% or
higher. The integration tests need Docker. They start their own Neo4j server
from the image `neo4j:latest` with Testcontainers, and do not use the server of
a normal run. Testcontainers removes the container when the tests stop.

### Configuration

Each setting has a command line option, an environment variable and a property.
Structure reads the value of a setting from these sources, highest priority
first:

1. the command line option, for example `--neo4j-bolt-uri`;
2. the environment variable, for example `STRUCTURE_NEO4J_BOLT_URI`;
3. the system property, for example `-Dstructure.neo4j.bolt.uri`;
4. the key in a settings file, for example `structure.neo4j.bolt.uri` (see
   Settings files below);
5. the default.

| Option             | Environment variable       | Property                   | Default                 | Description                                                                |
| ------------------ | -------------------------- | -------------------------- | ----------------------- | -------------------------------------------------------------------------- |
| `--help`           | `STRUCTURE_HELP`           | `structure.help`           |                         | Print usage and exit                                                       |
| `--log-level`      | `STRUCTURE_LOG_LEVEL`      | `structure.log.level`      | `info`                  | `info`, `debug` or `trace`; `trace` also prints stack traces               |
| `--unsafe`         | `STRUCTURE_UNSAFE`         | `structure.unsafe`         |                         | Allow all event bus addresses                                              |
| `--bundle-path`    | `STRUCTURE_BUNDLE_PATH`    | `structure.bundle.path`    | see below               | Comma-separated bundle paths, relative to Structure's home                 |
| `--home`           | `STRUCTURE_HOME`           | `structure.home`           | see below               | Structure's home directory                                                 |
| `--state-dir`      | `STRUCTURE_STATE_DIR`      | `structure.state.dir`      | see below               | Directory of the files that Structure writes, relative to Structure's home |
| `--jks-file`       | `STRUCTURE_JKS_FILE`       | `structure.jks.file`       | `structure.jks`         | JKS keystore for HTTPS, relative to `state-dir`; made if missing           |
| `--jks-pass`       | `STRUCTURE_JKS_PASS`       | `structure.jks.pass`       | `changeit`              | Keystore password                                                          |
| `--neo4j-bolt-uri` | `STRUCTURE_NEO4J_BOLT_URI` | `structure.neo4j.bolt.uri` | `bolt://localhost:7687` | Neo4j Bolt server                                                          |
| `--neo4j-http-uri` | `STRUCTURE_NEO4J_HTTP_URI` | `structure.neo4j.http.uri` | `http://localhost:7474` | Neo4j Query API, for the tools in ext                                      |
| `--neo4j-user`     | `STRUCTURE_NEO4J_USER`     | `structure.neo4j.user`     | `neo4j`                 | Neo4j user                                                                 |
| `--neo4j-pass`     | `STRUCTURE_NEO4J_PASS`     | `structure.neo4j.pass`     | required                | Neo4j password                                                             |
| `--https-host`     | `STRUCTURE_HTTPS_HOST`     | `structure.https.host`     | `localhost`             | HTTPS server host; also used in schema URIs                                |
| `--https-port`     | `STRUCTURE_HTTPS_PORT`     | `structure.https.port`     | `8443`                  | HTTPS server port                                                          |
| `--rest-host`      | `STRUCTURE_REST_HOST`      | `structure.rest.host`      | `localhost`             | Host of the REST server in ext/rest                                        |
| `--rest-port`      | `STRUCTURE_REST_PORT`      | `structure.rest.port`      | `3443`                  | Port of the REST server in ext/rest                                        |
| `--worker-timeout` | `STRUCTURE_WORKER_TIMEOUT` | `structure.worker.timeout` | `300`                   | Maximum worker run time, in seconds                                        |
| `--job-pool`       | `STRUCTURE_JOB_POOL`       | `structure.job.pool`       | 4 × CPU cores           | Maximum number of jobs that run at the same time                           |

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
the jar, for all of this section: the mode, the bundles and the webroot, the
settings files and `state-dir`. A settings file can not set Structure's home, as
Structure finds the settings files in Structure's home.

Relative paths in settings are relative to Structure's home. Structure never
uses the current working directory, so you can start it from any directory. The
mode tells Structure where its files are:

|                     | Development mode                         | Release mode                                                                 |
| ------------------- | ---------------------------------------- | ---------------------------------------------------------------------------- |
| Bundles and webroot | `src/main/resources` in Structure's home | Structure's home                                                             |
| Settings files      | `src/main/resources` in Structure's home | `/etc/structure`, `etc` in Structure's home, `$XDG_CONFIG_HOME/structure`    |
| `state-dir`         | `target` in Structure's home             | `/var/lib/structure`, `var` in Structure's home, `$XDG_STATE_HOME/structure` |

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

#### Tools in ext

At each start, Structure writes the value of each setting to `structure.json` in
`state-dir`. The tools in `ext` read their settings from this file, so start
Structure first. A tool finds `state-dir` in this order, and uses the first
match:

1. `STRUCTURE_STATE_DIR`, if it is set, relative to Structure's home;
2. in development mode: `target` in Structure's home;
3. the first of `/var/lib/structure` and `var` in Structure's home that has a
   `structure.json` that the tool can read;
4. `$XDG_STATE_HOME/structure`.

If Structure uses another `state-dir`, set `STRUCTURE_STATE_DIR` for the tools
too. Structure sets it for `ext/util/bootstrap`, which it starts itself. The
REST server keeps its users in `state-dir`, so it must be able to write there.
The REPL keeps its cache in `state-dir` too.

#### Passwords

`structure.json` can have passwords. Thus only its owner can read it, as for
`structure.jks` and `structure.key`. Since users can see command lines with
`ps`, prefer to give passwords in the environment or in a settings file that
only you can read.
