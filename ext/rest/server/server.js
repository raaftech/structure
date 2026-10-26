"use strict";

const loopback = require("loopback");
const boot = require("loopback-boot");
const path = require("path");
const https = require("https");
const fs = require("fs");
const BusHelper = require(path.join(__dirname, "..", "common", "libraries", "busHelper"));
const settings = require(path.join(__dirname, "..", "common", "libraries", "settings"));

const app = (module.exports = loopback());

const eventBusUrl = settings.structureUrl("eventbus");
app.eventBus = new BusHelper(eventBusUrl);

app.start = function () {
  // The key and certificate of Structure, next to its keystore.
  const config = settings.config();
  const directory = path.dirname(config["jks-file"]);
  const ssl = {
    key: fs.readFileSync(path.join(directory, "structure.key")),
    cert: fs.readFileSync(path.join(directory, "structure.crt")),
  };
  const server = https.createServer(ssl, app);
  server.listen(config["rest-port"], config["rest-host"], function () {
    const baseUrl = "https://" + config["rest-host"] + ":" + config["rest-port"];
    app.emit("started", baseUrl);

    console.log("Using eventbus url: %s", eventBusUrl);
    console.log("Web server listening at: %s", baseUrl);

    if (app.get("loopback-component-explorer")) {
      let explorerPath = app.get("loopback-component-explorer").mountPath;
      console.log("Browse your REST API at %s%s", baseUrl, explorerPath);
    }
  });
  return server;
};

// Bootstrap the application, configure models, datasources and middleware.
// Sub-apps like REST API are mounted via boot scripts.
boot(app, __dirname, function (err) {
  if (err) throw err;

  // start the server if `$ node server.js`
  if (require.main === module) app.start();
});
