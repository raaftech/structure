// Generated from output/introspection.schema.json by scripts/model.ts. Do not edit.

import type { Json } from "../json.ts";
import type * as manualSchema from "../base/manual.ts";

/** The active actors, their message handlers and their schema documents. */
export type Introspection = {
  /** The request, response and handler schemas with all referenced documents, by absolute document URI. */
  schemas: { [key: string]: { [key: string]: Json } };
  /** Each actor class that has an active actor, by class name. */
  classes: { [key: string]: Class };
  /** Each active actor, by actor ID. */
  actors: { [key: string]: Actor };
};

export type Actor = {
  class: string;
  addresses: string[];
};

export type Class = {
  /** Each message handler, by handler name. */
  handlers: { [key: string]: Handler };
};

export type Handler = {
  /** The URI of the argument schema. */
  inputSchemaUri: string;
  /** The URI of the schema for successful result.data. */
  outputSchemaUri: string;
  help: manualSchema.Manual;
};
