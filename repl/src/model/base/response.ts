// Generated from base/response.schema.json by scripts/model.ts. Do not edit.

import type { Json } from "../json.ts";

/** Schema for Structure responses to requests. The result has data or an error. */
export type Response = {
  request: { [key: string]: Json };
  result: {
    data?: Json;
    error?: string;
  };
};
