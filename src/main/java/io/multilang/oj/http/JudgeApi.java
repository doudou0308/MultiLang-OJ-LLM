package io.multilang.oj.http;

import io.multilang.oj.attempts.AttemptStore;
import io.multilang.oj.catalog.Catalog;
import io.multilang.oj.catalog.Exercise;
import io.multilang.oj.runner.Language;
import io.multilang.oj.runner.LocalEvaluation;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2")
public class JudgeApi {
    private final Catalog catalog;
    private final LocalEvaluation evaluator;
    private final AttemptStore attempts;

    public JudgeApi(Catalog catalog, LocalEvaluation evaluator, AttemptStore attempts) {
        this.catalog = catalog;
        this.evaluator = evaluator;
        this.attempts = attempts;
    }

    public record ExerciseView(String slug, String title, String statement, Exercise.Policy policy,
                               List<Exercise.Case> examples) {
        static ExerciseView from(Exercise exercise) {
            return new ExerciseView(exercise.slug(), exercise.title(), exercise.statement(), exercise.policy(),
                    exercise.cases().stream().filter(Exercise.Case::example).toList());
        }
    }

    public record Draft(String slug, String title, String statement, boolean archived, Exercise.Policy policy,
                        List<Exercise.Case> cases) {
        Exercise toExercise() {
            return new Exercise(0, slug, title, statement, archived, policy, cases);
        }
    }

    public record Submission(Language language, String source) {}

    public record Trial(Language language, String source, String input) {}

    @GetMapping("/exercises")
    public List<Catalog.Summary> exercises() {
        return catalog.list(false);
    }

    @GetMapping("/exercises/{slug}")
    public ExerciseView exercise(@PathVariable String slug) {
        return ExerciseView.from(catalog.find(slug, false));
    }

    @GetMapping("/authoring/exercises")
    public List<Catalog.Summary> authoringList() {
        return catalog.list(true);
    }

    @GetMapping("/authoring/exercises/{slug}")
    public Exercise authoringDetails(@PathVariable String slug) {
        return catalog.find(slug, true);
    }

    @PostMapping(value = "/authoring/exercises", consumes = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public Exercise create(@RequestBody Draft draft) {
        if (draft.archived()) {
            throw new IllegalArgumentException("新题目创建后可在编辑页归档");
        }
        return catalog.create(draft.toExercise());
    }

    @PutMapping(value = "/authoring/exercises/{slug}", consumes = "application/json")
    public Exercise replace(@PathVariable String slug, @RequestBody Draft draft) {
        return catalog.replace(slug, draft.toExercise());
    }

    @PostMapping(value = "/exercises/{slug}/trials", consumes = "application/json")
    public LocalEvaluation.Report trial(@PathVariable String slug, @RequestBody Trial trial) {
        Exercise exercise = catalog.find(slug, false);
        if (trial.input() == null || Exercise.utf8Size(trial.input()) > 8192) {
            throw new IllegalArgumentException("自测输入须在 8 KB 内");
        }
        return evaluator.tryInput(exercise.policy(), trial.language(), trial.source(), trial.input());
    }

    @PostMapping(value = "/exercises/{slug}/attempts", consumes = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public AttemptStore.Detail submit(@PathVariable String slug, @RequestBody Submission submission) {
        Exercise exercise = catalog.find(slug, false);
        LocalEvaluation.Report result = evaluator.evaluate(exercise, submission.language(), submission.source());
        return attempts.save(exercise, submission.language(), submission.source(), result);
    }

    @GetMapping("/attempts")
    public List<AttemptStore.Summary> history(@RequestParam(required = false) String exercise) {
        return attempts.recent(exercise);
    }

    @GetMapping("/attempts/{id}")
    public AttemptStore.Detail detail(@PathVariable long id) {
        return attempts.find(id);
    }
}
