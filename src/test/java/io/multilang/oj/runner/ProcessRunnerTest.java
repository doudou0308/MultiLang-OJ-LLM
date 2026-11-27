package io.multilang.oj.runner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class ProcessRunnerTest {
    @TempDir
    Path directory;

    private final ProcessRunner runner = new ProcessRunner();

    @Test
    void stopsProcessWhenExecutionTimeLimitIsReached() throws IOException, InterruptedException {
        ProcessRunner.Result result = runner.run(
                List.of("sh", "-c", "sleep 2"), directory, new byte[0], 100, 1024);

        assertTrue(result.timedOut(), "the process should be marked as timed out");
        assertFalse(result.outputLimit(), "a timeout must not be reported as output overflow");
    }

    @Test
    void stopsProcessWhenStdoutExceedsOutputLimit() throws IOException, InterruptedException {
        ProcessRunner.Result result = runner.run(
                List.of("sh", "-c", "printf '0123456789abcdef0123456789abcdef'"),
                directory, new byte[0], 3000, 16);

        assertTrue(result.outputLimit(), "stdout beyond the configured byte limit should be rejected");
        assertTrue(result.stdout().getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 16);
    }

    @Test
    void stopsProcessWhenStderrExceedsOutputLimit() throws IOException, InterruptedException {
        ProcessRunner.Result result = runner.run(
                List.of("sh", "-c", "printf '0123456789abcdef0123456789abcdef' >&2"),
                directory, new byte[0], 3000, 16);

        assertTrue(result.outputLimit(), "stderr beyond the configured byte limit should be rejected");
        assertTrue(result.stderr().getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 16);
    }
}
