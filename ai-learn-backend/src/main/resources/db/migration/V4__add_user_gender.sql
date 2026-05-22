ALTER TABLE users
    ADD COLUMN gender VARCHAR(16) NULL COMMENT '用户性别编码，MALE表示男，FEMALE表示女，NULL表示未设置' AFTER avatar;
