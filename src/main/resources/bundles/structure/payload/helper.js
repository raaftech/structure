"use strict";

const { spawnSync } = require("node:child_process");
const graphLib = require("./graphLib");

class Helper {
  constructor(types) {
    this.types = types;
  }

  bootstrap(hostname) {
    const make = (name) => {
      const type = this.types.find((item) => item.name === name);
      return graphLib.createInstance(type, {
        name: type.name,
        desc: type.desc,
        ver: "unknown",
        shortName: type.name,
        longName: type.name,
      });
    };
    const root = graphLib.createInstance(
      this.types.find((type) => type.name === "ComponentGroup"),
      { name: hostname, desc: "The Structure host" },
    );
    root.name = hostname;
    const os = make("OperatingSystem");
    const entities = [
      graphLib.createInstance(
        this.types.find((type) => type.name === "DnsName"),
        {
          name: hostname,
          desc: "The Structure host name",
          dnsname: hostname,
        },
      ),
      graphLib.createInstance(
        this.types.find((type) => type.name === "Ipv4Address"),
        {
          name: "127.0.0.1",
          desc: "The local IPv4 address",
          addr: "127.0.0.1",
          reachable: true,
        },
      ),
      make("Structure"),
    ];
    for (const [command, type] of [
      ["sh", "Sh"],
      ["python3", "Python3x"],
      ["python2", "Python2x"],
      ["nmap", "Nmap"],
      ["ansible", "Ansible"],
      ["ssh", "OpenSshClient"],
      ["ping", "Iputils"],
    ]) {
      const result = spawnSync(command, ["--version"], { timeout: 5000 });
      if (!result.error) entities.push(make(type));
    }
    for (const entity of entities) graphLib.relateEntity(os, "HAS", "outgoing", entity);
    graphLib.relateEntity(os, "MEMBER_OF", "outgoing", root);
    return { entities, os, root };
  }
}

module.exports = Helper;
