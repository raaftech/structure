// Generated from node/instance/runInstrument.schema.json by scripts/model.ts. Do not edit.

import type { Json } from "../../json.ts";

/** Schema for every InstanceActor's runInstrument method. */
export type RunInstrument = {
  instrument: string;
  timeAllocated?: number;
  arguments?: {
    "/"?: Json;
    [key: string]: string | Json | undefined;
  };
};
