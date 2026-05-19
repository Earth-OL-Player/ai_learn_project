-- 基线版本：直接创建当前业务需要的最终表结构，已移除历史下线表与过渡变更。
CREATE TABLE users (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(32) NOT NULL COMMENT '用户名',
    nickname VARCHAR(64) NOT NULL COMMENT '昵称',
    avatar VARCHAR(255) NULL COMMENT '头像地址',
    email VARCHAR(128) NOT NULL COMMENT '邮箱',
    password_hash VARCHAR(100) NOT NULL COMMENT '密码哈希',
    experience INT NOT NULL DEFAULT 0 COMMENT '经验值',
    level_code VARCHAR(16) NOT NULL DEFAULT 'LV1' COMMENT '等级编码',
    rank_code VARCHAR(32) NOT NULL DEFAULT 'QI_REFINING' COMMENT '修仙境界编码',
    super_admin TINYINT NOT NULL DEFAULT 0 COMMENT '是否超级管理员',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_nickname (nickname),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

CREATE TABLE questions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '题目ID',
    code VARCHAR(64) NOT NULL COMMENT '题目编码',
    question TEXT NOT NULL COMMENT '题目',
    question_type VARCHAR(80) NOT NULL COMMENT '题目分类，直接使用题目表字符串',
    standard_answer TEXT NOT NULL COMMENT '参考答案',
    importance_score DECIMAL(5,1) NOT NULL DEFAULT 60.0 COMMENT '重要性评分，百分制',
    occurrence_count INT NOT NULL DEFAULT 0 COMMENT '真实面试出现次数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    PRIMARY KEY (id),
    UNIQUE KEY uk_questions_code (code),
    KEY idx_questions_question_type (question_type),
    KEY idx_questions_type_importance (question_type, importance_score),
    KEY idx_questions_code_deleted (code, deleted),
    KEY idx_questions_created_at (created_at),
    KEY idx_questions_deleted_type (deleted, question_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目表';

CREATE TABLE user_question_stats (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户题目汇总ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    question_code VARCHAR(64) NOT NULL COMMENT '题目编码',
    answer_count INT NOT NULL DEFAULT 0 COMMENT '答题次数',
    best_score INT NOT NULL DEFAULT 0 COMMENT '历史最高分',
    last_score INT NOT NULL DEFAULT 0 COMMENT '最近一次得分',
    first_answered_at DATETIME NULL COMMENT '首次答题时间',
    last_answered_at DATETIME NULL COMMENT '最近答题时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_question_stats_user_code (user_id, question_code),
    KEY idx_user_question_stats_code (question_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户题目答题汇总表';

CREATE TABLE user_practice_sessions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户当前刷题状态ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    question_code VARCHAR(64) NULL COMMENT '当前题目编码',
    phase VARCHAR(32) NOT NULL DEFAULT 'QUESTIONING' COMMENT '刷题阶段',
    last_score INT NULL COMMENT '当前题最近一次得分',
    last_answer_text TEXT NULL COMMENT '当前题最近一次答案原文，用于本题讨论阶段提供上下文',
    last_grading_summary TEXT NULL COMMENT '当前题最近一次评分摘要，用于本题多轮追问上下文',
    discussion_history_json TEXT NULL COMMENT '当前题讨论历史JSON，用于AI短期多轮记忆',
    discussion_follow_up_count INT NOT NULL DEFAULT 0 COMMENT '当前题评分后连续有效追问次数',
    started_at DATETIME NULL COMMENT '当前题开始时间',
    answered_at DATETIME NULL COMMENT '当前题最近评分时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_practice_sessions_user_id (user_id),
    KEY idx_user_practice_sessions_question_code (question_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户当前刷题状态表';

CREATE TABLE suggestions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '建议ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '提交用户ID',
    content VARCHAR(2000) NOT NULL COMMENT '建议内容',
    type VARCHAR(32) NOT NULL COMMENT '建议类型',
    like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    PRIMARY KEY (id),
    KEY idx_suggestions_user_id (user_id),
    KEY idx_suggestions_like_created (like_count, created_at),
    KEY idx_suggestions_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户建议表';

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
    KEY idx_comments_parent_like_created (parent_id, like_count, created_at),
    KEY idx_comments_parent_created (parent_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评论表';

CREATE TABLE comment_likes (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评论点赞ID',
    comment_id BIGINT UNSIGNED NOT NULL COMMENT '评论ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '点赞用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_comment_likes_comment_user (comment_id, user_id),
    KEY idx_comment_likes_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评论点赞明细表';

CREATE TABLE suggestion_likes (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '建议点赞ID',
    suggestion_id BIGINT UNSIGNED NOT NULL COMMENT '建议ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '点赞用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_suggestion_likes_suggestion_user (suggestion_id, user_id),
    KEY idx_suggestion_likes_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='建议点赞明细表';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='徽章定义表';

CREATE TABLE user_badges (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户徽章ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    badge_id BIGINT UNSIGNED NOT NULL COMMENT '徽章ID',
    acquired_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '获得时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_badges_user_badge (user_id, badge_id),
    KEY idx_user_badges_user_id (user_id),
    KEY idx_user_badges_badge_id (badge_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户徽章表';

CREATE TABLE system_settings (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '系统设置ID',
    setting_key VARCHAR(64) NOT NULL COMMENT '设置键',
    setting_value VARCHAR(255) NOT NULL COMMENT '设置值',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_system_settings_key (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统设置表';
