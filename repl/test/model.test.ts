import assert from "node:assert/strict";
import { readdirSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, it } from "node:test";
import { fileURLToPath } from "node:url";
import { generate, read } from "../scripts/model.ts";
import type { Json } from "../src/model/json.ts";

const SCHEMAS = fileURLToPath(new URL("../../src/main/resources/schema", import.meta.url));
const MODEL = fileURLToPath(new URL("../src/model", import.meta.url));

type Schema = { [key: string]: Json };

function model(schemas: { [path: string]: Schema }): Map<string, string> {
  return generate(new Map(Object.entries(schemas)));
}

// The types of one schema, without the header and the imports.
function types(schema: Schema): string {
  return model({ "a.schema.json": schema }).get("a.ts")!.replace(/^(\/\/|import) .*\n/gm, "").trimStart();
}

describe("model", () => {
  it("is the same as the model that the schemas give", () => {
    const committed = readdirSync(MODEL, { recursive: true, encoding: "utf8" })
      .filter((file) => file.endsWith(".ts"))
      .map((file): [string, string] => [file.split("\\").join("/"), readFileSync(join(MODEL, file), "utf8")]);
    assert.deepEqual(new Map(committed.sort()), new Map([...generate(read(SCHEMAS))].sort()), "Run: npm run model");
  });

  it("gives the JSON types", () => {
    assert.equal(types({ type: "string" }), "export type A = string;\n");
    assert.equal(types({ type: "integer" }), "export type A = number;\n");
    assert.equal(types({ type: ["boolean", "null"] }), "export type A = boolean | null;\n");
    assert.equal(types({ enum: ["a", 1] }), 'export type A = "a" | 1;\n');
    assert.equal(types({}), "export type A = Json;\n");
  });

  it("gives the description as a comment", () => {
    assert.equal(types({ description: "A */ B.", type: "string" }), "/** A *\\/ B. */\nexport type A = string;\n");
  });

  it("gives the type of the items of an array", () => {
    assert.equal(types({ type: "array", items: { type: "string" } }), "export type A = string[];\n");
    assert.equal(types({ type: "array", items: { type: ["string", "null"] } }), "export type A = (string | null)[];\n");
    assert.equal(types({ type: "array" }), "export type A = Json[];\n");
  });

  it("gives the properties of an object", () => {
    const schema = {
      type: "object",
      required: ["a"],
      additionalProperties: false,
      properties: { a: { type: "string" }, "b c": { type: "object", properties: { d: { type: "boolean" } } } },
    };
    assert.equal(types(schema), 'export type A = {\n  a: string;\n  "b c"?: {\n    d?: boolean;\n  };\n};\n');
  });

  it("gives an index signature for the other properties of an object", () => {
    assert.equal(types({ type: "object" }), "export type A = { [key: string]: Json };\n");
    assert.equal(types({ type: "object", additionalProperties: false }), "export type A = { [key: string]: never };\n");
    assert.equal(
      types({ additionalProperties: { type: "string" } }),
      "export type A = { [key: string]: string };\n",
    );
    assert.equal(
      types({ patternProperties: { "^a$": { type: "string" }, "^b$": { type: "integer" } } }),
      "export type A = { [key: string]: string | number };\n",
    );
  });

  it("puts each property type in the index type", () => {
    const schema = { required: ["a"], properties: { a: { type: "string" }, b: { type: "integer" } }, patternProperties: { "^c$": { type: "boolean" } } };
    assert.equal(
      types(schema),
      "export type A = {\n  a: string;\n  b?: number;\n  [key: string]: boolean | string | number | undefined;\n};\n",
    );
  });

  it("gives a union of anyOf and oneOf for a schema without a type", () => {
    assert.equal(types({ anyOf: [{ type: "string" }, { type: "integer" }] }), "export type A = string | number;\n");
    assert.equal(types({ oneOf: [{ type: "string" }, { type: "string" }] }), "export type A = string;\n");
  });

  it("ignores anyOf and oneOf of a schema with a type, thus the type is wider", () => {
    assert.equal(types({ type: "string", oneOf: [{ enum: ["a"] }] }), "export type A = string;\n");
  });

  it("ignores keywords that only check values", () => {
    assert.equal(types({ type: "string", pattern: "^a$", maxLength: 2, not: { enum: ["b"] } }), "export type A = string;\n");
  });

  it("gives a type for each definition", () => {
    const schema = { $ref: "#/definitions/b", definitions: { b: { type: "string" }, c: { $ref: "#" } } };
    assert.equal(types(schema), "export type A = B;\n\nexport type B = string;\n\nexport type C = A;\n");
  });

  it("gives no type for a schema with only definitions", () => {
    assert.equal(types({ description: "Common.", definitions: { b: { type: "string" } } }), "export type B = string;\n");
  });

  it("imports the types of other schemas", () => {
    const schemas = {
      "a.schema.json": { type: "array", items: { $ref: "b/c.schema.json#/definitions/d" } },
      "b/c.schema.json": { type: "object", properties: { e: { $ref: "../a.schema.json" } }, definitions: { d: {} } },
    };
    const files = model(schemas);
    assert.match(files.get("a.ts")!, /^import type \* as cSchema from "\.\/b\/c\.ts";\n\nexport type A = cSchema\.D\[\];$/m);
    assert.match(files.get("b/c.ts")!, /^import type \{ Json \} from "\.\.\/json\.ts";\nimport type \* as aSchema from "\.\.\/a\.ts";$/m);
  });

  it("gives each imported module its own alias", () => {
    const schema = { anyOf: [{ $ref: "b/c.schema.json" }, { $ref: "d/c.schema.json" }] };
    const file = model({ "a.schema.json": schema, "b/c.schema.json": {}, "d/c.schema.json": {} }).get("a.ts")!;
    assert.match(file, /^export type A = cSchema\.C \| cSchema2\.C;$/m);
  });

  it("imports Json only when a type uses it", () => {
    const files = model({ "a.schema.json": { type: "string" }, "b.schema.json": {} });
    assert.doesNotMatch(files.get("a.ts")!, /import/);
    assert.match(files.get("b.ts")!, /^import type \{ Json \} from "\.\/json\.ts";$/m);
    assert.match(files.get("json.ts")!, /^export type Json = /m);
  });

  it("gives an error for an unknown keyword", () => {
    assert.throws(() => types({ allOf: [] }), new Error("a.schema.json: unknown keyword allOf"));
  });

  it("gives an error for an unknown $ref", () => {
    assert.throws(() => types({ $ref: "b.schema.json" }), new Error("a.schema.json: unknown $ref b.schema.json"));
    assert.throws(() => types({ $ref: "#/definitions/b" }), new Error("a.schema.json: unknown $ref #/definitions/b"));
    assert.throws(() => types({ $ref: "#/properties/b", properties: { b: {} } }), Error);
  });

  it("gives an error for two types with the same name", () => {
    assert.throws(
      () => types({ type: "string", definitions: { a: { type: "string" } } }),
      new Error("a.schema.json: the type A already exists"),
    );
    assert.throws(
      () => types({ definitions: { "b-c": { type: "string" }, bC: { type: "string" } } }),
      new Error("a.schema.json: the type BC already exists"),
    );
  });
});
