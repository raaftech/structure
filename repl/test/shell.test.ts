import assert from "node:assert/strict";
import { PassThrough } from "node:stream";
import { describe, it } from "node:test";
import type { Bus } from "../src/bus.ts";
import { Commands, EVENTS } from "../src/commands.ts";
import type { Json } from "../src/model/json.ts";
import { complete, shell } from "../src/shell.ts";
import { BUS, INTROSPECTION, UUID } from "./introspection.ts";

const commands = new Commands(INTROSPECTION);

// A bus that keeps the requests. It replies with the handler name, or
// with an error for the instance.
class FakeBus {
  requests: [string, string, Json][] = [];
  registered: string[] = [];
  stopped = false;
  listener: (body: Json) => void = () => {};
  onerror: (error: Error) => void = () => {};
  onclose: () => void = () => {};

  async request(address: string, msgh: string, args: Json) {
    this.requests.push([address, msgh, args]);
    return { request: {}, result: address.endsWith(UUID) ? { error: "It failed." } : { data: [msgh] } };
  }

  register(address: string, listener: (body: Json) => void) {
    this.registered.push(address);
    this.listener = listener;
    return () => {
      this.stopped = true;
    };
  }
}

// The text that a stream gets.
function collect(stream: PassThrough): () => string {
  let text = "";
  stream.on("data", (chunk) => (text += chunk));
  return () => text;
}

// Run the shell on lines of input that end, without a terminal.
async function run(bus: FakeBus, input: PassThrough) {
  const output = new PassThrough();
  const errors = new PassThrough();
  const out = collect(output);
  const err = collect(errors);
  const code = await shell(bus as unknown as Bus, commands, input, output, errors);
  return { code, output: out(), errors: err() };
}

describe("shell", () => {
  it("runs each line as a command, and writes the data or the error", async () => {
    const bus = new FakeBus();
    const input = new PassThrough();
    input.end("repository.types.getAll\n\n  \nnope.getAll\nentitymanager.run --name 'a b' --all\n");
    const { code, output, errors } = await run(bus, input);
    assert.equal(code, 0);
    assert.deepEqual(bus.requests, [
      [`${BUS}.repository.types`, "getAll", {}],
      [`${BUS}.manager.entitymanager`, "run", { name: "a b", all: true }],
    ]);
    assert.equal(output, `${JSON.stringify(["getAll"], null, 2)}\n${JSON.stringify(["run"], null, 2)}\n`);
    assert.equal(errors, "Unknown target: nope\n");
  });

  it("writes the error of a handler", async () => {
    const bus = new FakeBus();
    const input = new PassThrough();
    input.end(`${UUID}.getCapabilities\n`);
    assert.equal((await run(bus, input)).errors, "It failed.\n");
  });

  it("writes each event, and stops to receive events at the end", async () => {
    const bus = new FakeBus();
    const input = new PassThrough();
    const result = run(bus, input);
    bus.listener({ source: "StructureManager", event: "selfDiscovery", data: { status: "done" } });
    input.end();
    const { output } = await result;
    assert.deepEqual(bus.registered, [EVENTS]);
    assert.equal(output, 'StructureManager selfDiscovery {"status":"done"}\n');
    assert.equal(bus.stopped, true);
  });

  it("writes the errors of the bridge", async () => {
    const bus = new FakeBus();
    const input = new PassThrough();
    const result = run(bus, input);
    bus.onerror(new Error("access_denied"));
    input.end();
    assert.equal((await result).errors, "access_denied\n");
  });

  it("ends with exit code 1 when the connection closes", async () => {
    const bus = new FakeBus();
    const input = new PassThrough();
    const result = run(bus, input);
    bus.onclose();
    const { code, errors } = await result;
    assert.equal(code, 1);
    assert.equal(errors, "The connection is closed\n");
  });
});

describe("complete", () => {
  const completions = (line: string) => complete(commands, line);

  it("completes a target, with a dot", () => {
    assert.deepEqual(completions("en"), [["entitymanager."], "en"]);
    assert.deepEqual(completions("ty"), [["type.", "type.types."], "ty"]);
    assert.deepEqual(completions("4c"), [[`${UUID}.`], "4c"]);
    assert.equal(completions("")[0].length, commands.targets.size);
  });

  it("completes a handler after the dot of a target", () => {
    assert.deepEqual(completions("type."), [["type.getSubtypes"], "type."]);
    assert.deepEqual(completions("TYPE.get"), [["TYPE.getSubtypes"], "TYPE.get"]);
    assert.deepEqual(completions("repository.types.g"), [["repository.types.getAll"], "repository.types.g"]);
  });

  it("completes nothing for an unknown target", () => {
    assert.deepEqual(completions("nope.g"), [[], "nope.g"]);
    assert.deepEqual(completions("nope.getAll --"), [[], "--"]);
  });

  it("completes the options of the handler", () => {
    assert.deepEqual(completions("entitymanager.run --a"), [["--amount", "--all", "--arguments"], "--a"]);
    assert.deepEqual(completions("entitymanager.run --no"), [["--no-all"], "--no"]);
    assert.equal(completions("entitymanager.run ")[0].length, 9);
    assert.equal(completions("entitymanager.run --all ")[0].length, 9);
  });

  it("completes nothing where an option needs a value", () => {
    assert.deepEqual(completions("entitymanager.run --name "), [[], ""]);
    assert.deepEqual(completions("entitymanager.run --name Li"), [[], "Li"]);
  });
});
