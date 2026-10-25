package tech.raaf.structure.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * The settings of Structure. {@link #get(Setting)} reads the value of a
 * setting from these sources, highest priority first: the command line,
 * the environment, the system properties, config.properties on the
 * class path, and the default of the setting.
 *
 * From the name neo4j-bolt-uri and the prefix structure follow the
 * option --neo4j-bolt-uri, the environment variable
 * STRUCTURE_NEO4J_BOLT_URI and the property structure.neo4j.bolt.uri.
 *
 * @author Freek Alleman <freek@raaf.tech>
 * @since 2016-09-30
 */
public final class Configuration {

  // The name prefix of the settings of a normal run
  private static final String PREFIX = "structure";
  private static final Properties FILE = load("/config.properties");
  private static Configuration current = new Configuration(
    Collections.emptyMap(),
    System.getenv(),
    System.getProperties(),
    FILE
  );

  // The sources of values, highest priority first
  private final Map<String, String> arguments;
  private final Map<String, String> environment;
  private final Properties system;
  private final Properties file;

  Configuration(Map<String, String> arguments, Map<String, String> environment, Properties system, Properties file) {
    this.arguments = arguments;
    this.environment = environment;
    this.system = system;
    this.file = file;
  }

  /**
   * Use the given command line arguments as the highest priority source.
   * @throws ParseException when an argument is not a valid option.
   */
  public static void init(String[] args) throws ParseException {
    current = new Configuration(parse(args), System.getenv(), System.getProperties(), FILE);
  }

  /**
   * Get the value of a setting.
   * @throws IllegalArgumentException when the value is missing or not valid.
   */
  public static <T> T get(Setting<T> setting) {
    return get(PREFIX, setting);
  }

  /**
   * Get the value of a setting, with a different name prefix for the
   * environment variable and the property. For example, the tests use
   * test.structure, which reads TEST_STRUCTURE_NEO4J_BOLT_URI.
   * @throws IllegalArgumentException when the value is missing or not valid.
   */
  public static <T> T get(String prefix, Setting<T> setting) {
    return current.resolve(prefix, setting);
  }

  /**
   * Get the value of each setting.
   * @throws IllegalArgumentException when a value is missing or not valid.
   */
  public static void check() {
    for (Setting<?> setting : Setting.ALL) {
      get(setting);
    }
  }

  /**
   * Write the value of each setting to target/structure.json in home,
   * for the tools in ext. Only the owner can read the file, as it has
   * the passwords.
   */
  public static void write() throws IOException {
    current.write(get(Setting.HOME).resolve("target/structure.json"));
  }

  void write(Path file) throws IOException {
    final Map<String, Object> values = new LinkedHashMap<>();
    for (Setting<?> setting : Setting.ALL) {
      if (setting != Setting.HELP) {
        final Object value = resolve(PREFIX, setting);
        values.put(setting.name, value instanceof Path ? value.toString() : value);
      }
    }
    Files.createDirectories(file.toAbsolutePath().getParent());
    Files.deleteIfExists(file);
    Files.createFile(file, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
    new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(file.toFile(), values);
  }

  // True when the log level is trace
  public static boolean isTraceModeOn() {
    return "trace".equalsIgnoreCase(get(Setting.LOG_LEVEL));
  }

  // Print the options, with their defaults
  public static void printUsage() {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.setOptionComparator(null);
    formatter.setWidth(100);
    formatter.printHelp(
      "structure [options]",
      "",
      options(),
      "\nEach option also has an environment variable and a property, which is a system property or " +
        "a key in config.properties. For --neo4j-pass, these are STRUCTURE_NEO4J_PASS and " +
        "structure.neo4j.pass."
    );
  }

  <T> T resolve(String prefix, Setting<T> setting) {
    final String property = property(prefix, setting);
    final String value = Stream.of(
      arguments.get(setting.name),
      environment.get(variable(prefix, setting)),
      system.getProperty(property),
      file.getProperty(property),
      setting.fallback
    )
      .filter(Objects::nonNull)
      .findFirst()
      .orElseThrow(() ->
        new IllegalArgumentException(
          String.format("%s has no value: set %s or %s", setting.name, variable(prefix, setting), property)
        )
      );
    try {
      return setting.modifier.apply(setting.type.parser.apply(value));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
        String.format("%s is not a valid %s: %s", setting.name, setting.type.name, value),
        e
      );
    }
  }

  static String property(String prefix, Setting<?> setting) {
    return (prefix + "." + setting.name).replace('-', '.');
  }

  static String variable(String prefix, Setting<?> setting) {
    return property(prefix, setting).toUpperCase(Locale.ROOT).replace('.', '_');
  }

  // The given options by name; a flag has the value true
  static Map<String, String> parse(String[] args) throws ParseException {
    final CommandLine line = new DefaultParser().parse(options(), args);
    if (!line.getArgList().isEmpty()) {
      throw new ParseException("Unexpected argument: " + line.getArgList().get(0));
    }
    final Map<String, String> arguments = new HashMap<>();
    for (Option option : line.getOptions()) {
      arguments.put(option.getLongOpt(), option.getValue("true"));
    }
    return arguments;
  }

  private static Options options() {
    final Options options = new Options();
    for (Setting<?> setting : Setting.ALL) {
      final boolean flag = setting.type == Setting.Type.FLAG;
      final String fallback =
        setting.fallback == null ? " (required)" : flag ? "" : String.format(" (default: %s)", setting.fallback);
      options.addOption(
        Option.builder()
          .longOpt(setting.name)
          .hasArg(!flag)
          .argName(setting.type.name)
          .desc(setting.description + fallback)
          .build()
      );
    }
    return options;
  }

  private static Properties load(String resource) {
    final Properties properties = new Properties();
    try (InputStream stream = Configuration.class.getResourceAsStream(resource)) {
      if (stream != null) {
        properties.load(stream);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return properties;
  }

  /**
   * A setting: a name, a type, a default, a description and a modifier
   * that {@link Configuration#get(Setting)} applies after it parses the
   * value. A setting without a default is required.
   */
  public static final class Setting<T> {

    public static final Setting<Boolean> HELP = new Setting<>("help", Type.FLAG, "false", "Print usage and exit");
    public static final Setting<String> LOG_LEVEL = new Setting<>(
      "log-level",
      Type.TEXT,
      "info",
      "info, debug or trace; trace also prints stack traces"
    );
    public static final Setting<Boolean> UNSAFE = new Setting<>(
      "unsafe",
      Type.FLAG,
      "false",
      "Allow all event bus addresses"
    );
    public static final Setting<List<String>> BUNDLE_PATH = new Setting<>(
      "bundle-path",
      Type.LIST,
      "src/main/resources/bundles",
      "Comma-separated bundle paths"
    );
    public static final Setting<Path> HOME = new Setting<>(
      "home",
      Type.PATH,
      System.getProperty("user.dir"),
      "Structure home directory"
    );
    // A relative path is relative to home
    public static final Setting<Path> JKS_FILE = new Setting<>(
      "jks-file",
      Type.PATH,
      "target/structure.jks",
      "JKS keystore for HTTPS, relative to home; made if missing",
      file -> Configuration.get(HOME).resolve(file)
    );
    public static final Setting<String> JKS_PASS = new Setting<>(
      "jks-pass",
      Type.TEXT,
      "changeit",
      "Keystore password"
    );
    public static final Setting<String> NEO4J_BOLT_URI = new Setting<>(
      "neo4j-bolt-uri",
      Type.TEXT,
      "bolt://localhost:7687",
      "Neo4j Bolt server"
    );
    public static final Setting<String> NEO4J_HTTP_URI = new Setting<>(
      "neo4j-http-uri",
      Type.TEXT,
      "http://localhost:7474",
      "Neo4j Query API, for the tools in ext"
    );
    public static final Setting<String> NEO4J_USER = new Setting<>("neo4j-user", Type.TEXT, "neo4j", "Neo4j user");
    public static final Setting<String> NEO4J_PASS = new Setting<>("neo4j-pass", Type.TEXT, null, "Neo4j password");
    public static final Setting<String> HTTPS_HOST = new Setting<>(
      "https-host",
      Type.TEXT,
      "localhost",
      "HTTPS server host; also used in schema URIs"
    );
    public static final Setting<Integer> HTTPS_PORT = new Setting<>(
      "https-port",
      Type.INTEGER,
      "8443",
      "HTTPS server port"
    );
    public static final Setting<String> REST_HOST = new Setting<>(
      "rest-host",
      Type.TEXT,
      "localhost",
      "Host of the REST server in ext/rest"
    );
    public static final Setting<Integer> REST_PORT = new Setting<>(
      "rest-port",
      Type.INTEGER,
      "3443",
      "Port of the REST server in ext/rest"
    );
    // Vert.x takes nanoseconds
    public static final Setting<Long> WORKER_TIMEOUT = new Setting<>(
      "worker-timeout",
      Type.LONG,
      "300",
      "Maximum worker run time, in seconds",
      TimeUnit.SECONDS::toNanos
    );
    // Jobs are processes that mostly wait on the network
    public static final Setting<Integer> JOB_POOL = new Setting<>(
      "job-pool",
      Type.INTEGER,
      String.valueOf(4 * Runtime.getRuntime().availableProcessors()),
      "Maximum number of jobs that run at the same time"
    );

    // All settings, in the order of the usage
    private static final List<Setting<?>> ALL = Collections.unmodifiableList(
      Arrays.asList(
        HELP,
        LOG_LEVEL,
        UNSAFE,
        BUNDLE_PATH,
        HOME,
        JKS_FILE,
        JKS_PASS,
        NEO4J_BOLT_URI,
        NEO4J_HTTP_URI,
        NEO4J_USER,
        NEO4J_PASS,
        HTTPS_HOST,
        HTTPS_PORT,
        REST_HOST,
        REST_PORT,
        WORKER_TIMEOUT,
        JOB_POOL
      )
    );

    private final String name;
    private final Type<T> type;
    private final String fallback;
    private final String description;
    private final UnaryOperator<T> modifier;

    Setting(String name, Type<T> type, String fallback, String description) {
      this(name, type, fallback, description, UnaryOperator.identity());
    }

    Setting(String name, Type<T> type, String fallback, String description, UnaryOperator<T> modifier) {
      this.name = name;
      this.type = type;
      this.fallback = fallback;
      this.description = description;
      this.modifier = modifier;
    }

    // The type of a setting: a name for the usage and a parser
    static final class Type<T> {

      static final Type<Boolean> FLAG = new Type<>("flag", Type::flag);
      static final Type<String> TEXT = new Type<>("text", value -> value);
      static final Type<Integer> INTEGER = new Type<>("integer", Integer::valueOf);
      static final Type<Long> LONG = new Type<>("long", Long::valueOf);
      static final Type<Double> DECIMAL = new Type<>("decimal", Double::valueOf);
      static final Type<Path> PATH = new Type<>("path", value -> Paths.get(value));
      static final Type<List<String>> LIST = new Type<>("list", Type::list);

      private final String name;
      private final Function<String, T> parser;

      private Type(String name, Function<String, T> parser) {
        this.name = name;
        this.parser = parser;
      }

      private static Boolean flag(String value) {
        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
          throw new IllegalArgumentException("Not true or false: " + value);
        }
        return Boolean.valueOf(value);
      }

      private static List<String> list(String value) {
        return Stream.of(value.split(","))
          .map(String::trim)
          .filter(item -> !item.isEmpty())
          .collect(Collectors.toList());
      }
    }
  }
}
