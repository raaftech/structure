package tech.raaf.structure.base;

import io.vertx.core.Vertx;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.raaf.structure.graph.Group;
import tech.raaf.structure.graph.repositories.*;

/**
 * Holds the shared objects of Structure: Vert.x and the repositories.
 *
 * @author Rubin Simons <rubin@raaf.tech>
 * @since 2014-11-12
 */
public class Context {

  // The repositories have no state, so one object of each is enough
  private static final NodeRepository nodeRepository = new NodeRepository();
  private static final ComponentRepository componentRepository = new ComponentRepository();
  private static final TypeRepository typeRepository = new TypeRepository();
  private static final InstanceRepository instanceRepository = new InstanceRepository();
  private static final InstrumentRepository instrumentRepository = new InstrumentRepository();
  private static final ResultRepository resultRepository = new ResultRepository();
  private static final ProfileRepository profileRepository = new ProfileRepository();
  private static final GroupingRepository groupingRepository = new GroupingRepository();
  private static Vertx vertx = null;
  private static AtomicBoolean instrumentsLoaded = new AtomicBoolean();
  private static final Logger log = LoggerFactory.getLogger(Context.class);

  // The hostname of the machine structure runs on
  private static String hostname = null;
  // The identifier used to identify this structure instance in the database.
  private static String selfIdentifier = null;
  // The Grouping representing the server Structure is running on
  private static Group structure;
  // The base URI of the schemas that Structure serves.
  private static URI schemaNamespace;

  public static void setHostname(String hostname) {
    if (Context.hostname == null) {
      log.info("Setting hostname to {}", hostname);
      Context.hostname = hostname;
    }
  }

  public static void setSelfIdentifier(String selfIdentifier) {
    log.info("Setting self identifier to {}", selfIdentifier);
    Context.selfIdentifier = selfIdentifier;
  }

  public static void setSelfGroup(Group structure) {
    Context.structure = structure;
  }

  public static String getHostname() {
    return hostname;
  }

  public static String getSelfIdentifier() {
    return selfIdentifier;
  }

  /**
   * This returns the group that represents this Structure instance in the database.
   * @return See description.
   */
  public static Group getSelfGroup() {
    return structure;
  }

  public static URI getSchemaNamespace() {
    return schemaNamespace;
  }

  public static void setSchemaNamespace(URI schemaNamespace) {
    Context.schemaNamespace = schemaNamespace;
  }

  public static NodeRepository getNodeRepository() {
    return nodeRepository;
  }

  public static ComponentRepository getComponentRepository() {
    return componentRepository;
  }

  public static TypeRepository getTypeRepository() {
    return typeRepository;
  }

  public static InstanceRepository getInstanceRepository() {
    return instanceRepository;
  }

  public static InstrumentRepository getInstrumentRepository() {
    return instrumentRepository;
  }

  public static ResultRepository getResultRepository() {
    return resultRepository;
  }

  public static ProfileRepository getProfileRepository() {
    return profileRepository;
  }

  public static GroupingRepository getGroupingRepository() {
    return groupingRepository;
  }

  public static Vertx getVertx() {
    return vertx;
  }

  public static void setVertx(Vertx vertxRef) {
    vertx = vertxRef;
  }

  public static void setInstrumentsLoaded(boolean state) {
    instrumentsLoaded.set(state);
  }

  public static boolean getInstrumentsLoaded() {
    return instrumentsLoaded.get();
  }
}
