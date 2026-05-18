CREATE TABLE system_settings (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '系统设置ID',
    setting_key VARCHAR(64) NOT NULL COMMENT '设置键',
    setting_value VARCHAR(255) NOT NULL COMMENT '设置值',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_system_settings_key (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统设置表';

INSERT INTO system_settings(setting_key, setting_value)
VALUES('MAX_USERS', '10000')
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value);
