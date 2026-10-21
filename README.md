Structure
=========

Need to explain stuff here.

### Quick notes on how to run
Structure needs a Neo4j server. The tests need a second server: they
delete all its data.

```bash
java -version # Needs to be Java 8.x
git clone git@github.com:raaftech/structure.git
export STRUCTURE_TEST_NEO4J_URI=bolt://localhost:7688 STRUCTURE_TEST_NEO4J_PASSWORD=secret
mvn package
cd src/main/resources/webroot
bower install
cd -
export STRUCTURE_NEO4J_URI=bolt://localhost:7687 STRUCTURE_NEO4J_PASSWORD=secret
java -jar target/structure-*-fat.jar -l DEBUG -u
curl http://localhost:8080/eventbus # Vert.x Socks.js bridge.
```

`STRUCTURE_NEO4J_URI` defaults to `bolt://localhost:7687`, and
`STRUCTURE_NEO4J_USERNAME` and `STRUCTURE_TEST_NEO4J_USERNAME` default
to `neo4j`. Use Neo4j Browser on the server to look at the graph.
