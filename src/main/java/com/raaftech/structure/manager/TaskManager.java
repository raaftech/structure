package com.raaftech.structure.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.annotations.SchemaPath;
import com.raaftech.structure.base.Configuration;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.base.GenericActor;
import com.raaftech.structure.graph.Instance;
import com.raaftech.structure.graph.Result;
import com.raaftech.structure.helper.ProcessRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.IntStream;

/**
 * TaskManager manages jobs, which in turn are grouped by tasks.
 * A job is a process encapsulated by a ProcessRunner coupled with a status.
 */

// TODO: timeout for jobs/tasks

public class TaskManager extends GenericActor {

    /**
     * Jobs have the status QUEUED when they are waiting to be executed. RUNNING when they're being executed
     * and CANCELLED when they're waiting to get removed from the queue, or waiting to get destroyed if they're
     * already running.
     */
    private enum JobStatus {
        CANCELLED,
        ERROR,
        FINISHED,
        QUEUED,
        RUNNING
    }
    /**
     * Tasks can be READY, meaning its jobs can be executed. If a task is PAUSED we don't execute its queued jobs,
     * in stead we'll move them to the back of the queue.
     */
    private enum TaskStatus {
        SCHEDULED,
        PAUSED,
        FINISHED
    }

    /**
     * A private class to group a ProcessRunner with a status.
     */
    private class Job {
        public  JobStatus status = JobStatus.QUEUED;
        public String taskId;
        public ProcessRunner process;
        public ObjectNode metadata;
        public Long timeStarted;
        public int timeAllocated;

        public Job(ProcessRunner process, String taskId, ObjectNode metadata) {
            this.process = process;
            this.taskId = taskId;
            this.metadata = metadata;
            this.timeAllocated = TaskManager.defaultJobTime;
        }

        public Job(ProcessRunner process, String taskId, ObjectNode metadata, int timeAllocated) {
            this(process, taskId, metadata);
            this.timeAllocated = timeAllocated;
        }

        public ObjectNode toJson() {
            ObjectNode json = mapper.createObjectNode();
            json.put("status", status.name());
            Iterator<Map.Entry<String, JsonNode>> elements = metadata.fields();
            while (elements.hasNext()) {
                Map.Entry<String, JsonNode> el = elements.next();
                json.set(el.getKey(), el.getValue());
            }

            if (process.getProcess() != null && !process.isRunning()) {
                ProcessRunner.ProcessResult processResult = process.finish();
                JsonNode result = json.set("result", processResult.toJson());
                ((ObjectNode)result).put("process", process.getCommand());
            }

            return json;
        }
    }

    /**
     * A private class to group jobs as a task.
     */
    private class Task {
        public TaskStatus status = TaskStatus.SCHEDULED;
        public String id = UUID.randomUUID().toString();
        public boolean sequential;
        public ArrayList<Job> jobs = new ArrayList<>();
        private int finishedCount = 0;

        public Task(String id, boolean sequential) {
            this.id = id;
            this.sequential = sequential;
        }

        public void finishedOne() {
            finishedCount++;
        }

        public boolean isFinished() {
            return finishedCount == jobs.size();
        }

        public ObjectNode toJson() {
            ObjectNode json = mapper.createObjectNode();

            json.put("status", status.name())
                    .put("id", id);
            ArrayNode jobData = json.putArray("jobs");
            jobs.stream()
                    .map(Job::toJson)
                    .forEach(jobData::add);

            return json;
        }
    }

    private final static Logger log = LoggerFactory.getLogger(TaskManager.class);

    // The queue of jobs
    private static Queue<Job> jobQueue = new ConcurrentLinkedQueue<>();
    // A hashmap holding Tasks and their UUID's. Used to retrieve tasks
    // This is being polled by the periodic event defined after this.
    private static Map<String, Task> taskMap = new HashMap<>();
    // The currently running jobs.
    private static List<Job> runningJobs = new ArrayList<>();
    // The default timeout for a job.
    private final static int defaultJobTime = 3000;

    /**
     * The constructor creates a periodic task that polls the queue.
     * @param maxConcurrentProcesses Size of the job pool.
     */
    public TaskManager(int maxConcurrentProcesses) {
        log.info("Constructing TaskManager with a job pool of size " + maxConcurrentProcesses + ".");
        // The timer that takes jobs from the queue and runs them.
        Context.getVertx().setPeriodic(100L, id -> {
            // Do we have room to run some jobs?
            if (runningJobs.size() < maxConcurrentProcesses) {
                // For every free spot in the running jobs list...
                IntStream.range(0, maxConcurrentProcesses-runningJobs.size())
                        .forEach(i -> {
                            // Take a job from the queue
                            Job job = jobQueue.poll();
                            // Check if we actually have a job. Queue might be empty.
                            if (job != null) {
                                Task task = taskMap.get(job.taskId);
                                int jobId = task.jobs.indexOf(job);
                                // Check if the task is still ready to run
                                if (task.status == TaskStatus.SCHEDULED) {
                                    // If the job didn't get canceled while it was in the queue run it.
                                    if (job.status != JobStatus.CANCELLED) {
                                        if ((task.sequential
                                                && (jobId == 0
                                                || (jobId > 0
                                                        && ((task.jobs.get(jobId-1).status == JobStatus.FINISHED)
                                                        // For now we won't cancel a whole task because a job failed
                                                        || (task.jobs.get(jobId-1).status == JobStatus.CANCELLED)))))
                                            || !task.sequential) {
                                            try {
                                                runJob(job);
                                            } catch (InterruptedException | IOException e) {
                                                log.error("Ran into error while executing job " + task.id + "." + jobId);
                                                if (Configuration.isTraceModeOn()) {
                                                    e.printStackTrace();
                                                }
                                                job.status = JobStatus.ERROR;
                                                runningJobs.remove(job);
                                                task.finishedOne();
                                                // Notify interested parties
                                                publishStatus("jobError", mapper.createObjectNode()
                                                        .put("taskId", task.id)
                                                        .put("jobId", task.jobs.indexOf(job)));
                                            }
                                        } else {
                                            jobQueue.offer(job);
                                        }
                                    } else {
                                        log.debug("Got cancelled job " + task.id + "." + task.jobs.indexOf(job)
                                                + " from queue, skipping.");
                                        task.finishedOne();
                                    }
                                } else {
                                    // Requeue job, because the task is paused
                                    jobQueue.offer(job);
                                }
                            }
                        });
            }
        });
    }

    /**
     * Add a job to the queue and set its status to QUEUED.
     * @param job The job to queue.
     */
    private void queueJob(Job job) {
        job.status = JobStatus.QUEUED;
        jobQueue.add(job);
    }

    /**
     * Run a specific job.
     * @param job A job that does some work. Delivered from the queue.
     */
    private void runJob(Job job) throws IOException, InterruptedException {
        runningJobs.add(job);

        // Execute the process
        job.timeStarted = System.currentTimeMillis();
        job.process.run();
        job.status = JobStatus.RUNNING;

        // Get the task this job belongs to
        Task task = taskMap.get(job.taskId);

        // Notify interested parties
        int jobId = task.jobs.indexOf(job);
        publishStatus("jobRunning", mapper.createObjectNode()
                .put("taskId", job.taskId)
                .put("jobId", jobId));

        // Create a timer that polls the process. This approach is used so we can monitor
        // the status of the process without blocking. This in turn results in us being able to destroy the
        // process while it's running.
        vertx.setPeriodic(100L, id -> {
            // If the process is done running. Also check the status to avoid a race condition where
            // the code block is executed twice for the same job.
            if (!job.process.isRunning() && job.status != JobStatus.FINISHED) {
                job.status = JobStatus.FINISHED;
                // Remove the job from the running jobs list.
                runningJobs.remove(job);
                // Notify interested parties
                publishStatus("jobFinished", mapper.createObjectNode()
                        .put("taskId", task.id)
                        .put("jobId", task.jobs.indexOf(job)));
                // Decrease task's to-do
                task.finishedOne();
                // Cancel this timer.
                vertx.cancelTimer(id);
            }

            // Did we run out of time?
            if (job.process.isRunning() && System.currentTimeMillis() - job.timeStarted > job.timeAllocated) {
                job.status = JobStatus.CANCELLED;
                log.debug("Cancelling running job " + task.id + "." + task.jobs.indexOf(job) + " because it"
                        + " been exceeded the allocated time of " + job.timeAllocated + "ms.");
            }

            // Did the job get cancelled while it's running?
            if (job.status == JobStatus.CANCELLED) {
                log.debug("Destroying running job " + task.id + "." + task.jobs.indexOf(job));
                // Cancel the job
                job.process.cancel();
                // Decrease the job's to-do
                task.finishedOne();
                // Remove it from the running jobs list.
                runningJobs.remove(job);
                // Cancel this timer.
                vertx.cancelTimer(id);
            }

            if (task.isFinished()) {
                task.status = TaskStatus.FINISHED;
                log.info("Task " + job.taskId + " is finished.");
                // Remove task from taskMap.
                taskMap.remove(task.id);
                // Notify interested parties
                publishStatus("taskFinished", mapper.createObjectNode()
                        .put("taskId", task.id)
                        .set("taskObj", task.toJson()));
                // Save result
                ProcessRunner.ProcessResult jobResult = job.process.finish();
                Result.Builder resultBuilder = new Result.Builder()
                        .taskId(job.taskId)
                        .process(job.process.getCommand())
                        .returnCode(jobResult.returnCode)
                        .stderr(jobResult.stderr)
                        .stdout(jobResult.stdout);
                /*
                If this job was provided with a stateMap, see if we can map the exit code to a specific state.
                In case the exit code is unknown, the state will be set to an error state "UNKNOWN".
                 */
                if (job.metadata.has("stateMap")) {
                    JsonNode stateMap = job.metadata.get("stateMap");
                    if (stateMap.has(Integer.toString(jobResult.returnCode))) {
                        JsonNode state = stateMap.get(Integer.toString(jobResult.returnCode));
                        resultBuilder.state(new Result.State(state.get("code").asInt(),
                                state.get("interpretation").asText(), state.get("description").asText()));
                    } else {
                        resultBuilder.state(new Result.State(2, "UNKNOWN", "Received an unknown exit code."));
                    }
                }
                Result result = resultBuilder.build();
                Instance origin = result.add(UUID.fromString(job.metadata.get("originUuid").asText()));
                /*
                result.add returns the origin instance, on which we want to save the result
                We save the origin itself, which will cause the save to propagate to the result chain.
                If we would just save the result, it won't update the head of the result list to the latest result.
                 */
                origin.save();
                try {
                    log.info(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(task.toJson()));
                } catch (JsonProcessingException e) {
                    if (Configuration.isTraceModeOn()) {
                        e.printStackTrace();
                    }
                }
            }

            // Check if the queue and the running process list are empty, meaning we've finished all the work.
            if (jobQueue.size() == 0 && runningJobs.size() == 0) {
                log.info("Completed all the work managed by TaskManager.");
            }
        });
    }

    /**
     * Cancel a specific job.
     * @return A JSON object containing an error field if something went wrong.
     */
    @SuppressWarnings("unused")
    @SchemaPath("resource:/schema/manager/taskmanager/canceljob.schema.json")
    public MessageHandler cancelJob() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                JsonNode args = request.get("args");
                ObjectNode result = mapper.createObjectNode();
                String taskId = args.get("taskId").asText();
                int jobId = args.get("jobId").asInt();

                Task task = taskMap.get(taskId);
                if (task != null) {
                    if (jobId < task.jobs.size()) {
                        Job job = task.jobs.get(jobId);
                        if (job != null) {
                            job.status = JobStatus.CANCELLED;
                            // Notify interested parties
                            publishStatus("jobCancelled", mapper.createObjectNode()
                                    .put("taskId", task.id)
                                    .put("jobId", jobId));
                        } else {
                            // This shouldn't happen.
                            result.put("error", "Job was null. This shouldn't happen.");
                        }
                    } else {
                        result.put("error", "Task " + taskId + " did not have a job with id " + jobId + ".");
                    }
                } else {
                    result.put("error", "Couldn't retrieve task with id " + taskId + ".");
                }

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

    /**
     * Cancel a complete task. This cancels all the running and queued tasks.
     * @return A JSON object containing a data-array of all the cancelled jobs in this task, or an error field
     * if something went wrong.
     */
    @SuppressWarnings("unused")
    @SchemaPath("resource:/schema/manager/taskmanager/task.schema.json")
    public MessageHandler cancelTask() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                JsonNode args = request.get("args");
                ObjectNode result = mapper.createObjectNode();
                String taskId = args.get("taskId").asText();

                Task task = taskMap.get(taskId);
                if (task != null) {
                    ArrayNode resultData = result.putArray("data");
                    task.jobs.stream()
                            .filter(job -> job.status != JobStatus.FINISHED)
                            .filter(job -> job.status != JobStatus.CANCELLED)
                            .forEach(job -> {
                                job.status = JobStatus.CANCELLED;
                                resultData.add(taskId + "." + task.jobs.indexOf(job));
                            });
                    // Notify interested parties
                    publishStatus("taskCancelled", mapper.createObjectNode()
                            .put("taskId", task.id));
                } else {
                    result.put("error", "Couldn't find task with id " + taskId + ".");
                }
                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

    /**
     * Get task information from the queue.
     * @return A JSON object containing an array of objects. Each object represents a task.
     */
    @SuppressWarnings("unused")
    public MessageHandler queueStatus() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();
                ObjectNode data = result.putObject("data");

                taskMap.entrySet().stream()
                        .forEach(entry -> {
                            String taskId = entry.getKey();
                            Task task = entry.getValue();
                            data.set(taskId, task.toJson());
                        });

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

    /**
     * Queue a task to be run by the TaskManager.
     * @return A response, containing the task id, or an error.
     */
    @SuppressWarnings("unused")
    @SchemaPath("resource:/schema/manager/taskmanager/queuetask.schema.json")
    public MessageHandler queueTask() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                JsonNode args = request.get("args");
                ObjectNode result = mapper.createObjectNode();
                ArrayNode jobs = (ArrayNode) args.get("jobs");

                // Construct a new task
                String taskId = args.get("taskId").asText();
                boolean sequential = args.has("sequential") ? args.get("sequential").asBoolean() : false;
                Task task = new Task(taskId, sequential);
                result.put("data", taskId);

                // Add the given jobs to the task
                Iterator<JsonNode> jobIterator = jobs.elements();
                while (jobIterator.hasNext()) {
                    ObjectNode jobData = (ObjectNode) jobIterator.next();
                    String command = jobData.get("command").asText();
                    ObjectNode metadata = (ObjectNode) jobData.get("metadata");
                    ProcessRunner processRunner = new ProcessRunner(command);
                    Job job;
                    if (jobData.has("timeAllocated")) {
                        job = new Job(processRunner, task.id, metadata, jobData.get("timeAllocated").asInt());
                    } else {
                        job = new Job(processRunner, task.id, metadata);
                    }
                    task.jobs.add(job);
                }
                task.jobs.stream().forEach(job -> queueJob(job));
                taskMap.put(task.id, task);

                log.info("Queued task with id " + task.id + ".");

                // Notify interested parties
                publishStatus("taskScheduled", task.toJson());

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

    /**
     * Get information about a particular job.
     * @return A JSON object containing information about a particular task, or an error-field if a given task
     * doesn't exist.
     */
    @SuppressWarnings("unused")
    @SchemaPath("resource:/schema/manager/taskmanager/task.schema.json")
    public MessageHandler taskStatus() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                JsonNode args = request.get("args");
                ObjectNode result = mapper.createObjectNode();
                String taskId = args.get("taskId").asText();

                Task task = taskMap.get(taskId);
                if (task != null) {
                    result.set("data", task.toJson());
                } else {
                    result.put("error", "Couldn't find task with id " + taskId + ".");
                }

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

    /**
     * Toggle the pause status of a particular task.
     * @return A JSON object containing the current status of the task, or an error-field if a given task
     * doesn't exist.
     */
    @SuppressWarnings("unused")
    @SchemaPath("resource:/schema/manager/taskmanager/task.schema.json")
    public MessageHandler toggleTaskPause() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                JsonNode args = request.get("args");
                ObjectNode result = mapper.createObjectNode();
                String taskId = args.get("taskId").asText();

                Task task = taskMap.get(taskId);
                if (task != null) {
                    if (task.status == TaskStatus.SCHEDULED) {
                        task.status = TaskStatus.PAUSED;
                    } else if (task.status == TaskStatus.PAUSED) {
                        task.status = TaskStatus.SCHEDULED;
                    }
                    result.put("data", task.status.name());
                    // Notify interested parties
                    publishStatus("taskPaused", mapper.createObjectNode()
                            .put("taskId", task.id)
                            .put("status", task.status.name()));
                } else {
                    result.put("error", "Couldn't find task with id " + taskId + ".");
                }

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

}
