// Generated from manager/entitymanager/group.schema.json by scripts/model.ts. Do not edit.

import type * as commonSchema from "../../common.ts";

/** Schema for EntityManager's group and ungroup methods. */
export type Group = {
  group: commonSchema.Uuid;
  member: commonSchema.Uuid;
};
