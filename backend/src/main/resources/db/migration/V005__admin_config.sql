-- V005: AI model config + prompt template tables.
-- NOTE: Seed data is applied by ConfigSeeder (Java @PostConstruct), NOT by reading
-- filesystem files at deploy time. This SQL only defines the schema.

CREATE TABLE IF NOT EXISTS ai_models (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    provider      VARCHAR(50)  NOT NULL DEFAULT 'deepseek',
    base_url      VARCHAR(300) NOT NULL,
    model_name    VARCHAR(100) NOT NULL,
    timeout_ms    INT          NOT NULL DEFAULT 60000,
    temperature   DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    max_tokens    INT          NOT NULL DEFAULT 2048,
    api_key_ref   VARCHAR(200) NOT NULL,
    is_enabled    TINYINT(1)   NOT NULL DEFAULT 1,
    is_default    TINYINT(1)   NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_model_enabled_default (is_enabled, is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型配置(密钥仅存引用)';

CREATE TABLE IF NOT EXISTS prompt_templates (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    type          VARCHAR(50)  NOT NULL,
    version       INT          NOT NULL,
    content       TEXT         NOT NULL,
    variables     JSON         NULL,
    content_hash  VARCHAR(64)  NOT NULL,
    description   VARCHAR(500) NULL,
    is_enabled    TINYINT(1)   NOT NULL DEFAULT 0,
    is_archived   TINYINT(1)   NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_type_version (type, version),
    INDEX idx_prompt_type_enabled (type, is_enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Prompt模板(不可变版本, 记录content_hash)';
