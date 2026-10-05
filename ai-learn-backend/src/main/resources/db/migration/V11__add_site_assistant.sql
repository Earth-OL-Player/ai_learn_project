-- 站内助手会话与幂等执行状态；关联由业务代码维护，不新增外键。
CREATE TABLE assistant_session (
    id VARCHAR(32) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    INDEX idx_assistant_session_user (user_id, updated_at),
    INDEX idx_assistant_session_expiry (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE assistant_run (
    id VARCHAR(32) PRIMARY KEY,
    session_id VARCHAR(32) NOT NULL,
    user_id BIGINT NOT NULL,
    client_request_id VARCHAR(64) NOT NULL,
    client_instance_id VARCHAR(64) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    content TEXT NOT NULL,
    page_context_json TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    model_fingerprint VARCHAR(64) NOT NULL,
    model_name VARCHAR(200) NOT NULL,
    execution_epoch INT NOT NULL DEFAULT 1,
    tool_count INT NOT NULL DEFAULT 0,
    lease_expires_at DATETIME NOT NULL,
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_assistant_run_request (user_id, client_request_id),
    INDEX idx_assistant_run_session (session_id, created_at),
    INDEX idx_assistant_run_user (user_id, status),
    INDEX idx_assistant_run_expiry (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE assistant_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(32) NOT NULL,
    run_id VARCHAR(32) NOT NULL,
    message_json MEDIUMTEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_assistant_message_session (session_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE assistant_operation (
    id VARCHAR(32) PRIMARY KEY,
    run_id VARCHAR(32) NOT NULL,
    user_id BIGINT NOT NULL,
    tool_call_id VARCHAR(128) NOT NULL,
    type VARCHAR(64) NOT NULL,
    payload_json TEXT NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    payload_version INT NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL,
    authorized BOOLEAN NOT NULL DEFAULT FALSE,
    next_step INT NOT NULL DEFAULT 0,
    action_plan_json TEXT NOT NULL,
    result_json MEDIUMTEXT NULL,
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_assistant_operation_call (run_id, tool_call_id),
    INDEX idx_assistant_operation_run (run_id, created_at),
    INDEX idx_assistant_operation_expiry (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
