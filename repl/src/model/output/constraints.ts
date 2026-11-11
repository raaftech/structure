// Generated from output/constraints.schema.json by scripts/model.ts. Do not edit.

import type * as entitySchema from "../entity.ts";
import type * as commonSchema from "../common.ts";

/** The type constraints of a type, by direction. */
export type Constraints = {
  incoming: ConstraintList;
  outgoing: ConstraintList;
};

export type ConstraintList = Constraint[];

/** One type constraint, by relation name. */
export type Constraint = {
  [key: string]: {
    cardinality: entitySchema.Cardinality;
    from: commonSchema.Uuid;
    to: commonSchema.Uuid;
  };
};
