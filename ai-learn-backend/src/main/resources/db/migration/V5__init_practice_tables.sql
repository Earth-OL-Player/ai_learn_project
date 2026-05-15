CREATE TABLE agent_sessions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '刷题会话ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    question_id BIGINT UNSIGNED NOT NULL COMMENT '题目ID',
    status VARCHAR(32) NOT NULL DEFAULT 'STARTED' COMMENT '会话状态',
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    submitted_at DATETIME NULL COMMENT '提交时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_agent_sessions_user_id (user_id),
    KEY idx_agent_sessions_question_id (question_id),
    KEY idx_agent_sessions_status (status),
    CONSTRAINT fk_agent_sessions_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_agent_sessions_question_id FOREIGN KEY (question_id) REFERENCES questions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='刷题会话表';

CREATE TABLE answer_records (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '答题记录ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    question_id BIGINT UNSIGNED NOT NULL COMMENT '题目ID',
    session_id BIGINT UNSIGNED NOT NULL COMMENT '刷题会话ID',
    user_answer TEXT NOT NULL COMMENT '用户答案',
    score INT NOT NULL COMMENT '得分',
    is_correct TINYINT NOT NULL DEFAULT 0 COMMENT '是否基本正确',
    ai_feedback JSON NULL COMMENT '结构化反馈，本期保存规则评分结果',
    improvement_advice VARCHAR(1000) NULL COMMENT '改进建议',
    duration_seconds INT NULL COMMENT '答题耗时',
    first_attempt TINYINT NOT NULL DEFAULT 1 COMMENT '是否首次作答',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_answer_records_user_id_created_at (user_id, created_at),
    KEY idx_answer_records_question_id (question_id),
    KEY idx_answer_records_session_id (session_id),
    CONSTRAINT fk_answer_records_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_answer_records_question_id FOREIGN KEY (question_id) REFERENCES questions (id),
    CONSTRAINT fk_answer_records_session_id FOREIGN KEY (session_id) REFERENCES agent_sessions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答题记录表';
