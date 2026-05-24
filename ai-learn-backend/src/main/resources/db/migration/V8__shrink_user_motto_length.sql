UPDATE users
SET motto = LEFT(motto, 60)
WHERE motto IS NOT NULL AND CHAR_LENGTH(motto) > 60;

ALTER TABLE users
    MODIFY COLUMN motto VARCHAR(60) NULL COMMENT '用户座右铭，NULL表示未设置';
