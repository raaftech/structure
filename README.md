# Structure

### Building

To build Structure, you need Java 8, Maven, Node.js with npm, and Bower. Maven
builds with the JDK 8 using `~/.m2/toolchains.xml`:

```xml
<toolchains>
  <toolchain>
    <type>jdk</type>
    <provides><version>1.8</version></provides>
    <configuration><jdkHome>/usr/lib/jvm/java-8-openjdk</jdkHome></configuration>
  </toolchain>
</toolchains>
```

Build the jar without the tests, which need a running Neo4j server of their own
(see Testing). The below will result in `target/structure-0.10.0-fat.jar`:

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
"${JAVA_HOME}/bin/java" -jar target/structure-0.10.0-fat.jar --log-level debug --unsafe
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

`mvn verify` runs the tests, and checks that line coverage is 60% or higher.
Tests read the Neo4j settings with the prefix `test.structure`, for example,
`TEST_STRUCTURE_NEO4J_BOLT_URI` or `test.structure.neo4j.bolt.uri`. They use the
same defaults as a normal run. The tests delete all data on their Neo4j server,
so, as a safeguard, they refuse to run if `TEST_STRUCTURE_NEO4J_BOLT_URI` is the
same as the URI of a normal run.

To run the tests next to a server on port 7687, start a second Neo4j from the
same installation with its own home and configuration. Change `/etc/neo4j` to
the `conf` directory of your installation:

```bash
export NEO4J_HOME=/tmp/structure-neo4j NEO4J_CONF=/tmp/structure-neo4j/conf
mkdir -p $NEO4J_CONF
sed -e 's/^#\?server.bolt.listen_address=.*/server.bolt.listen_address=localhost:7688/' \
    -e 's/^#\?server.http.enabled=.*/server.http.enabled=false/' \
    /etc/neo4j/neo4j.conf > $NEO4J_CONF/neo4j.conf
neo4j-admin dbms set-initial-password structure
neo4j start
export TEST_STRUCTURE_NEO4J_BOLT_URI=bolt://localhost:7688
export TEST_STRUCTURE_NEO4J_PASS=structure
mvn verify
neo4j stop
```

### Settings

Each setting has a command line option, an environment variable and a property.
Structure reads the value of a setting from these sources, highest priority
first:

1. the command line option, for example `--neo4j-bolt-uri`;
2. the environment variable, for example `STRUCTURE_NEO4J_BOLT_URI`;
3. the system property, for example `-Dstructure.neo4j.bolt.uri`;
4. the key in a settings file, for example `structure.neo4j.bolt.uri`. Structure
   reads `config.properties` on the class path if it exists, then
   `/etc/structure/config.properties`, then
   `$XDG_CONFIG_HOME/structure/config.properties` (default
   `~/.config/structure/config.properties`). A later file has priority;
5. the default.

| Option             | Environment variable       | Property                   | Default                 | Description                                                      |
| ------------------ | -------------------------- | -------------------------- | ----------------------- | ---------------------------------------------------------------- |
| `--help`           | `STRUCTURE_HELP`           | `structure.help`           |                         | Print usage and exit                                             |
| `--log-level`      | `STRUCTURE_LOG_LEVEL`      | `structure.log.level`      | `info`                  | `info`, `debug` or `trace`; `trace` also prints stack traces     |
| `--unsafe`         | `STRUCTURE_UNSAFE`         | `structure.unsafe`         |                         | Allow all event bus addresses                                    |
| `--bundle-path`    | `STRUCTURE_BUNDLE_PATH`    | `structure.bundle.path`    | see below               | Comma-separated bundle paths, relative to home                   |
| `--home`           | `STRUCTURE_HOME`           | `structure.home`           | see below               | Structure home directory                                         |
| `--state-dir`      | `STRUCTURE_STATE_DIR`      | `structure.state.dir`      | see below               | Directory of the files that Structure writes, relative to home   |
| `--jks-file`       | `STRUCTURE_JKS_FILE`       | `structure.jks.file`       | `structure.jks`         | JKS keystore for HTTPS, relative to `state-dir`; made if missing |
| `--jks-pass`       | `STRUCTURE_JKS_PASS`       | `structure.jks.pass`       | `changeit`              | Keystore password                                                |
| `--neo4j-bolt-uri` | `STRUCTURE_NEO4J_BOLT_URI` | `structure.neo4j.bolt.uri` | `bolt://localhost:7687` | Neo4j Bolt server                                                |
| `--neo4j-http-uri` | `STRUCTURE_NEO4J_HTTP_URI` | `structure.neo4j.http.uri` | `http://localhost:7474` | Neo4j Query API, for the tools in ext                            |
| `--neo4j-user`     | `STRUCTURE_NEO4J_USER`     | `structure.neo4j.user`     | `neo4j`                 | Neo4j user                                                       |
| `--neo4j-pass`     | `STRUCTURE_NEO4J_PASS`     | `structure.neo4j.pass`     | required                | Neo4j password                                                   |
| `--https-host`     | `STRUCTURE_HTTPS_HOST`     | `structure.https.host`     | `localhost`             | HTTPS server host; also used in schema URIs                      |
| `--https-port`     | `STRUCTURE_HTTPS_PORT`     | `structure.https.port`     | `8443`                  | HTTPS server port                                                |
| `--rest-host`      | `STRUCTURE_REST_HOST`      | `structure.rest.host`      | `localhost`             | Host of the REST server in ext/rest                              |
| `--rest-port`      | `STRUCTURE_REST_PORT`      | `structure.rest.port`      | `3443`                  | Port of the REST server in ext/rest                              |
| `--worker-timeout` | `STRUCTURE_WORKER_TIMEOUT` | `structure.worker.timeout` | `300`                   | Maximum worker run time, in seconds                              |
| `--job-pool`       | `STRUCTURE_JOB_POOL`       | `structure.job.pool`       | 4 × CPU cores           | Maximum number of jobs that run at the same time                 |

Structure has two modes. The mode tells Structure where its files are:

- **Development mode**: Structure runs from this repository, for example
  `target/structure-0.10.0-fat.jar`. Home is the repository. The bundles and the
  webroot are in `src/main/resources`. The default of `state-dir` is `target`.
- **Release mode**: Structure runs from an installation, for example
  `/opt/structure/lib/structure.jar`. Home is `/opt/structure`. The bundles and
  the webroot are in home. The default of `state-dir` is
  `$XDG_STATE_HOME/structure`, or `~/.local/state/structure`. A system service
  can set `STRUCTURE_STATE_DIR=/var/lib/structure`.

In `state-dir`, Structure writes the keystore, `structure.json`, the generated
scripts and the file cache of Vert.x, and runs its commands. Structure never
uses the working directory.

Home is always the parent of the directory of the jar. Development mode needs
all of these: the jar is in `target` in home, and home has a `pom.xml` file and
a `src/main/java` directory. Relative paths in settings are relative to home.

`--help` and `--unsafe` are flags. On the command line they have no value. In
the other sources, their value is `true` or `false`. Structure prints the usage
and stops if it does not know an option, or if a setting has no valid value.

At each start, Structure writes the value of each setting to `structure.json` in
`state-dir`. The tools in `ext/repl` and `ext/rest` read their settings from
this file, so start Structure first. Only the owner can read the file, as it has
the passwords.

Since users can see command lines with `ps`, prefer to give passwords in the
environment or in a settings file that only you can read.
