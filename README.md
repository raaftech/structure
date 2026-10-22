Structure
=========

Need to explain stuff here.

### Quick notes on how to run
Structure needs a Neo4j server. The tests need a second server: they
delete all its data.

```bash
java -version # Needs to be Java 8.x
git clone git@github.com:raaftech/structure.git
export STRUCTURE_TEST_NEO4J_URI=bolt://localhost:7688 STRUCTURE_TEST_NEO4J_PASSWORD=structure
mvn package
cd src/main/resources/webroot
bower install
cd -
export STRUCTURE_NEO4J_URI=bolt://localhost:7687 STRUCTURE_NEO4J_PASSWORD=structure
java -jar target/structure-*-fat.jar -l DEBUG -u
curl http://localhost:8080/eventbus # Vert.x Socks.js bridge.
```

`STRUCTURE_NEO4J_URI` defaults to `bolt://localhost:7687`, and
`STRUCTURE_NEO4J_USERNAME` and `STRUCTURE_TEST_NEO4J_USERNAME` default
to `neo4j`. Use Neo4j Browser on the server to look at the graph. Neo4j
needs passwords of 8 or more characters.

### Test server
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
STRUCTURE_TEST_NEO4J_URI=bolt://localhost:7688 STRUCTURE_TEST_NEO4J_PASSWORD=structure mvn verify
neo4j stop
```
