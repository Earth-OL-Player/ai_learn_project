-- sprint2617 成长等级和修仙境界段位默认值刷新。
ALTER TABLE users
    MODIFY level_code VARCHAR(16) NOT NULL DEFAULT 'LV1' COMMENT '等级编码',
    MODIFY rank_code VARCHAR(32) NOT NULL DEFAULT 'QI_REFINING' COMMENT '修仙境界编码';

UPDATE users
SET rank_code = 'QI_REFINING'
WHERE rank_code IN ('BRONZE', 'SILVER', 'GOLD', 'PLATINUM', 'DIAMOND', 'KING');
