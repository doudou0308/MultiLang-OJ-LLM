CREATE TABLE IF NOT EXISTS exercises (
    exercise_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    slug VARCHAR(72) NOT NULL UNIQUE,
    title VARCHAR(120) NOT NULL,
    statement_text TEXT NOT NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS execution_policies (
    exercise_id BIGINT PRIMARY KEY,
    time_ms INTEGER NOT NULL,
    output_bytes INTEGER NOT NULL,
    comparison VARCHAR(20) NOT NULL,
    FOREIGN KEY (exercise_id) REFERENCES exercises(exercise_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS exercise_cases (
    exercise_id BIGINT NOT NULL,
    position_no INTEGER NOT NULL,
    standard_input TEXT NOT NULL,
    expected_output TEXT NOT NULL,
    is_example BOOLEAN NOT NULL,
    PRIMARY KEY (exercise_id, position_no),
    FOREIGN KEY (exercise_id) REFERENCES exercises(exercise_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS attempts (
    attempt_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exercise_id BIGINT NOT NULL,
    exercise_title VARCHAR(120) NOT NULL,
    language_id VARCHAR(12) NOT NULL,
    source_text MEDIUMTEXT NOT NULL,
    verdict VARCHAR(24) NOT NULL,
    case_count INTEGER NOT NULL,
    compiler_message TEXT NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    FOREIGN KEY (exercise_id) REFERENCES exercises(exercise_id),
    INDEX ix_attempt_exercise (exercise_id, attempt_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS attempt_checks (
    attempt_id BIGINT NOT NULL,
    position_no INTEGER NOT NULL,
    is_example BOOLEAN NOT NULL,
    verdict VARCHAR(24) NOT NULL,
    elapsed_ms BIGINT NOT NULL,
    observed_output TEXT NOT NULL,
    PRIMARY KEY (attempt_id, position_no),
    FOREIGN KEY (attempt_id) REFERENCES attempts(attempt_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
