// Generated from manager/entitymanager/read.schema.json by scripts/model.ts. Do not edit.

import type * as commonSchema from "../../common.ts";

/** Schema for EntityManager's read method. */
export type Read = {
  labels: string[];
  start: commonSchema.Uuid;
  includeStart?: boolean;
};
