package tech.raaf.structure.graph;

import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.graph.repositories.ResultRepository;

public class Result extends Instance {

  public static final String NAME = "name";
  public static final String DESC = "desc";
  public static final String PROCESS = "process";
  public static final String TASKID = "task";
  public static final String STDOUT = "stdout";
  public static final String STDERR = "stderr";
  public static final String RETURNCODE = "returnCode";
  public static final String DATECREATED = "dateCreated";
  public static final String USER = "user";
  public static final String STATECODE = "stateCode";
  public static final String STATEINTERPRETATION = "stateInterpretation";
  public static final String STATEDESCRIPTION = "stateDescription";
  public static final String ERROR = "error";

  public static final ResultRepository resultRepository = Context.getResultRepository();

  static final Logger log = LoggerFactory.getLogger(Result.class);

  /**
   * A state is a simple object that allows us to express the "goodness" of a return value.
   */
  public static class State {

    protected final int code;
    protected final String interpretation;
    protected final String description;

    public State(int code, String interpretation, String description) {
      Validate.inclusiveBetween(0, 2, code);
      Objects.requireNonNull(interpretation);
      Objects.requireNonNull(description);
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

  // Empty constructor for NodeMapper
  public Result() {}

  /**
   * Persist the result to database.
   * @return The result this was called on.
   */
  public Result save() {
    Graph.transaction(this::write);
    return this;
  }

  public String getProcess() {
    return (String) data.get(PROCESS);
  }

  public String getStdout() {
    return (String) data.get(STDOUT);
  }

  public String getStderr() {
    return (String) data.get(STDERR);
  }

  public long getDate() {
    return ((Number) data.get(DATECREATED)).longValue();
  }

  public int getReturnCode() {
    return ((Number) data.get(RETURNCODE)).intValue();
  }

  public String getTaskId() {
    return (String) data.get(TASKID);
  }

  public Optional<String> getUser() {
    return data.containsKey(USER) ? Optional.of((String) data.get(USER)) : Optional.empty();
  }

  public Optional<State> getState() {
    final Optional<State> maybeState;
    if (data.containsKey(STATECODE)) {
      maybeState = Optional.of(
        new Result.State(
          ((Number) data.get(STATECODE)).intValue(),
          (String) data.get(STATEINTERPRETATION),
          (String) data.get(STATEDESCRIPTION)
        )
      );
    } else {
      maybeState = Optional.empty();
    }
    return maybeState;
  }
}
