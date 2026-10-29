"use strict";

const assert = require("node:assert/strict");
const { execFile } = require("node:child_process");
const { randomUUID } = require("node:crypto");
const path = require("node:path");
const { promisify } = require("node:util");
const test = require("node:test");
const BusConnector = require("./busConnector");

test("utilities use the real HTTPS bridge and graph", { skip: !process.env.STRUCTURE_TEST_URL }, async (t) => {
  const client = new BusConnector(process.env.STRUCTURE_TEST_URL);
  t.after(() => client.close());
  const types = await client.getAllTypes();
  const ipv4 = types.find((type) => type.name === "Ipv4Address");
  assert.ok(ipv4);
  const uuid = randomUUID();
  const address = {
    name: "Test address", desc: "An address for discovery.",
    meta: { uuid, relations: { INSTANCE_OF: { direction: "outgoing", to: [ipv4.meta.uuid] } } },
    data: { name: "Test address", desc: "An address for discovery.", addr: "192.0.2.10", reachable: false },
  };
  assert.deepEqual(await client.createInstance({ entities: [address] }), [uuid]);
  assert.equal((await client.getInstanceByPredicate("addr", "192.0.2.10")).meta.uuid, uuid);
  assert.deepEqual(await client.getInstanceByPredicate("addr", '" OR true //'), {});
  assert.deepEqual(await client.getInstanceByPredicate('addr`] RETURN n //', "192.0.2.10"), {});
  address.data.reachable = true;
  assert.deepEqual(await client.updateInstance({ entities: [address] }), [uuid]);
  assert.equal((await client.getInstanceByPredicate("addr", "192.0.2.10")).data.reachable, true);
  await assert.rejects(client.request("tech.raaf.structure.bus.repository.instances", "findOneByData", {}), /validate/);
  await assert.rejects(client.createInstance({ entities: [{}] }), /validate/);

  const run = (tool, args, env = {}) => promisify(execFile)(process.execPath, [
    "--require", path.join(__dirname, "fixtures/discovery.js"), path.join(__dirname, tool, "main.js"), ...args,
  ], { env: { ...process.env, ...env }, timeout: 45000 });

  await run("bootstrap", ["-c", "-h", "bootstrap-test.example"]);
  assert.ok((await client.getInstanceByPredicate("dnsname", "bootstrap-test.example")).meta);
  await run("bootstrap", ["-h", "bootstrap-ansible.example"]);
  assert.ok((await client.getInstanceByPredicate("dnsname", "bootstrap-ansible.example")).meta);
  await run("net-discovery", ["-c", "192.0.2.0/24"]);
  const network = await client.getInstanceByPredicate("cidr", "192.0.2.0/24");
  assert.ok(network.meta);
  await run("net-discovery", ["-c", "192.0.2.0/24"]);
  assert.equal((await client.getInstanceByPredicate("cidr", "192.0.2.0/24")).meta.uuid, network.meta.uuid);
  await run("net-discovery", ["-i", "192.0.2.10"]);
  const scanned = await client.getInstanceByPredicate("addr", "192.0.2.10");
  assert.equal(scanned.meta.uuid, uuid);
  assert.equal(scanned.data.reachable, true);
  assert.ok(scanned.meta.relations.HAS.to.length);
  await assert.rejects(run("net-discovery", ["-i", "192.0.2.254"]), /No IP address found/);
  await assert.rejects(run("net-discovery", ["-i", "192.0.2.10"], { STRUCTURE_TEST_SCAN_ERROR: "1" }), /Scan failed/);
  await run("os-discovery", ["-i", "192.0.2.30"]);
  const osAddress = await client.getInstanceByPredicate("addr", "192.0.2.30");
  assert.ok(osAddress.meta);
  await run("os-discovery", ["-i", "192.0.2.30"]);
  assert.equal((await client.getInstanceByPredicate("addr", "192.0.2.30")).meta.uuid, osAddress.meta.uuid);
});
