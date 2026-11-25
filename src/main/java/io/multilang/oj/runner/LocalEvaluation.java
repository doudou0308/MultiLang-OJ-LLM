package io.multilang.oj.runner;

import io.multilang.oj.catalog.Exercise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.stream.Stream;

@Service
public class LocalEvaluation {
    private static final Logger LOG = LoggerFactory.getLogger(LocalEvaluation.class);
    private static final int COMPILE_TIMEOUT_MS = 15000;
    private static final int COMPILE_OUTPUT_BYTES = 8192;
    private final Semaphore slots = new Semaphore(2);
    private final ProcessRunner processes = new ProcessRunner();

    public enum Verdict { ACCEPTED, WRONG_OUTPUT, COMPILE_ERROR, TIME_LIMIT, OUTPUT_LIMIT, RUNTIME_ERROR }

    public record Check(int position, boolean example, Verdict verdict, long elapsedMs, String output) {}

    public record Report(Verdict verdict, int caseCount, String compilerMessage, List<Check> checks) {
        public Report {
            checks = List.copyOf(checks);
        }
    }

    public Report evaluate(Exercise exercise, Language language, String source) {
        return inWorkspace(language, source, (build, root) -> {
            List<Check> checks = new ArrayList<>();
            for (int i = 0; i < exercise.cases().size(); i++) {
                Exercise.Case test = exercise.cases().get(i);
                checks.add(check(language, build, root, i + 1, test.example(),
                        test.input(), test.expected(), exercise.policy()));
            }
            Verdict verdict = checks.stream().map(Check::verdict)
                    .filter(result -> result != Verdict.ACCEPTED).findFirst().orElse(Verdict.ACCEPTED);
            return new Report(verdict, exercise.cases().size(), "", checks);
        }, exercise.cases().size());
    }

    public Report tryInput(Exercise.Policy rules, Language language, String source, String input) {
        return inWorkspace(language, source, (build, root) -> {
            Check check = check(language, build, root, 1, true, input, null, rules);
            return new Report(check.verdict(), 1, "", List.of(check));
        }, 1);
    }

    private Report inWorkspace(Language language, String source, CompiledTask task, int caseCount) {
        if (language == null || source == null || source.isBlank() || Exercise.utf8Size(source) > 32000) {
            throw new IllegalArgumentException("请选择语言并输入 32 KB 以内的源代码");
        }
        if (!slots.tryAcquire()) {
            throw new RunnerBusy();
        }
        Path root = null;
        try {
            root = Files.createTempDirectory("local-judge-").toAbsolutePath();
            Path build = Files.createDirectory(root.resolve("build"));
            Files.writeString(build.resolve(language.fileName()), source, StandardCharsets.UTF_8);
            ProcessRunner.Result compiled = processes.run(language.compile(build), build, new byte[0],
                    COMPILE_TIMEOUT_MS, COMPILE_OUTPUT_BYTES);
            if (compiled.exitCode() != 0 || compiled.timedOut() || compiled.outputLimit()) {
                String diagnostic = compiled.timedOut() ? "编译超时"
                        : compiled.outputLimit() ? "编译输出超过限制"
                        : (compiled.stderr() + compiled.stdout()).replace(root.toString(), "<workspace>");
                return new Report(Verdict.COMPILE_ERROR, caseCount, diagnostic, List.of());
            }
            return task.run(build, root);
        } catch (IOException ex) {
            LOG.warn("{} toolchain or local runner unavailable: {}", language, ex.getClass().getSimpleName());
            throw new RunnerUnavailable("无法启动 " + language + " 工具链或创建本地运行目录");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RunnerUnavailable("本地判题已中断");
        } finally {
            try {
                if (root != null) {
                    clean(root);
                }
            } finally {
                slots.release();
            }
        }
    }

    private Check check(Language language, Path build, Path root, int position, boolean example,
                        String input, String expected, Exercise.Policy rules)
            throws IOException, InterruptedException {
        Path run = Files.createDirectory(root.resolve("case-" + position));
        ProcessRunner.Result result = processes.run(language.run(build), run, input.getBytes(StandardCharsets.UTF_8),
                rules.timeMs(), rules.outputBytes());
        Verdict verdict;
        if (result.timedOut()) verdict = Verdict.TIME_LIMIT;
        else if (result.outputLimit()) verdict = Verdict.OUTPUT_LIMIT;
        else if (result.exitCode() != 0) verdict = Verdict.RUNTIME_ERROR;
        else if (expected == null || compare(rules.comparison(), result.stdout(), expected)) verdict = Verdict.ACCEPTED;
        else verdict = Verdict.WRONG_OUTPUT;
        String output = example ? result.stdout() : "";
        return new Check(position, example, verdict, result.elapsedMs(), output);
    }

    static boolean compare(Exercise.Comparison method, String actual, String expected) {
        return normalize(method, actual).equals(normalize(method, expected));
    }

    private static String normalize(Exercise.Comparison method, String text) {
        String lines = text.replace("\r\n", "\n").replace('\r', '\n');
        if (method == Exercise.Comparison.TOKENS) {
            return lines.strip().replaceAll("(?U)\\s+", " ");
        }
        return lines;
    }

    private static void clean(Path root) {
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException | RuntimeException ex) {
            LOG.warn("Unable to remove a local evaluation workspace: {}", ex.getClass().getSimpleName());
        }
    }

    @FunctionalInterface
    private interface CompiledTask {
        Report run(Path build, Path root) throws IOException, InterruptedException;
    }

    public static class RunnerBusy extends RuntimeException {}

    public static class RunnerUnavailable extends RuntimeException {
        public RunnerUnavailable(String message) {
            super(message);
        }
    }
}
