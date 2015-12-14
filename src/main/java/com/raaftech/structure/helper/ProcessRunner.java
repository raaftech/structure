package com.raaftech.structure.helper;

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
import java.util.concurrent.TimeUnit;

/**
 * Created by boy on 9/16/15.
 */
public class ProcessRunner {

    private static String readFile(String path) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, Charset.defaultCharset());
    }

    /**
     * Run a command on the system.
     * ProcessBuilder has some pitfalls, ProcessRunner takes care of those.
     * @param command The command to run, e.g. "uname -a"
     * @param timeout The amount of time in milliseconds to wait before destroying the process.
     * @return a ProcessResult which contains the return code, stdout and stderr.
     * @throws IOException Thrown when the system can't find the called program.
     * @throws InterruptedException Thrown when the current thread is interrupted while waiting.
     */
    public static ProcessResult run(String command, long timeout) throws IOException, InterruptedException {
        // We use a TempFiles so we don't have to deal with
        // consuming buffers to avoid deadlocking. Most OS-es buffer new files in memory
        // first anyway, so reading these TempFiles should happen from memory.
        final ProcessResult processResult = new ProcessResult();
        // TODO: This is probably not portable, need to test on windows
        FileAttribute<Set<PosixFilePermission>> attrs = PosixFilePermissions.asFileAttribute(
                PosixFilePermissions.fromString("rw-------"));
        final File tempOut = Files.createTempFile("out", null, attrs).toFile();
        final File tempErr = Files.createTempFile("out", null, attrs).toFile();
        final ProcessBuilder processBuilder = new ProcessBuilder();

        // Clean up our TempFiles
        tempOut.deleteOnExit();
        tempErr.deleteOnExit();

        // Setup our ProcessBuilder, redirecting stdout and stderr to our TempFiles
        processBuilder.command(tokenizeCommand(command))
                .redirectOutput(tempOut)
                .redirectError(tempErr);
        Process process = processBuilder.start();

        // Wait for process to exit for a max of timeout ms.
        if (!process.waitFor(timeout, TimeUnit.MILLISECONDS)) {
            process.destroy();
        }
        // Read output and return
        processResult.process = command;
        processResult.returnCode = process.waitFor();
        processResult.stdout = readFile(tempOut.getPath());
        processResult.stderr = readFile(tempErr.getPath());

        return processResult;
    }

    private static String[] tokenizeCommand(String commandLine) {
        String[] tokens = commandLine.split("\\s");
        ArrayList<String> process = new ArrayList<String>();
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

        if (state == State.IN_DOUBLEQUOTES || state == state.IN_QUOTES) {
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
    }
}
