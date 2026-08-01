-- V002: Add result_json to analysis_tasks (bounded snapshot for history/trace detail)
ALTER TABLE analysis_tasks
    ADD COLUMN result_json JSON NULL COMMENT '完整分析结果快照(截断/脱敏)';

-- analysis_steps already exists from V001 (step_type/status/duration etc.)
-- No further DDL needed for the steps table.
