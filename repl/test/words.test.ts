import assert from "node:assert/strict";
import { describe, it } from "node:test";
import { CommandError } from "../src/commands.ts";
import { words } from "../src/words.ts";

describe("words", () => {
  it("splits a line at spaces and tabs", () => {
    assert.deepEqual(words("  types.findOneByName \t--name   Linux  "), ["types.findOneByName", "--name", "Linux"]);
    assert.deepEqual(words(""), []);
    assert.deepEqual(words("   "), []);
  });

  it("keeps each character in single quotes", () => {
    assert.deepEqual(words(`--entities '{"name": "a \\ b"}'`), ["--entities", '{"name": "a \\ b"}']);
  });

  it("escapes a double quote and a backslash in double quotes", () => {
    assert.deepEqual(words('"a \\"b\\" \\\\ \\n"'), ['a "b" \\ \\n']);
  });

  it("escapes the next character with a backslash outside quotes", () => {
    assert.deepEqual(words("a\\ b \\'c"), ["a b", "'c"]);
    assert.deepEqual(words("a\\"), ["a\\"]);
  });

  it("joins the quoted parts of a word", () => {
    assert.deepEqual(words(`--name=Lin'ux'"es"`), ["--name=Linuxes"]);
  });

  it("gives an empty word for empty quotes", () => {
    assert.deepEqual(words(`--name '' ""`), ["--name", "", ""]);
  });

  it("gives an error for a quote without an end", () => {
    assert.throws(() => words("--name 'Linux"), new CommandError("The ' quote has no end"));
    assert.throws(() => words('--name "Linux'), new CommandError('The " quote has no end'));
  });
});
