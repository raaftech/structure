# Structure

Need to explain stuff here.

### Quick notes on how to run

Structure needs a Neo4j server. The tests need a second server: they
delete all its data.

```bash
java -version # Needs to be Java 8.x
git clone git@github.com:raaftech/structure.git
export TEST_STRUCTURE_NEO4J_URI=bolt://localhost:7688 TEST_STRUCTURE_NEO4J_PASS=structure
mvn package
cd src/main/resources/webroot
bower install
cd -
export STRUCTURE_NEO4J_PASS=structure
java -jar target/structure-*-fat.jar --log-level debug --unsafe
curl --cacert target/structure.crt https://localhost:8443/eventbus # Vert.x Socks.js bridge.
```

Use Neo4j Browser on the server to look at the graph. Neo4j needs
passwords of 8 or more characters.

### Settings

Each setting has a command line option, an environment variable and a
property. Structure reads the value of a setting from these sources,
highest priority first:

1. the command line option, for example `--neo4j-uri`;
2. the environment variable, for example `STRUCTURE_NEO4J_URI`;
3. the system property, for example `-Dstructure.neo4j.uri`;
4. the key in `config.properties` on the class path, for example
   `structure.neo4j.uri`;
5. the default.

| Option             | Environment variable       | Property                   | Default                       | Description                                                  |
| ------------------ | -------------------------- | -------------------------- | ----------------------------- | ------------------------------------------------------------ |
| `--help`           | `STRUCTURE_HELP`           | `structure.help`           |                               | Print usage and exit                                         |
| `--log-level`      | `STRUCTURE_LOG_LEVEL`      | `structure.log.level`      | `info`                        | `info`, `debug` or `trace`; `trace` also prints stack traces |
| `--unsafe`         | `STRUCTURE_UNSAFE`         | `structure.unsafe`         |                               | Allow all event bus addresses                                |
| `--bundle-path`    | `STRUCTURE_BUNDLE_PATH`    | `structure.bundle.path`    | `src/main/resources/bundles`  | Comma-separated bundle paths                                 |
| `--home`           | `STRUCTURE_HOME`           | `structure.home`           | working directory             | Structure home directory                                     |
| `--jks-file`       | `STRUCTURE_JKS_FILE`       | `structure.jks.file`       | `<home>/target/structure.jks` | JKS keystore for HTTPS, relative to home; made if missing    |
| `--jks-pass`       | `STRUCTURE_JKS_PASS`       | `structure.jks.pass`       | `changeit`                    | Keystore password                                            |
| `--neo4j-uri`      | `STRUCTURE_NEO4J_URI`      | `structure.neo4j.uri`      | `bolt://localhost:7687`       | Neo4j server                                                 |
| `--neo4j-user`     | `STRUCTURE_NEO4J_USER`     | `structure.neo4j.user`     | `neo4j`                       | Neo4j user                                                   |
| `--neo4j-pass`     | `STRUCTURE_NEO4J_PASS`     | `structure.neo4j.pass`     | required                      | Neo4j password                                               |
| `--https-host`     | `STRUCTURE_HTTPS_HOST`     | `structure.https.host`     | `localhost`                   | HTTPS server host; also used in schema URIs                  |
| `--https-port`     | `STRUCTURE_HTTPS_PORT`     | `structure.https.port`     | `8443`                        | HTTPS server port                                            |
| `--worker-timeout` | `STRUCTURE_WORKER_TIMEOUT` | `structure.worker.timeout` | `300`                         | Maximum worker run time, in seconds                          |
| `--job-pool`       | `STRUCTURE_JOB_POOL`       | `structure.job.pool`       | 4 × CPU cores                 | Maximum number of jobs that run at the same time             |

`--help` and `--unsafe` are flags. On the command line they have no
value. In the other sources, their value is `true` or `false`.
Structure prints the usage and stops if it does not know an option, or
if a setting has no valid value.

Other users can see command lines with `ps`. Thus, give passwords in
the environment or in `config.properties`. Do not commit passwords in
`src/main/resources/config.properties`. To use a `config.properties`
outside the jar, put its directory first on the class path:

```bash
java -cp /etc/structure:target/structure-0.10.0-fat.jar tech.raaf.structure.base.Application
```

### HTTPS

Structure always uses HTTPS, with the keystore in `jks-file`. If the
keystore does not exist, Structure makes it with `keytool`. The
keystore then has a self-signed certificate for `localhost` and
`https-host`. Structure also exports the certificate to `structure.crt`
in the directory of the keystore. Clients can trust this certificate,
for example with `curl --cacert target/structure.crt` or
`NODE_EXTRA_CA_CERTS=target/structure.crt`. Structure uses the same
keystore at each start. To make a new certificate, for example after
you change `https-host`, delete the keystore. `mvn clean` also deletes
it.

### Testing

The tests read the Neo4j settings with the prefix `test.structure`, for
example `TEST_STRUCTURE_NEO4J_URI` or `test.structure.neo4j.uri`. They
use the same defaults as a normal run. The tests delete all data on
their server, so they stop if `TEST_STRUCTURE_NEO4J_URI` is the same as
the URI of a normal run.

To run the tests next to a server on port 7687, start a second Neo4j
from the same installation with its own home and configuration. Change
`/etc/neo4j` to the `conf` directory of your installation.

```bash
export NEO4J_HOME=/tmp/structure-neo4j NEO4J_CONF=/tmp/structure-neo4j/conf
mkdir -p $NEO4J_CONF
sed -e 's/^#\?server.bolt.listen_address=.*/server.bolt.listen_address=localhost:7688/' \
    -e 's/^#\?server.http.enabled=.*/server.http.enabled=false/' \
    /etc/neo4j/neo4j.conf > $NEO4J_CONF/neo4j.conf
neo4j-admin dbms set-initial-password structure
neo4j start
TEST_STRUCTURE_NEO4J_URI=bolt://localhost:7688 TEST_STRUCTURE_NEO4J_PASS=structure mvn verify
neo4j stop
```
