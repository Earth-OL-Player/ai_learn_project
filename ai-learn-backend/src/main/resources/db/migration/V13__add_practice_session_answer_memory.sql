ALTER TABLE user_practice_sessions
    ADD COLUMN last_answer_text TEXT NULL COMMENT '当前题最近一次答案原文，用于本题讨论阶段提供上下文'
        AFTER last_score;
