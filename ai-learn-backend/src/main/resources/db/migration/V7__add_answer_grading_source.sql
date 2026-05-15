ALTER TABLE answer_records
    ADD COLUMN grading_source VARCHAR(32) NOT NULL DEFAULT 'LOCAL_RULE' COMMENT '评分来源' AFTER ai_feedback;
