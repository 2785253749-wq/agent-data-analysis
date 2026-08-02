-- V007: Add stable failure_category to analysis_steps for dashboard aggregation.
-- Categories: SQL_VALIDATION / QUERY_EXECUTION / MODEL_TIMEOUT / MODEL_RESPONSE / UNEXPECTED
ALTER TABLE analysis_steps
    ADD COLUMN failure_category VARCHAR(50) NULL COMMENT '稳定失败分类';
ALTER TABLE analysis_steps
    ADD INDEX idx_step_failure_category (failure_category);
