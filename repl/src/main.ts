// Run the command in the arguments, for example: types.getAll. The
// STRUCTURE_URL environment variable sets the URL of the bridge.

import { Bus } from "./bus.ts";
import { Commands } from "./commands.ts";

const url = process.env.STRUCTURE_URL ?? "wss://localhost:8443/eventbus/websocket";

let bus: Bus | undefined;
try {
  bus = await Bus.connect(url);
  const commands = await Commands.introspect(bus);
  const { address, request } = commands.parse(process.argv.slice(2));
  const { result } = await bus.request(address, request.msgh, request.args);
  if (result.error !== undefined) {
    throw new Error(result.error);
  }
  console.log(JSON.stringify(result.data, null, 2));
} catch (error) {
  console.error((error as Error).message);
  process.exitCode = 1;
} finally {
  bus?.close();
}
