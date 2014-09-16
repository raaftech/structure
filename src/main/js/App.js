// Copyright © 2008-2014 RAAF Technology bv

var container = require("vertx/container"),
    console = require("vertx/console");

// Webserver configuration.
var webServerConfig = {
    port: 8080,
    host: "0.0.0.0",
    bridge: true,
    web_root: "web",
    index_page: "html/index.html",
    inbound_permitted: [
        { address_re: 'org.openpcf.neo4vertx.*' },
        { address_re: 'org.openpcf.universal.*' }
    ]
};

// Neo4vertx configuration.
var neo4jConfig = {
    // mode: "default",
    mode: "remote",
    // path: "target/neo4j-db",
    baseAddress: "org.openpcf.neo4vertx.",
    restUrl: "http://localhost:7474/db/data/cypher"
};

// Deploy our modules.
container.deployModule("io.vertx~mod-web-server~2.0.0-final", webServerConfig, 1, deploymentCompleteHandler);
container.deployModule("org.openpcf~neo4vertx~1.3.2", neo4jConfig, 1, deploymentCompleteHandler);

// Deploy our verticles.
container.deployVerticle("org.openpcf.universal.actor.SchemaManager", deploymentCompleteHandler);
container.deployVerticle("Frame.js", deploymentCompleteHandler);
container.deployVerticle("Query.js", deploymentCompleteHandler);

// Display deployment status.
function deploymentCompleteHandler(error, deploymentID){
    if (error) {
        console.log("Something went wrong with deployment: ");
        console.log(error);
    } else {
        console.log("Deployment Completed succesfully, deployment ID: " + deploymentID);
    }
}
