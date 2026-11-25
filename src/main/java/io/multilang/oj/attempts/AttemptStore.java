package io.multilang.oj.attempts;

import io.multilang.oj.catalog.Exercise;
import io.multilang.oj.runner.Language;
import io.multilang.oj.runner.LocalEvaluation.Check;
import io.multilang.oj.runner.LocalEvaluation.Report;
import io.multilang.oj.runner.LocalEvaluation.Verdict;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class AttemptStore {
    private static final String SELECT = """
            SELECT a.attempt_id, e.slug, a.exercise_title, a.language_id, a.verdict, a.case_count, a.created_at
            FROM attempts a JOIN exercises e ON e.exercise_id = a.exercise_id
            """;
    private static final RowMapper<Summary> SUMMARY = (rs, n) -> new Summary(rs.getLong(1), rs.getString(2),
            rs.getString(3), Language.valueOf(rs.getString(4)), Verdict.valueOf(rs.getString(5)), rs.getInt(6),
            rs.getTimestamp(7).toInstant());
    private final JdbcTemplate db;

    public AttemptStore(JdbcTemplate db) {
        this.db = db;
    }

    public record Summary(long id, String exercise, String title, Language language, Verdict verdict,
                          int caseCount, Instant submittedAt) {}

    public record Detail(Summary summary, String source, Report result) {}

    @Transactional
    public Detail save(Exercise exercise, Language language, String source, Report report) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        db.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO attempts (exercise_id, exercise_title, language_id, source_text, verdict,
                                          case_count, compiler_message)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, exercise.id());
            ps.setString(2, exercise.title());
            ps.setString(3, language.name());
            ps.setString(4, source);
            ps.setString(5, report.verdict().name());
            ps.setInt(6, report.caseCount());
            ps.setString(7, report.compilerMessage());
            return ps;
        }, key);
        long id = key.getKey().longValue();
        for (Check check : report.checks()) {
            db.update("""
                    INSERT INTO attempt_checks (attempt_id, position_no, is_example, verdict, elapsed_ms,
                                                observed_output)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, id, check.position(), check.example(), check.verdict().name(), check.elapsedMs(),
                    check.output());
        }
        return find(id);
    }

    public List<Summary> recent(String slug) {
        if (slug == null) {
            return db.query(SELECT + " ORDER BY a.attempt_id DESC LIMIT 50", SUMMARY);
        }
        return db.query(SELECT + " WHERE e.slug = ? ORDER BY a.attempt_id DESC LIMIT 50", SUMMARY, slug);
    }

    public Detail find(long id) {
        Summary summary = db.query(SELECT + " WHERE a.attempt_id = ?", SUMMARY, id).stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException("提交记录不存在"));
        List<Check> checks = db.query("""
                SELECT position_no, is_example, verdict, elapsed_ms, observed_output FROM attempt_checks
                WHERE attempt_id = ? ORDER BY position_no
                """, (rs, n) -> new Check(rs.getInt(1), rs.getBoolean(2), Verdict.valueOf(rs.getString(3)),
                rs.getLong(4), rs.getString(5)), id);
        return db.queryForObject("SELECT source_text, compiler_message FROM attempts WHERE attempt_id = ?",
                (rs, n) -> new Detail(summary, rs.getString(1),
                        new Report(summary.verdict(), summary.caseCount(), rs.getString(2), checks)), id);
    }
}
