"use strict";

const graphLib = require("./graphLib");
const nmapLib = require("./nmapLib");

module.exports = async function discoverAddress(client, helper, ip, args = "-p22,80") {
  const address = await client.getInstanceByPredicate("addr", ip);
  if (!address.meta) {
    throw new Error("No IP address found for " + ip);
  }

  const hosts = await nmapLib.nmapScan(ip, args);
  const host = hosts.find((item) => item.ip === ip);
  const reachable = Boolean(host);
  let changed = address.data.reachable !== reachable;
  address.data.reachable = reachable;

  if (host && host.hostname) {
    let dns = await client.getInstanceByPredicate("dnsname", host.hostname);
    if (!dns.meta) {
      dns = helper.createDnsname(host.hostname);
      const created = await client.createInstance({ entities: [dns] });
      if (!Array.isArray(created) || !created.includes(dns.meta.uuid)) {
        throw new Error("Could not create DNS name: " + JSON.stringify(created));
      }
    }
    const relation = address.meta.relations.HAS;
    if (!relation || !relation.to.includes(dns.meta.uuid)) {
      graphLib.relateEntity(address, "HAS", "outgoing", dns);
      changed = true;
    }
  }

  if (changed) {
    const updated = await client.updateInstance({ entities: [address] });
    if (!Array.isArray(updated) || !updated.includes(address.meta.uuid)) {
      throw new Error("Could not update IP address: " + JSON.stringify(updated));
    }
    return updated;
  }
  return [address.meta.uuid];
};
