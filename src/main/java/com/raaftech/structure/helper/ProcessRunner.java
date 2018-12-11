package com.raaftech.structure.helper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.base.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

import static io.vertx.core.json.Json.mapper;

/**
 * Created by Boy van Duuren &lt;boy@raaftech.com&gt;.
 */
public class ProcessRunner {

    private final static Logger log = LoggerFactory.getLogger(ProcessRunner.class);

    private final ProcessBuilder processBuilder = new ProcessBuilder();
    private final ProcessResult processResult = new ProcessResult();
    private final String command;
    private Process process;

    File tempErr;
    File tempOut;

    public ProcessRunner(String command) {
        this.command = command;
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

        // Setup our ProcessBuilder, redirecting stdout and stderr to our TempFiles
        processBuilder.command(tokenizeCommand(command)).redirectOutput(tempOut).redirectError(tempErr);
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

    private static final boolean isPosix =
    FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

    private static String[] tokenizeCommand(String commandLine) {
        String[] tokens = commandLine.split("\\s");
        ArrayList<String> process = new ArrayList<>();
        State state = State.NORMAL;
        String currentNewToken = null;

        for (String token : tokens) {
            switch (state) {
            case NORMAL:
                if (token.contains("\"")) {
                    state = State.IN_DOUBLEQUOTES;
                    currentNewToken = token.replace("\"", "");
                    if ((token.length() - currentNewToken.length()) % 2 == 0) {
                        process.add(currentNewToken);
                        state = State.NORMAL;
                    }
                } else if (token.contains("'")) {
                    state = State.IN_QUOTES;
                    currentNewToken = token.replace("'", "");
                    if ((token.length() - currentNewToken.length()) % 2 == 0) {
                        process.add(currentNewToken);
                        state = State.NORMAL;
                    }
                } else {
                    process.add(token);
                }
                break;

            case IN_DOUBLEQUOTES:
                currentNewToken = currentNewToken.concat(" " + token.replace("\"", ""));
                if (token.contains("\"")) {
                    process.add(currentNewToken);
                    state = State.NORMAL;
                }
                break;

            case IN_QUOTES:
                currentNewToken = currentNewToken.concat(" " + token.replace("'", ""));
                if (token.contains("'")) {
                    process.add(currentNewToken);
                    state = State.NORMAL;
                }
                break;
            }
        }

        if (state == State.IN_DOUBLEQUOTES || state == State.IN_QUOTES) {
            throw new IllegalArgumentException("String contains uneven amount of quotes.");
        }

        return process.toArray(new String[process.size()]);
    }

    enum State {
        NORMAL, IN_DOUBLEQUOTES, IN_QUOTES
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
