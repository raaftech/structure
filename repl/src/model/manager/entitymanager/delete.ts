// Generated from manager/entitymanager/delete.schema.json by scripts/model.ts. Do not edit.

import type { Json } from "../../json.ts";
import type * as commonSchema from "../../common.ts";

/** Schema for EntityManager's delete method. */
export type Delete = {
  labels: Json[];
  node: commonSchema.Uuid;
  includeStart?: boolean;
};
