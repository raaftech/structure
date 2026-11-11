// Generated from output/node.schema.json by scripts/model.ts. Do not edit.

import type { Json } from "../json.ts";
import type * as commonSchema from "../common.ts";

/** The short form of one node. A type, an instance and a component each add properties. An instance also has each data property as a string. */
export type Node = {
  name: string;
  uuid: commonSchema.Uuid;
  desc?: string | null;
  checksum?: string | null;
  /** The glyph of the type, as a hexadecimal number. */
  glyphId?: string;
  isa?: commonSchema.Uuid;
  instanceof?: commonSchema.Uuid;
  /** The data schema of a type. */
  schema?: { [key: string]: Json };
  components?: {
    name: string;
    uuid: commonSchema.Uuid;
  }[];
  _nodeType?: string;
  _hierarchyPath?: string;
};
