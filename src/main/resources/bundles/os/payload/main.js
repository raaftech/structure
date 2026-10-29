"use strict";

const cmd = require("command-line-args");
const getUsage = require("command-line-usage");
const ansibleLib = require("./ansibleLib");
const Helper = require("./helper");
const settings = require("settings");
const BusConnector = require("busConnector");
const persist = require("persist");

const optionDefs = [{ name: "ip", alias: "i", type: String }];
const options = cmd(optionDefs);
const usage = getUsage([
  { header: "Ansible OS discovery", content: "Discover an operating system at an IP address." },
  { header: "Options", optionList: optionDefs },
]);

if (!options.ip) {
  console.error("An IP address is required\n" + usage);
  process.exit(1);
}

const main = async () => {
  const client = new BusConnector(settings.structureUrl("eventbus/websocket").replace(/^http/, "ws"));
  try {
    const helper = new Helper(await client.getAllTypes());
    const found = await client.getInstanceByPredicate("addr", options.ip);
    const address = found.meta ? found : helper.ipData(options.ip, true);
    const facts = await ansibleLib.setup(options.ip);
    if (facts.error) throw new Error(JSON.stringify(facts.error));
    const graph = helper.transform(facts, address);
    const entities = found.meta ? graph.entities : [address, ...graph.entities];
    console.log(await persist(client, [...entities, graph.os, graph.root]));
  } finally {
    client.close();
  }
};

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
