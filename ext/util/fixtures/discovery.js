"use strict";

const path = require("node:path");
const tool = path.basename(path.dirname(process.argv[1]));

// Replace external scans; keep the client, bridge and graph in use.
if (tool === "net-discovery") {
  require("../net-discovery/nmapLib").nmapScan = async () => {
    if (process.env.STRUCTURE_TEST_SCAN_ERROR) throw new Error("Scan failed");
    const index = process.argv.indexOf("-i");
    return [{ ip: index < 0 ? "192.0.2.21" : process.argv[index + 1], hostname: "discovered.example" }];
  };
} else {
  require("../" + tool + "/ansibleLib").setup = async () => ({
    distribution: "Linux",
    distribution_version: "test",
    service_manager: "systemd",
    hostname: "os-discovery-test.example",
    fqdn: "os-discovery-test.example",
  });
}
