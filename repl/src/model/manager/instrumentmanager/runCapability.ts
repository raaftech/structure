// Generated from manager/instrumentmanager/runCapability.schema.json by scripts/model.ts. Do not edit.

import type { Json } from "../../json.ts";
import type * as commonSchema from "../../common.ts";

/** Schema for InstrumentManager's runCapability method. */
export type RunCapability = {
  capability: string;
  instances: commonSchema.Uuid[];
  timeAllocated?: number;
  arguments?: {
    "/"?: Json;
    [key: string]: string | Json | undefined;
  };
};
