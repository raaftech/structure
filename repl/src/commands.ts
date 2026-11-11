// The commands of the event bus, from the introspect handler. A command
// is a target and a handler, and options, for example:
// types.findOneByName --name Linux.

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

const STRUCTURE = "tech.raaf.structure.bus.manager.structuremanager";

export class Commands {
  // Each target, by the shortest unique suffix of its address.
  readonly targets = new Map<string, Target>();
  readonly #schemas: Introspection["schemas"];

  constructor({ classes, actors, schemas }: Introspection) {
    this.#schemas = schemas;
    const targets = Object.values(actors).flatMap((actor) =>
      actor.addresses.map((address) => ({ address, handlers: classes[actor.class].handlers })),
    );
    const counts = new Map<string, number>();
    for (const suffix of targets.flatMap(({ address }) => suffixes(address))) {
      counts.set(suffix, (counts.get(suffix) ?? 0) + 1);
    }
    for (const target of targets) {
      const name = suffixes(target.address).find((suffix) => counts.get(suffix) === 1) ?? target.address;
      this.targets.set(name, target);
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

  // The only target with an address that ends with the name, any case.
  target(name: string): Target {
    const key = name.toLowerCase();
    const matches = [...this.targets].filter(([, { address }]) => address === key || address.endsWith(`.${key}`));
    if (matches.length === 0) {
      throw new CommandError(`Unknown target: ${name}`);
    }
    if (matches.length > 1) {
      throw new CommandError(`Target ${name} is ambiguous: ${matches.map(([short]) => short).join(", ")}`);
    }
    return matches[0][1];
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

  // The call of a command. A handler name has no dot, thus the last dot
  // of the first word comes before the handler.
  parse(words: string[]): Call {
    const [word = "", ...rest] = words;
    const dot = word.lastIndexOf(".");
    const name = word.slice(0, dot);
    const msgh = word.slice(dot + 1);
    if (dot < 1 || !msgh) {
      throw new CommandError("Give a target and a handler, for example: types.getAll");
    }
    const target = this.target(name);
    const handler = Object.keys(target.handlers).find((key) => key.toLowerCase() === msgh.toLowerCase());
    if (!handler) {
      throw new CommandError(`Unknown handler of ${name}: ${msgh}`);
    }
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

// The suffixes of an address, shortest first: c, b.c and a.b.c.
function suffixes(address: string): string[] {
  const parts = address.split(".");
  return parts.map((_, index) => parts.slice(-1 - index).join("."));
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
