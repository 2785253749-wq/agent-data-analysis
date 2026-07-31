-- ============================================================
-- V001: Initial schema — core tables for agent analysis platform
-- ============================================================

-- Dataset registry
CREATE TABLE IF NOT EXISTS datasets (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(200)  NOT NULL COMMENT '数据集名称',
    description   TEXT          COMMENT '数据集描述',
    table_name    VARCHAR(200)  NOT NULL COMMENT '对应的数据库表/视图名',
    org_id        BIGINT        NOT NULL DEFAULT 0 COMMENT '组织ID（多租户隔离）',
    is_enabled    TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_org_table (org_id, table_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据集注册表';

-- Dataset field definitions (used for SQL field whitelist validation)
CREATE TABLE IF NOT EXISTS dataset_fields (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    dataset_id    BIGINT        NOT NULL,
    field_name    VARCHAR(200)  NOT NULL COMMENT '字段名',
    field_alias   VARCHAR(200)  COMMENT '字段中文别名',
    data_type     VARCHAR(50)   NOT NULL COMMENT '数据类型（varchar/int/decimal/datetime等）',
    is_dimension  TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '是否为维度字段',
    is_metric     TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '是否为指标字段',
    is_filterable TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '是否可用作过滤条件',
    description   TEXT          COMMENT '字段说明',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dataset_field (dataset_id, field_name),
    FOREIGN KEY (dataset_id) REFERENCES datasets(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据集字段定义表';

-- Metric definitions (standardized KPI formulas)
CREATE TABLE IF NOT EXISTS metrics_definitions (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    dataset_id    BIGINT        NOT NULL,
    metric_name   VARCHAR(200)  NOT NULL COMMENT '指标名称',
    formula       TEXT          NOT NULL COMMENT '计算公式/SQL片段',
    description   TEXT          COMMENT '指标说明',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (dataset_id) REFERENCES datasets(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标定义表';

-- Analysis tasks (each user question = one task)
CREATE TABLE IF NOT EXISTS analysis_tasks (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT        NOT NULL COMMENT '用户ID',
    dataset_id    BIGINT        NOT NULL COMMENT '数据集ID',
    question      TEXT          NOT NULL COMMENT '用户原始问题',
    status        VARCHAR(30)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/COMPLETED/FAILED/CANCELLED',
    intent_json   JSON          COMMENT '意图识别结果',
    sql_text      TEXT          COMMENT '生成的SQL',
    result_summary TEXT         COMMENT '分析结果摘要',
    error_message TEXT          COMMENT '错误信息',
    token_usage   JSON          COMMENT 'Token用量统计',
    started_at    DATETIME      COMMENT '开始执行时间',
    completed_at  DATETIME      COMMENT '完成时间',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_status (user_id, status),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分析任务表';

-- Analysis steps (trace each step within a task)
CREATE TABLE IF NOT EXISTS analysis_steps (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id       BIGINT        NOT NULL,
    step_type     VARCHAR(50)   NOT NULL COMMENT 'INTENT/SQL_GEN/SQL_VALIDATE/QUERY/INTERPRET/CHART',
    step_order    INT           NOT NULL COMMENT '执行顺序',
    status        VARCHAR(30)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/COMPLETED/FAILED/SKIPPED',
    input_json    JSON          COMMENT '步骤输入',
    output_json   JSON          COMMENT '步骤输出',
    error_message TEXT          COMMENT '错误信息',
    duration_ms   BIGINT        COMMENT '耗时（毫秒）',
    model_name    VARCHAR(100)  COMMENT '使用的模型名称',
    prompt_version VARCHAR(50)  COMMENT 'Prompt版本号',
    started_at    DATETIME      COMMENT '开始时间',
    completed_at  DATETIME      COMMENT '完成时间',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES analysis_tasks(id) ON DELETE CASCADE,
    INDEX idx_task_steps (task_id, step_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分析步骤表（Agent执行追踪）';

-- Audit log
CREATE TABLE IF NOT EXISTS audit_log (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT        COMMENT '用户ID',
    action        VARCHAR(100)  NOT NULL COMMENT '操作类型',
    resource_type VARCHAR(100)  COMMENT '资源类型',
    resource_id   BIGINT        COMMENT '资源ID',
    detail        JSON          COMMENT '详细信息',
    ip_address    VARCHAR(50)   COMMENT '请求IP',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_user (user_id),
    INDEX idx_audit_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';
