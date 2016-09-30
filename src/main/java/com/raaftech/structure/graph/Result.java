package com.raaftech.structure.graph;

import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.repositories.ResultRepository;
import org.neo4j.graphdb.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.annotation.GraphProperty;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

public class Result extends Node {

    public static final ResultRepository resultRepository = Context.getResultRepository();

    final static Logger log = LoggerFactory.getLogger(Result.class);

    @RelatedTo(type="NEXT_RESULT", direction = Direction.OUTGOING)
    Result next;
    @RelatedTo(type="NEXT_RESULT", direction = Direction.INCOMING)
    Result previous;

    @GraphProperty(propertyType = Long.class)
    private LocalDateTime date;
    private String process;
    private String taskId;
    private String stdout;
    private String stderr;
    private int retcode;

    // Empty constructor for Spring
    public Result() {}

    /**
     * Results are used to store output of controllers.
     * They can be used as input for parsers.
     * @param process The process that generated the result.
     * @param taskId The task that the execution of the controller belongs to.
     * @param stdout The output on stdout of the process.
     * @param stderr The output on stderr of the process.
     * @param retcode The return code of the process.
     */
    public Result(String process, String taskId, String stdout, String stderr, int retcode) {
        super("Result", "A result");
        date = LocalDateTime.now();
        this.process = process;
        this.taskId = taskId;
        this.stdout = stdout;
        this.stderr = stderr;
        this.retcode = retcode;
    }

    /**
     * Persist the result to database.
     * @return The result this was called on.
     */
    public Result save() {
        resultRepository.save(this);
        return this;
    }

    public String getProcess() {
        return process;
    }

    public String getStdout() {
        return stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public int getRetcode() {
        return retcode;
    }

    public String getTaskId() {
        return taskId;
    }

    public Result getNext() {
        return next;
    }

    public Result setNext(Result next) {
        if (this.next == null) {
            next.previous = this;
            this.next = next;
        } else {
            throw new IllegalStateException("Can't set next result on \""
                    + getUuid() + "\", it already has one.");
        }
        return this;
    }

    public Result getPrevious() {
        return previous;
    }

    public List<Result> getThisAndNewer() {
        LinkedList<Result> results = new LinkedList<>();

        Result head = this;
        do {
            results.add(head);
            head = head.next;
        } while (head != null);

        return results;
    }

    public List<Result> getThisAndOlder() {
        LinkedList<Result> results = new LinkedList<>();

        Result head = this;
        do {
            results.add(head);
            head = head.previous;
        } while (head != null);

        return results;
    }

}
