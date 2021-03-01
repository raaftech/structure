Structure
=========

Need to explain stuff here.

### Quick notes on how to run
```
export JAVA_HOME="$(/usr/libexec/java_home -v 1.8.0)"
git clone git@github.com:raaftech/structure.git
mvn package
cd src/main/resources/webroot
npm install -g bower
bower install
cd -
java -jar target/structure-*-fat.jar -l DEBUG -u -s
curl --insecure https://localhost:8443/eventbus # Vert.x Socks.js bridge.
curl http://localhost:7474 # Neo4j web console
```

