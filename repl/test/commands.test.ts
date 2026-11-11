import assert from "node:assert/strict";
import { describe, it } from "node:test";
import type { Bus } from "../src/bus.ts";
import { CommandError, Commands } from "../src/commands.ts";
import type { Handler, Introspection } from "../src/model/output/introspection.ts";

const BUS = "tech.raaf.structure.bus";
const ARGS = "resource:/schema/base/args.schema.json";
const RUN = "resource:/schema/manager/run.schema.json";
const COMMON = "resource:/schema/common.schema.json";
const ENTITY = "resource:/schema/entity.schema.json";
const UUID = "4c64d4f7-7e60-4db8-b3c7-d6fc3b0f0221";

function handler(inputSchemaUri: string): Handler {
  return { inputSchemaUri, outputSchemaUri: ARGS, help: { name: "", synopsis: {}, description: "" } };
}

const INTROSPECTION: Introspection = {
  classes: {
    [`${BUS}.manager.entitymanager`]: { handlers: { run: handler(RUN) } },
    [`${BUS}.repository.types`]: { handlers: { getAll: handler(ARGS) } },
    [`${BUS}.typeactor`]: { handlers: { getSubtypes: handler(ARGS) } },
    [`${BUS}.instanceactor`]: { handlers: { getCapabilities: handler(ARGS) } },
  },
  actors: {
    a: { class: `${BUS}.manager.entitymanager`, addresses: [`${BUS}.manager.entitymanager`] },
    b: { class: `${BUS}.repository.types`, addresses: [`${BUS}.repository.types`] },
    c: { class: `${BUS}.typeactor`, addresses: [`${BUS}.node.a79ce509`, `${BUS}.node.type`] },
    d: { class: `${BUS}.typeactor`, addresses: [`${BUS}.node.b1bc963a`, `${BUS}.node.type.types`] },
    e: { class: `${BUS}.instanceactor`, addresses: [`${BUS}.node.${UUID}`] },
  },
  schemas: {
    [ARGS]: { type: "object" },
    [COMMON]: { definitions: { uuid: { type: "string" } } },
    [ENTITY]: { type: "object" },
    [RUN]: {
      type: "object",
      properties: {
        name: { type: "string" },
        start: { $ref: "../common.schema.json#/definitions/uuid" },
        amount: { type: "integer" },
        all: { type: "boolean" },
        labels: { type: "array", items: { type: "string" } },
        values: { type: "array" },
        entities: { type: "array", items: { $ref: "../entity.schema.json" } },
        arguments: { type: "object" },
      },
    },
  },
};

const commands = new Commands(INTROSPECTION);

describe("Commands", () => {
  it("names each target by the shortest unique suffix of its address", () => {
    assert.deepEqual(
      [...commands.targets.keys()].sort(),
      ["a79ce509", "b1bc963a", UUID, "entitymanager", "repository.types", "type", "type.types"].sort(),
    );
    assert.equal(commands.targets.get("type.types")!.address, `${BUS}.node.type.types`);
  });

  it("gives each target the handlers of its class", () => {
    assert.deepEqual(Object.keys(commands.target("type").handlers), ["getSubtypes"]);
    assert.deepEqual(Object.keys(commands.target(UUID).handlers), ["getCapabilities"]);
  });

  it("finds a target by any unique suffix of its address, in any case", () => {
    assert.equal(commands.target("EntityManager").address, `${BUS}.manager.entitymanager`);
    assert.equal(commands.target("node.type.types").address, `${BUS}.node.type.types`);
    assert.equal(commands.target(`${BUS}.repository.types`).address, `${BUS}.repository.types`);
  });

  it("gives an error for an unknown target", () => {
    assert.throws(() => commands.target("nodes"), new CommandError("Unknown target: nodes"));
    assert.throws(() => commands.target("ntitymanager"), new CommandError("Unknown target: ntitymanager"));
  });

  it("gives an error for a suffix of more than one address", () => {
    assert.throws(
      () => commands.target("types"),
      new CommandError("Target types is ambiguous: repository.types, type.types"),
    );
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
    assert.deepEqual(commands.parse(["type.GETSUBTYPES"]), {
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
      "entitymanager.run",
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
    assert.deepEqual(commands.parse(["entitymanager.run", "--no-all"]).request.args, { all: false });
  });

  it("gives the text of a value that is not valid JSON to Structure", () => {
    assert.deepEqual(commands.parse(["entitymanager.run", "--amount", "many"]).request.args, { amount: "many" });
  });

  it("gives an error for an unknown handler", () => {
    assert.throws(() => commands.parse(["type.read"]), new CommandError("Unknown handler of type: read"));
  });

  it("gives an error for an unknown option", () => {
    assert.throws(() => commands.parse(["type.getSubtypes", "--all"]), CommandError);
  });

  it("gives an error for a positional argument", () => {
    assert.throws(() => commands.parse(["entitymanager.run", "Linux"]), CommandError);
  });

  it("gives an error without a target and a handler", () => {
    const error = new CommandError("Give a target and a handler, for example: types.getAll");
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
