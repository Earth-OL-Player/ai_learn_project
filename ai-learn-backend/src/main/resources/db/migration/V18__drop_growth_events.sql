-- sprint2621 去除个人中心成长明细数据库表，保留 badges 与 user_badges 徽章功能。
SET @drop_sql = IF((SELECT COUNT(1) FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'growth_events' AND CONSTRAINT_NAME = 'fk_growth_events_user_id' AND CONSTRAINT_TYPE = 'FOREIGN KEY') > 0, 'ALTER TABLE `growth_events` DROP FOREIGN KEY `fk_growth_events_user_id`', 'SELECT 1');
PREPARE drop_statement FROM @drop_sql; EXECUTE drop_statement; DEALLOCATE PREPARE drop_statement;

DROP TABLE IF EXISTS growth_events;
