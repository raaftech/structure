"use strict";

const graphLib = require("./graphLib");

class Helper {
  constructor(types) {
    this.types = types;
  }

  type(name) {
    const type = this.types.find((item) => item.name === name || item.meta.uuid === name);
    if (!type) throw new Error("Missing type: " + name);
    return type;
  }

  isType(type, name) {
    if (type.name === name || type.meta.uuid === this.type(name).meta.uuid) return true;
    return (type.meta.relations.ISA?.to || []).some((parent) =>
      this.isType(
        this.types.find((item) => item.name === parent || item.meta.uuid === parent),
        name,
      ),
    );
  }

  match(name, base) {
    const types = this.types.filter((type) => this.isType(type, base));
    return name ? graphLib.findTypeByFuzzyNameMatch(types, name) || this.type(base) : this.type(base);
  }

  software(type, version = "") {
    return graphLib.createInstance(type, {
      name: type.name,
      desc: type.desc,
      ver: version,
      shortName: type.name,
      longName: type.name,
    });
  }

  transform(facts, address, local) {
    const os = this.software(
      this.match(facts.distribution, "OperatingSystem"),
      facts.distribution_version || "unknown",
    );
    const root = graphLib.createInstance(this.type("ComponentGroup"), {
      name: facts.fqdn || facts.hostname || facts.nodename || address.data.addr,
      desc: "A discovered host",
    });
    root.name = root.data.name;
    const entities = [
      this.software(this.match(facts.shell?.split("/").pop(), "Interpreter")),
      this.software(this.match(facts.service_manager, "ServiceManager")),
      this.software(this.type("IcmpServer")),
    ];
    if (!local) entities.push(this.software(this.type("SshServer")));
    for (const entity of [address, ...entities]) graphLib.relateEntity(os, "HAS", "outgoing", entity);
    graphLib.relateEntity(os, "MEMBER_OF", "outgoing", root);
    return { entities, os, root };
  }

  ipData(ip) {
    return graphLib.createInstance(this.type("Ipv4Address"), {
      name: ip,
      desc: "An IPv4 address",
      addr: ip,
      reachable: true,
    });
  }
}

module.exports = Helper;
