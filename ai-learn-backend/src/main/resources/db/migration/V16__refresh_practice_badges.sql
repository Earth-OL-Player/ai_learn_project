-- sprint2620 刷题勋章规则刷新，只保留本期定义的 11 个勋章。
ALTER TABLE user_practice_sessions
    ADD COLUMN discussion_follow_up_count INT NOT NULL DEFAULT 0 COMMENT '当前题评分后连续有效追问次数'
        AFTER last_answer_text;

-- 清理已不属于本期范围的历史用户勋章，避免个人中心继续展示旧规则。
DELETE ub
FROM user_badges ub
JOIN badges b ON b.id = ub.badge_id
WHERE b.rule_code NOT IN (
    'FIRST_ANSWER',
    'ANSWER_10',
    'ANSWER_100',
    'ANSWER_300',
    'LEARNING_3_DAYS',
    'LEARNING_30_DAYS',
    'LEARNING_100_DAYS',
    'LATE_NIGHT',
    'EARLY_MORNING',
    'WEEKEND_PRACTICE',
    'ASK_TO_END'
);

-- 清理旧勋章定义，本项目无历史包袱，不保留旧高分或专项题型徽章。
DELETE FROM badges
WHERE rule_code NOT IN (
    'FIRST_ANSWER',
    'ANSWER_10',
    'ANSWER_100',
    'ANSWER_300',
    'LEARNING_3_DAYS',
    'LEARNING_30_DAYS',
    'LEARNING_100_DAYS',
    'LATE_NIGHT',
    'EARLY_MORNING',
    'WEEKEND_PRACTICE',
    'ASK_TO_END'
);

-- 刷新本期入门类、坚持类、隐藏/稀有类勋章定义。
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
