"use strict";

const fs = require("node:fs");
const path = require("node:path");

let settings;

const config = () => {
  if (!settings) {
    const directory = process.env.STRUCTURE_STATE_DIR;
    if (!directory || !path.isAbsolute(directory)) {
      throw new Error("STRUCTURE_STATE_DIR must be an absolute path");
    }
    settings = JSON.parse(fs.readFileSync(path.join(directory, "structure.json"), "utf8"));
  }
  return settings;
};

const structureUrl = (endpoint) =>
  "https://" + config()["https-host"] + ":" + config()["https-port"] + "/" + endpoint;

module.exports = { config, structureUrl };
