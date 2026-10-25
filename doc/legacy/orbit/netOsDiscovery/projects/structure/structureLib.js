"use strict";

const R = require("ramda");
const graphLib = require("./graphLib");

const createEntity = (db, type) => {
  let find = graphLib.findTypeByName(db);
  let ent = find(type);
  return graphLib.createInstance(ent, {
    name: ent.name,
    desc: ent.desc,
    ver: "x",
    shortName: R.toLower(ent.name),
    longName: ent.name,
  });
};

const createOS = (db, type) => createEntity(db, type);

const createSshServer = (db) => createEntity(db, "SshServer");

const createSMBServer = (db) => createEntity(db, "SambaServer");

const createInterpreter = (db, type) => createEntity(db, type);

const createPythonInterpreter = (db) => createEntity(db, "Python2x");

const createDns = (db, dnsData) => {
  let find = graphLib.findTypeByName(db);
  let ent = find("DnsName");
  return graphLib.createInstance(ent, {
    name: ent.name,
    desc: ent.desc,
    dnsname: R.defaultTo(dnsData.ip, dnsData.hostname),
  });
};

const createGrouping = (db, name) => {
  let ent = graphLib.findTypeByName(db, "ComponentGroup");
  return graphLib.createInstance(ent, {
    name: name,
    desc: "Grouping of items that make up a computer",
  });
};

const createComputer = (db, osType, ipEntity, dnsData, ssh, smb) => {
  let grouping = createGrouping(db, "group-" + R.defaultTo(dnsData.ip, dnsData.hostname));

  let os = createOS(db, osType);
  let relate = graphLib.relateEntity(os, "HAS", "outgoing");
  let relateTo = R.compose(R.head, relate);
  let computer = [];

  grouping = R.head(graphLib.relateEntity(grouping, "MEMBER_OF", "incoming", os));

  let dns = createDns(db, dnsData);
  os = relateTo(dns);
  os = relateTo(ipEntity);
  computer.push(ipEntity);
  computer.push(dns);

  if (os.name === "OperatingSystem") return [ipEntity, dns, os, grouping];

  let interpreter = "UnixShell";
  if (os.name === "Windows") {
    interpreter = "CmdShell";
  }
  let shell = createInterpreter(db, interpreter);

  //relate shell
  os = relateTo(shell);
  computer.push(shell);

  //relate ssh
  if (ssh) {
    let c = createSshServer(db);
    computer.push(c);
    os = relateTo(c);
  }
  //relate samba
  if (smb) {
    let s = createSMBServer(db);
    computer.push(s);
    os = relateTo(s);
  }

  //TODO this is a workaround for the time being, remove later
  //Python2x software relation
  let py = createPythonInterpreter(db);
  computer.push(py);
  os = relateTo(py);

  computer.push(os);
  computer.push(grouping);
  return computer;
};

module.exports = {
  createComputer: createComputer,
};
