import assert from "node:assert/strict";
import { describe, it } from "node:test";
import type { Bus } from "../src/bus.ts";
import { CommandError, Commands } from "../src/commands.ts";
import { ARGS, BUS, handler, INTROSPECTION, RUN, UUID } from "./introspection.ts";

const commands = new Commands(INTROSPECTION);

describe("Commands", () => {
  it("names each target by its bus address without the prefix", () => {
    assert.deepEqual(
      [...commands.targets.keys()].sort(),
      [
        "manager.entitymanager",
        "node.a79ce509",
        "node.b1bc963a",
        `node.${UUID}`,
        "node.type",
        "node.type.types",
        "repository.types",
      ].sort(),
    );
    assert.equal(commands.targets.get("node.type.types")!.address, `${BUS}.node.type.types`);
  });

  it("gives each target the handlers of its class", () => {
    assert.deepEqual(Object.keys(commands.target("node.type").handlers), ["getSubtypes"]);
    assert.deepEqual(Object.keys(commands.target(`node.${UUID}`).handlers), ["getCapabilities"]);
  });

  it("finds a target by its name or its bus address, in any case", () => {
    assert.equal(commands.target("Manager.EntityManager").address, `${BUS}.manager.entitymanager`);
    assert.equal(commands.target(`${BUS}.repository.types`).address, `${BUS}.repository.types`);
  });

  it("gives an error for an unknown target", () => {
    assert.throws(() => commands.target("types"), new CommandError("Unknown target: types"));
    assert.throws(() => commands.target("entitymanager"), new CommandError("Unknown target: entitymanager"));
  });

  it("gives the options of a handler, with the types of the argument schema", () => {
    assert.deepEqual(
      commands.options(handler(RUN)),
      new Map(
        Object.entries({
          name: { type: "string", multiple: false },
          start: { type: "string", multiple: false },
          amount: { type: "integer", multiple: false },
          all: { type: "boolean", multiple: false },
          labels: { type: "string", multiple: true },
          values: { type: undefined, multiple: true },
          entities: { type: "object", multiple: true },
          arguments: { type: "object", multiple: false },
        }),
      ),
    );
  });

  it("gives no options for a schema without properties", () => {
    assert.deepEqual(commands.options(handler(ARGS)), new Map());
  });

  it("makes a call with the address and the request", () => {
    assert.deepEqual(commands.parse(["node.type.GETSUBTYPES"]), {
      address: `${BUS}.node.type`,
      request: { msgh: "getSubtypes", args: {} },
    });
  });

  it("splits a target with dots from its handler at the last dot", () => {
    assert.equal(commands.parse(["repository.types.getAll"]).address, `${BUS}.repository.types`);
    assert.equal(commands.parse([`${BUS}.node.type.getSubtypes`]).address, `${BUS}.node.type`);
  });

  it("converts the options to the types of the argument schema", () => {
    const { args } = commands.parse([
      "manager.entitymanager.run",
      "--name=12",
      "--start",
      "1234",
      "--amount",
      "3",
      "--all",
      "--labels",
      "Type",
      "--labels",
      "true",
      "--values",
      "Type",
      "--values",
      "2",
      "--entities",
      '{"name":"Linux"}',
      "--arguments",
      '{"path":"/"}',
    ]).request;
    assert.deepEqual(args, {
      name: "12",
      start: "1234",
      amount: 3,
      all: true,
      labels: ["Type", "true"],
      values: ["Type", 2],
      entities: [{ name: "Linux" }],
      arguments: { path: "/" },
    });
  });

  it("sets a boolean option to false with --no-", () => {
    assert.deepEqual(commands.parse(["manager.entitymanager.run", "--no-all"]).request.args, { all: false });
  });

  it("gives the text of a value that is not valid JSON to Structure", () => {
    assert.deepEqual(commands.parse(["manager.entitymanager.run", "--amount", "many"]).request.args, { amount: "many" });
  });

  it("gives an error for an unknown handler", () => {
    assert.throws(() => commands.parse(["node.type.read"]), new CommandError("Unknown handler of node.type: read"));
  });

  it("gives an error for an unknown option", () => {
    assert.throws(() => commands.parse(["node.type.getSubtypes", "--all"]), CommandError);
  });

  it("gives an error for a positional argument", () => {
    assert.throws(() => commands.parse(["manager.entitymanager.run", "Linux"]), CommandError);
  });

  it("gives an error without a target and a handler", () => {
    const error = new CommandError("Give a target and a handler, for example: repository.types.getAll");
    for (const words of [[], ["type"], [".getSubtypes"], ["type."]]) {
      assert.throws(() => commands.parse(words), error);
    }
  });

  it("gets the commands from the introspect handler", async () => {
    const requests: string[][] = [];
    const bus = {
      request: async (address: string, msgh: string) => {
        requests.push([address, msgh]);
        return { request: {}, result: { data: INTROSPECTION } };
      },
    } as unknown as Bus;
    const introspected = await Commands.introspect(bus);
    assert.deepEqual(requests, [[`${BUS}.manager.structuremanager`, "introspect"]]);
    assert.deepEqual([...introspected.targets.keys()], [...commands.targets.keys()]);
  });

  it("gives an error when the introspection fails", async () => {
    const bus = { request: async () => ({ request: {}, result: { error: "failed" } }) } as unknown as Bus;
    await assert.rejects(Commands.introspect(bus), new Error("Introspection failed: failed"));
  });
});
