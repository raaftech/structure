#!/usr/bin/env node
"use strict";

// Shows (-g) or sets (-s <version>) the version in all project files.
// The release.json file gets its version from pom.xml during the build.
const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");
const { parseArgs } = require("util");

const root = path.resolve(__dirname, "../..");
const usage = "Usage: node ext/repl/version.js -g | -s <version>";

// Finds the version in group 2 of re, between groups 1 and 3.
const pattern = (re) => ({
  get: (text) => text.match(re)?.[2],
  set: (text, version) => text.replace(re, (_, pre, _old, post) => pre + version + post),
});

// npm writes lock files as JSON.stringify output with 2 spaces.
const lock = {
  get: (text) => JSON.parse(text).version,
  set: (text, version) => {
    const json = JSON.parse(text);
    json.version = version;
    if (json.packages?.[""]) json.packages[""].version = version;
    return JSON.stringify(json, null, 2) + "\n";
  },
};

const json = pattern(/^(  "version": ")([^"]+)(")/m);

const files = execFileSync("git", ["ls-files", "*package.json", "*package-lock.json"], {
  cwd: root,
  encoding: "utf8",
})
  .split("\n")
  .filter(Boolean);

const targets = [
  ["pom.xml", pattern(/^(  <version>)([^<]+)(<\/version>)$/m)],
  ["doc/legacy/docker/Dockerfile", pattern(/(target\/structure-)(\S+)(-fat\.jar)/)],
  ...files.map((file) => [file, file.endsWith("package-lock.json") ? lock : json]),
];

const read = (file) => fs.readFileSync(path.join(root, file), "utf8");

const show = () => {
  const width = Math.max(...targets.map(([file]) => file.length));
  for (const [file, target] of targets) {
    console.log(file.padEnd(width), target.get(read(file)) ?? "(not found)");
  }
};

let values;
try {
  ({ values } = parseArgs({
    options: { get: { type: "boolean", short: "g" }, set: { type: "string", short: "s" } },
  }));
} catch (e) {
  console.error(`${e.message}\n${usage}`);
  process.exit(1);
}

if (values.set !== undefined) {
  if (!/^\d+\.\d+\.\d+(-[0-9A-Za-z.-]+)?$/.test(values.set)) {
    console.error(`Not a valid version: ${values.set}\n${usage}`);
    process.exit(1);
  }
  for (const [file, target] of targets) {
    fs.writeFileSync(path.join(root, file), target.set(read(file), values.set));
  }
  show();
} else if (values.get) {
  show();
} else {
  console.error(usage);
  process.exit(1);
}
