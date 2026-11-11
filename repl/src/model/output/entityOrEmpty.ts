// Generated from output/entityOrEmpty.schema.json by scripts/model.ts. Do not edit.

import type { Json } from "../json.ts";
import type * as entitySchema from "../entity.ts";

/** One node as an entity, or an empty object if there is no such node. */
export type EntityOrEmpty = entitySchema.Entity | { [key: string]: Json };
