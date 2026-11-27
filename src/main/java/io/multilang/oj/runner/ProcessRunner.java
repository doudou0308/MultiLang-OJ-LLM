package io.multilang.oj.runner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class ProcessRunner {
    private static final Set<String> ENVIRONMENT =
            Set.of("PATH", "SYSTEMROOT", "WINDIR", "PATHEXT", "COMSPEC");
    private static final boolean LINUX = System.getProperty("os.name").equalsIgnoreCase("Linux");

    record Result(int exitCode, boolean timedOut, boolean outputLimit, long elapsedMs,
                  String stdout, String stderr) {}

    Result run(List<String> command, Path directory, byte[] input, int timeoutMs, int outputLimit)
            throws IOException, InterruptedException {
        Path inputFile = directory.resolve("stdin.txt");
        Files.write(inputFile, input);
        ProcessBuilder builder = new ProcessBuilder(command).directory(directory.toFile())
                .redirectInput(inputFile.toFile());
        Map<String, String> env = builder.environment();
        env.keySet().removeIf(name -> !ENVIRONMENT.contains(name.toUpperCase(Locale.ROOT)));
        for (String name : List.of("HOME", "TEMP", "TMP", "TMPDIR")) {
            env.put(name, directory.toString());
        }

        if (LINUX) {
            return runWithLinuxPipes(builder, timeoutMs, outputLimit);
        }
        long started = System.nanoTime();
        Process process = builder.start();
        return collect(process, process.getInputStream(), process.getErrorStream(), started,
                timeoutMs, outputLimit);
    }

    private Result runWithLinuxPipes(ProcessBuilder builder, int timeoutMs, int outputLimit)
            throws IOException, InterruptedException {
        // The Linux JDK can close its own pipes as soon as it reaps the direct child.
        Path pipeDirectory = Files.createTempDirectory("judge-output-");
        Path stdout = pipeDirectory.resolve("stdout");
        Path stderr = pipeDirectory.resolve("stderr");
        try {
            createPipes(stdout, stderr);
            // Holding both ends open lets the JVM open the readers and start the child.
            try (FileChannel stdoutKeeper = openPipeKeeper(stdout);
                 FileChannel stderrKeeper = openPipeKeeper(stderr);
                 FileChannel stdoutReader = FileChannel.open(stdout, StandardOpenOption.READ);
                 FileChannel stderrReader = FileChannel.open(stderr, StandardOpenOption.READ)) {
                builder.redirectOutput(stdout.toFile()).redirectError(stderr.toFile());
                long started = System.nanoTime();
                Process process = builder.start();
                try {
                    stdoutKeeper.close();
                    stderrKeeper.close();
                } catch (IOException exception) {
                    stop(process);
                    throw exception;
                }
                return collect(process, Channels.newInputStream(stdoutReader),
                        Channels.newInputStream(stderrReader), started, timeoutMs, outputLimit);
            }
        } finally {
            Files.deleteIfExists(stdout);
            Files.deleteIfExists(stderr);
            Files.deleteIfExists(pipeDirectory);
        }
    }

    private static void createPipes(Path stdout, Path stderr) throws IOException, InterruptedException {
        Process setup = new ProcessBuilder("mkfifo", stdout.toString(), stderr.toString())
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
        try {
            if (!setup.waitFor(2, TimeUnit.SECONDS) || setup.exitValue() != 0) {
                throw new IOException("Could not create judge output pipes");
            }
        } finally {
            if (setup.isAlive()) {
                setup.destroyForcibly();
            }
        }
    }

    private static FileChannel openPipeKeeper(Path pipe) throws IOException {
        return FileChannel.open(pipe, StandardOpenOption.READ, StandardOpenOption.WRITE);
    }

    private Result collect(Process process, InputStream stdoutStream, InputStream stderrStream,
                           long started, int timeoutMs, int outputLimit) throws InterruptedException {
        AtomicBoolean overflow = new AtomicBoolean();
        Capture stdout = new Capture(stdoutStream, outputLimit, overflow);
        Capture stderr = new Capture(stderrStream, outputLimit, overflow);
        Thread out = drain(stdout, "judge-stdout");
        Thread err = drain(stderr, "judge-stderr");
        boolean timedOut = false;
        try {
            while (process.isAlive() && !overflow.get()) {
                long remaining = timeoutMs - elapsed(started);
                if (remaining <= 0) {
                    timedOut = true;
                    break;
                }
                process.waitFor(Math.min(remaining, 15), TimeUnit.MILLISECONDS);
            }
            long elapsedMs = elapsed(started);
            if (process.isAlive()) {
                stop(process);
                process.waitFor(500, TimeUnit.MILLISECONDS);
            }
            // A detached child may still own a pipe. Never wait indefinitely for it.
            out.join(300);
            err.join(300);
            boolean openPipe = out.isAlive() || err.isAlive();
            return new Result(process.isAlive() || openPipe ? -1 : process.exitValue(), timedOut,
                    overflow.get(), elapsedMs, stdout.text(), stderr.text());
        } finally {
            if (process.isAlive()) {
                stop(process);
            }
        }
    }

    private static long elapsed(long started) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
    }

    private static Thread drain(Capture capture, String name) {
        Thread thread = new Thread(capture, name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static void stop(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }

    private static final class Capture implements Runnable {
        private final InputStream stream;
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private final int max;
        private final AtomicBoolean overflow;

        private Capture(InputStream stream, int max, AtomicBoolean overflow) {
            this.stream = stream;
            this.max = max;
            this.overflow = overflow;
        }

        @Override
        public void run() {
            try (stream) {
                byte[] buffer = new byte[4096];
                int n;
                while ((n = stream.read(buffer)) != -1) {
                    int keep = append(buffer, n);
                    if (keep < n) {
                        overflow.set(true);
                        break;
                    }
                }
            } catch (IOException ignored) {
                // Killing the process may close its pipes before the last read.
            }
        }

        private synchronized int append(byte[] incoming, int length) {
            int keep = Math.min(length, max - bytes.size());
            bytes.write(incoming, 0, keep);
            return keep;
        }

        synchronized String text() {
            return bytes.toString(StandardCharsets.UTF_8);
        }
    }
}
