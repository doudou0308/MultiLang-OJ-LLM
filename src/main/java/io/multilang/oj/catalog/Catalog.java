package io.multilang.oj.catalog;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class Catalog {
    private final JdbcTemplate db;

    public Catalog(JdbcTemplate db) {
        this.db = db;
    }

    public record Summary(long id, String slug, String title, boolean archived) {}

    public List<Summary> list(boolean includeArchived) {
        return db.query("""
                SELECT exercise_id, slug, title, archived FROM exercises
                WHERE archived = FALSE OR ? ORDER BY exercise_id
                """, (rs, n) -> new Summary(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getBoolean(4)),
                includeArchived);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Exercise find(String slug, boolean includeArchived) {
        return load(slug, includeArchived);
    }

    @Transactional
    public Exercise create(Exercise draft) {
        db.update("INSERT INTO exercises (slug, title, statement_text) VALUES (?, ?, ?)",
                draft.slug(), draft.title(), draft.statement());
        Long id = db.queryForObject("SELECT exercise_id FROM exercises WHERE slug = ?", Long.class, draft.slug());
        writeSpecification(id, draft);
        return load(draft.slug(), true);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Exercise replace(String slug, Exercise draft) {
        if (!slug.equals(draft.slug())) {
            throw new IllegalArgumentException("题目标识创建后不可修改");
        }
        Long id = db.query("SELECT exercise_id FROM exercises WHERE slug = ? FOR UPDATE",
                (rs, n) -> rs.getLong(1), slug).stream().findFirst().orElseThrow(Catalog::missing);
        db.update("UPDATE exercises SET title = ?, statement_text = ?, archived = ? WHERE exercise_id = ?",
                draft.title(), draft.statement(), draft.archived(), id);
        writeSpecification(id, draft);
        return load(slug, true);
    }

    private void writeSpecification(long id, Exercise draft) {
        Exercise.Policy rules = draft.policy();
        db.update("""
                INSERT INTO execution_policies (exercise_id, time_ms, output_bytes, comparison) VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE time_ms = ?, output_bytes = ?, comparison = ?
                """, id, rules.timeMs(), rules.outputBytes(), rules.comparison().name(),
                rules.timeMs(), rules.outputBytes(), rules.comparison().name());
        db.update("DELETE FROM exercise_cases WHERE exercise_id = ?", id);
        for (int i = 0; i < draft.cases().size(); i++) {
            Exercise.Case row = draft.cases().get(i);
            db.update("""
                    INSERT INTO exercise_cases
                    (exercise_id, position_no, standard_input, expected_output, is_example) VALUES (?, ?, ?, ?, ?)
                    """, id, i + 1, row.input(), row.expected(), row.example());
        }
    }

    private Exercise load(String slug, boolean includeArchived) {
        return db.query("""
                SELECT e.exercise_id, e.slug, e.title, e.statement_text, e.archived,
                       p.time_ms, p.output_bytes, p.comparison
                FROM exercises e JOIN execution_policies p ON e.exercise_id = p.exercise_id
                WHERE e.slug = ? AND (e.archived = FALSE OR ?)
                """, (rs, n) -> new Exercise(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                rs.getBoolean(5), policy(rs), cases(rs.getLong(1))), slug, includeArchived)
                .stream().findFirst().orElseThrow(Catalog::missing);
    }

    private static Exercise.Policy policy(ResultSet rs) throws SQLException {
        return new Exercise.Policy(rs.getInt(6), rs.getInt(7), Exercise.Comparison.valueOf(rs.getString(8)));
    }

    private List<Exercise.Case> cases(long id) {
        return db.query("""
                SELECT is_example, standard_input, expected_output FROM exercise_cases
                WHERE exercise_id = ? ORDER BY position_no
                """, (rs, n) -> new Exercise.Case(rs.getBoolean(1), rs.getString(2), rs.getString(3)), id);
    }

    private static NoSuchElementException missing() {
        return new NoSuchElementException("题目不存在");
    }
}
