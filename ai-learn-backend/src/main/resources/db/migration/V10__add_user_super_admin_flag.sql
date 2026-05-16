ALTER TABLE users
    ADD COLUMN super_admin TINYINT NOT NULL DEFAULT 0 COMMENT '是否超级管理员' AFTER rank_code;
