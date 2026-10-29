package tech.raaf.structure.helper.loader;

import java.nio.file.Path;
import java.util.Map;

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
}
