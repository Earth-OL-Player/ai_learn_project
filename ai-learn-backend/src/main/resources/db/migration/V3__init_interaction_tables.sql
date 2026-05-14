CREATE TABLE suggestions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '建议ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '提交用户ID',
    title VARCHAR(80) NOT NULL COMMENT '建议标题',
    content VARCHAR(2000) NOT NULL COMMENT '建议内容',
    type VARCHAR(32) NOT NULL COMMENT '建议类型',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    PRIMARY KEY (id),
    KEY idx_suggestions_user_id (user_id),
    KEY idx_suggestions_status_created_at (status, created_at),
    CONSTRAINT fk_suggestions_user_id FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户建议表';

CREATE TABLE comments (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '评论用户ID',
    content VARCHAR(1000) NOT NULL COMMENT '评论内容',
    parent_id BIGINT UNSIGNED NULL COMMENT '父评论ID',
    like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    PRIMARY KEY (id),
    KEY idx_comments_user_id (user_id),
    KEY idx_comments_created_at (created_at),
    CONSTRAINT fk_comments_user_id FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';
