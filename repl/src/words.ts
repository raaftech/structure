// Split a line of the shell into words, with the quotes of a POSIX
// shell. Single quotes keep each character. In double quotes, a
// backslash escapes a double quote or a backslash. Outside quotes, a
// backslash escapes the next character.

import { CommandError } from "./commands.ts";

export function words(line: string): string[] {
  const result: string[] = [];
  let word = "";
  let started = false;
  let quote: string | undefined;
  for (let index = 0; index < line.length; index++) {
    const character = line[index];
    const next = line[index + 1];
    if (quote === "'") {
      if (character === "'") {
        quote = undefined;
      } else {
        word += character;
      }
    } else if (quote === '"') {
      if (character === '"') {
        quote = undefined;
      } else if (character === "\\" && (next === '"' || next === "\\")) {
        word += next;
        index++;
      } else {
        word += character;
      }
    } else if (/\s/.test(character)) {
      if (started) {
        result.push(word);
        word = "";
        started = false;
      }
    } else {
      started = true;
      if (character === "'" || character === '"') {
        quote = character;
      } else if (character === "\\" && next !== undefined) {
        word += next;
        index++;
      } else {
        word += character;
      }
    }
  }
  if (quote) {
    throw new CommandError(`The ${quote} quote has no end`);
  }
  if (started) {
    result.push(word);
  }
  return result;
}
