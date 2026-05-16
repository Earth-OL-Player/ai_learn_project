CREATE TABLE user_question_stats (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户题目汇总ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    question_code VARCHAR(64) NOT NULL COMMENT '题目编码',
    answer_count INT NOT NULL DEFAULT 0 COMMENT '答题次数',
    best_score INT NOT NULL DEFAULT 0 COMMENT '历史最高分',
    last_score INT NOT NULL DEFAULT 0 COMMENT '最近一次得分',
    first_answered_at DATETIME NULL COMMENT '首次答题时间',
    last_answered_at DATETIME NULL COMMENT '最近答题时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_question_stats_user_code (user_id, question_code),
    KEY idx_user_question_stats_code (question_code),
    CONSTRAINT fk_user_question_stats_user_id FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户题目答题汇总表';

CREATE TABLE user_practice_sessions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户当前刷题状态ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    question_code VARCHAR(64) NULL COMMENT '当前题目编码',
    phase VARCHAR(32) NOT NULL DEFAULT 'QUESTIONING' COMMENT '刷题阶段',
    last_score INT NULL COMMENT '当前题最近一次得分',
    started_at DATETIME NULL COMMENT '当前题开始时间',
    answered_at DATETIME NULL COMMENT '当前题最近评分时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_practice_sessions_user_id (user_id),
    KEY idx_user_practice_sessions_question_code (question_code),
    CONSTRAINT fk_user_practice_sessions_user_id FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户当前刷题状态表';
