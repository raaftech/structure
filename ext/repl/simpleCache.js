"use strict";

const R = require("ramda");
const fs = require("fs");
const path = require("path");
const settings = require("./settings");

// The cache is in the state-dir of Structure.
const file = () => path.join(settings.config()["state-dir"], "tui.cache");

class SimpleCache {
  constructor() {
    this.cache = {
      query: {
        address: "",
        cmd: "",
      },
      neo: {
        query: "",
      },
      structure: {
        token: "",
      },
    };
  }

  get(section, field) {
    return R.path([section, field], this.cache);
  }

  set(section, field, value) {
    this.cache = R.assocPath([section, field], value, this.cache);
  }

  init() {
    let self = this;
    let load = R.tryCatch(
      (file) => JSON.parse(fs.readFileSync(file, { encoding: "utf8" })),
      (file) => self.cache,
    );
    this.cache = load(file());
  }

  save() {
    fs.writeFileSync(file(), JSON.stringify(this.cache), { encoding: "utf8" });
  }
}

module.exports = SimpleCache;
