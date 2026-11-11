// Generated from output/task.schema.json by scripts/model.ts. Do not edit.

/** One task with its jobs. */
export type Task = {
  status: TaskStatus;
  id: string;
  jobs: Job[];
};

export type TaskStatus = "SCHEDULED" | "PAUSED" | "FINISHED";

export type JobStatus = "CANCELLED" | "ERROR" | "FINISHED" | "QUEUED" | "RUNNING";

/** One job. It also has each metadata property of the job. */
export type Job = {
  status: JobStatus;
  /** The job result, when the process is done. */
  result?: {
    process: string;
    returnCode?: number;
    stdout?: string;
    stderr?: string;
  };
};
