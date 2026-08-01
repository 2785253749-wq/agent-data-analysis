-- V003: Multi-turn conversations.
-- Design decision: no conversation_messages table — analysis_tasks ARE the conversation turns.
--   user message   = analysis_tasks.question
--   assistant reply = analysis_tasks.result_json
--   latest message  = question of the most recent linked task

CREATE TABLE IF NOT EXISTS conversations (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    title           VARCHAR(200) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE / ARCHIVED
    dataset_id      BIGINT       NULL,
    context_summary JSON         NULL,
    task_count      INT          NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_conv_user_status (user_id, status),
    INDEX idx_conv_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多轮分析会话';

-- Link tasks to a conversation (nullable → old single-shot tasks remain untouched).
ALTER TABLE analysis_tasks
    ADD COLUMN conversation_id BIGINT NULL AFTER user_id;
ALTER TABLE analysis_tasks
    ADD INDEX idx_task_conv (conversation_id);
