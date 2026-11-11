// Generated from manager/taskmanager/cancelJob.schema.json by scripts/model.ts. Do not edit.

import type * as commonSchema from "../../common.ts";

/** Schema for TaskManager's cancelJob method. */
export type CancelJob = {
  taskId: commonSchema.Uuid;
  jobId: number;
};
