CREATE INDEX idx_questions_owner_source_deleted ON questions(owner_user_id, source_type, deleted);
CREATE INDEX idx_questions_deleted_source ON questions(deleted, source_type);
