CREATE TABLE rag_index_tasks (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'RAG入库任务ID',
    task_id VARCHAR(64) NOT NULL COMMENT '外部任务ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '触发用户ID',
    source_type VARCHAR(32) NOT NULL COMMENT '来源类型',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态',
    message VARCHAR(1000) NULL COMMENT '任务消息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_rag_index_tasks_task_id (task_id),
    KEY idx_rag_index_tasks_user_created (user_id, created_at),
    KEY idx_rag_index_tasks_status (status),
    CONSTRAINT fk_rag_index_tasks_user_id FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG入库任务表';
