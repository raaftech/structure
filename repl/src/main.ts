// Run the command in the arguments, for example
// repository.types.getAll. Without arguments, start a shell. The
// STRUCTURE_URL environment variable sets the URL of the bridge.

import { Bus } from "./bus.ts";
import { BRIDGE, Commands, EVENTS } from "./commands.ts";
import { colours, error, event, json } from "./output.ts";
import { shell } from "./shell.ts";

const url = process.env.STRUCTURE_URL ?? BRIDGE;
const args = process.argv.slice(2);

let bus: Bus | undefined;
try {
  bus = await Bus.connect(url);
  const commands = await Commands.introspect(bus);
  if (args.length === 0) {
    process.exitCode = await shell(bus, commands, process.stdin, process.stdout, process.stderr);
  } else {
    // The data goes to stdout, thus events go to stderr.
    bus.register(EVENTS, (body) => process.stderr.write(`${event(body, colours(process.stderr))}\n`));
    process.stdout.write(`${json(await commands.call(bus, args), colours(process.stdout))}\n`);
  }
} catch (failure) {
  process.stderr.write(`${error((failure as Error).message, colours(process.stderr))}\n`);
  process.exitCode = 1;
} finally {
  bus?.close();
}
