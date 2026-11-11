// The text of response data, errors and events, with colour if the
// stream is a terminal that has colours.

import { styleText } from "node:util";
import type { Json } from "./model/json.ts";

type Format = Parameters<typeof styleText>[0];

// A JSON string, a key, a literal or a number of JSON text.
const TOKEN = /("(?:[^"\\]|\\.)*")(\s*:)?|\b(?:true|false|null)\b|-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?/g;

// True if the stream is a terminal with colours. NO_COLOR stops them.
export function colours(stream: { isTTY?: boolean; hasColors?: () => boolean }): boolean {
  return Boolean(stream.isTTY && stream.hasColors?.());
}

// JSON text, with two spaces for each level, or on one line.
export function json(value: Json, colour: boolean, indent = 2): string {
  const text = JSON.stringify(value, null, indent);
  if (!colour) {
    return text;
  }
  return text.replace(TOKEN, (token, string?: string, colon?: string) => {
    if (string !== undefined) {
      return colon ? style("cyan", string, true) + colon : style("green", string, true);
    }
    return style(/^-?\d/.test(token) ? "yellow" : "magenta", token, true);
  });
}

export function error(message: string, colour: boolean): string {
  return style("red", message, colour);
}

// The name of a handler, for example in a list of completions.
export function handler(name: string, colour: boolean): string {
  return style("green", name, colour);
}

// An event, for example: StructureManager selfDiscovery {"status":"done"}.
export function event(body: Json, colour: boolean): string {
  if (typeof body !== "object" || body === null || Array.isArray(body)) {
    return json(body, colour, 0);
  }
  const { source, event, data } = body;
  return `${style("blue", `${source} ${event}`, colour)} ${json(data ?? null, colour, 0)}`;
}

function style(format: Format, text: string, colour: boolean): string {
  return colour ? styleText(format, text, { validateStream: false }) : text;
}
