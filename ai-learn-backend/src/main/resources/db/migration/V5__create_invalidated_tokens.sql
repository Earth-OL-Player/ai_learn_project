CREATE TABLE invalidated_tokens (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '失效令牌记录ID',
    token_id VARCHAR(64) NOT NULL COMMENT 'JWT唯一标识jti',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '退出登录用户ID',
    expires_at DATETIME NOT NULL COMMENT '令牌原始过期时间',
    invalidated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '服务端失效时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_invalidated_tokens_token_id (token_id),
    KEY idx_invalidated_tokens_user_id (user_id),
    KEY idx_invalidated_tokens_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='JWT服务端失效表';
