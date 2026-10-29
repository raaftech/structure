"use strict";

const assert = require("node:assert/strict");
const { spawnSync } = require("node:child_process");
const test = require("node:test");
const discoverAddress = require("./address");
const Helper = require("./helper");
const nmapLib = require("./nmapLib");
const dnsType = require("../../../src/main/resources/bundles/dns/dnsname.type.json");

const ip = "192.0.2.10";
const addressUuid = "dd951d13-e4f7-4786-8cb4-781ad0f7041c";
const dnsUuid = "035ec07b-d180-47dd-8fa3-30b8cb92882c";

function setup(t, hosts = [{ ip, hostname: "host.example" }]) {
  const address = {
    name: "An existing address",
    desc: "An IPv4 address.",
    data: { name: ip, desc: "An IPv4 address.", addr: ip, reachable: false },
    meta: {
      uuid: addressUuid,
      relations: {
        INSTANCE_OF: { direction: "outgoing", to: ["f7fbe917-9d49-4df5-95f6-6e132067a424"] },
        HAS: { direction: "outgoing", to: [dnsUuid] },
      },
    },
  };
  const dns = new Helper([dnsType]).createDnsname("old.example");
  dns.meta.uuid = dnsUuid;
  const entities = new Map([
    [addressUuid, address],
    [dnsUuid, dns],
  ]);
  const client = {
    getInstanceByPredicate: t.mock.fn(async (property, value) =>
      structuredClone([...entities.values()].find((entity) => entity.data[property] === value) || {}),
    ),
    createInstance: t.mock.fn(async ({ entities: created }) => {
      for (const entity of created) {
        assert.ok(!entities.has(entity.meta.uuid));
        entities.set(entity.meta.uuid, structuredClone(entity));
      }
      return created.map((entity) => entity.meta.uuid);
    }),
    updateInstance: t.mock.fn(async ({ entities: updated }) => {
      for (const entity of updated) {
        assert.ok(entities.has(entity.meta.uuid));
        entities.set(entity.meta.uuid, structuredClone(entity));
      }
      return updated.map((entity) => entity.meta.uuid);
    }),
  };
  const scan = t.mock.method(nmapLib, "nmapScan", async () => hosts);
  const run = () => discoverAddress(client, new Helper([dnsType]), ip, "-p22,80");
  return { entities, address, dns, client, scan, run };
}

test("address discovery updates its origin and attaches the discovered DNS name", async (t) => {
  const { entities, client, scan, run } = setup(t);
  assert.deepEqual(await run(), [addressUuid]);
  assert.deepEqual(scan.mock.calls[0].arguments, [ip, "-p22,80"]);
  const address = entities.get(addressUuid);
  assert.equal(address.name, "An existing address");
  assert.equal(address.data.reachable, true);
  assert.deepEqual(address.meta.relations.INSTANCE_OF.to, ["f7fbe917-9d49-4df5-95f6-6e132067a424"]);
  const dns = [...entities.values()].find((entity) => entity.data.dnsname === "host.example");
  assert.deepEqual(address.meta.relations.HAS.to, [dnsUuid, dns.meta.uuid]);
  assert.equal(entities.size, 3);
  assert.equal(client.createInstance.mock.callCount(), 1);
  assert.equal(client.updateInstance.mock.callCount(), 1);
});

test("repeated discovery reuses the DNS name and keeps one relation", async (t) => {
  const { entities, client, run } = setup(t);
  await run();
  const first = structuredClone([...entities.values()]);
  await run();
  assert.deepEqual([...entities.values()], first);
  assert.equal(client.createInstance.mock.callCount(), 1);
  assert.equal(client.updateInstance.mock.callCount(), 1);
});

test("an address with no DNS name gets no placeholder DNS node", async (t) => {
  const { entities, client, run } = setup(t, [{ ip }]);
  await run();
  assert.equal(entities.get(addressUuid).data.reachable, true);
  assert.deepEqual(entities.get(addressUuid).meta.relations.HAS.to, [dnsUuid]);
  assert.equal(client.createInstance.mock.callCount(), 0);
});

test("an empty scan marks the existing address unreachable", async (t) => {
  const { entities, address, client, run } = setup(t, []);
  address.data.reachable = true;
  await run();
  assert.equal(entities.get(addressUuid).data.reachable, false);
  assert.equal(entities.size, 2);
  assert.equal(client.createInstance.mock.callCount(), 0);
});

test("an absent scan target is marked unreachable and keeps its relations", async (t) => {
  const { entities, address, client, run } = setup(t, [{ ip: "192.0.2.11", hostname: "other.example" }]);
  address.data.reachable = true;
  await run();
  assert.equal(entities.get(addressUuid).data.reachable, false);
  assert.deepEqual(entities.get(addressUuid).meta.relations, address.meta.relations);
  assert.equal(entities.size, 2);
  assert.equal(client.createInstance.mock.callCount(), 0);
});

test("a missing origin stops discovery before the scan", async (t) => {
  const { entities, client, scan, run } = setup(t);
  entities.delete(addressUuid);
  await assert.rejects(run(), /No IP address found/);
  assert.equal(scan.mock.callCount(), 0);
  assert.equal(client.createInstance.mock.callCount(), 0);
  assert.equal(client.updateInstance.mock.callCount(), 0);
});

test("a scan failure leaves the origin unchanged", async (t) => {
  const { entities, client, scan, run } = setup(t);
  const before = structuredClone([...entities.values()]);
  scan.mock.mockImplementation(async () => {
    throw new Error("Scan failed");
  });
  await assert.rejects(run(), /Scan failed/);
  assert.deepEqual([...entities.values()], before);
  assert.equal(client.createInstance.mock.callCount(), 0);
  assert.equal(client.updateInstance.mock.callCount(), 0);
});

test("a failed DNS creation cannot add a relation to a missing node", async (t) => {
  const { client, run } = setup(t);
  client.createInstance.mock.mockImplementation(async () => "DNS creation failed");
  await assert.rejects(run(), /DNS creation failed/);
  assert.equal(client.updateInstance.mock.callCount(), 0);
});

test("an address update error fails discovery", async (t) => {
  const { client, run } = setup(t, [{ ip }]);
  client.updateInstance.mock.mockImplementation(async () => ["Address update failed"]);
  await assert.rejects(run(), /Address update failed/);
});

test("the CLI requires one nonempty scan target", () => {
  for (const args of [[], ["-i", ip, "-c", "192.0.2.0/24"], ["-i", ""]]) {
    const result = spawnSync(process.execPath, [require.resolve("./main"), ...args], {
      encoding: "utf8",
    });
    assert.equal(result.status, 1, result.stderr);
    assert.match(result.stdout, /Give one CIDR or one IP address/);
  }
});
