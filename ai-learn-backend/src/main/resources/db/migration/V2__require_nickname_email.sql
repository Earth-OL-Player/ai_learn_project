UPDATE users
SET nickname = username
WHERE nickname IS NULL OR nickname = '';

UPDATE users
SET email = CONCAT(username, '@placeholder.local')
WHERE email IS NULL OR email = '';

UPDATE users target_user
JOIN (
    SELECT nickname, MIN(id) AS keep_id
    FROM users
    GROUP BY nickname
    HAVING COUNT(*) > 1
) duplicate_user ON target_user.nickname = duplicate_user.nickname
SET target_user.nickname = LEFT(CONCAT(target_user.nickname, '_', target_user.id), 64)
WHERE target_user.id <> duplicate_user.keep_id;

ALTER TABLE users
    MODIFY nickname VARCHAR(64) NOT NULL COMMENT '昵称',
    MODIFY email VARCHAR(128) NOT NULL COMMENT '邮箱';

ALTER TABLE users
    ADD UNIQUE KEY uk_users_nickname (nickname);
