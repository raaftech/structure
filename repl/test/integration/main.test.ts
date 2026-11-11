// Run the REPL against a Structure at STRUCTURE_URL. Without a bridge,
// the tests that need one are skipped. They do not change the graph.

import assert from "node:assert/strict";
import { execFile } from "node:child_process";
import { describe, it } from "node:test";
import { fileURLToPath } from "node:url";
import { Bus } from "../../src/bus.ts";
import { BRIDGE } from "../../src/commands.ts";

const MAIN = fileURLToPath(new URL("../../src/main.ts", import.meta.url));
const url = process.env.STRUCTURE_URL ?? BRIDGE;

// The reason to skip, or false if the bridge is there.
async function reachable(): Promise<string | false> {
  try {
    (await Bus.connect(url, 5000)).close();
    return false;
  } catch (error) {
    return (error as Error).message;
  }
}

// Run the REPL with arguments and input, and give what it writes.
function main(args: string[], input = "", env = process.env): Promise<{ code: number; stdout: string; stderr: string }> {
  return new Promise((resolve) => {
    const child = execFile(process.execPath, [MAIN, ...args], { env, timeout: 30000 }, (error, stdout, stderr) =>
      resolve({ code: error ? Number(error.code) : 0, stdout, stderr }),
    );
    child.stdin!.end(input);
  });
}

describe("REPL without a bridge", () => {
  it("gives an error and exit code 1", async () => {
    const { code, stdout, stderr } = await main(["structuremanager.info"], "", {
      ...process.env,
      STRUCTURE_URL: "wss://localhost:1/eventbus/websocket",
    });
    assert.equal(code, 1);
    assert.equal(stdout, "");
    assert.match(stderr, /^Cannot connect to wss:\/\/localhost:1\/eventbus\/websocket/);
  });
});

describe("REPL with a bridge", { skip: await reachable() }, () => {
  it("runs a command in the arguments, and writes the data as JSON", async () => {
    const { code, stdout } = await main(["structuremanager.info"]);
    assert.equal(code, 0);
    const data = JSON.parse(stdout);
    assert.deepEqual(Object.keys(data), ["structure", "graph", "jvm"]);
    assert.equal(typeof data.structure.bootstrapped, "boolean");
  });

  it("finds a target by a longer suffix of its address", async () => {
    const { code, stdout } = await main(["repository.types.findOneByName", "--name", "Type"]);
    assert.equal(code, 0);
    assert.equal(JSON.parse(stdout).name, "Type");
  });

  it("converts the options to the types of the argument schema", async () => {
    const { code, stdout } = await main(["instances.nLastCreated", "--amount", "1", "--labels", "Instance"]);
    assert.equal(code, 0);
    assert.ok(JSON.parse(stdout).length <= 1);
  });

  it("gives the help of one handler", async () => {
    const { code, stdout } = await main(["types.help", "--name", "getAll"]);
    assert.equal(code, 0);
    assert.deepEqual(Object.keys(JSON.parse(stdout)), ["getAll"]);
  });

  it("gives the error of Structure and exit code 1", async () => {
    const { code, stdout, stderr } = await main(["types.findOneByName"]);
    assert.equal(code, 1);
    assert.equal(stdout, "");
    assert.match(stderr, /^Arguments didn't validate against schema/);
  });

  it("gives the error of a command that is not valid and exit code 1", async () => {
    const { code, stderr } = await main(["nope.getAll"]);
    assert.equal(code, 1);
    assert.equal(stderr, "Unknown target: nope\n");
  });

  it("runs each line of the input in the shell", async () => {
    const input = "structuremanager.version\n\ntypes.findOneByName --name 'Type'\nnope.getAll\n";
    const { code, stdout, stderr } = await main([], input);
    assert.equal(code, 0);
    assert.match(stdout, /"name": "Structure"/);
    assert.match(stdout, /"name": "Type"/);
    assert.doesNotMatch(stdout, /structure> /);
    assert.match(stderr, /^Unknown target: nope$/m);
  });
});
