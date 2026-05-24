-- sprint202627：新增模型配置、兑换码和用户模型权益表。
CREATE TABLE model_configs (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '模型配置ID',
    model_level VARCHAR(16) NOT NULL COMMENT '模型等级：BASIC/PRO/SUPER',
    model_name VARCHAR(128) NOT NULL COMMENT '模型名称',
    base_url VARCHAR(512) NULL COMMENT 'OpenAI兼容模型服务基础地址',
    api_key VARCHAR(512) NULL COMMENT '模型服务API Key',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_model_configs_level (model_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模型等级配置表';

CREATE TABLE redemption_codes (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '兑换码ID',
    code VARCHAR(32) COLLATE utf8mb4_bin NOT NULL COMMENT '兑换码，大小写敏感',
    code_type VARCHAR(32) NOT NULL COMMENT '兑换码类型',
    status VARCHAR(16) NOT NULL DEFAULT 'UNUSED' COMMENT '兑换码状态：UNUSED/USED',
    used_by_user_id BIGINT UNSIGNED NULL COMMENT '兑换用户ID',
    used_at DATETIME NULL COMMENT '兑换时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    PRIMARY KEY (id),
    UNIQUE KEY uk_redemption_codes_code (code),
    KEY idx_redemption_codes_status_type (status, code_type),
    KEY idx_redemption_codes_used_user (used_by_user_id),
    KEY idx_redemption_codes_deleted_created (deleted, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模型权益兑换码表';

CREATE TABLE user_model_entitlements (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户模型权益ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    model_level VARCHAR(16) NOT NULL COMMENT '模型等级：PRO/SUPER',
    entitlement_kind VARCHAR(16) NOT NULL COMMENT '权益类型：MONTHLY/PERMANENT',
    status VARCHAR(16) NOT NULL COMMENT '权益状态',
    remaining_days INT NOT NULL DEFAULT 0 COMMENT '月度权益剩余天数',
    last_consumed_at DATETIME NULL COMMENT '月度权益最近扣减时间',
    started_at DATETIME NULL COMMENT '权益首次生效时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_model_entitlements_user_level_kind (user_id, model_level, entitlement_kind),
    KEY idx_user_model_entitlements_user_status (user_id, status),
    KEY idx_user_model_entitlements_level_status (model_level, entitlement_kind, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户模型权益表';

INSERT INTO model_configs(model_level, model_name, base_url, api_key)
VALUES
    ('BASIC', 'deepseek-v4-flash', NULL, NULL),
    ('PRO', 'deepseek-v4-pro', NULL, NULL),
    ('SUPER', 'gpt-5.5', NULL, NULL);
