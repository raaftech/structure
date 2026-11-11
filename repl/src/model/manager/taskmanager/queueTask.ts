// Generated from manager/taskmanager/queueTask.schema.json by scripts/model.ts. Do not edit.

import type * as commonSchema from "../../common.ts";

/** Schema for TaskManager's queueTask method. */
export type QueueTask = {
  taskId?: commonSchema.Uuid;
  sequential?: boolean;
  /** Files that TaskManager deletes when the task is finished. */
  temporaryFiles?: string[];
  jobs: {
    command: string;
    timeAllocated?: number;
    metadata?: {
      instrumentName: string;
      instrumentUuid: string;
      originName: string;
      originUuid: string;
      capability: string;
      timeAllocated?: number;
      stateMap?: { [key: string]: State };
    };
  }[];
};

export type State = {
  /** A code representing the "goodness" of the result, 0 being "good", 2 being "bad". */
  code: number;
  /** A short word conveying the meaning of "stateCode", e.g. "OK", "MAYBE", "NOK" */
  interpretation: string;
  /** A longer description of the reason for the status code. */
  description: string;
};
