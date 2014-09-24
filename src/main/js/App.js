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
        { address_re: 'org.openpcf.universal.*' }
    ]
};

// Deploy our modules.
container.deployModule("io.vertx~mod-web-server~2.0.0-final", webServerConfig, 1, deploymentCompleteHandler);

// Deploy our verticles.
container.deployVerticle("org.openpcf.universal.actor.SchemaManager", deploymentCompleteHandler);
container.deployVerticle("org.openpcf.universal.actor.QueryManager", deploymentCompleteHandler);
//container.deployVerticle("Frame.js", deploymentCompleteHandler);
//container.deployVerticle("Query.js", deploymentCompleteHandler);

// Display deployment status.
function deploymentCompleteHandler(error, deploymentID){
    if (error) {
        console.log("Something went wrong with deployment: ");
        console.log(error);
    } else {
        console.log("Deployment Completed succesfully, deployment ID: " + deploymentID);
    }
}
