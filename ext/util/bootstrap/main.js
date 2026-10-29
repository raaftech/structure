"use strict";

const cmd = require("command-line-args");
const getUsage = require("command-line-usage");
const ansibleLib = require("./ansibleLib");
const Helper = require("./helper");
const settings = require("./settings");
const BusConnector = require("../busConnector");
const persist = require("../persist");

const optionDefs = [
  { name: "compat", alias: "c", type: Boolean },
  { name: "hostname", alias: "h", type: String },
];
const options = cmd(optionDefs);
const usage = getUsage([
  { header: "Structure bootstrap", content: "Discover the Structure host with Ansible." },
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
    let graph;
    if (options.compat) {
      graph = helper.bootstrapCompatMode(options.hostname);
    } else {
      const facts = await ansibleLib.setup("localhost");
      if (facts.error) throw new Error(JSON.stringify(facts.error));
      graph = helper.bootstrapTransform(facts, "127.0.0.1", "ssh", options.hostname);
    }
    console.log(await persist(client, [...graph.entities, graph.os, graph.root]));
  } finally {
    client.close();
  }
};

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
