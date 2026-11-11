// Run the command in the arguments, for example: types.getAll. The
// STRUCTURE_URL environment variable sets the URL of the bridge.

import { Bus } from "./bus.ts";
import { Commands } from "./commands.ts";
import { colours, error, event, json } from "./output.ts";

const EVENTS = "tech.raaf.structure.bus.events";
const url = process.env.STRUCTURE_URL ?? "wss://localhost:8443/eventbus/websocket";

let bus: Bus | undefined;
try {
  bus = await Bus.connect(url);
  // The data goes to stdout, thus events go to stderr.
  bus.register(EVENTS, (body) => process.stderr.write(`${event(body, colours(process.stderr))}\n`));
  const commands = await Commands.introspect(bus);
  const { address, request } = commands.parse(process.argv.slice(2));
  const { result } = await bus.request(address, request.msgh, request.args);
  if (result.error !== undefined) {
    throw new Error(result.error);
  }
  process.stdout.write(`${json(result.data ?? null, colours(process.stdout))}\n`);
} catch (failure) {
  process.stderr.write(`${error((failure as Error).message, colours(process.stderr))}\n`);
  process.exitCode = 1;
} finally {
  bus?.close();
}
