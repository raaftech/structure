import assert from "node:assert/strict";
import { describe, it } from "node:test";
import { stripVTControlCharacters } from "node:util";
import { colours, error, event, json } from "../src/output.ts";

const VALUE = { a: "x1 true", n: -1.5e3, b: true, z: null, l: [1, "two"], o: { 'k"q': "v" } };

// The text in each colour code, for example { 36: ['"a"'] }.
function coloured(text: string): { [colour: number]: string[] } {
  const parts: { [colour: number]: string[] } = {};
  for (const [, code, part] of text.matchAll(/\x1b\[(\d+)m(.*?)\x1b\[39m/g)) {
    (parts[Number(code)] ??= []).push(part);
  }
  return parts;
}

describe("output", () => {
  it("gives JSON without colour", () => {
    assert.equal(json(VALUE, false), JSON.stringify(VALUE, null, 2));
    assert.equal(json(VALUE, false, 0), JSON.stringify(VALUE));
  });

  it("gives JSON with colour that is the same text", () => {
    assert.equal(stripVTControlCharacters(json(VALUE, true)), JSON.stringify(VALUE, null, 2));
  });

  it("gives keys, strings, numbers and literals a colour", () => {
    assert.deepEqual(coloured(json(VALUE, true)), {
      36: ['"a"', '"n"', '"b"', '"z"', '"l"', '"o"', '"k\\"q"'],
      32: ['"x1 true"', '"two"', '"v"'],
      33: ["-1500", "1"],
      35: ["true", "null"],
    });
  });

  it("gives an error in red", () => {
    assert.equal(error("boom", false), "boom");
    assert.deepEqual(coloured(error("boom", true)), { 31: ["boom"] });
  });

  it("gives an event on one line, with its source and its name", () => {
    const body = { source: "StructureManager", event: "selfDiscovery", data: { status: "done" } };
    assert.equal(event(body, false), 'StructureManager selfDiscovery {"status":"done"}');
    assert.equal(stripVTControlCharacters(event(body, true)), event(body, false));
    assert.deepEqual(coloured(event(body, true))[34], ["StructureManager selfDiscovery"]);
  });

  it("gives an event that is not an object as JSON", () => {
    assert.equal(event("started", false), '"started"');
  });

  it("uses colour only on a terminal that has colours", () => {
    assert.equal(colours({ isTTY: true, hasColors: () => true }), true);
    assert.equal(colours({ isTTY: true, hasColors: () => false }), false);
    assert.equal(colours({ isTTY: false, hasColors: () => true }), false);
    assert.equal(colours({}), false);
  });
});
