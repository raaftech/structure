// An introspection with a few targets and handlers, for the tests.

import type { Handler, Introspection } from "../src/model/output/introspection.ts";

export const BUS = "tech.raaf.structure.bus";
export const ARGS = "resource:/schema/base/args.schema.json";
export const RUN = "resource:/schema/manager/run.schema.json";
const COMMON = "resource:/schema/common.schema.json";
const ENTITY = "resource:/schema/entity.schema.json";
export const UUID = "4c64d4f7-7e60-4db8-b3c7-d6fc3b0f0221";

export function handler(inputSchemaUri: string): Handler {
  return { inputSchemaUri, outputSchemaUri: ARGS, help: { name: "", synopsis: {}, description: "" } };
}

export const INTROSPECTION: Introspection = {
  classes: {
    [`${BUS}.manager.entitymanager`]: { handlers: { run: handler(RUN) } },
    [`${BUS}.repository.types`]: { handlers: { getAll: handler(ARGS) } },
    [`${BUS}.typeactor`]: { handlers: { getSubtypes: handler(ARGS) } },
    [`${BUS}.instanceactor`]: { handlers: { getCapabilities: handler(ARGS) } },
  },
  actors: {
    a: { class: `${BUS}.manager.entitymanager`, addresses: [`${BUS}.manager.entitymanager`] },
    b: { class: `${BUS}.repository.types`, addresses: [`${BUS}.repository.types`] },
    c: { class: `${BUS}.typeactor`, addresses: [`${BUS}.node.a79ce509`, `${BUS}.node.type`] },
    d: { class: `${BUS}.typeactor`, addresses: [`${BUS}.node.b1bc963a`, `${BUS}.node.type.types`] },
    e: { class: `${BUS}.instanceactor`, addresses: [`${BUS}.node.${UUID}`] },
  },
  schemas: {
    [ARGS]: { type: "object" },
    [COMMON]: { definitions: { uuid: { type: "string" } } },
    [ENTITY]: { type: "object" },
    [RUN]: {
      type: "object",
      properties: {
        name: { type: "string" },
        start: { $ref: "../common.schema.json#/definitions/uuid" },
        amount: { type: "integer" },
        all: { type: "boolean" },
        labels: { type: "array", items: { type: "string" } },
        values: { type: "array" },
        entities: { type: "array", items: { $ref: "../entity.schema.json" } },
        arguments: { type: "object" },
      },
    },
  },
};
