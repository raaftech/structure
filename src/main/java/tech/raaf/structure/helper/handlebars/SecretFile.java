package tech.raaf.structure.helper.handlebars;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;

/**
 * SecretFile can be used to give a secret, such as a password, to a
 * program in a file. The block helper writes its content to a file that
 * only the owner can read, and gives the path of the file. The template
 * sets the format of the content that the program reads.
 */
public class SecretFile implements Helper<Object> {

  private final Path directory;
  private final List<Path> files;

  // Each file that this helper writes goes to files, to delete later
  public SecretFile(Path directory, List<Path> files) {
    this.directory = directory;
    this.files = files;
  }

  @Override
  public CharSequence apply(Object context, Options options) throws IOException {
    Files.createDirectories(directory);
    final Path file = Files.createTempFile(
      directory,
      "secret",
      null,
      PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))
    );
    files.add(file);
    Files.write(file, options.fn().toString().getBytes(StandardCharsets.UTF_8));
    // Only commands that Structure runs can read the file, these use sh
    return new Handlebars.SafeString(Quote.SH.quote(file.toString()));
  }
}
