"use strict";

const assert = require("node:assert/strict");
const test = require("node:test");
const persist = require("./persist");

const entity = (uuid, targets = []) => ({
  meta: { uuid, relations: { HAS: { direction: "outgoing", to: targets } } },
});

test("conflicts redirect relations before dependent entities are created", async () => {
  const leaf = entity("new");
  const parent = entity("parent", ["new", "old"]);
  const network = entity("network", ["new"]);
  const client = {
    async createInstance({ entities: [item] }) {
      if (item === leaf) return JSON.stringify({ code: 409, uuid: "new", conflict: "old" });
      assert.deepEqual(item.meta.relations.HAS.to, ["old"]);
      return [item.meta.uuid];
    },
  };
  assert.deepEqual(await persist(client, [leaf, parent], [network]), ["old", "parent"]);
  assert.deepEqual(network.meta.relations.HAS.to, ["old"]);
});

test("a failed creation stops dependent writes", async () => {
  let calls = 0;
  const client = { async createInstance() { calls++; throw new Error("Write failed"); } };
  await assert.rejects(persist(client, [entity("first"), entity("second")]), /Write failed/);
  assert.equal(calls, 1);
});

test("an empty creation result is a failure", async () => {
  const client = { async createInstance() { return []; } };
  await assert.rejects(persist(client, [entity("first")]), /Could not create entity/);
});
