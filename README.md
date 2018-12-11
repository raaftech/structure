Structure
=========

Need to explain stuff here.

### Quick notes on how to run
```
git clone git@github.com:raaftech/structure.git
mvn package
cd src/main/resources/webroot
bower install
cd -
java -jar target/structure-*-fat.jar -l DEBUG -u -s 
curl https://localhost:8443/eventbus
```

### Warnings and errors to clean up

```
[WARN] ContextHandler: o.e.j.s.ServletContextHandler@47ffe971{/,null,null} contextPath ends with /
[WARN] ContextHandler: Empty contextPath
[DEBUG] Application: Setting InboundPermitted and OutboundPermitted to any address (".*")
[INFO] Application: Creating indices.
[ERROR] Configuration: bundlePathProperty is empty, defaulting to 'bundles'
```