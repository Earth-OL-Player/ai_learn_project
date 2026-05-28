-- sprint202630：为现有列表、统计和导出 SQL 补充组合索引。
-- 题库默认分页按未删除、创建时间和主键倒序读取。
CREATE INDEX idx_questions_deleted_created_id
    ON questions (deleted, created_at, id);

-- 题库管理端默认分页按未删除和主键正序读取。
CREATE INDEX idx_questions_deleted_id
    ON questions (deleted, id);

-- AI 刷题候选题默认按重要度和主键倒序截取。
CREATE INDEX idx_questions_deleted_importance_id
    ON questions (deleted, importance_score, id);

-- 题型过滤后的刷题候选题和面经文档按题型、重要度读取。
CREATE INDEX idx_questions_deleted_type_importance_id
    ON questions (deleted, question_type, importance_score, id);

-- 个人刷题记录按用户、答题次数和最近答题时间分页。
CREATE INDEX idx_user_question_stats_user_answer_last_id
    ON user_question_stats (user_id, answer_count, last_answered_at, id);

-- 评论默认排序、子评论计数和一级子评论查询共用父级组合索引。
CREATE INDEX idx_comments_deleted_parent_created_id
    ON comments (deleted, parent_id, created_at, id);

-- 评论热门排序需要同时过滤未删除、父级并按点赞数分页。
CREATE INDEX idx_comments_deleted_parent_like_created_id
    ON comments (deleted, parent_id, like_count, created_at, id);

-- 建议默认排序按未删除和创建时间分页。
CREATE INDEX idx_suggestions_deleted_created_id
    ON suggestions (deleted, created_at, id);

-- 建议热门排序按未删除、点赞数和创建时间分页。
CREATE INDEX idx_suggestions_deleted_like_created_id
    ON suggestions (deleted, like_count, created_at, id);

-- 用户后台默认分页按未删除和主键读取。
CREATE INDEX idx_users_deleted_id
    ON users (deleted, id);

-- 兑换码后台按状态、类型和创建时间分页或导出。
CREATE INDEX idx_redemption_codes_deleted_status_type_created_id
    ON redemption_codes (deleted, status, code_type, created_at, id);
