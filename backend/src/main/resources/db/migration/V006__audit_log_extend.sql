-- V006: Extend audit_log for P4 (operator_name + result).
ALTER TABLE audit_log
    ADD COLUMN operator_name VARCHAR(100) NULL COMMENT '操作者',
    ADD COLUMN result VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS/FAILED';
ALTER TABLE audit_log ADD INDEX idx_audit_operator (operator_name);
ALTER TABLE audit_log ADD INDEX idx_audit_action (action);
