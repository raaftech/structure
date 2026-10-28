package tech.raaf.structure.schema;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import tech.raaf.structure.base.Configuration;
import tech.raaf.structure.base.Configuration.Setting;

public class ManualTest extends SchemaTest {

  @Override
  protected Set<String> good() {
    // The sample, and each help file of a message handler
    try (Stream<Path> files = Files.list(Configuration.get(Setting.HOME).resolve("src/main/resources/help"))) {
      return Stream.concat(Stream.of("schema/manualGood01.json"), files.map(file -> "help/" + file.getFileName()))
        .collect(Collectors.toSet());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  protected Set<String> bad() {
    return ImmutableSet.of(
      "schema/manualBad01.json" // testing with bad string pattern
    );
  }

  @Override
  protected String schema() {
    return "resource:/schema/base/manual.schema.json";
  }
}
