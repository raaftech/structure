"use strict";

const assert = require("node:assert/strict");
const test = require("node:test");
const BusConnector = require("./busConnector");

function setup(t, timeout = 1000, open = true) {
  class Socket extends EventTarget {
    static OPEN = 1;
    readyState = 0;
    sent = [];
    constructor() {
      super();
      if (open) queueMicrotask(() => {
        this.readyState = 1;
        this.dispatchEvent(new Event("open"));
      });
    }
    send(data) { this.sent.push(JSON.parse(data)); }
    close() { this.readyState = 3; }
    reply(data) { this.dispatchEvent(new MessageEvent("message", { data: JSON.stringify(data) })); }
  }
  const original = globalThis.WebSocket;
  globalThis.WebSocket = Socket;
  const client = new BusConnector("wss://localhost/eventbus/websocket", timeout);
  t.after(() => {
    client.close();
    globalThis.WebSocket = original;
  });
  return client;
}

test("concurrent requests get their own replies", async (t) => {
  const client = setup(t);
  const first = client.getAllTypes();
  const second = client.getInstanceByPredicate("addr", "192.0.2.10");
  await client.ready;
  assert.equal(client.socket.sent[0].address, "tech.raaf.structure.bus.repository.types");
  assert.deepEqual(client.socket.sent[1].body, {
    msgh: "findOneByData", args: { property: "addr", value: "192.0.2.10" },
  });
  const [a, b] = client.socket.sent;
  client.socket.reply({ address: b.replyAddress, body: { result: { data: {} } } });
  client.socket.reply({ address: a.replyAddress, body: { result: { data: ["type"] } } });
  assert.deepEqual(await first, ["type"]);
  assert.deepEqual(await second, {});
  assert.equal(client.pending.size, 0);
});

test("server errors reject requests and conflicts remain available", async (t) => {
  const client = setup(t);
  const update = client.updateInstance({ entities: [] });
  const rejected = assert.rejects(update, /Invalid entity/);
  await client.ready;
  client.socket.reply({ address: client.socket.sent[0].replyAddress, body: { result: { error: "Invalid entity" } } });
  await rejected;
  const create = client.createInstance({ entities: [] });
  await Promise.resolve();
  const conflict = '{"code":409,"uuid":"new","conflict":"old"}';
  client.socket.reply({ address: client.socket.sent[1].replyAddress, body: { result: { error: conflict } } });
  assert.equal(await create, conflict);
});

test("bridge errors reject all pending requests", async (t) => {
  const client = setup(t);
  const first = assert.rejects(client.getAllTypes(), /access denied/);
  const second = assert.rejects(client.getInstanceByPredicate("addr", "x"), /access denied/);
  await client.ready;
  client.socket.reply({ type: "err", message: "access denied" });
  await Promise.all([first, second]);
  assert.equal(client.pending.size, 0);
});

test("a disconnect rejects pending requests", async (t) => {
  const client = setup(t);
  const pending = assert.rejects(client.getAllTypes(), /connection closed/);
  await client.ready;
  client.socket.dispatchEvent(new Event("close"));
  await pending;
});

test("requests have a timeout", async (t) => {
  const client = setup(t, 10);
  await assert.rejects(client.getAllTypes(), /request timed out/);
  assert.equal(client.pending.size, 0);
});

test("connections have a timeout", async (t) => {
  const unopened = setup(t, 10, false);
  await assert.rejects(unopened.getAllTypes(), /connection timed out/);
});

test("invalid replies reject requests", async (t) => {
  const client = setup(t);
  const pending = assert.rejects(client.getAllTypes(), /Invalid event bus reply/);
  await client.ready;
  client.socket.reply({ address: client.socket.sent[0].replyAddress, body: {} });
  await pending;
});

test("binary bridge replies are decoded as UTF-8", async (t) => {
  const client = setup(t);
  const pending = client.getAllTypes();
  await client.ready;
  const data = new TextEncoder().encode(JSON.stringify({
    address: client.socket.sent[0].replyAddress, body: { result: { data: ["type"] } },
  })).buffer;
  client.socket.dispatchEvent(new MessageEvent("message", { data }));
  assert.deepEqual(await pending, ["type"]);
});
