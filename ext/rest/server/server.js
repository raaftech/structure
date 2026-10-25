"use strict";

const loopback = require("loopback");
const boot = require("loopback-boot");
const path = require("path");
const https = require("https");
const fs = require("fs");
const BusHelper = require(path.join(__dirname, "..", "common", "libraries", "busHelper"));
const helper = require(path.join(__dirname, "..", "common", "libraries", "helper"));

const app = (module.exports = loopback());

const eventBusUrl = helper.structureUrl("eventbus");
app.eventBus = new BusHelper(eventBusUrl);

app.start = function () {
  // The key and certificate of Structure, next to its keystore
  const settings = helper.config();
  const directory = path.dirname(settings["jks-file"]);
  const ssl = {
    key: fs.readFileSync(path.join(directory, "structure.key")),
    cert: fs.readFileSync(path.join(directory, "structure.crt")),
  };
  const server = https.createServer(ssl, app);
  server.listen(settings["rest-port"], settings["rest-host"], function () {
    const baseUrl = "https://" + settings["rest-host"] + ":" + settings["rest-port"];
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
