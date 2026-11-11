// Generated from manager/entitymanager/relate.schema.json by scripts/model.ts. Do not edit.

import type * as commonSchema from "../../common.ts";

/** Schema for EntityManager's relate and unrelate methods. */
export type Relate = {
  from: commonSchema.Uuid;
  relation: string;
  to: commonSchema.Uuid;
};
