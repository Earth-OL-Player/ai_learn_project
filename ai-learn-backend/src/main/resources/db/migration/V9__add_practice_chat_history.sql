ALTER TABLE user_practice_sessions
    ADD COLUMN chat_history_json MEDIUMTEXT NULL COMMENT '当前题跨端展示聊天记录JSON，仅保留最近一轮刷题对话'
        AFTER discussion_follow_up_count;
