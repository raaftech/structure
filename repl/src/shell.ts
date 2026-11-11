// An interactive shell for the commands of the event bus. Each line is
// a command, as on the command line. Tab completes targets, handlers
// and options. Ctrl-C clears the line, and Ctrl-D ends the shell.

import { clearLine, createInterface, cursorTo } from "node:readline";
import type { Readable, Writable } from "node:stream";
import type { Bus } from "./bus.ts";
import { type Commands, EVENTS } from "./commands.ts";
import { colours, error, event, handler, json } from "./output.ts";
import { words } from "./words.ts";

const PROMPT = "structure> ";

// A stream that can be a terminal.
type Terminal = { isTTY?: boolean; hasColors?: () => boolean; columns?: number };

// Run the shell until the input ends. Gives the exit code.
export async function shell(
  bus: Bus,
  commands: Commands,
  input: Readable & Terminal,
  output: Writable & Terminal,
  errors: Writable & Terminal,
): Promise<number> {
  const terminal = Boolean(input.isTTY && output.isTTY);
  const lines = createInterface({
    input,
    output,
    terminal,
    prompt: PROMPT,
    completer: (line: string) => show(complete(commands, line)),
  });
  let busy = false;
  let closed = false;
  // Write a line above the prompt, and then the prompt again.
  const write = (stream: Writable & Terminal, text: string) => {
    if (terminal && !busy) {
      clearLine(output, 0);
      cursorTo(output, 0);
    }
    stream.write(`${text}\n`);
    if (terminal && !busy) {
      lines.prompt(true);
    }
  };
  const stop = bus.register(EVENTS, (body) => write(output, event(body, colours(output))));
  bus.onerror = (failure) => write(errors, error(failure.message, colours(errors)));
  bus.onclose = () => {
    closed = true;
    // A command that waits gets this error itself.
    if (!busy) {
      write(errors, error("The connection is closed", colours(errors)));
    }
    lines.close();
  };
  // Readline shows completions without colour, thus show them here. If
  // all completions start the same, insert that start, as bash does.
  const show = ([items, part]: [string[], string]): [string[], string] => {
    const start = common(items);
    if (items.length < 2 || (start.length > part.length && start.startsWith(part))) {
      return [items.length < 2 ? items : [start], part];
    }
    output.write(`\n${columns(items, output.columns || 80, colours(output))}\n`);
    lines.prompt(true);
    return [[], part];
  };
  lines.on("SIGINT", () => {
    lines.write(null, { ctrl: true, name: "e" });
    lines.write(null, { ctrl: true, name: "u" });
  });
  if (terminal) {
    lines.prompt();
  }
  for await (const line of lines) {
    if (line.trim()) {
      busy = true;
      try {
        output.write(`${json(await commands.call(bus, words(line)), colours(output))}\n`);
      } catch (failure) {
        errors.write(`${error((failure as Error).message, colours(errors))}\n`);
      }
      busy = false;
    }
    if (terminal && !closed) {
      lines.prompt();
    }
  }
  if (terminal) {
    output.write("\n");
  }
  bus.onclose = () => {};
  stop();
  return closed ? 1 : 0;
}

// The completions of the last word of a line, as readline needs them:
// the completions, and the part of the line that they complete. In the
// first word, this is the part after the last dot.
export function complete(commands: Commands, line: string): [string[], string] {
  const current = /\S*$/.exec(line)![0];
  const before = line.slice(0, line.length - current.length).trim().split(/\s+/).filter(Boolean);
  if (before.length) {
    return [options(commands, before, current), current];
  }
  return [targets(commands, current), current.slice(current.lastIndexOf(".") + 1)];
}

// Completions in columns that fit the width, with handlers in colour.
// A handler is a completion without a dot at the end, and not an option.
export function columns(items: string[], width: number, colour: boolean): string {
  const size = Math.max(...items.map((item) => item.length)) + 2;
  const count = Math.max(1, Math.floor(width / size));
  return items
    .map((item, index) => {
      const text = item.endsWith(".") || item.startsWith("-") ? item : handler(item, colour);
      const end = index === items.length - 1 ? "" : (index + 1) % count === 0 ? "\n" : " ".repeat(size - item.length);
      return text + end;
    })
    .join("");
}

// The start that all items have.
function common(items: string[]): string {
  return items.reduce((start, item) => {
    let length = 0;
    while (length < start.length && start[length] === item[length]) {
      length++;
    }
    return start.slice(0, length);
  }, items[0] ?? "");
}

// The first word, one part at a time: the handlers if the text before
// the last dot is a target, and the next parts of the target names.
function targets(commands: Commands, current: string): string[] {
  const prefix = current.toLowerCase();
  const dot = prefix.lastIndexOf(".");
  const parts = new Set<string>();
  for (const name of commands.targets.keys()) {
    if (name.startsWith(prefix)) {
      const end = name.indexOf(".", prefix.length);
      parts.add(end === -1 ? `${name.slice(dot + 1)}.` : name.slice(dot + 1, end + 1));
    }
  }
  const target = dot > 0 ? commands.targets.get(prefix.slice(0, dot)) : undefined;
  const handlers = Object.keys(target?.handlers ?? {}).filter((handler) =>
    handler.toLowerCase().startsWith(prefix.slice(dot + 1)),
  );
  return [...handlers, ...[...parts].sort()];
}

// The options of the handler, except where an option needs a value.
function options(commands: Commands, before: string[], current: string): string[] {
  if (current && !current.startsWith("-")) {
    return [];
  }
  let options;
  try {
    const { target, msgh } = commands.command(before[0]);
    options = commands.options(target.handlers[msgh]);
  } catch {
    return [];
  }
  const previous = before.at(-1)!;
  const option = options.get(previous.slice(2));
  if (previous.startsWith("--") && option && !(option.type === "boolean" && !option.multiple)) {
    return [];
  }
  return [...options]
    .flatMap(([name, { type, multiple }]) => (type === "boolean" && !multiple ? [`--${name}`, `--no-${name}`] : [`--${name}`]))
    .filter((name) => name.startsWith(current));
}
