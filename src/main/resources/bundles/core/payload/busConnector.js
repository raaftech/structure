"use strict";

const { randomUUID } = require("node:crypto");

const bus = "tech.raaf.structure.bus.";

class BusConnector {
  constructor(url, timeout = 10000) {
    this.timeout = timeout;
    this.pending = new Map();
    this.socket = new WebSocket(url);
    this.socket.binaryType = "arraybuffer";
    this.ready = new Promise((resolve, reject) => {
      this.rejectConnection = reject;
      this.connectionTimer = setTimeout(() => this.fail(new Error("Event bus connection timed out")), timeout);
      this.socket.addEventListener(
        "open",
        () => {
          clearTimeout(this.connectionTimer);
          this.ping = setInterval(() => this.socket.send(JSON.stringify({ type: "ping" })), 5000);
          resolve();
        },
        { once: true },
      );
    });
    this.socket.addEventListener("message", (event) => {
      try {
        const data = typeof event.data === "string" ? event.data : Buffer.from(event.data).toString("utf8");
        const message = JSON.parse(data);
        if (message.type === "err") {
          this.fail(new Error(message.message || message.failureType));
          return;
        }
        const pending = this.pending.get(message.address);
        if (pending) {
          clearTimeout(pending.timer);
          this.pending.delete(message.address);
          pending.resolve(message.body);
        }
      } catch (error) {
        this.fail(error);
      }
    });
    this.socket.addEventListener("error", (event) =>
      this.fail(event.error || new Error("Event bus connection failed")),
    );
    this.socket.addEventListener("close", () => this.fail(new Error("Event bus connection closed")));
  }

  fail(error) {
    clearTimeout(this.connectionTimer);
    clearInterval(this.ping);
    this.rejectConnection(error);
    for (const pending of this.pending.values()) {
      clearTimeout(pending.timer);
      pending.reject(error);
    }
    this.pending.clear();
    this.socket.close();
  }

  close() {
    this.fail(new Error("Event bus client closed"));
  }

  async request(address, msgh, args = {}) {
    await this.ready;
    if (this.socket.readyState !== WebSocket.OPEN) throw new Error("Event bus connection closed");
    const replyAddress = randomUUID();
    const reply = await new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        this.pending.delete(replyAddress);
        reject(new Error("Event bus request timed out: " + address + " " + msgh));
      }, this.timeout);
      this.pending.set(replyAddress, { resolve, reject, timer });
      this.socket.send(JSON.stringify({ type: "send", address, replyAddress, body: { msgh, args } }));
    });
    if (reply.result && Object.hasOwn(reply.result, "error")) {
      throw new Error(typeof reply.result.error === "string" ? reply.result.error : JSON.stringify(reply.result.error));
    }
    if (!reply.result || !Object.hasOwn(reply.result, "data")) throw new Error("Invalid event bus reply");
    return reply.result.data;
  }

  getAllTypes() {
    return this.request(bus + "repository.types", "getAll");
  }

  getInstanceByPredicate(property, value) {
    return this.request(bus + "repository.instances", "findOneByData", { property, value });
  }

  getInstance(uuid) {
    return this.request(bus + "node." + uuid, "json");
  }

  getOperatingSystem(uuid) {
    return this.request(bus + "repository.instances", "findOperatingSystem", { node: uuid });
  }

  specialize(instance, isa) {
    return this.request(bus + "manager.entitymanager", "specialize", { instance, isa });
  }

  createInstance(args) {
    return this.request(bus + "manager.entitymanager", "create", args).catch((error) => {
      // Entity conflicts are JSON strings used to repair relations.
      try {
        if (JSON.parse(error.message).code === 409) return error.message;
      } catch {}
      throw error;
    });
  }

  updateInstance(args) {
    return this.request(bus + "manager.entitymanager", "update", args);
  }
}

module.exports = BusConnector;
