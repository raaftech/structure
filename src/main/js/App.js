var container = require("vertx/container"),
    console = require("vertx/console");

// webserver configuration
var webServerConfig = {
    port: 8080,
    host: "0.0.0.0",
    bridge: true,
    web_root: "web",
    index_page: "html/index.html",
    inbound_permitted: [
        { address_re: 'neo4j-graph.*' },
        { address: 'frame.component.isa' },
        { address: 'frame.component.has' },
        { address: 'frame.protocol' },
        { address: 'query.run' },
        { address: 'schema.version' }
    ]
};

// neo4vertx configuration
var neo4jConfig = {
    // mode: "default",
    mode: "remote",
    // path: "target/neo4j-db",
    baseAddress: "neo4j-graph",
    restUrl: "http://localhost:7474/db/data/cypher"
};

// deploy mod-web-server
container.deployModule("io.vertx~mod-web-server~2.0.0-final", webServerConfig, 1, deploymentCompleteHandler);

// deploy neo4vertx
container.deployModule("org.openpcf~neo4vertx~1.3.2", neo4jConfig, 1, deploymentCompleteHandler);

// deploy schema verticle
container.deployVerticle("Schema.java", deploymentCompleteHandler)

// deploy frame verticle
container.deployVerticle("Frame.js", deploymentCompleteHandler);

// deploy query verticle
container.deployVerticle("Query.js", deploymentCompleteHandler);

// display deployment status
function deploymentCompleteHandler(error, deploymentID){
    if (error) {
        console.log("Something went wrong with deployment: ");
        console.log(error);
    } else {
        console.log("Deployment Completed succesfully, deployment ID: " + deploymentID);
    }
}
