package io.multilang.oj.runner;

import io.multilang.oj.catalog.Exercise;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocalEvaluationTest {
    @Test
    void exactComparisonNormalizesLineEndingsButRetainsSpacing() {
        assertTrue(LocalEvaluation.compare(Exercise.Comparison.EXACT, "a\r\nb\r\n", "a\nb\n"));
        assertFalse(LocalEvaluation.compare(Exercise.Comparison.EXACT, "a \nb\n", "a\nb\n"));
        assertFalse(LocalEvaluation.compare(Exercise.Comparison.EXACT, "a\n", "a"));
    }

    @Test
    void tokenComparisonRetainsTokenBoundariesAndOrder() {
        assertTrue(LocalEvaluation.compare(Exercise.Comparison.TOKENS, "  alpha\r\n beta\t", "alpha beta"));
        assertFalse(LocalEvaluation.compare(Exercise.Comparison.TOKENS, "alphabeta", "alpha beta"));
        assertFalse(LocalEvaluation.compare(Exercise.Comparison.TOKENS, "beta alpha", "alpha beta"));
    }

    @Test
    void validatesRulesAndLimitsEncodedInput() {
        assertThrows(IllegalArgumentException.class,
                () -> new Exercise.Policy(99, 4096, Exercise.Comparison.TOKENS));
        assertThrows(IllegalArgumentException.class, () -> new Exercise.Case(true, "灯".repeat(3000), ""));
        assertThrows(IllegalArgumentException.class, () -> new Exercise(0, "x", "title", "statement", false,
                new Exercise.Policy(1000, 4096, Exercise.Comparison.TOKENS), List.of()));
    }
}
