// The commands of the event bus, from the introspect handler. A command
// is a target and a handler, and options, for example:
// repository.types.findOneByName --name Linux.

import { type ParseArgsOptionsConfig, parseArgs } from "node:util";
import type { Bus } from "./bus.ts";
import type { Request } from "./model/base/request.ts";
import type { Json } from "./model/json.ts";
import type { Class, Handler, Introspection } from "./model/output/introspection.ts";

// A JSON schema document. The model has no type for draft-04 itself.
type Schema = { [key: string]: Json };

// A bus address and the handlers of its actor class.
export interface Target {
  address: string;
  handlers: Class["handlers"];
}

// An option of a handler, with the JSON type of its value or values.
export interface Option {
  type?: Json;
  multiple: boolean;
}

// The request of a command, and the bus address to send it to.
export interface Call {
  address: string;
  request: Request;
}

// A command that is not valid.
export class CommandError extends Error {}

// The start of each bus address of Structure.
const PREFIX = "tech.raaf.structure.bus.";

const STRUCTURE = `${PREFIX}manager.structuremanager`;

// The bus address where actors publish events.
export const EVENTS = `${PREFIX}events`;

// The URL of the bridge of a Structure on this host.
export const BRIDGE = "wss://localhost:8443/eventbus/websocket";

export class Commands {
  // Each target, by its bus address without the prefix, for example
  // repository.types or node.<uuid>.
  readonly targets = new Map<string, Target>();
  readonly #schemas: Introspection["schemas"];

  constructor({ classes, actors, schemas }: Introspection) {
    this.#schemas = schemas;
    for (const actor of Object.values(actors)) {
      for (const address of actor.addresses) {
        this.targets.set(name(address), { address, handlers: classes[actor.class].handlers });
      }
    }
  }

  // Get the commands from the introspect handler of StructureManager.
  static async introspect(bus: Bus): Promise<Commands> {
    const { result } = await bus.request(STRUCTURE, "introspect");
    if (result.error !== undefined) {
      throw new Error(`Introspection failed: ${result.error}`);
    }
    return new Commands(result.data as unknown as Introspection);
  }

  // The target with a name or a bus address, in any case.
  target(text: string): Target {
    const target = this.targets.get(name(text.toLowerCase()));
    if (!target) {
      throw new CommandError(`Unknown target: ${text}`);
    }
    return target;
  }

  // The options of a handler, from the properties of its argument schema.
  options(handler: Handler): Map<string, Option> {
    const [schema, uri] = this.#resolve({ $ref: handler.inputSchemaUri }, handler.inputSchemaUri);
    const properties = isObject(schema.properties) ? schema.properties : {};
    return new Map(
      Object.entries(properties).map(([name, property]) => {
        const [value, base] = this.#resolve(property, uri);
        const array = value.type === "array";
        return [name, { type: array ? this.#resolve(value.items, base)[0].type : value.type, multiple: array }];
      }),
    );
  }

  // The target and the handler name of the first word of a command. A
  // handler name has no dot, thus the last dot comes before it.
  command(word: string): { target: Target; msgh: string } {
    const dot = word.lastIndexOf(".");
    const name = word.slice(0, dot);
    const msgh = word.slice(dot + 1);
    if (dot < 1 || !msgh) {
      throw new CommandError("Give a target and a handler, for example: repository.types.getAll");
    }
    const target = this.target(name);
    const handler = Object.keys(target.handlers).find((key) => key.toLowerCase() === msgh.toLowerCase());
    if (!handler) {
      throw new CommandError(`Unknown handler of ${name}: ${msgh}`);
    }
    return { target, msgh: handler };
  }

  // The call of a command: the first word, and options.
  parse(words: string[]): Call {
    const [word = "", ...rest] = words;
    const { target, msgh: handler } = this.command(word);
    const options = this.options(target.handlers[handler]);
    const config: ParseArgsOptionsConfig = {};
    for (const [key, { type, multiple }] of options) {
      config[key] = { type: type === "boolean" && !multiple ? "boolean" : "string", multiple };
    }
    let values;
    try {
      ({ values } = parseArgs({ args: rest, options: config, allowNegative: true }));
    } catch (error) {
      throw new CommandError((error as Error).message);
    }
    const args: { [key: string]: Json } = {};
    for (const [key, value] of Object.entries(values)) {
      const convert = (item: string | boolean): Json =>
        typeof item === "string" && options.get(key)?.type !== "string" ? json(item) : item;
      args[key] = Array.isArray(value) ? value.map(convert) : convert(value!);
    }
    return { address: target.address, request: { msgh: handler, args } };
  }

  // Send the call of a command, and give the data of the response.
  async call(bus: Bus, words: string[]): Promise<Json> {
    const { address, request } = this.parse(words);
    const { result } = await bus.request(address, request.msgh, request.args);
    if (result.error !== undefined) {
      throw new Error(result.error);
    }
    return result.data ?? null;
  }

  // A schema with each $ref followed, and the URI of its document.
  #resolve(schema: Json | undefined, uri: string): [Schema, string] {
    while (isObject(schema) && typeof schema.$ref === "string") {
      const url = new URL(schema.$ref, uri);
      const fragment = url.hash;
      url.hash = "";
      uri = url.href;
      schema = pointer(this.#schemas[uri], fragment);
    }
    return [isObject(schema) ? schema : {}, uri];
  }
}

// The name of a bus address: the address without the prefix.
function name(address: string): string {
  return address.startsWith(PREFIX) ? address.slice(PREFIX.length) : address;
}

// The value at the JSON pointer of a fragment, for example #/a/b.
function pointer(document: Json | undefined, fragment: string): Json | undefined {
  return decodeURIComponent(fragment)
    .split("/")
    .slice(1)
    .map((key) => key.replaceAll("~1", "/").replaceAll("~0", "~"))
    .reduce<Json | undefined>(
      (value, key) => (typeof value === "object" && value !== null ? (value as Schema)[key] : undefined),
      document,
    );
}

function isObject(value: Json | undefined): value is Schema {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

// Text that is not JSON stays text. Structure checks the types.
function json(text: string): Json {
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}
