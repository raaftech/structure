"use strict";

const cmd = require("command-line-args");
const getUsage = require("command-line-usage");
const nmapLib = require("./nmapLib");
const discoverAddress = require("./address");
const Helper = require("./helper");
const settings = require("./settings");
const BusConnector = require("../busConnector");
const persist = require("../persist");

const optionDefs = [
  { name: "cidr", alias: "c", type: String },
  { name: "ip", alias: "i", type: String },
  { name: "net", alias: "n", type: String },
  { name: "args", alias: "a", type: String },
];
const options = cmd(optionDefs);
const usage = getUsage([
  { header: "Structure network discovery", content: "Scan one network or one existing IPv4 address with nmap." },
  { header: "Options", optionList: optionDefs },
]);

if (Boolean(options.cidr) === Boolean(options.ip)) {
  console.log("Give one CIDR or one IP address.");
  console.log(usage);
  process.exit(1);
}

const main = async () => {
  const client = new BusConnector(settings.structureUrl("eventbus/websocket").replace(/^http/, "ws"));
  try {
    const helper = new Helper(await client.getAllTypes());
    const args = options.args === undefined ? "-p22,80" : options.args;
    if (options.ip) return await discoverAddress(client, helper, options.ip, args);

    const found = await client.getInstanceByPredicate("cidr", options.cidr);
    const network = found.meta ? found : helper.createNetwork(options.cidr, options.net);
    const hosts = await nmapLib.nmapScan(options.cidr, args);
    const graph = helper.transform(network, hosts);
    await persist(client, graph.slice(0, -1), [network]);
    return found.meta
      ? await client.updateInstance({ entities: [network] })
      : await persist(client, [network]);
  } finally {
    client.close();
  }
};

main().then((result) => console.log(JSON.stringify(result, null, 2))).catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
