// Generated from entity.schema.json by scripts/model.ts. Do not edit.

import type { Json } from "./json.ts";
import type * as commonSchema from "./common.ts";

/** Schema for Structure entities. */
export type Entity = {
  name: string;
  desc: Desc;
  data?: { [key: string]: Json };
  file?: {
    [key: string]: {
      path?: string;
      content?: string;
      checksum?: Checksum;
      format: Format;
      encoding: Encoding;
    };
  };
  meta: {
    uuid?: commonSchema.Uuid;
    abstract?: boolean;
    relations: {
      [key: string]: {
        direction: Direction;
        to: commonSchema.UuidOrNameList;
      };
    };
    constraints?: { [key: string]: Constraint[] };
    identity?: commonSchema.StringOrStringList;
    checksum?: Checksum;
    bundle?: string;
    path?: string;
  };
};

export type Direction = "incoming" | "outgoing";

export type Cardinality = "OPTIONAL_TO_ONE" | "OPTIONAL_TO_MANY" | "REQUIRED_TO_ONE" | "REQUIRED_TO_MANY";

export type Constraint = {
  to: commonSchema.UuidOrName;
  cardinality: Cardinality;
};

export type Desc = string;

export type Checksum = string;

export type Format = "json" | "hbs" | "script";

export type Encoding = "base64" | "text";
