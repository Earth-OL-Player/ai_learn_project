ALTER TABLE user_practice_sessions
    ADD COLUMN last_grading_summary TEXT NULL COMMENT '当前题最近一次评分摘要，用于本题多轮追问上下文'
        AFTER last_answer_text,
    ADD COLUMN discussion_history_json TEXT NULL COMMENT '当前题讨论历史JSON，用于AI短期多轮记忆'
        AFTER last_grading_summary;
