import assert from "node:assert/strict";
import { afterEach, beforeEach, describe, it, mock } from "node:test";
import { Bus, BusError } from "../src/bus.ts";
import type { Json } from "../src/model/json.ts";

// A socket that keeps the frames that Bus sends.
class FakeSocket {
  binaryType = "blob";
  sent: { [key: string]: Json }[] = [];
  onmessage: ((event: { data: unknown }) => void) | null = null;
  onclose: (() => void) | null = null;

  send(data: string) {
    this.sent.push(JSON.parse(data));
  }

  close() {
    this.onclose?.();
  }

  // The bridge sends binary frames.
  receive(frame: Json) {
    this.onmessage?.({ data: new TextEncoder().encode(JSON.stringify(frame)).buffer });
  }

  last() {
    return this.sent.at(-1)!;
  }
}

const ADDRESS = "tech.raaf.structure.bus.manager.structuremanager";
const EVENTS = "tech.raaf.structure.bus.events";

describe("Bus", () => {
  let socket: FakeSocket;
  let bus: Bus;

  beforeEach(() => {
    mock.timers.enable({ apis: ["setTimeout", "setInterval"] });
    socket = new FakeSocket();
    bus = new Bus(socket as unknown as WebSocket);
  });

  afterEach(() => {
    bus.close();
    mock.timers.reset();
  });

  it("reads binary frames", () => {
    assert.equal(socket.binaryType, "arraybuffer");
  });

  it("sends a request and gives the response", async () => {
    const response = bus.request(ADDRESS, "introspect", { all: true });
    const frame = socket.last();
    assert.equal(frame.type, "send");
    assert.equal(frame.address, ADDRESS);
    assert.deepEqual(frame.body, { msgh: "introspect", args: { all: true } });
    assert.equal(String(frame.replyAddress).length, 36);
    const body = { request: { msgh: "introspect" }, result: { data: 1 } };
    socket.receive({ type: "rec", address: frame.replyAddress, body });
    assert.deepEqual(await response, body);
  });

  it("matches each response to its request", async () => {
    const first = bus.request(ADDRESS, "first");
    const firstReply = socket.last().replyAddress;
    const second = bus.request(ADDRESS, "second");
    const secondReply = socket.last().replyAddress;
    socket.receive({ type: "rec", address: secondReply, body: { request: {}, result: { data: 2 } } });
    socket.receive({ type: "rec", address: firstReply, body: { request: {}, result: { data: 1 } } });
    assert.equal((await first).result.data, 1);
    assert.equal((await second).result.data, 2);
  });

  it("sends empty args by default", () => {
    bus.request(ADDRESS, "help").catch(() => {});
    assert.deepEqual(socket.last().body, { msgh: "help", args: {} });
  });

  it("gives the error of the bridge for a request", async () => {
    const response = bus.request(ADDRESS, "introspect");
    socket.receive({
      type: "err",
      address: socket.last().replyAddress,
      failureCode: -1,
      failureType: "NO_HANDLERS",
      message: `No handlers for address ${ADDRESS}`,
    });
    await assert.rejects(response, new BusError(`No handlers for address ${ADDRESS}`));
  });

  it("stops to wait after the timeout", async () => {
    const response = bus.request(ADDRESS, "introspect", {}, 1000);
    mock.timers.tick(1000);
    await assert.rejects(response, new BusError(`No response from ${ADDRESS} after 1000 ms`));
  });

  it("ignores a response after the timeout", async () => {
    const response = bus.request(ADDRESS, "introspect", {}, 1000);
    const replyAddress = socket.last().replyAddress;
    mock.timers.tick(1000);
    socket.receive({ type: "rec", address: replyAddress, body: { request: {}, result: {} } });
    await assert.rejects(response, BusError);
  });

  it("gives bridge errors without a request to onerror", () => {
    const errors: BusError[] = [];
    bus.onerror = (error) => errors.push(error);
    socket.receive({ type: "err", body: "access_denied" });
    assert.deepEqual(errors, [new BusError("access_denied")]);
  });

  it("registers once for each address", () => {
    const bodies: Json[] = [];
    const first = bus.register(EVENTS, (body) => bodies.push(["first", body]));
    const second = bus.register(EVENTS, (body) => bodies.push(["second", body]));
    assert.deepEqual(socket.sent, [{ type: "register", address: EVENTS }]);
    socket.receive({ type: "rec", address: EVENTS, body: { event: "reload" } });
    first();
    socket.receive({ type: "rec", address: EVENTS, body: { event: "reload" } });
    assert.equal(socket.sent.length, 1);
    second();
    assert.deepEqual(socket.last(), { type: "unregister", address: EVENTS });
    socket.receive({ type: "rec", address: EVENTS, body: { event: "reload" } });
    assert.deepEqual(bodies, [
      ["first", { event: "reload" }],
      ["second", { event: "reload" }],
      ["second", { event: "reload" }],
    ]);
  });

  it("unregisters only once", () => {
    const stop = bus.register(EVENTS, () => {});
    stop();
    stop();
    assert.equal(socket.sent.filter((frame) => frame.type === "unregister").length, 1);
  });

  it("sends a ping every 5 seconds", () => {
    mock.timers.tick(4999);
    assert.equal(socket.sent.length, 0);
    mock.timers.tick(1);
    assert.deepEqual(socket.sent, [{ type: "ping" }]);
    mock.timers.tick(5000);
    assert.equal(socket.sent.length, 2);
  });

  it("gives an error to waiting requests when the connection closes", async () => {
    let closed = 0;
    bus.onclose = () => closed++;
    const response = bus.request(ADDRESS, "introspect");
    socket.close();
    await assert.rejects(response, new BusError("The connection is closed"));
    await assert.rejects(bus.request(ADDRESS, "introspect"), new BusError("The connection is closed"));
    bus.close();
    assert.equal(closed, 1);
  });

  it("sends nothing after the connection closes", () => {
    bus.close();
    bus.register(EVENTS, () => {});
    mock.timers.tick(10000);
    assert.equal(socket.sent.length, 0);
  });
});
