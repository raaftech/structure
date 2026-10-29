package tech.raaf.structure.helper.loader;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public final class BundleFiles {

  private static volatile Map<String, Path> sources = Map.of();

  private BundleFiles() {}

  static void register(Map<String, Path> files) {
    sources = Map.copyOf(files);
  }

  public static Path resolve(String uuid, String relativePath) {
    Path source = sources.get(uuid);
    if (source == null) {
      throw new IllegalArgumentException("No bundle file for entity " + uuid);
    }
    return source.getParent().resolve(relativePath).normalize();
  }

  public static Optional<Path> clientDirectory() {
    return Optional.ofNullable(sources.get("fe3ea22e-7096-436a-8a6e-cf0eb2fb2e50")).map(source ->
      source.getParent().resolve("../payload").normalize()
    );
  }
}
