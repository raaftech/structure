"use strict";

/*
  The settings of the running Structure, for the tools in ext. Structure
  writes them at each start to structure.json in its state-dir. Each tool
  in ext has a copy of this file; only the path to home differs.
*/

const fs = require("fs");
const os = require("os");
const path = require("path");

// The home of Structure, the parent of ext.
const home = path.join(__dirname, "..", "..", "..");

// True in a Maven source tree, as in Structure.
const isDevEnv = () =>
  fs.existsSync(path.join(home, "pom.xml")) && fs.existsSync(path.join(home, "src", "main", "java"));

/*
  The state-dir of Structure: STRUCTURE_STATE_DIR if it is set, relative
  to home; else target in a source tree; else the first of
  /var/lib/structure and var in home that has a structure.json that we
  can read; else structure in $XDG_STATE_HOME, which is ~/.local/state
  by default.
*/
const stateDir = () => {
  if (process.env.STRUCTURE_STATE_DIR) {
    return path.resolve(home, process.env.STRUCTURE_STATE_DIR);
  }
  if (isDevEnv()) {
    return path.join(home, "target");
  }
  const readable = (directory) => {
    try {
      fs.accessSync(path.join(directory, "structure.json"), fs.constants.R_OK);
      return true;
    } catch (error) {
      return false;
    }
  };
  const found = ["/var/lib/structure", path.join(home, "var")].find(readable);
  return found || path.join(process.env.XDG_STATE_HOME || path.join(os.homedir(), ".local", "state"), "structure");
};

let settings = null;

// The settings, as Structure wrote them at its last start.
const config = () => {
  if (settings === null) {
    const file = path.join(stateDir(), "structure.json");
    try {
      settings = JSON.parse(fs.readFileSync(file, "utf8"));
    } catch (error) {
      throw new Error("Can't read the settings of Structure, start Structure first: " + error.message);
    }
  }
  return settings;
};

/*
  The URL of a path on Structure.
  @param endpoint The path, for example "eventbus".
*/
const structureUrl = (endpoint) => "https://" + config()["https-host"] + ":" + config()["https-port"] + "/" + endpoint;

module.exports = {
  config: config,
  stateDir: stateDir,
  structureUrl: structureUrl,
};
