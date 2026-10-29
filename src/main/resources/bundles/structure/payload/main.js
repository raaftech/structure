"use strict";

const cmd = require("command-line-args");
const getUsage = require("command-line-usage");
const Helper = require("./helper");
const settings = require("settings");
const BusConnector = require("busConnector");
const persist = require("persist");

const optionDefs = [{ name: "hostname", alias: "h", type: String }];
const options = cmd(optionDefs);
const usage = getUsage([
  { header: "Structure bootstrap", content: "Register the Structure host with minimal data." },
  { header: "Options", optionList: optionDefs },
]);

if (!options.hostname) {
  console.error("A hostname is required\n" + usage);
  process.exit(1);
}

const main = async () => {
  const client = new BusConnector(settings.structureUrl("eventbus/websocket").replace(/^http/, "ws"));
  try {
    const helper = new Helper(await client.getAllTypes());
    const graph = helper.bootstrap(options.hostname);
    console.log(await persist(client, [...graph.entities, graph.os, graph.root]));
  } finally {
    client.close();
  }
};

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
