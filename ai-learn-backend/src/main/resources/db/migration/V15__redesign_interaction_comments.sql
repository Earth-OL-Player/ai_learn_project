-- sprint2619 建议评论区改为轻量评论流，补充点赞明细。
UPDATE suggestions
SET type = 'FEATURE'
WHERE type NOT IN ('FEATURE', 'EXPERIENCE', 'BUG', 'CONTENT');

ALTER TABLE suggestions
    DROP INDEX idx_suggestions_status_created_at;

ALTER TABLE suggestions
    DROP COLUMN title,
    DROP COLUMN status,
    ADD COLUMN like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数' AFTER type;

ALTER TABLE suggestions
    ADD KEY idx_suggestions_like_created (like_count, created_at),
    ADD KEY idx_suggestions_created_at (created_at);

ALTER TABLE comments
    ADD KEY idx_comments_parent_like_created (parent_id, like_count, created_at),
    ADD KEY idx_comments_parent_created (parent_id, created_at);

CREATE TABLE comment_likes (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评论点赞ID',
    comment_id BIGINT UNSIGNED NOT NULL COMMENT '评论ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '点赞用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_comment_likes_comment_user (comment_id, user_id),
    KEY idx_comment_likes_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论点赞明细表';

CREATE TABLE suggestion_likes (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '建议点赞ID',
    suggestion_id BIGINT UNSIGNED NOT NULL COMMENT '建议ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '点赞用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_suggestion_likes_suggestion_user (suggestion_id, user_id),
    KEY idx_suggestion_likes_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='建议点赞明细表';
