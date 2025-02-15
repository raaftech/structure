Structure
=========

Need to explain stuff here.

### Quick notes on how to run
```
export JAVA_HOME="$(/usr/libexec/java_home -v 1.8.0)"
git clone git@github.com:raaftech/structure.git
mvn package
cd src/main/resources/webroot
bower install
cd -
java -jar target/structure-*-fat.jar -l DEBUG -u
curl http://localhost:8080/eventbus # Vert.x Socks.js bridge.
curl http://localhost:7474 # Neo4j web console
```

