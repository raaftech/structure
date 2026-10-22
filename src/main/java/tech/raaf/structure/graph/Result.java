package tech.raaf.structure.graph;

import com.google.common.collect.ImmutableMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.exceptions.IllegalEntityType;
import tech.raaf.structure.graph.repositories.InstanceRepository;
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

  private static final InstanceRepository instanceRepository = Context.getInstanceRepository();

  /**
   * A state is a simple object that allows us to express the "goodness" of a return value.
   */
  public static class State {

    protected final int code;
    protected final String interpretation;
    protected final String description;

    public State(int code, String interpretation, String description) {
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

  public Result getNext() {
    return relatedOne("-[:NEXT_RESULT]->(b:Result)");
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

  public Result setNext(Result next) {
    if (getNext() == null) {
      Graph.query(
        "MATCH (a:Result {uuid: $from}), (b:Result {uuid: $to}) MERGE (a)-[:NEXT_RESULT]->(b)",
        ImmutableMap.of("from", getUuid(), "to", next.getUuid())
      );
    } else {
      throw new IllegalStateException("Can't set next result on \"" + getUuid() + "\", it already has one.");
    }
    return this;
  }

  public Result getPrevious() {
    return relatedOne("<-[:NEXT_RESULT]-(b:Result)");
  }

  public List<Result> getThisAndNewer() {
    LinkedList<Result> results = new LinkedList<>();

    Result head = this;
    do {
      results.add(head);
      head = head.getNext();
    } while (head != null);

    return results;
  }

  public List<Result> getThisAndOlder() {
    LinkedList<Result> results = new LinkedList<>();

    Result head = this;
    do {
      results.add(head);
      head = head.getPrevious();
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
