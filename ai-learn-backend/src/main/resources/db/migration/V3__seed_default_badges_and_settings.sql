-- 基线默认数据：初始化当前生效的徽章规则和系统配置。
INSERT INTO badges(name, description, icon, rule_code) VALUES
('初次启程', '完成第 1 道题', '🚀', 'FIRST_ANSWER'),
('十题小成', '累计完成 10 道题', '🔟', 'ANSWER_10'),
('百题修炼', '累计完成 100 道题', '💯', 'ANSWER_100'),
('大成圆满', '累计完成 300 道题', '🏆', 'ANSWER_300'),
('三日不辍', '总共学习 3 天', '🌱', 'LEARNING_3_DAYS'),
('月度坚持者', '总共学习 30 天', '📅', 'LEARNING_30_DAYS'),
('百日成神', '总共学习 100 天', '🔥', 'LEARNING_100_DAYS'),
('深夜修行者', '晚上 22:00 后完成刷题', '🌙', 'LATE_NIGHT'),
('清晨启动者', '早上 6:00-8:00 完成刷题', '🌅', 'EARLY_MORNING'),
('周末不摆烂', '周六或周日完成刷题', '🎒', 'WEEKEND_PRACTICE'),
('问到底', '单题评分后连续追问 3 次', '❓', 'ASK_TO_END')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    icon = VALUES(icon),
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO system_settings(setting_key, setting_value)
VALUES('MAX_USERS', '10000')
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value);
