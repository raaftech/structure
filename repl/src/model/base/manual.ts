// Generated from base/manual.schema.json by scripts/model.ts. Do not edit.

import type { Json } from "../json.ts";
import type * as commonSchema from "../common.ts";

/** Schema for Structure manual and/or documentation format. */
export type Manual = {
  name: commonSchema.CaseInsensitiveAlphaNum;
  synopsis: { [key: string]: Json };
  description: string;
  options?: Json[];
  files?: Json[];
  environment?: Json[];
  diagnostics?: Json[];
  examples?: Json[];
  authors?: Json[];
  "see also"?: Json[];
};
