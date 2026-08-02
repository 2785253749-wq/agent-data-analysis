-- V008: users + dataset_access + identity migration.
-- Constraint 2: map old user_id semantics (admin → 0, others → username.hashCode) onto
-- the new users.id so history/conversations/audit keep their ownership.

CREATE TABLE IF NOT EXISTS users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    display_name  VARCHAR(100) NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'ANALYST',
    org_id        BIGINT       NOT NULL DEFAULT 0,
    is_enabled    TINYINT(1)   NOT NULL DEFAULT 1,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_org (org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表(BCrypt, 绝不返回密码)';

CREATE TABLE IF NOT EXISTS dataset_access (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    dataset_id  BIGINT NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_dataset (user_id, dataset_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据集授权表';

-- Identity seed is applied by ConfigSeeder (Java @PostConstruct) because it needs a
-- BCrypt hash; this SQL only creates the schema. The seeder inserts admin (ADMIN)
-- and grants a demo dataset to the default analyst account.
