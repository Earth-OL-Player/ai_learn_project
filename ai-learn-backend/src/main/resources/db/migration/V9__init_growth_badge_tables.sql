CREATE TABLE badges (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '徽章ID',
    name VARCHAR(64) NOT NULL COMMENT '徽章名称',
    description VARCHAR(255) NOT NULL COMMENT '徽章说明',
    icon VARCHAR(64) NOT NULL COMMENT '徽章图标',
    rule_code VARCHAR(64) NOT NULL COMMENT '规则编码',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_badges_rule_code (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='徽章定义表';

CREATE TABLE user_badges (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户徽章ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    badge_id BIGINT UNSIGNED NOT NULL COMMENT '徽章ID',
    acquired_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '获得时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_badges_user_badge (user_id, badge_id),
    KEY idx_user_badges_user_id (user_id),
    CONSTRAINT fk_user_badges_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_badges_badge_id FOREIGN KEY (badge_id) REFERENCES badges (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户徽章表';

CREATE TABLE growth_events (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '成长流水ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    event_type VARCHAR(32) NOT NULL COMMENT '事件类型',
    title VARCHAR(120) NOT NULL COMMENT '流水标题',
    description VARCHAR(500) NULL COMMENT '流水说明',
    experience_delta INT NOT NULL DEFAULT 0 COMMENT '经验变化',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_growth_events_user_created (user_id, created_at),
    CONSTRAINT fk_growth_events_user_id FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成长事件流水表';

INSERT INTO badges(name, description, icon, rule_code) VALUES
('初次启程', '完成第 1 道题，迈出 AI 学习第一步', '🚀', 'FIRST_ANSWER'),
('坚持三天', '连续学习 3 天', '🔥', 'STREAK_3_DAYS'),
('七日修炼', '连续学习 7 天', '🌟', 'STREAK_7_DAYS'),
('高分选手', '单题得分达到 90 分以上', '🏅', 'HIGH_SCORE'),
('查漏补缺', '低分题复刷后提升 20 分以上', '🧩', 'SCORE_IMPROVED'),
('RAG 初体验', '完成 RAG 相关题目 10 道', '📚', 'RAG_10'),
('Agent 探索者', '完成 Agent 相关题目 10 道', '🤖', 'AGENT_10');
