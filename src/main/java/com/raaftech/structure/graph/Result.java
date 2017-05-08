package com.raaftech.structure.graph;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.base.*;
import com.raaftech.structure.exceptions.IllegalEntityType;
import com.raaftech.structure.exceptions.ResultBuilderException;
import com.raaftech.structure.graph.repositories.InstanceRepository;
import com.raaftech.structure.graph.repositories.ResultRepository;
import com.raaftech.structure.graph.repositories.TypeRepository;
import org.apache.commons.lang3.Validate;
import org.neo4j.graphdb.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.vertx.core.json.Json.mapper;

/**
 * @Deprecated We need to redesign the domain classes to use Entity.
 */
@Deprecated
public class Result extends Instance {

    private static final String NAME = "name";
    private static final String DESC = "desc";
    private static final String PROCESS = "process";
    private static final String TASKID = "task";
    private static final String STDOUT = "stdout";
    private static final String STDERR = "stderr";
    private static final String RETURNCODE = "returnCode";
    private static final String DATECREATED = "dateCreated";
    private static final String USER = "user";
    private static final String STATECODE = "stateCode";
    private static final String STATEINTERPRETATION = "stateInterpretation";
    private static final String STATEDESCRIPTION = "stateDescription";
    private static final String ERROR = "error";

    public static final ResultRepository resultRepository = Context.getResultRepository();

    final static Logger log = LoggerFactory.getLogger(Result.class);

    @RelatedTo(type="NEXT_RESULT", direction = Direction.OUTGOING)
    Result next;
    @RelatedTo(type="NEXT_RESULT", direction = Direction.INCOMING)
    Result previous;

    private static final TypeRepository typeRepository = Context.getTypeRepository();
    private static final InstanceRepository instanceRepository = Context.getInstanceRepository();

    /**
     * A state is a simple object that allows us to express the "goodness" of a return value.
     */
    public static class State {
        final private int code;
        final private String interpretation;
        final private String description;

        public State(int code,  String interpretation, String description) {
            Validate.inclusiveBetween(0, 2, code);
            Validate.notNull(interpretation);
            Validate.notNull(description);
            this.code = code;
            this.interpretation = interpretation;
            this.description = description;
        }
        public int getCode() {
            return code;
        }
        public String getDescription() {
            return description;
        }
        public String getInterpretation() {
            return interpretation;
        }
    }

    public static class Builder {
        // Required
        private String process;
        private String taskId;
        // Optional
        private String stdout;
        private String stderr;
        private Integer returnCode;
        private String user;
        private State state;
        private String error;


        public Builder() {}
        public Builder process(String process) {
            this.process = process;
            return this;
        }
        public Builder taskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

        // Optional fields
        public Builder stdout(String stdout) {
            this.stdout = stdout;
            return this;
        }
        public Builder stderr(String stderr) {
            this.stderr = stderr;
            return this;
        }
        public Builder returnCode(int returnCode) {
            this.returnCode = returnCode;
            return this;
        }
        public Builder user(String user) {
            this.user = user;
            return this;
        }
        public Builder state(State state) {
            this.state = state;
            return this;
        }
        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public Result build() {
            if (process == null || taskId == null) {
                log.error("Error while building Result");
                throw new ResultBuilderException("Not all required parameters were set.");
            }
            Type resultType = typeRepository.findOneByName("Result");
            ObjectNode json = mapper.createObjectNode()
                    .put(NAME, "Result")
                    .put(DESC, "A result.")
                    .put(PROCESS, process)
                    .put(TASKID, taskId)
                    .put(DATECREATED, System.currentTimeMillis());

            if (stdout != null) {
                json.put(STDOUT, stdout);
            }
            if (stderr != null) {
                json.put(STDERR, stderr);
            }
            if (returnCode != null) {
                json.put(RETURNCODE, returnCode);
            }
            if (user != null) {
                json.put(USER, user);
            }
            if (error != null) {
                json.put(ERROR, error);
            }
            if (state != null) {
                json.put(STATECODE, state.code);
                json.put(STATEINTERPRETATION, state.interpretation);
                json.put(STATEDESCRIPTION, state.description);
            }

            return new Result(json, resultType);
        }
    }

    // Empty constructor for Spring
    public Result() {}

    private Result(ObjectNode json, Type type) {
        super(json, type);

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
        return (String) data.getProperty(PROCESS);
    }

    public String getStdout() {
        return (String) data.getProperty(STDOUT);
    }

    public String getStderr() {
        return (String) data.getProperty(STDERR);
    }

    public long getDate() {
        return Long.parseLong((String) data.getProperty(DATECREATED));
    }

    public int getReturnCode() {
        return Integer.parseInt((String) data.getProperty(RETURNCODE));
    }

    public String getTaskId() {
        return (String) data.getProperty(TASKID);
    }

    public Result getNext() {
        return next;
    }

    public Optional<String> getUser() {
        return data.hasProperty(USER) ?
                Optional.of((String) data.getProperty(USER)) :
                Optional.empty();
    }

    public Optional<State> getState() {
        final Optional<State> maybeState;
        if (data.hasProperty(STATECODE)) {
            maybeState = Optional.of(new Result.State(Integer.parseInt((String) data.getProperty(STATECODE)),
                    (String) data.getProperty(STATEINTERPRETATION), (String) data.getProperty(STATEDESCRIPTION)));
        } else {
            maybeState = Optional.empty();
        }
        return maybeState;
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

    /**
     * Add this result to an instance.
     * @param instance The instance this should be added to.
     * @return The instance this result was added to. Make sure that that instance
     * is saved, otherwise the relationship between the instance and this result won't
     * persist.
     */
    public Instance add(Instance instance) {
        instance.addResult(this);
        return instance;
    }

    /**
     * Add this result to an instance.
     * @param instanceUuid The UUID of the instance this should be added to.
     * @return The instance this result was added to. Make sure that that instance
     * is saved, otherwise the relationship between the instance and this result won't
     * persist.
     */
    public Instance add(UUID instanceUuid) {
        Instance foundInstance = instanceRepository.findByUuid(instanceUuid.toString());
        Validate.notNull(foundInstance);
        return add(foundInstance);
    }

    /**
     * We need to override this because our model views Results as instances as well,
     * however, adding a result to a result doesn't make sense, so we throw an Exception
     * when that happens.
     * @param result A result.
     * @return Nothing, this always throws an {@link IllegalEntityType}.
     */
    @Override
    public Instance addResult(Result result) {
        throw new IllegalEntityType("Cannot add a result to a result.");
    }

}
