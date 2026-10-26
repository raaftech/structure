"use strict";

// The REST server keeps its users in the state-dir of Structure
const path = require("path");
const settings = require(path.join(__dirname, "..", "..", "settings"));

module.exports = {
  users: { file: path.join(settings.config()["state-dir"], "credentials.json") },
};
