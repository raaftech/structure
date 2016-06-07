package com.raaftech.structure.helper;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Set;

import static io.vertx.core.json.Json.mapper;

/**
 * Created by Boy van Duuren &lt;boy@raaftech.com&gt;.
 */
public class ProcessRunner {

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
            processResult.stderr = readFile(tempErr.getPath());
            processResult.stdout = readFile(tempOut.getPath());
        } catch (IOException e) {
            e.printStackTrace();
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
        // TODO: This is probably not portable, need to test on windows
        FileAttribute<Set<PosixFilePermission>> attrs = PosixFilePermissions.asFileAttribute(
                PosixFilePermissions.fromString("rw-------"));
        tempOut = Files.createTempFile("out", null, attrs).toFile();
        tempErr = Files.createTempFile("out", null, attrs).toFile();

        // Clean up our TempFiles
        tempOut.deleteOnExit();
        tempErr.deleteOnExit();

        // Setup our ProcessBuilder, redirecting stdout and stderr to our TempFiles
        processBuilder.command(tokenizeCommand(command))
                .redirectOutput(tempOut)
                .redirectError(tempErr);
        process = processBuilder.start();
    }

    private static String[] tokenizeCommand(String commandLine) {
        String[] tokens = commandLine.split("\\s");
        ArrayList<String> process = new ArrayList<>();
        State state = State.NORMAL;
        String currentNewToken = null;

        for (String token: tokens) {
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
        public int returnCode;
        public String process;
        public String stdout;
        public String stderr;

        @Override
        public String toString() {
            return "return code: " + returnCode + "\n" +
                    "stdout: " + stdout + "\n" +
                    "stderr: " + stderr;
        }

        public ObjectNode toJson() {
            return mapper.createObjectNode()
                    .put("returnCode", returnCode)
                    .put("stdout", stdout)
                    .put("stderr", stderr);
        }
    }
}
