package io.multilang.oj.runner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

@EnabledOnOs(OS.LINUX)
class ProcessRunnerTest {
    @TempDir
    Path directory;

    private final ProcessRunner runner = new ProcessRunner();

    @Test
    void preservesInputOutputAndExitCode() throws IOException, InterruptedException {
        ProcessRunner.Result result = runner.run(
                List.of("sh", "-c", "cat; printf 'diagnostic' >&2; exit 7"),
                directory, "input\n".getBytes(StandardCharsets.UTF_8), 3000, 1024);

        assertEquals(7, result.exitCode());
        assertEquals("input\n", result.stdout());
        assertEquals("diagnostic", result.stderr());
        assertFalse(result.timedOut());
        assertFalse(result.outputLimit());
    }

    @Test
    void preservesTimeoutAndOutputLimit() throws IOException, InterruptedException {
        ProcessRunner.Result timedOut = runner.run(List.of("sleep", "5"),
                directory, new byte[0], 100, 1024);
        assertTrue(timedOut.timedOut());
        assertFalse(timedOut.outputLimit());

        ProcessRunner.Result truncated = runner.run(List.of("printf", "abcdef"),
                directory, new byte[0], 3000, 4);
        assertFalse(truncated.timedOut());
        assertTrue(truncated.outputLimit());
        assertEquals("abcd", truncated.stdout());
    }

    @Test
    void returnsWhenDetachedChildKeepsOutputPipesOpen() throws IOException, InterruptedException {
        Path childPidFile = directory.resolve("child.pid");
        long started = System.nanoTime();

        try {
            ProcessRunner.Result result = runner.run(
                    List.of("sh", "-c",
                            "sleep 5 & child=$!; printf '%s' \"$child\" > child.pid; exit 0"),
                    directory, new byte[0], 3000, 1024);

            long elapsedMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(
                    System.nanoTime() - started);

            assertTrue(Files.exists(childPidFile), "the child process should have recorded its PID");
            assertTrue(elapsedMs < 2000,
                    "the runner should return without waiting for the detached child to exit");
            assertEquals(-1, result.exitCode(),
                    "an open inherited output pipe must not be reported as a normal exit");
        } finally {
            if (Files.exists(childPidFile)) {
                long childPid = Long.parseLong(Files.readString(childPidFile).trim());
                ProcessHandle.of(childPid).ifPresent(ProcessHandle::destroyForcibly);
            }
        }
    }
}
