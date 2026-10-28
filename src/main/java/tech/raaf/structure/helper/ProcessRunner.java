package tech.raaf.structure.helper;

import static tech.raaf.structure.helper.JsonHelper.mapper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.raaf.structure.base.Configuration;
import tech.raaf.structure.base.Configuration.Setting;
import tools.jackson.databind.node.ObjectNode;

/**
 * @author Boy van Duuren <boy@raaf.tech>
 */
public class ProcessRunner {

  private static final Logger log = LoggerFactory.getLogger(ProcessRunner.class);

  private final ProcessBuilder processBuilder = new ProcessBuilder();
  private final ProcessResult processResult = new ProcessResult();
  private final String command;
  private Process process;

  File tempErr;
  File tempOut;

  public ProcessRunner(String command) {
    this.command = command;
    environment().put("STRUCTURE_STATE_DIR", Configuration.get(Setting.STATE_DIR).toString());
    environment().put("NODE_EXTRA_CA_CERTS", KeystoreHelper.crt(Configuration.get(Setting.JKS_FILE)).toString());
  }

  private static String readFile(String path) throws IOException {
    byte[] encoded = Files.readAllBytes(Paths.get(path));
    return new String(encoded, Charset.defaultCharset());
  }

  public void cancel() {
    process.destroy();
  }

  public ProcessResult finish() {
    try {
      processResult.process = command;
      processResult.stderr = tempErr != null ? Optional.of(readFile(tempErr.getPath())) : Optional.empty();
      processResult.stdout = tempOut != null ? Optional.of(readFile(tempOut.getPath())) : Optional.empty();
      try {
        processResult.returnCode = process != null ? Optional.of(process.exitValue()) : Optional.empty();
      } catch (IllegalThreadStateException e) {
        if (Configuration.isTraceModeOn()) {
          log.error(e.getMessage());
          e.printStackTrace();
        }
        processResult.returnCode = Optional.empty();
      }
    } catch (IOException e) {
      if (Configuration.isTraceModeOn()) {
        e.printStackTrace();
      }
    }

    return processResult;
  }

  // The environment of the process; change it before run.
  public Map<String, String> environment() {
    return processBuilder.environment();
  }

  public String getCommand() {
    return command;
  }

  public Process getProcess() {
    return process;
  }

  public boolean isRunning() {
    return process.isAlive();
  }

  /**
   * Run a command on the system.
   * ProcessBuilder has some pitfalls, ProcessRunner takes care of those.
   * @throws IOException Thrown when the system can't find the called program.
   * @throws InterruptedException Thrown when the current thread is interrupted while waiting.
   */
  public void run() throws IOException, InterruptedException {
    // We use a TempFiles so we don't have to deal with
    // consuming buffers to avoid deadlocking. Most OS-es buffer new files in memory
    // first anyway, so reading these TempFiles should happen from memory.
    tempOut = createTempFile("out");
    tempErr = createTempFile("out");

    // Clean up our TempFiles
    tempOut.deleteOnExit();
    tempErr.deleteOnExit();

    // Run in state-dir, never in the working directory of Structure.
    final Path directory = Configuration.get(Setting.STATE_DIR);
    Files.createDirectories(directory);

    // Setup our ProcessBuilder, redirecting stdout and stderr to our TempFiles
    processBuilder
      .command(tokenizeCommand(command))
      .directory(directory.toFile())
      .redirectOutput(tempOut)
      .redirectError(tempErr);
    process = processBuilder.start();
  }

  private static File createTempFile(String name) {
    try {
      if (isPosix) {
        final Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-------");
        final FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(perms);
        return Files.createTempFile(name, null, attr).toFile();
      } else {
        return Files.createTempFile(name, null).toFile();
      }
    } catch (final IOException e) {
      log.error(String.format("Cannot create temporary file %s: ", name), e);
      return null;
    }
  }

  private static final boolean isPosix = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

  // Split a command into words as sh does, without escapes or expansions
  private static String[] tokenizeCommand(String command) {
    final List<String> words = new ArrayList<>();
    final StringBuilder word = new StringBuilder();
    boolean inWord = false;
    for (int i = 0; i < command.length(); i++) {
      final char c = command.charAt(i);
      if (c == '\'' || c == '"') {
        final int end = command.indexOf(c, i + 1);
        if (end < 0) {
          throw new IllegalArgumentException("String contains uneven amount of quotes.");
        }
        word.append(command, i + 1, end);
        i = end;
        inWord = true;
      } else if (Character.isWhitespace(c)) {
        if (inWord) {
          words.add(word.toString());
          word.setLength(0);
          inWord = false;
        }
      } else {
        word.append(c);
        inWord = true;
      }
    }
    if (inWord) {
      words.add(word.toString());
    }
    return words.toArray(new String[0]);
  }

  public static class ProcessResult {

    public Optional<Integer> returnCode;
    public String process;
    public Optional<String> stdout;
    public Optional<String> stderr;

    @Override
    public String toString() {
      return "return code: " + returnCode + "\n" + "stdout: " + stdout + "\n" + "stderr: " + stderr;
    }

    public ObjectNode toJson() {
      ObjectNode json = mapper.createObjectNode().put("process", process);
      if (returnCode.isPresent()) {
        json.put("returnCode", returnCode.get());
      }
      if (stdout.isPresent()) {
        json.put("stdout", stdout.get());
      }
      if (stderr.isPresent()) {
        json.put("stderr", stderr.get());
      }
      return json;
    }
  }
}
