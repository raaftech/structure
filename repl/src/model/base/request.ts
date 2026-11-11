// Generated from base/request.schema.json by scripts/model.ts. Do not edit.

import type { Json } from "../json.ts";
import type * as commonSchema from "../common.ts";

/** Schema for Structure request messages. */
export type Request = {
  msgh: commonSchema.CaseInsensitiveAlphaNum;
  args?: { [key: string]: Json };
  error?: { [key: string]: Json };
  valid?: boolean;
};
